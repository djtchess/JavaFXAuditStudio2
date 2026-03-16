package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
    }

    @Test
    void doFilter_setsXContentTypeOptionsNosniff() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    @Test
    void doFilter_setsXFrameOptionsDeny() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }

    @Test
    void doFilter_setsCacheControlNoStore() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void doFilter_callsFilterChain() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
