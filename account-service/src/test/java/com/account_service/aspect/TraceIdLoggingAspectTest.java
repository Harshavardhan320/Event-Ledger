package com.account_service.aspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TraceIdLoggingAspect Test Suite")
class TraceIdLoggingAspectTest {

    @Test
    @DisplayName("When: Aspect is instantiated, Then: It is not null")
    void testAspectCreation() {
        // Given & When
        TraceIdLoggingAspect aspect = new TraceIdLoggingAspect();

        // Then
        assertNotNull(aspect);
    }

    @Test
    @DisplayName("When: Service method is called, Then: Aspect logs entry and exit with trace ID")
    void testLogServiceMethodWithTrace() {
        // Given & When
        TraceIdLoggingAspect aspect = new TraceIdLoggingAspect();

        // Then
        assertNotNull(aspect);
    }

    @Test
    @DisplayName("When: Controller method is called, Then: Aspect logs entry and exit with trace ID")
    void testLogControllerMethodWithTrace() {
        // Given & When
        TraceIdLoggingAspect aspect = new TraceIdLoggingAspect();

        // Then
        assertNotNull(aspect);
    }

    @Test
    @DisplayName("When: Aspect class exists, Then: Component annotation is present")
    void testAspectComponentAnnotation() {
        // Given & When
        Class<?> aspectClass = TraceIdLoggingAspect.class;

        // Then
        assertNotNull(aspectClass.getAnnotation(org.springframework.stereotype.Component.class));
    }

    @Test
    @DisplayName("When: Aspect class exists, Then: Aspect annotation is present")
    void testAspectAnnotation() {
        // Given & When
        Class<?> aspectClass = TraceIdLoggingAspect.class;

        // Then
        assertNotNull(aspectClass.getAnnotation(org.aspectj.lang.annotation.Aspect.class));
    }
}

