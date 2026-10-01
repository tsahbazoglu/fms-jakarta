package tr.org.tspb.web.filter;

import static tr.org.tspb.constants.ProjectConstants.*;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebFilter(filterName = "CasLoginFilter", urlPatterns = {"/*"})
public class CasLoginFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CasLoginFilter.class.getName());

    public static final String CAS_TOKEN_COOKIE = "token";
    public static final String CAS_TOKEN_COOKIE_ALT = "cas_token";
    public static final String CAS_TOKEN_COOKIE_AUTH = "auth_token";

    public static final String DEFAULT_LOGIN_URL = "http://localhost:8088/api/login";
    public static final String DEFAULT_GATE_VERIFY_URL = "http://localhost:8088/api/gate/verify";
    public static final String DEFAULT_CAS_PORTAL_URL = "http://localhost:8088/?project=tspb";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = path.substring(contextPath.length());

        if (isPublicPath(relativePath)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. Look at browser cookie for authentication token (fallback to query param/header)
        String token = extractToken(req);
        GateVerifyResult ctx = null;

        HttpSession session = req.getSession(false);
        // If session already holds this exact token and is authenticated and not expired
        if (session != null && token != null && token.equals(session.getAttribute("token"))
                && session.getAttribute("jaasLoginName") != null) {
            Long expiry = (Long) session.getAttribute("tokenExpiry");
            if (expiry == null || System.currentTimeMillis() < expiry) {
                ctx = (GateVerifyResult) session.getAttribute("gateVerifyResult");
            }
        }

        // 2. Check token with POST method to localhost:8088/api/login if not cached
        if (ctx == null && token != null && !token.isBlank()) {
            String project = resolveProject(req);
            ctx = verifyToken(token, project);
            if (ctx == null || !ctx.isValid()) {
                LOGGER.log(Level.WARNING, "Invalid or expired CAS token for URI: {0}", relativePath);
                ctx = null;
            }
        }

        // 3. Process authenticated token
        if (ctx != null && ctx.isValid()) {
            final String companyId = ctx.getCompanyId();
            final String userId = ctx.getUserId();
            final String principalName = (companyId != null && !companyId.isBlank()) ? companyId : userId;
            final GateVerifyResult verifiedCtx = ctx;

            HttpSession currentSession = req.getSession(true);
            currentSession.setAttribute("jaasLoginName", principalName);
            currentSession.setAttribute("companyId", companyId != null ? companyId : principalName);
            currentSession.setAttribute("token", token);
            currentSession.setAttribute("tokenUserId", userId);
            currentSession.setAttribute("gateVerifyResult", ctx);
            currentSession.setAttribute(LOGGED_USER, principalName.toUpperCase());

            List<String> combinedRoles = new ArrayList<>();
            if (verifiedCtx.getRoles() != null) {
                combinedRoles.addAll(verifiedCtx.getRoles());
            }
            if (verifiedCtx.getPermissions() != null) {
                for (String perm : verifiedCtx.getPermissions()) {
                    if (!combinedRoles.contains(perm)) {
                        combinedRoles.add(perm);
                    }
                }
            }
            currentSession.setAttribute(LOGGED_USER_ROLES, combinedRoles);

            long ttlSeconds = ctx.getExpiresInSeconds() > 0 ? ctx.getExpiresInSeconds() : 3600;
            currentSession.setAttribute("tokenExpiry", System.currentTimeMillis() + (ttlSeconds * 1000L));

            // Ensure cookie is available on client side
            ensureTokenCookie(req, res, token);

            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(req) {
                @Override
                public String getRemoteUser() {
                    return principalName;
                }

                @Override
                public Principal getUserPrincipal() {
                    return () -> principalName;
                }

                @Override
                public boolean isUserInRole(String role) {
                    if (role == null) return false;
                    if ("**".equals(role) || "*".equals(role)) {
                        return true;
                    }
                    if ("fmsuser".equalsIgnoreCase(role) || principalName.equalsIgnoreCase(role)) {
                        return true;
                    }
                    if (verifiedCtx.getRoles() != null && verifiedCtx.getRoles().contains(role)) {
                        return true;
                    }
                    if (verifiedCtx.getPermissions() != null) {
                        if (verifiedCtx.getPermissions().contains("*") || verifiedCtx.getPermissions().contains(role)) {
                            return true;
                        }
                    }
                    return super.isUserInRole(role);
                }
            };

            chain.doFilter(wrappedRequest, response);
            return;
        }

        // 4. In case of fail, clear any invalid session attributes and redirect to http://localhost:8088/?project=tspb
        if (session != null) {
            session.removeAttribute("token");
            session.removeAttribute("jaasLoginName");
            session.removeAttribute("companyId");
            session.removeAttribute("gateVerifyResult");
            session.removeAttribute(LOGGED_USER);
            session.removeAttribute(LOGGED_USER_ROLES);
        }

        String redirectUrl = resolveRedirectUrl(req);
        LOGGER.log(Level.INFO, "Authentication failed or token missing for {0}, redirecting to {1}",
                new Object[]{relativePath, redirectUrl});

        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            res.setContentType("text/xml");
            res.setCharacterEncoding("UTF-8");
            res.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\""
                    + redirectUrl + "\"/></partial-response>");
            return;
        }

        res.sendRedirect(redirectUrl);
    }

    /**
     * Looks at browser cookie for authentication token.
     * Also checks query parameter or Authorization header as fallbacks.
     */
    public static String extractToken(HttpServletRequest req) {
        // 1. Look at browser cookie first
        String token = extractTokenFromCookies(req);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        // 2. Query parameter
        token = req.getParameter(CAS_TOKEN_COOKIE);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        token = req.getParameter(CAS_TOKEN_COOKIE_ALT);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        token = req.getParameter("ticket");
        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        // 3. Authorization header: Bearer <token>
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearer = authHeader.substring(7).trim();
            if (!bearer.isBlank()) {
                return bearer;
            }
        }

        return null;
    }

    public static String extractTokenFromCookies(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CAS_TOKEN_COOKIE.equalsIgnoreCase(c.getName())
                        || CAS_TOKEN_COOKIE_ALT.equalsIgnoreCase(c.getName())
                        || CAS_TOKEN_COOKIE_AUTH.equalsIgnoreCase(c.getName())) {
                    String val = c.getValue();
                    if (val != null && !val.isBlank()) {
                        return val.trim();
                    }
                }
            }
        }
        return null;
    }

    public static final String DEFAULT_PROJECT = "tspb";

    /**
     * Resolves the target project identifier (defaulting to "tspb").
     */
    public static String resolveProject(HttpServletRequest req) {
        String project = null;
        if (req != null) {
            project = req.getParameter("project");
            if (project == null || project.isBlank()) {
                project = (String) req.getAttribute("project");
            }
        }
        if (project == null || project.isBlank()) {
            project = System.getProperty("cas.project");
        }
        if (project == null || project.isBlank()) {
            project = System.getenv("CAS_PROJECT");
        }
        if (project == null || project.isBlank()) {
            project = DEFAULT_PROJECT;
        }
        return project.trim();
    }

    /**
     * Checks token on localhost:8088/api/login via POST method using default project.
     */
    public static GateVerifyResult verifyToken(String token) {
        return verifyToken(token, resolveProject(null));
    }

    /**
     * Checks token on localhost:8088/api/login via POST method sending token and project
     * as POST request body parameters.
     */
    public static GateVerifyResult verifyToken(String token, String project) {
        if (token == null || token.isBlank()) {
            return null;
        }

        if (project == null || project.isBlank()) {
            project = resolveProject(null);
        }

        String targetUrl = getLoginVerifyUrl();
        GateVerifyResult result = postTokenVerification(targetUrl, token, project);
        if (result != null && result.isValid()) {
            return result;
        }

        // Fallback to /api/gate/verify if /api/login did not return a valid result
        if (!targetUrl.equals(DEFAULT_GATE_VERIFY_URL)) {
            GateVerifyResult fallback = postTokenVerification(DEFAULT_GATE_VERIFY_URL, token, project);
            if (fallback != null && fallback.isValid()) {
                return fallback;
            }
        }

        return result;
    }

    public static String createVerifyRequestBody(String token, String project) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"token\":\"").append(escapeJson(token)).append("\"");
        if (project != null && !project.isBlank()) {
            sb.append(",\"project\":\"").append(escapeJson(project.trim())).append("\"");
            sb.append(",\"projectId\":\"").append(escapeJson(project.trim())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static GateVerifyResult postTokenVerification(String verifyUrl, String token, String project) {
        try {
            String jsonBody = createVerifyRequestBody(token, project);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(verifyUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    JsonObject json = reader.readObject();
                    GateVerifyResult result = new GateVerifyResult();
                    result.setValid(json.containsKey("valid") && json.getBoolean("valid", false));
                    if (json.containsKey("userId") && !json.isNull("userId")) {
                        result.setUserId(json.getString("userId"));
                    }
                    if (json.containsKey("companyId") && !json.isNull("companyId")) {
                        result.setCompanyId(json.getString("companyId"));
                    }
                    if (json.containsKey("expiresInSeconds") && !json.isNull("expiresInSeconds")) {
                        result.setExpiresInSeconds(json.getJsonNumber("expiresInSeconds").longValue());
                    }
                    if (json.containsKey("roles") && !json.isNull("roles")) {
                        JsonArray rolesArr = json.getJsonArray("roles");
                        List<String> roles = new ArrayList<>();
                        for (JsonValue v : rolesArr) {
                            roles.add(v.toString().replace("\"", ""));
                        }
                        result.setRoles(roles);
                    }
                    if (json.containsKey("permissions") && !json.isNull("permissions")) {
                        JsonArray permsArr = json.getJsonArray("permissions");
                        List<String> permissions = new ArrayList<>();
                        for (JsonValue v : permsArr) {
                            permissions.add(v.toString().replace("\"", ""));
                        }
                        result.setPermissions(permissions);
                    }
                    return result;
                }
            } else {
                LOGGER.log(Level.WARNING, "CAS token check returned HTTP status {0} for URL {1}",
                        new Object[]{statusCode, verifyUrl});
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error calling CAS verification endpoint ({0}): {1}",
                    new Object[]{verifyUrl, e.getMessage()});
        }
        return null;
    }

    public static String getLoginVerifyUrl() {
        String url = System.getProperty("cas.login.url");
        if (url == null || url.isBlank()) {
            url = System.getenv("CAS_LOGIN_URL");
        }
        if (url == null || url.isBlank()) {
            url = System.getProperty("cas.gate.verify.url");
        }
        if (url == null || url.isBlank()) {
            url = System.getenv("CAS_GATE_VERIFY_URL");
        }
        if (url == null || url.isBlank()) {
            url = DEFAULT_LOGIN_URL;
        }
        return url.trim();
    }

    public static String resolveRedirectUrl(HttpServletRequest req) {
        String portalUrl = System.getProperty("cas.portal.url");
        if (portalUrl == null || portalUrl.isBlank()) {
            portalUrl = System.getenv("CAS_PORTAL_URL");
        }
        if (portalUrl != null && !portalUrl.isBlank()) {
            return portalUrl.trim();
        }
        return DEFAULT_CAS_PORTAL_URL;
    }

    private void ensureTokenCookie(HttpServletRequest req, HttpServletResponse res, String token) {
        Cookie[] cookies = req.getCookies();
        boolean hasTokenCookie = false;
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CAS_TOKEN_COOKIE.equalsIgnoreCase(c.getName()) && token.equals(c.getValue())) {
                    hasTokenCookie = true;
                    break;
                }
            }
        }
        if (!hasTokenCookie) {
            Cookie c = new Cookie(CAS_TOKEN_COOKIE, token);
            c.setPath("/");
            c.setHttpOnly(false);
            res.addCookie(c);

            Cookie cAlt = new Cookie(CAS_TOKEN_COOKIE_ALT, token);
            cAlt.setPath("/");
            cAlt.setHttpOnly(false);
            res.addCookie(cAlt);
        }
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.startsWith("/api/")
                || lower.startsWith("/public/")
                || lower.startsWith("/muhurdar")
                || lower.startsWith("/webstart/")
                || lower.startsWith("/j_security_check")
                || lower.contains("jakarta.faces.resource")
                || lower.contains("javax.faces.resource")
                || lower.endsWith(".css")
                || lower.endsWith(".js")
                || lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".svg")
                || lower.endsWith(".ico")
                || lower.endsWith(".woff")
                || lower.endsWith(".woff2")
                || lower.endsWith(".ttf");
    }

    @Override
    public void destroy() {}

    /**
     * DTO representing result of CAS Gate verification.
     */
    public static class GateVerifyResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private boolean valid;
        private String userId;
        private String companyId;
        private List<String> roles = Collections.emptyList();
        private List<String> permissions = Collections.emptyList();
        private long expiresInSeconds;
        private String error;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getCompanyId() { return companyId; }
        public void setCompanyId(String companyId) { this.companyId = companyId; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }

        public long getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public String userId() { return userId; }
        public String companyId() { return companyId; }
        public List<String> perms() { return permissions; }
        public List<String> roles() { return roles; }
    }
}
