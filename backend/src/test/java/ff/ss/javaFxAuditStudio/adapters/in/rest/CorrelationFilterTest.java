package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationFilterTest {

    private static final String HEADER_NAME = "X-Correlation-Id";

    private CorrelationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationFilter();
        MDC.clear();
    }

    @Test
    void doFilter_setsCorrelationIdHeader_whenNoneInRequest() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;
        String correlationId;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        correlationId = response.getHeader(HEADER_NAME);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).isNotBlank();
    }

    @Test
    void doFilter_usesExistingCorrelationId_whenPresentInRequest() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;
        String existingId;
        String returnedId;

        existingId = "my-existing-correlation-id";
        request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, existingId);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        returnedId = response.getHeader(HEADER_NAME);
        assertThat(returnedId).isEqualTo(existingId);
    }

    @Test
    void doFilter_cleansMdc_afterRequest() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("sessionId")).isNull();
    }

    @Test
    void doFilter_setsUuidFormat_whenNoHeaderProvided() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;
        MockFilterChain chain;
        String correlationId;

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        correlationId = response.getHeader(HEADER_NAME);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).hasSize(36);
        assertThat(correlationId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void doFilter_pushesSessionIdIntoMdc_whenSessionPathMatches() throws Exception {
        MockHttpServletRequest request;
        MockHttpServletResponse response;

        request = new MockHttpServletRequest("GET", "/api/v1/analysis/sessions/session-123/run");
        response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertThat(MDC.get("sessionId")).isEqualTo("session-123"));
    }
}
