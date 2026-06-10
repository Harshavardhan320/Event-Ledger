package com.account_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceIdInterceptor Test Suite")
class TraceIdInterceptorTest {

    private TraceIdInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @Test
    @DisplayName("When: Request has no trace ID header, Then: New trace ID is generated")
    void testPreHandleGeneratesNewTraceId() throws Exception {
        // Given
        request.setMethod("GET");
        request.setRequestURI("/accounts/ACC001");

        // When
        boolean result = interceptor.preHandle(request, response, null);

        // Then
        assertTrue(result);
        String traceId = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
        assertEquals(traceId, response.getHeader(TraceIdInterceptor.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("When: Request has trace ID header, Then: Existing trace ID is used")
    void testPreHandleUsesExistingTraceId() throws Exception {
        // Given
        String existingTraceId = "existing-trace-12345";
        request.addHeader(TraceIdInterceptor.TRACE_ID_HEADER, existingTraceId);
        request.setMethod("POST");
        request.setRequestURI("/accounts/ACC001/transactions");

        // When
        boolean result = interceptor.preHandle(request, response, null);

        // Then
        assertTrue(result);
        String traceIdInMDC = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
        assertEquals(existingTraceId, traceIdInMDC);
        assertEquals(existingTraceId, response.getHeader(TraceIdInterceptor.TRACE_ID_HEADER));
    }

    @Test
    @DisplayName("When: Request has empty trace ID header, Then: New trace ID is generated")
    void testPreHandleGeneratesNewTraceIdForEmpty() throws Exception {
        // Given
        request.addHeader(TraceIdInterceptor.TRACE_ID_HEADER, "");
        request.setMethod("GET");
        request.setRequestURI("/health");

        // When
        boolean result = interceptor.preHandle(request, response, null);

        // Then
        assertTrue(result);
        String traceId = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
        assertNotNull(traceId);
        assertFalse(traceId.isEmpty());
    }

    @Test
    @DisplayName("When: preHandle is called multiple times, Then: Each call generates or uses unique trace IDs")
    void testPreHandleMultipleCalls() throws Exception {
        // Given
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        // When
        interceptor.preHandle(request1, response1, null);
        String traceId1 = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
        MDC.clear();

        interceptor.preHandle(request2, response2, null);
        String traceId2 = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);

        // Then
        assertNotNull(traceId1);
        assertNotNull(traceId2);
        assertNotEquals(traceId1, traceId2);
    }

    @Test
    @DisplayName("When: afterCompletion is called without exception, Then: Trace ID is logged and cleared")
    void testAfterCompletionSuccess() throws Exception {
        // Given
        String traceId = "test-trace-success";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);
        response.setStatus(200);

        // When
        interceptor.afterCompletion(request, response, null, null);

        // Then
        assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: afterCompletion is called with exception, Then: Exception is logged and trace ID is cleared")
    void testAfterCompletionWithException() throws Exception {
        // Given
        String traceId = "test-trace-error";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);
        Exception exception = new RuntimeException("Test exception");
        response.setStatus(500);

        // When
        interceptor.afterCompletion(request, response, null, exception);

        // Then
        assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: Response status is set correctly, Then: Status is available in afterCompletion")
    void testResponseStatusInAfterCompletion() throws Exception {
        // Given
        String traceId = UUID.randomUUID().toString();
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);
        response.setStatus(404);

        // When
        interceptor.afterCompletion(request, response, null, null);

        // Then
        assertEquals(404, response.getStatus());
        assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: Trace ID format is UUID, Then: Valid UUID format is maintained")
    void testTraceIdUUIDFormat() throws Exception {
        // Given
        request.setMethod("GET");
        request.setRequestURI("/test");

        // When
        interceptor.preHandle(request, response, null);
        String traceId = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);

        // Then
        assertNotNull(traceId);
        assertDoesNotThrow(() -> UUID.fromString(traceId));
    }

    @Test
    @DisplayName("When: Trace ID is propagated to response header, Then: Header is set correctly")
    void testTraceIdInResponseHeader() throws Exception {
        // Given
        String customTraceId = "custom-trace-xyz";
        request.addHeader(TraceIdInterceptor.TRACE_ID_HEADER, customTraceId);

        // When
        interceptor.preHandle(request, response, null);

        // Then
        String responseHeader = response.getHeader(TraceIdInterceptor.TRACE_ID_HEADER);
        assertEquals(customTraceId, responseHeader);
    }

    @Test
    @DisplayName("When: Multiple requests are processed, Then: Each has independent trace ID in MDC")
    void testMultipleRequestsIndependentTraceIds() throws Exception {
        // Given
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        request1.setMethod("GET");

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        request2.setMethod("POST");

        // When
        interceptor.preHandle(request1, response1, null);
        String traceId1 = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);

        MDC.clear();
        interceptor.preHandle(request2, response2, null);
        String traceId2 = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);

        // Then
        assertNotEquals(traceId1, traceId2);
    }

    @Test
    @DisplayName("When: afterCompletion is called with different status codes, Then: Status is available")
    void testAfterCompletionVariousStatusCodes() throws Exception {
        // Given
        int[] statusCodes = {200, 201, 400, 404, 500};

        // When & Then
        for (int statusCode : statusCodes) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, "test-trace");
            resp.setStatus(statusCode);

            interceptor.afterCompletion(request, resp, null, null);

            assertEquals(statusCode, resp.getStatus());
            assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
        }
    }

    @Test
    @DisplayName("When: Trace ID constant values are correct, Then: Headers match expected values")
    void testTraceIdConstants() {
        // When & Then
        assertEquals("X-Trace-Id", TraceIdInterceptor.TRACE_ID_HEADER);
        assertEquals("traceId", TraceIdInterceptor.TRACE_ID_MDC_KEY);
    }

    @Test
    @DisplayName("When: MDC is cleared after afterCompletion, Then: No trace ID remains")
    void testMDCCleanup() throws Exception {
        // Given
        String traceId = "test-cleanup";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);

        // When
        interceptor.afterCompletion(request, response, null, null);

        // Then
        assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }
}

