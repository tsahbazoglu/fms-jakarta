package tr.org.tspb.web.filter;

import static tr.org.tspb.constants.ProjectConstants.*;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import tr.org.tspb.common.services.LdapService;
import tr.org.tspb.constants.exceptions.LdapException;

/**
 *
 * @author Telman Şahbazoğlu
 */
@WebFilter(filterName = "LoginFilter", urlPatterns = {"/*"})
public class LoginFilter implements Filter {

    @Inject
    private LdapService ldapService;

    private static final boolean debug = false;

    public static String getStackTrace(Throwable t) {
        String stackTrace = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            stackTrace = sw.getBuffer().toString();
        } catch (IOException ex) {
            Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stackTrace;
    }

    private FilterConfig filterConfig = null;

    public LoginFilter() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        if (debug) {
            log("LoginFilter:doFilter()");
        }
        HttpServletRequest req = ((HttpServletRequest) request);

        if (req.getRemoteUser() != null
                && req.getSession(false) != null
                && req.getSession(false).getAttribute(LOGGED_USER_ROLES) == null
                && req.getSession(false).getAttribute(LOGGED_USER) == null) {

            List<String> loggedUserRoles = new ArrayList<>();

            try {
                List<String> allLdapRoles = ldapService.findAllRoles();

                for (String role : allLdapRoles) {
                    if (req.isUserInRole(role)) {
                        loggedUserRoles.add(role);
                    }
                }

            } catch (LdapException ex) {
                Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
            }

            req.getSession(false).setAttribute(LOGGED_USER_ROLES, loggedUserRoles);
            req.getSession(false).setAttribute(LOGGED_USER, req.getRemoteUser().toUpperCase());
        }

        Throwable problem = null;
        try {
            chain.doFilter(request, response);
        } catch (Throwable t) {
            problem = t;
            // ---> ADD THIS DEBUG LOG <---
            Logger.getLogger(LoginFilter.class.getName()).log(
                    Level.SEVERE,
                    "CAPTURE-DEBUG: Filter caught exception on URI: " + req.getRequestURI(),
                    t
            );
        }

        if (problem != null) {
            if (problem instanceof ServletException) {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException) {
                throw (IOException) problem;
            }
            sendProcessingError(problem, response);
        }
    }

    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public void destroy() {
    }

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null && debug) {
            log("LoginFilter:Initializing filter");
        }
    }

    @Override
    public String toString() {
        if (filterConfig == null) {
            return ("LoginFilter()");
        }
        StringBuffer sb = new StringBuffer("LoginFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }

    private void sendProcessingError(Throwable t, ServletResponse response) {
        String stackTrace = getStackTrace(t);

        if (stackTrace != null && !stackTrace.isEmpty()) {
            try {
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n");

                pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
                pw.print(stackTrace);
                pw.print("</pre></body>\n</html>");
                pw.close();
                ps.close();
                response.getOutputStream().close();
            } catch (IOException ex) {
                Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                ps.close();
                response.getOutputStream().close();
            } catch (IOException ex) {
                Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void log(String msg) {
        filterConfig.getServletContext().log(msg);
    }
}