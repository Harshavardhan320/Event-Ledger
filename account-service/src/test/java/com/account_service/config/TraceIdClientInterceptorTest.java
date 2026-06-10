package com.account_service.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TraceIdClientInterceptor Test Suite")
class TraceIdClientInterceptorTest {

    @Test
    @DisplayName("When: TraceIdClientInterceptor is instantiated, Then: It is not null")
    void testClientInterceptorCreation() {
        // Given & When
        TraceIdClientInterceptor interceptor = new TraceIdClientInterceptor();

        // Then
        assertNotNull(interceptor);
    }

    @Test
    @DisplayName("When: TraceIdClientInterceptor is created, Then: It implements ClientHttpRequestInterceptor")
    void testClientInterceptorInterface() {
        // Given & When
        TraceIdClientInterceptor interceptor = new TraceIdClientInterceptor();

        // Then
        assertTrue(interceptor instanceof org.springframework.http.client.ClientHttpRequestInterceptor);
    }

    @Test
    @DisplayName("When: Interceptor class exists, Then: Component annotation is present")
    void testInterceptorComponentAnnotation() {
        // Given & When
        Class<?> interceptorClass = TraceIdClientInterceptor.class;

        // Then
        assertNotNull(interceptorClass.getAnnotation(org.springframework.stereotype.Component.class));
    }
}

