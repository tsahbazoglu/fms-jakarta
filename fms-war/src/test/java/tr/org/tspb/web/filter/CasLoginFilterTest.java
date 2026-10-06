package tr.org.tspb.web.filter;

import com.dadhawk.auth.client.CasLoginFilter;
import com.dadhawk.auth.client.CasLoginException;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;
import tr.org.tspb.common.services.LoginController;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.*;

import static org.junit.Assert.*;

public class CasLoginFilterTest {

    @Test
    public void testResolveRedirectUrlDefault() {
        System.clearProperty("cas.portal.url");
        HttpServletRequest request = createMockRequest("/", null, null, null);
        String redirectUrl = CasLoginFilter.resolveRedirectUrl(request);
        assertEquals("http://localhost:8088/?project=tspb", redirectUrl);
    }

    @Test
    public void testGetLoginVerifyUrlDefault() {
        System.clearProperty("cas.login.url");
        System.clearProperty("cas.gate.verify.url");
        String verifyUrl = CasLoginFilter.getLoginVerifyUrl();
        assertEquals("http://localhost:8088/api/login", verifyUrl);
    }

    @Test
    public void testExtractTokenFromCookie() {
        Cookie[] cookies = new Cookie[]{
                new Cookie("unrelated", "value"),
                new Cookie("token", "cookie-jwt-token-123")
        };
        HttpServletRequest request = createMockRequest("/dashboard", cookies, null, null);
        String token = CasLoginFilter.extractToken(request);
        assertEquals("cookie-jwt-token-123", token);
    }

    @Test
    public void testExtractTokenFromAltCookie() {
        Cookie[] cookies = new Cookie[]{
                new Cookie("cas_token", "cas-cookie-token-456")
        };
        HttpServletRequest request = createMockRequest("/dashboard", cookies, null, null);
        String token = CasLoginFilter.extractToken(request);
        assertEquals("cas-cookie-token-456", token);
    }

    @Test
    public void testExtractTokenFromParameter() {
        Map<String, String> params = new HashMap<>();
        params.put("token", "param-token-789");
        HttpServletRequest request = createMockRequest("/dashboard", null, params, null);
        String token = CasLoginFilter.extractToken(request);
        assertEquals("param-token-789", token);
    }

