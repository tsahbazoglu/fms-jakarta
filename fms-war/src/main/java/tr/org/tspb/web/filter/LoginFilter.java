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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import tr.org.tspb.common.services.LdapService;
import tr.org.tspb.constants.exceptions.LdapException;

@WebFilter(filterName = "LoginFilter", urlPatterns = {"/*"})
public class LoginFilter implements Filter {

    @Inject
    private LdapService ldapService;

    private static final boolean debug = false;
    private FilterConfig filterConfig = null;

    public static String getStackTrace(Throwable t) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.getBuffer().toString();
        } catch (IOException ex) {
            Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (debug) {
            log("LoginFilter:doFilter()");
        }
        HttpServletRequest req = (HttpServletRequest) request;

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("token".equalsIgnoreCase(c.getName())
                        || "cas_token".equalsIgnoreCase(c.getName())
                        || "auth_token".equalsIgnoreCase(c.getName())) {
                    String token = c.getValue();
                    if (token != null && !token.isBlank()) {
                        com.dadhawk.auth.Ctx ctx = com.dadhawk.auth.Jwt.verify(token);
                        if (ctx != null && req.getSession(false) != null) {
                            req.getSession(false).setAttribute("jaasLoginName", ctx.companyId());
                            req.getSession(false).setAttribute("companyId", ctx.companyId());
                        }
                    }
                    break;
                }
            }
        }

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
            Logger.getLogger(LoginFilter.class.getName()).log(
                    Level.SEVERE, "CAPTURE-DEBUG: Filter caught exception on URI: " + req.getRequestURI(), t
            );
        }

        if (problem != null) {
            if (problem instanceof ServletException) throw (ServletException) problem;
            if (problem instanceof IOException) throw (IOException) problem;
            sendProcessingError(problem, response);
        }
    }

    public FilterConfig getFilterConfig() { return filterConfig; }
    public void setFilterConfig(FilterConfig filterConfig) { this.filterConfig = filterConfig; }
    public void destroy() {}

    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null && debug) {
            log("LoginFilter:Initializing filter");
        }
    }

    private void sendProcessingError(Throwable t, ServletResponse response) {
        Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, "Processing error in LoginFilter", t);
        try {
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter pw = response.getWriter()) {
                pw.print("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Hata</title></head><body>");
                pw.print("<div style='text-align:center;margin-top:50px;font-family:sans-serif;'>");
                pw.print("<h2>İşlem sırasında bir hata oluştu.</h2>");
                pw.print("<p>Lütfen daha sonra tekrar deneyiniz.</p>");
                pw.print("</div></body></html>");
            }
        } catch (IOException ex) {
            Logger.getLogger(LoginFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void log(String msg) {
        if (filterConfig != null) filterConfig.getServletContext().log(msg);
    }
}