package com.magadhexplora.api.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter();
    }

    @Test
    void allowsPostsUnderTheLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = request("POST", "/api/contact", "1.2.3.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus(), "request " + i + " should pass");
            assertNotNull(res.getHeader("X-RateLimit-Limit"));
        }
        verify(chain, atLeastOnce()).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blocks11thPostWithin60s() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        // First 10 pass
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = request("POST", "/api/contact", "5.6.7.8");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
        }
        // 11th gets 429
        MockHttpServletRequest req = request("POST", "/api/contact", "5.6.7.8");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertEquals(429, res.getStatus());
        assertNotNull(res.getHeader("Retry-After"));
    }

    @Test
    void doesNotLimitGetRequests() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest req = request("GET", "/api/packages", "9.9.9.9");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void doesNotLimitNonMatchedPaths() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = request("POST", "/api/something-else", "8.8.8.8");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void respectsXForwardedForHeader() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        // IP A exhausts the bucket
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = request("POST", "/api/contact", null);
            req.addHeader("X-Forwarded-For", "11.11.11.11, 192.168.1.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
        }
        // IP B (different XFF) should still get through
        MockHttpServletRequest req = request("POST", "/api/contact", null);
        req.addHeader("X-Forwarded-For", "22.22.22.22");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        assertEquals(200, res.getStatus());
    }

    private static MockHttpServletRequest request(String method, String uri, String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRequestURI(uri);
        if (remoteAddr != null) req.setRemoteAddr(remoteAddr);
        return req;
    }
}