    @Test
    public void testExtractTokenFromAuthorizationHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer header-token-abc");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String token = CasLoginFilter.extractToken(request);
        assertEquals("header-token-abc", token);
    }

    @Test
    public void testResolveProjectDefault() {
        System.clearProperty("cas.project");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, null);
        String project = CasLoginFilter.resolveProject(request);
        assertEquals("tspb", project);
    }

    @Test
    public void testResolveProjectFromParam() {
        Map<String, String> params = new HashMap<>();
        params.put("project", "azclerk");
        HttpServletRequest request = createMockRequest("/dashboard", null, params, null);
        String project = CasLoginFilter.resolveProject(request);
        assertEquals("azclerk", project);
    }

    @Test
    public void testCreateVerifyRequestBodyIncludesProject() {
        String body = CasLoginFilter.createVerifyRequestBody("jwt.token.val", "tspb");
        assertTrue(body.contains("\"token\":\"jwt.token.val\""));
        assertTrue(body.contains("\"project\":\"tspb\""));
        assertTrue(body.contains("\"projectId\":\"tspb\""));
    }

    @Test
    public void testCreateSubjectWithPrincipalAndRoles() {
        List<String> roles = Arrays.asList("fmsuser", "ADMIN", "REPORT_VIEW");
        Subject subject = CasLoginFilter.createSubject("COMPANY_123", "USER_ABC", roles);

        assertNotNull(subject);
        Set<Principal> principals = subject.getPrincipals();
        assertEquals(5, principals.size()); // COMPANY_123, USER_ABC, 3 roles

        Set<CasLoginFilter.JaasPrincipal> userPrincipals = subject.getPrincipals(CasLoginFilter.JaasPrincipal.class);
        assertEquals(2, userPrincipals.size());

        Set<CasLoginFilter.JaasRolePrincipal> rolePrincipals = subject.getPrincipals(CasLoginFilter.JaasRolePrincipal.class);
        assertEquals(3, rolePrincipals.size());

        boolean hasCompany = userPrincipals.stream().anyMatch(p -> "COMPANY_123".equals(p.getName()));
        boolean hasUser = userPrincipals.stream().anyMatch(p -> "USER_ABC".equals(p.getName()));
        boolean hasAdminRole = rolePrincipals.stream().anyMatch(p -> "ADMIN".equals(p.getName()));

        assertTrue(hasCompany);
        assertTrue(hasUser);
        assertTrue(hasAdminRole);
    }

    @Test
    public void testCreateSubjectWhenPrincipalEqualsUserId() {
        Subject subject = CasLoginFilter.createSubject("SAME_USER", "SAME_USER", Collections.singletonList("USER_ROLE"));
        assertNotNull(subject);
        Set<CasLoginFilter.JaasPrincipal> userPrincipals = subject.getPrincipals(CasLoginFilter.JaasPrincipal.class);
        assertEquals(1, userPrincipals.size());
        assertEquals("SAME_USER", userPrincipals.iterator().next().getName());
    }

    @Test
    public void testJaasPrincipalEqualsAndHashCode() {
        CasLoginFilter.JaasPrincipal p1 = new CasLoginFilter.JaasPrincipal("user1");
        CasLoginFilter.JaasPrincipal p2 = new CasLoginFilter.JaasPrincipal("user1");
        CasLoginFilter.JaasPrincipal p3 = new CasLoginFilter.JaasPrincipal("user2");

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertEquals("user1", p1.toString());
    }

    @Test
    public void testJaasRolePrincipalEqualsAndHashCode() {
        CasLoginFilter.JaasRolePrincipal r1 = new CasLoginFilter.JaasRolePrincipal("roleA");
        CasLoginFilter.JaasRolePrincipal r2 = new CasLoginFilter.JaasRolePrincipal("roleA");
        CasLoginFilter.JaasRolePrincipal r3 = new CasLoginFilter.JaasRolePrincipal("roleB");

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertEquals("roleA", r1.toString());
    }

    @Test
    public void testGetSubjectFromRequestAndSession() {
        final Map<String, Object> reqAttrs = new HashMap<>();
        final Map<String, Object> sessionAttrs = new HashMap<>();

        Subject testSubject = CasLoginFilter.createSubject("PRINCIPAL1", null, Collections.emptyList());

        HttpSession mockSession = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length > 0) {
                        return sessionAttrs.get(args[0]);
                    }
                    return null;
                }
        );

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length > 0) {
                        return reqAttrs.get(args[0]);
                    }
                    if ("getSession".equals(method.getName())) {
                        return mockSession;
                    }
                    return null;
                }
        );

        // Initially null
        assertNull(CasLoginFilter.getSubject(request));

        // When in session
        sessionAttrs.put("javax.security.auth.subject", testSubject);
        assertEquals(testSubject, CasLoginFilter.getSubject(request));

        // When in request attribute (precedence)
        Subject reqSubject = CasLoginFilter.createSubject("PRINCIPAL2", null, Collections.emptyList());
        reqAttrs.put("javax.security.auth.subject", reqSubject);
        assertEquals(reqSubject, CasLoginFilter.getSubject(request));
    }

    @Test
    public void testGetAuthLogoutUrlDefault() {
        System.clearProperty("cas.auth.logout.url");
        System.clearProperty("cas.logout.url");
        String logoutUrl = CasLoginFilter.getAuthLogoutUrl();
        assertEquals("http://localhost:8088/api/auth/logout", logoutUrl);
    }

    @Test
    public void testGetAuthLogoutUrlCustom() {
        System.setProperty("cas.auth.logout.url", "http://myauth:9090/api/auth/logout");
        try {
            assertEquals("http://myauth:9090/api/auth/logout", CasLoginFilter.getAuthLogoutUrl());
        } finally {
            System.clearProperty("cas.auth.logout.url");
        }
    }

    @Test
    public void testResolveBaseUrlLocalhost() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "localhost:8080");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String baseUrl = CasLoginFilter.resolveBaseUrl(request);
        assertEquals("http://localhost:8088", baseUrl);
    }

    @Test
    public void testResolveBaseUrlProductionDomain() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String baseUrl = CasLoginFilter.resolveBaseUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088", baseUrl);
    }

    @Test
    public void testResolveBaseUrlWithXForwardedHost() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Forwarded-Host", "portal.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        headers.put("Host", "internal-glassfish:8080");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String baseUrl = CasLoginFilter.resolveBaseUrl(request);
        assertEquals("https://portal.tspb.org.tr:8088", baseUrl);
    }

    @Test
    public void testResolveBaseUrlWithCustomIncomingPort() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr:8443");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String baseUrl = CasLoginFilter.resolveBaseUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088", baseUrl);
    }

    @Test
    public void testResolveBaseUrlWithCasPortOverride() {
        System.setProperty("cas.port", "8089");
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Host", "fms.tspb.org.tr");
            headers.put("X-Forwarded-Proto", "https");
            HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
            String baseUrl = CasLoginFilter.resolveBaseUrl(request);
            assertEquals("https://fms.tspb.org.tr:8089", baseUrl);
        } finally {
            System.clearProperty("cas.port");
        }
    }

    @Test
    public void testResolveRedirectUrlProduction() {
        System.clearProperty("cas.portal.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String redirectUrl = CasLoginFilter.resolveRedirectUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/?project=tspb", redirectUrl);
    }

    @Test
    public void testResolveRedirectUrlProductionWithProjectParam() {
        System.clearProperty("cas.portal.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        Map<String, String> params = new HashMap<>();
        params.put("project", "azclerk");
        HttpServletRequest request = createMockRequest("/dashboard", null, params, headers);
        String redirectUrl = CasLoginFilter.resolveRedirectUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/?project=azclerk", redirectUrl);
    }

    @Test
    public void testGetLoginVerifyUrlProduction() {
        System.clearProperty("cas.login.url");
        System.clearProperty("cas.gate.verify.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String verifyUrl = CasLoginFilter.getLoginVerifyUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/api/login", verifyUrl);
    }

    @Test
    public void testGetGateVerifyUrlProduction() {
        System.clearProperty("cas.gate.verify.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String verifyUrl = CasLoginFilter.getGateVerifyUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/api/gate/verify", verifyUrl);
    }

    @Test
    public void testGetAuthLogoutUrlProduction() {
        System.clearProperty("cas.auth.logout.url");
        System.clearProperty("cas.logout.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String logoutUrl = CasLoginFilter.getAuthLogoutUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/api/auth/logout", logoutUrl);
    }

    @Test
    public void testResolveBaseUrlFromSystemProperty() {
        System.setProperty("cas.base.url", "https://auth.tspb.org.tr");
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Host", "fms.tspb.org.tr");
            HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
            String baseUrl = CasLoginFilter.resolveBaseUrl(request);
            assertEquals("https://auth.tspb.org.tr", baseUrl);
        } finally {
            System.clearProperty("cas.base.url");
        }
    }

    @Test
    public void testLoginControllerResolveAuthLogoutUrlProduction() {
        System.clearProperty("cas.auth.logout.url");
        System.clearProperty("cas.logout.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "fms.tspb.org.tr:8080");
        headers.put("X-Forwarded-Proto", "https");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String logoutUrl = LoginController.resolveAuthLogoutUrl(request);
        assertEquals("https://fms.tspb.org.tr:8088/api/auth/logout", logoutUrl);
    }

    @Test
    public void testLoginControllerResolveAuthLogoutUrlLocalhost() {
        System.clearProperty("cas.auth.logout.url");
        System.clearProperty("cas.logout.url");
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "localhost:8080");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);
        String logoutUrl = LoginController.resolveAuthLogoutUrl(request);
        assertEquals("http://localhost:8088/api/auth/logout", logoutUrl);
    }

    private HttpServletRequest createMockRequest(String uri, Cookie[] cookies, Map<String, String> params, Map<String, String> headers) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getRequestURI".equals(name)) {
                            return uri != null ? uri : "/";
                        }
                        if ("getContextPath".equals(name)) {
                            return "";
                        }
                        if ("getCookies".equals(name)) {
                            return cookies;
                        }
                        if ("getParameter".equals(name) && args != null && args.length > 0) {
                            return params != null ? params.get(args[0]) : null;
                        }
                        if ("getHeader".equals(name) && args != null && args.length > 0) {
                            return headers != null ? headers.get(args[0]) : null;
                        }
                        if ("getMethod".equals(name)) {
                            return "GET";
                        }
                        if ("getScheme".equals(name)) {
                            if (headers != null && headers.containsKey("X-Forwarded-Proto")) {
                                return headers.get("X-Forwarded-Proto");
                            }
                            return "http";
                        }
                        if ("isSecure".equals(name)) {
                            return headers != null && "https".equalsIgnoreCase(headers.get("X-Forwarded-Proto"));
                        }
                        if ("getServerName".equals(name)) {
                            if (headers != null && headers.containsKey("Host")) {
                                String h = headers.get("Host");
                                int c = h.indexOf(':');
                                return c != -1 ? h.substring(0, c) : h;
                            }
                            return "localhost";
                        }
                        if ("getServerPort".equals(name)) {
                            return 80;
                        }
                        return null;
                    }
                }
        );
    }

    @Test
    public void testIsCasAvailableWhenServerResponds200() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = "OK".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        try {
            assertTrue(CasLoginFilter.isCasAvailable("http://localhost:" + port + "/", 2));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testIsCasAvailableWhenServerResponds503() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] response = "Service Unavailable".getBytes();
            exchange.sendResponseHeaders(503, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();
        try {
            assertFalse(CasLoginFilter.isCasAvailable("http://localhost:" + port + "/", 2));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testIsCasAvailableWhenConnectionRefused() {
        assertFalse(CasLoginFilter.isCasAvailable("http://localhost:64321/", 1));
    }

    @Test
    public void testIsCasAvailableBypassProperty() {
        System.setProperty("cas.check.availability", "false");
        try {
            assertTrue(CasLoginFilter.isCasAvailable("http://localhost:64321/", 1));
        } finally {
            System.clearProperty("cas.check.availability");
        }
    }

    @Test
    public void testBuildCasUnavailableHtmlContainsOnlyRetryButtonAndNoLoginButton() {
        String html = CasLoginFilter.buildCasUnavailableHtml();

        assertTrue("Should contain retry button", html.contains("btnRetry"));
        assertTrue("Should contain retry action", html.contains("window.location.reload()"));
        assertFalse("Should NOT contain login button", html.contains("idLoginButton"));
        assertFalse("Should NOT contain home link", html.contains("Ana Sayfa"));
    }

    @Test
    public void testUnavailableTextsForSupportedLanguages() {
        CasLoginFilter.CasUnavailableTexts tr = CasLoginFilter.getUnavailableTexts("tr");
        assertEquals("Giriş Servisine Erişilemiyor", tr.title);
        assertEquals("Tekrar Dene", tr.retryBtn);

        CasLoginFilter.CasUnavailableTexts en = CasLoginFilter.getUnavailableTexts("en");
        assertEquals("Login Service Not Accessible", en.title);
        assertEquals("Retry", en.retryBtn);

        CasLoginFilter.CasUnavailableTexts ru = CasLoginFilter.getUnavailableTexts("ru");
        assertEquals("Служба входа недоступна", ru.title);
        assertEquals("Повторить", ru.retryBtn);

        CasLoginFilter.CasUnavailableTexts az = CasLoginFilter.getUnavailableTexts("az");
        assertEquals("Giriş Xidmətinə Giriş Mümkün Deyil", az.title);
        assertEquals("Yenidən cəhd et", az.retryBtn);
    }

    @Test
    public void testCasUnavailableConfigProperties() {
        assertEquals("tr", CasLoginFilter.getCasLanguage());
        assertEquals("/public/logo_tspb.png", CasLoginFilter.getCasLogo());
        assertTrue(CasLoginFilter.getCasCopyright().contains("TSPB") || CasLoginFilter.getCasCopyright().contains("Türkiye Sermaye Piyasaları Birliği"));
    }

    @Test
    public void testErrorConfigEndpointReturnsJson() throws Exception {
        HttpServletRequest request = createMockRequest("/api/cas/error-config", null, null, null);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("getWriter".equals(method.getName())) {
                        return pw;
                    }
                    return null;
                }
        );

        final boolean[] chainCalled = new boolean[]{false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        CasLoginFilter filter = new CasLoginFilter();
        filter.doFilter(request, response, chain);

        assertFalse("Chain should NOT be called for error-config endpoint", chainCalled[0]);
        pw.flush();
        String json = sw.toString();
        assertTrue("JSON should contain language", json.contains("\"language\":\"tr\""));
        assertTrue("JSON should contain logo", json.contains("\"logo\":"));
        assertTrue("JSON should contain copyright", json.contains("\"copyright\":"));
    }

    @Test
    public void testSendCasUnavailableResponseThrowsCasLoginException() {
        HttpServletRequest request = createMockRequest("/dashboard", null, null, null);
        try {
            CasLoginFilter.sendCasUnavailableResponse(request, null, "http://localhost:8088/?project=tspb");
            fail("Expected CasLoginException to be thrown");
        } catch (CasLoginException e) {
            assertEquals(CasLoginFilter.CAS_UNAVAILABLE_MESSAGE, e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void testSendCasUnavailableResponseAjax() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("getWriter".equals(method.getName())) {
                        return pw;
                    }
                    if ("isCommitted".equals(method.getName())) {
                        return false;
                    }
                    return null;
                }
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("Faces-Request", "partial/ajax");
        HttpServletRequest request = createMockRequest("/dashboard", null, null, headers);

        CasLoginFilter.sendCasUnavailableResponse(request, response, "http://localhost:8088/?project=tspb");

        String output = sw.toString();
        assertTrue(output.contains("<partial-response>"));
        assertTrue(output.contains(CasLoginFilter.CAS_UNAVAILABLE_MESSAGE));
    }

    @Test
    public void testDoFilterWhenCasUnavailableThrowsCasLoginException() throws Exception {
        System.setProperty("cas.availability.url", "http://localhost:64321");
        System.setProperty("cas.check.timeout.seconds", "1");
        try {
            final List<String> redirects = new ArrayList<>();

            HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        if ("sendRedirect".equals(method.getName()) && args != null && args.length > 0) {
                            redirects.add((String) args[0]);
                            return null;
                        }
                        if ("isCommitted".equals(method.getName())) {
                            return false;
                        }
                        return null;
                    }
            );

            final Map<String, Object> reqAttrs = new HashMap<>();
            HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getRequestURI".equals(name)) {
                            return "/dashboard";
                        }
                        if ("getContextPath".equals(name)) {
                            return "";
                        }
                        if ("getMethod".equals(name)) {
                            return "GET";
                        }
                        if ("getAttribute".equals(name) && args != null && args.length > 0) {
                            return reqAttrs.get(args[0]);
                        }
                        if ("setAttribute".equals(name) && args != null && args.length > 1) {
                            reqAttrs.put((String) args[0], args[1]);
                            return null;
                        }
                        if ("getSession".equals(name)) {
                            return null;
                        }
                        return null;
                    }
            );

            final boolean[] chainCalled = new boolean[]{false};
            FilterChain chain = (req, res) -> chainCalled[0] = true;

            CasLoginFilter filter = new CasLoginFilter();
            try {
                filter.doFilter(request, response, chain);
                fail("Expected CasLoginException to be thrown by doFilter");
            } catch (CasLoginException e) {
                assertEquals(CasLoginFilter.CAS_UNAVAILABLE_MESSAGE, e.getMessage());
            }

            assertFalse("Chain should not be called", chainCalled[0]);
            assertTrue("Send redirect should not be called", redirects.isEmpty());
        } finally {
            System.clearProperty("cas.availability.url");
            System.clearProperty("cas.check.timeout.seconds");
        }
    }

    @Test
    public void testDoFilterDirectlyServesNoAccessHtml() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        HttpServletRequest request = createMockRequest("/no-access.html", null, null, null);
        List<String> redirects = new ArrayList<>();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("getWriter".equals(method.getName())) {
                        return pw;
                    }
                    if ("sendRedirect".equals(method.getName()) && args != null && args.length > 0) {
                        redirects.add((String) args[0]);
                    }
                    return null;
                }
        );

        final boolean[] chainCalled = new boolean[]{false};
        FilterChain chain = (req, res) -> chainCalled[0] = true;

        CasLoginFilter filter = new CasLoginFilter();
        filter.doFilter(request, response, chain);

        assertFalse("Chain should not be called since filter serves no-access page directly", chainCalled[0]);
        assertTrue("No redirect should occur for no-access page", redirects.isEmpty());
        String content = sw.toString();
        assertTrue("Served content should contain no-access page markup", content.contains("id_heading") || content.contains("Giriş"));
    }

    @Test
    public void testReadCasConfigFromSettingFile() {
        CasLoginFilter.resetCasConfigCache();
        File configFile = CasLoginFilter.findCasConfigFile();
        assertNotNull("setting.json or cas.json should be discovered in cas-config folder or root", configFile);
        assertTrue(configFile.exists());

        File configDir = CasLoginFilter.findCasConfigDir();
        assertNotNull("cas-config directory should be found", configDir);
        assertTrue(configDir.exists());

        File noAccessFile = CasLoginFilter.findNoAccessFile();
        assertNotNull("no-access.html file should be found", noAccessFile);
        assertTrue(noAccessFile.exists());

        String noAccessHtml = CasLoginFilter.getNoAccessHtml();
        assertNotNull(noAccessHtml);
        assertTrue(noAccessHtml.contains("id_heading") || noAccessHtml.contains("Giriş"));

        assertEquals("5", CasLoginFilter.getCasConfigProperty("timeout"));
        assertEquals("8088", CasLoginFilter.getCasConfigProperty("port"));
        assertEquals("tspb", CasLoginFilter.getCasConfigProperty("project"));
        assertEquals("true", CasLoginFilter.getCasConfigProperty("checkAvailability"));

        // Clean any system property overrides
        System.clearProperty("cas.check.timeout.seconds");
        System.clearProperty("cas.port");
        System.clearProperty("cas.project");

        assertEquals(5, CasLoginFilter.resolveCasCheckTimeout());
        assertEquals("8088", CasLoginFilter.resolveCasPort());
        assertEquals("tspb", CasLoginFilter.resolveProject(null));
    }

    @Test
    public void testCasConfigCustomFileAndSystemPropertyPrecedence() throws Exception {
        File tmpFile = File.createTempFile("custom-cas-", ".json");
        tmpFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(tmpFile)) {
            fw.write("{\"timeout\": 9, \"port\": \"8099\", \"project\": \"testproj\"}");
        }

        System.setProperty("cas.config.file", tmpFile.getAbsolutePath());
        CasLoginFilter.resetCasConfigCache();
        try {
            System.clearProperty("cas.check.timeout.seconds");
            System.clearProperty("cas.port");
            System.clearProperty("cas.project");

            assertEquals(9, CasLoginFilter.resolveCasCheckTimeout());
            assertEquals("8099", CasLoginFilter.resolveCasPort());
            assertEquals("testproj", CasLoginFilter.resolveProject(null));

            // System property precedence test
            System.setProperty("cas.check.timeout.seconds", "15");
            assertEquals(15, CasLoginFilter.resolveCasCheckTimeout());
        } finally {
            System.clearProperty("cas.config.file");
            System.clearProperty("cas.check.timeout.seconds");
            System.clearProperty("cas.port");
            System.clearProperty("cas.project");
            CasLoginFilter.resetCasConfigCache();
            tmpFile.delete();
        }
    }
}

