
package com.mekongmobile.smartcrm.survey.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ServiceKeyFilterTest {

    private static final String KEY =
            "unit-test-only-secret-key-with-32-plus-characters";

    private final ServiceKeyFilter filter = new ServiceKeyFilter(KEY);

    @Test
    void shouldRejectMissingKey() throws Exception {
        var request = new MockHttpServletRequest(
                "POST", "/api/survey-invitations"
        );
        request.setServletPath("/api/survey-invitations");

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(
                response.getContentAsString()
                        .contains("UNAUTHENTICATED")
        );
        assertNull(chain.getRequest());
    }

    @Test
    void shouldRejectInvalidKey() throws Exception {
        var request = new MockHttpServletRequest(
                "POST", "/api/survey-invitations"
        );
        request.setServletPath("/api/survey-invitations");
        request.addHeader("X-Service-Key", "wrong-key");

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldAllowValidKey() throws Exception {
        var request = new MockHttpServletRequest(
                "POST", "/api/survey-invitations"
        );
        request.setServletPath("/api/survey-invitations");
        request.addHeader("X-Service-Key", KEY);

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldIgnoreOtherEndpoints() throws Exception {
        var request = new MockHttpServletRequest(
                "GET", "/api/other"
        );

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
    }

    @Test
    void shouldRejectMissingKeyOnSendEndpoint() throws Exception {
        var request = new MockHttpServletRequest(
                "POST",
                "/api/survey-invitations/4/send"
        );
        request.setServletPath(
                "/api/survey-invitations/4/send"
        );

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(
                response.getContentAsString()
                        .contains("UNAUTHENTICATED")
        );
    }

    @Test
    void shouldAllowValidKeyOnSendEndpoint() throws Exception {
        var request = new MockHttpServletRequest(
                "POST",
                "/api/survey-invitations/4/send"
        );
        request.setServletPath(
                "/api/survey-invitations/4/send"
        );
        request.addHeader("X-Service-Key", KEY);

        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }


    @Test
    void shouldRejectMissingKeyForSendEndpoint()
            throws Exception {

        String path =
                "/api/survey-invitations/1/send";

        var request =
                new MockHttpServletRequest("POST", path);

        request.setServletPath(path);

        var response =
                new MockHttpServletResponse();

        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

}
