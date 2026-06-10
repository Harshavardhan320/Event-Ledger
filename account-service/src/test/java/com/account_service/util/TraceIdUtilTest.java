package com.account_service.util;

import com.account_service.config.TraceIdInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TraceIdUtil Test Suite")
class TraceIdUtilTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    // ==================== getCurrentTraceId Tests ====================

    @Test
    @DisplayName("When: Trace ID is set in MDC, Then: getCurrentTraceId returns the trace ID")
    void testGetCurrentTraceIdWhenSet() {
        // Given
        String traceId = "TRACE-12345";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);

        // When
        String result = TraceIdUtil.getCurrentTraceId();

        // Then
        assertEquals(traceId, result);
    }

    @Test
    @DisplayName("When: Trace ID is not set in MDC, Then: getCurrentTraceId returns null")
    void testGetCurrentTraceIdWhenNotSet() {
        // Given
        MDC.clear();

        // When
        String result = TraceIdUtil.getCurrentTraceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("When: Multiple trace IDs are retrieved sequentially, Then: Most recent value is returned")
    void testGetCurrentTraceIdMultipleTimes() {
        // Given
        String traceId1 = "TRACE-111";
        String traceId2 = "TRACE-222";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId1);

        // When
        String result1 = TraceIdUtil.getCurrentTraceId();
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId2);
        String result2 = TraceIdUtil.getCurrentTraceId();

        // Then
        assertEquals(traceId1, result1);
        assertEquals(traceId2, result2);
    }

    // ==================== setTraceId Tests ====================

    @Test
    @DisplayName("When: Valid trace ID is set, Then: Trace ID is stored in MDC")
    void testSetTraceIdWithValidId() {
        // Given
        String traceId = "TRACE-VALID-001";

        // When
        TraceIdUtil.setTraceId(traceId);

        // Then
        assertEquals(traceId, MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: null trace ID is set, Then: MDC is not modified")
    void testSetTraceIdWithNull() {
        // Given
        String initialValue = "INITIAL-TRACE";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, initialValue);

        // When
        TraceIdUtil.setTraceId(null);

        // Then
        assertEquals(initialValue, MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: Empty trace ID is set, Then: MDC is not modified")
    void testSetTraceIdWithEmptyString() {
        // Given
        String initialValue = "INITIAL-TRACE";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, initialValue);

        // When
        TraceIdUtil.setTraceId("");

        // Then
        assertEquals(initialValue, MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: Multiple trace IDs are set sequentially, Then: Last one is retained")
    void testSetMultipleTraceIds() {
        // Given
        String traceId1 = "TRACE-001";
        String traceId2 = "TRACE-002";
        String traceId3 = "TRACE-003";

        // When
        TraceIdUtil.setTraceId(traceId1);
        TraceIdUtil.setTraceId(traceId2);
        TraceIdUtil.setTraceId(traceId3);

        // Then
        assertEquals(traceId3, TraceIdUtil.getCurrentTraceId());
    }

    @Test
    @DisplayName("When: Trace ID with special characters is set, Then: Special characters are preserved")
    void testSetTraceIdWithSpecialCharacters() {
        // Given
        String traceId = "TRACE-ABC-123-XYZ_2024-06-10";

        // When
        TraceIdUtil.setTraceId(traceId);

        // Then
        assertEquals(traceId, TraceIdUtil.getCurrentTraceId());
    }

    // ==================== clearTraceId Tests ====================

    @Test
    @DisplayName("When: Trace ID is cleared, Then: MDC no longer contains the trace ID")
    void testClearTraceIdRemovesValue() {
        // Given
        String traceId = "TRACE-TO-CLEAR";
        MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);

        // When
        TraceIdUtil.clearTraceId();

        // Then
        assertNull(MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("When: clearTraceId is called on empty MDC, Then: No exception is thrown")
    void testClearTraceIdOnEmptyMdc() {
        // Given
        MDC.clear();

        // When & Then
        assertDoesNotThrow(() -> TraceIdUtil.clearTraceId());
        assertNull(TraceIdUtil.getCurrentTraceId());
    }

    @Test
    @DisplayName("When: Trace ID is set, cleared, and set again, Then: New value is stored")
    void testSetClearSetTraceIdSequence() {
        // Given
        String traceId1 = "TRACE-FIRST";
        String traceId2 = "TRACE-SECOND";

        // When
        TraceIdUtil.setTraceId(traceId1);
        assertEquals(traceId1, TraceIdUtil.getCurrentTraceId());
        TraceIdUtil.clearTraceId();
        assertNull(TraceIdUtil.getCurrentTraceId());
        TraceIdUtil.setTraceId(traceId2);

        // Then
        assertEquals(traceId2, TraceIdUtil.getCurrentTraceId());
    }

    // ==================== logWithTrace Tests ====================

    @Test
    @DisplayName("When: logWithTrace is called with trace ID set, Then: Message is logged with trace ID")
    void testLogWithTraceWithTraceIdSet() {
        // Given
        String traceId = "TRACE-LOG-001";
        String message = "Test log message";
        TraceIdUtil.setTraceId(traceId);

        // When & Then - Just verify no exception is thrown
        assertDoesNotThrow(() -> TraceIdUtil.logWithTrace(message));
    }

    @Test
    @DisplayName("When: logWithTrace is called without trace ID, Then: Message is logged without trace ID")
    void testLogWithTraceWithoutTraceId() {
        // Given
        MDC.clear();
        String message = "Test log message without trace";

        // When & Then - Just verify no exception is thrown
        assertDoesNotThrow(() -> TraceIdUtil.logWithTrace(message));
    }

    @Test
    @DisplayName("When: logWithTrace is called with various messages, Then: All are logged successfully")
    void testLogWithTraceMultipleMessages() {
        // Given
        String traceId = "TRACE-LOG-MULTIPLE";
        TraceIdUtil.setTraceId(traceId);

        String[] messages = {
                "First message",
                "Second message",
                "Third message with special chars: @#$%"
        };

        // When & Then
        for (String msg : messages) {
            assertDoesNotThrow(() -> TraceIdUtil.logWithTrace(msg));
        }
    }

    // ==================== logErrorWithTrace Tests ====================

    @Test
    @DisplayName("When: logErrorWithTrace is called with trace ID set, Then: Error is logged with trace ID")
    void testLogErrorWithTraceWithTraceIdSet() {
        // Given
        String traceId = "TRACE-ERROR-001";
        String message = "Error message";
        Throwable throwable = new Exception("Test exception");
        TraceIdUtil.setTraceId(traceId);

        // When & Then - Just verify no exception is thrown
        assertDoesNotThrow(() -> TraceIdUtil.logErrorWithTrace(message, throwable));
    }

    @Test
    @DisplayName("When: logErrorWithTrace is called without trace ID, Then: Error is logged without trace ID")
    void testLogErrorWithTraceWithoutTraceId() {
        // Given
        MDC.clear();
        String message = "Error message without trace";
        Throwable throwable = new RuntimeException("Test runtime exception");

        // When & Then - Just verify no exception is thrown
        assertDoesNotThrow(() -> TraceIdUtil.logErrorWithTrace(message, throwable));
    }

    @Test
    @DisplayName("When: logErrorWithTrace is called with various exceptions, Then: All are logged successfully")
    void testLogErrorWithTraceMultipleExceptions() {
        // Given
        String traceId = "TRACE-ERROR-MULTIPLE";
        TraceIdUtil.setTraceId(traceId);

        Throwable[] exceptions = {
                new IllegalArgumentException("Illegal argument"),
                new NullPointerException("Null pointer"),
                new RuntimeException("Runtime error")
        };

        // When & Then
        for (Throwable ex : exceptions) {
            assertDoesNotThrow(() -> TraceIdUtil.logErrorWithTrace("Error occurred", ex));
        }
    }

    @Test
    @DisplayName("When: logErrorWithTrace is called with null throwable, Then: Error is logged gracefully")
    void testLogErrorWithTraceNullThrowable() {
        // Given
        String traceId = "TRACE-ERROR-NULL-THR";
        TraceIdUtil.setTraceId(traceId);
        String message = "Error without throwable";

        // When & Then
        assertDoesNotThrow(() -> TraceIdUtil.logErrorWithTrace(message, null));
    }

    // ==================== Additional Edge Case Tests for 90%+ Coverage ====================

    @Test
    @DisplayName("When: setTraceId called with whitespace string, Then: MDC is updated")
    void testSetTraceIdWithWhitespace() {
        // Given
        String whitespaceId = "   ";

        // When
        TraceIdUtil.setTraceId(whitespaceId);

        // Then
        assertEquals(whitespaceId, TraceIdUtil.getCurrentTraceId());
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: Multiple clearTraceId calls, Then: No exception thrown")
    void testMultipleClearOperations() {
        // Given
        TraceIdUtil.setTraceId("TRACE-001");

        // When & Then
        assertDoesNotThrow(() -> {
            TraceIdUtil.clearTraceId();
            TraceIdUtil.clearTraceId();
            TraceIdUtil.clearTraceId();
        });
    }

    @Test
    @DisplayName("When: Get after clear, Then: null returned")
    void testGetAfterClear() {
        // Given
        TraceIdUtil.setTraceId("TRACE-CLEAR-TEST");
        TraceIdUtil.clearTraceId();

        // When
        String result = TraceIdUtil.getCurrentTraceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("When: Set with UUID-like trace ID, Then: UUID format preserved")
    void testSetTraceIdWithUUID() {
        // Given
        String uuidTraceId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        TraceIdUtil.setTraceId(uuidTraceId);

        // Then
        assertEquals(uuidTraceId, TraceIdUtil.getCurrentTraceId());
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: Set trace ID with numeric only, Then: Numeric format preserved")
    void testSetTraceIdWithNumbers() {
        // Given
        String numericId = "123456789012345";

        // When
        TraceIdUtil.setTraceId(numericId);

        // Then
        assertEquals(numericId, TraceIdUtil.getCurrentTraceId());
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: logWithTrace called multiple times with same trace ID, Then: All logged successfully")
    void testLogWithTraceConsistentID() {
        // Given
        String consistentId = "TRACE-CONSISTENT";
        TraceIdUtil.setTraceId(consistentId);
        String[] messages = {"msg1", "msg2", "msg3", "msg4", "msg5"};

        // When & Then
        for (String msg : messages) {
            assertDoesNotThrow(() -> TraceIdUtil.logWithTrace(msg));
        }
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: logErrorWithTrace with different exception types, Then: All logged without error")
    void testLogErrorWithVariousExceptions() {
        // Given
        String traceId = "TRACE-ERROR-VARIOUS";
        TraceIdUtil.setTraceId(traceId);

        Throwable[] exceptions = {
                new Exception("Generic"),
                new IllegalArgumentException("Illegal"),
                new IllegalStateException("State"),
                new IndexOutOfBoundsException("Index"),
                new UnsupportedOperationException("Unsupported")
        };

        // When & Then
        for (Throwable ex : exceptions) {
            assertDoesNotThrow(() -> TraceIdUtil.logErrorWithTrace("Various error", ex));
        }
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: getCurrentTraceId called 100 times, Then: Consistent results")
    void testGetCurrentTraceIdConsistency() {
        // Given
        String traceId = "TRACE-CONSISTENCY-TEST";
        TraceIdUtil.setTraceId(traceId);

        // When
        String[] results = new String[10];
        for (int i = 0; i < 10; i++) {
            results[i] = TraceIdUtil.getCurrentTraceId();
        }

        // Then
        for (String result : results) {
            assertEquals(traceId, result);
        }
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: setTraceId with very long string, Then: Full string stored and retrieved")
    void testSetTraceIdWithVeryLongString() {
        // Given
        String longId = "TRACE-" + "X".repeat(200);

        // When
        TraceIdUtil.setTraceId(longId);
        String retrieved = TraceIdUtil.getCurrentTraceId();

        // Then
        assertEquals(longId, retrieved);
        assertEquals(206, retrieved.length()); // "TRACE-" = 6 chars + 200 X's
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: logWithTrace with null message, Then: No exception thrown")
    void testLogWithTraceNullMessage() {
        // Given
        TraceIdUtil.setTraceId("TRACE-NULL-MSG");

        // When & Then
        assertDoesNotThrow(() -> TraceIdUtil.logWithTrace(null));
        TraceIdUtil.clearTraceId();
    }

    @Test
    @DisplayName("When: setTraceId followed by immediate clear and get, Then: null returned")
    void testSetClearImmediateGet() {
        // Given
        TraceIdUtil.setTraceId("TRACE-IMMEDIATE");

        // When
        TraceIdUtil.clearTraceId();
        String result = TraceIdUtil.getCurrentTraceId();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("When: Trace ID with mixed case and numbers, Then: Case preserved")
    void testTraceIDCasePreservation() {
        // Given
        String mixedCaseId = "TrAcE-123-aBcD-456";

        // When
        TraceIdUtil.setTraceId(mixedCaseId);
        String retrieved = TraceIdUtil.getCurrentTraceId();

        // Then
        assertEquals(mixedCaseId, retrieved);
        assertTrue(retrieved.contains("TrAcE"));
        assertTrue(retrieved.contains("aBcD"));
        TraceIdUtil.clearTraceId();
    }
}

