package com.account_service.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test cases for distributed tracing examples
 * Demonstrates how tracing works in various scenarios
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TracingExampleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test Example 1: Basic endpoint with automatic trace ID
     */
    @Test
    public void testBasicExample() throws Exception {
        mockMvc.perform(get("/api/examples/basic"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(content().string(containsString("successfully")))
                .andReturn();
        
        System.out.println("✓ Test 1 passed: Basic example with automatic trace ID");
    }

    /**
     * Test Example 2: Using TraceIdUtil
     */
    @Test
    public void testWithTraceUtil() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/examples/with-util"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String traceId = result.getResponse().getHeader("X-Trace-Id");
        String responseBody = result.getResponse().getContentAsString();
        
        assert traceId != null && responseBody.contains(traceId);
        System.out.println("✓ Test 2 passed: TraceIdUtil example with trace ID: " + traceId);
    }

    /**
     * Test Example 4: Multi-step process
     */
    @Test
    public void testMultiStepProcess() throws Exception {
        TracingExampleController.ExampleRequest request = 
            new TracingExampleController.ExampleRequest("TestName", 100);
        
        MvcResult result = mockMvc.perform(post("/api/examples/multi-step")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String traceId = result.getResponse().getHeader("X-Trace-Id");
        System.out.println("✓ Test 4 passed: Multi-step process with trace ID: " + traceId);
    }

    /**
     * Test Example 5: Nested method calls
     */
    @Test
    public void testNestedCalls() throws Exception {
        mockMvc.perform(get("/api/examples/nested"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();
        
        System.out.println("✓ Test 5 passed: Nested method calls with trace propagation");
    }

    /**
     * Test Example 6: Performance monitoring
     */
    @Test
    public void testPerformanceMonitoring() throws Exception {
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(get("/api/examples/performance"))
                .andExpect(status().isOk())
                .andReturn();

        long duration = System.currentTimeMillis() - startTime;
        String traceId = result.getResponse().getHeader("X-Trace-Id");
        
        System.out.println("✓ Test 6 passed: Performance monitoring. Duration: " + duration + "ms, Trace ID: " + traceId);
    }

    /**
     * Test: Error handling with trace ID
     */
    @Test
    public void testErrorHandlingWithTraceId() throws Exception {
        TracingExampleController.ExampleRequest invalidRequest = 
            new TracingExampleController.ExampleRequest("", -1); // Invalid input
        
        MvcResult result = mockMvc.perform(post("/api/examples/multi-step")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Trace-Id"))
                .andReturn();

        String traceId = result.getResponse().getHeader("X-Trace-Id");
        System.out.println("✓ Test 7 passed: Error handling with trace ID: " + traceId);
    }

    /**
     * Test: Trace ID persistence across multiple requests
     */
    @Test
    public void testTracePersistenceAcrossRequests() throws Exception {
        String customTraceId = "persistent-trace-12345";
        
        // First request
        MvcResult result1 = mockMvc.perform(get("/api/examples/basic")
                .header("X-Trace-Id", customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", customTraceId))
                .andReturn();

        String traceId1 = result1.getResponse().getHeader("X-Trace-Id");

        // Second request with same trace ID
        MvcResult result2 = mockMvc.perform(get("/api/examples/with-util")
                .header("X-Trace-Id", customTraceId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", customTraceId))
                .andReturn();

        String traceId2 = result2.getResponse().getHeader("X-Trace-Id");

        assert traceId1.equals(traceId2) && traceId1.equals(customTraceId);
        System.out.println("✓ Test 8 passed: Trace ID persistence: " + traceId1 + " == " + traceId2);
    }
}

