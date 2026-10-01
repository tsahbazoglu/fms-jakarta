package tr.org.tspb.web.filter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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
