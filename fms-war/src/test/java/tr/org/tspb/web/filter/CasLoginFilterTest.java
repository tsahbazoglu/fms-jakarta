package tr.org.tspb.web.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.Test;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
                        return null;
                    }
                }
        );
    }
}
