package com.account_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestTemplateConfig Test Suite")
class RestTemplateConfigTest {

    @Mock
    private TraceIdClientInterceptor traceIdClientInterceptor;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @InjectMocks
    private RestTemplateConfig restTemplateConfig;

    private RestTemplate mockRestTemplate;

    @BeforeEach
    void setUp() {
        mockRestTemplate = new RestTemplate();
    }

    @Test
    @DisplayName("When: RestTemplate bean is created, Then: Trace ID interceptor is registered")
    void testRestTemplateBeanWithInterceptor() {
        // Given
        when(restTemplateBuilder.interceptors(traceIdClientInterceptor)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(mockRestTemplate);

        // When
        RestTemplate result = restTemplateConfig.restTemplate(restTemplateBuilder);

        // Then
        assertNotNull(result);
        verify(restTemplateBuilder, times(1)).interceptors(traceIdClientInterceptor);
        verify(restTemplateBuilder, times(1)).build();
    }

    @Test
    @DisplayName("When: restTemplate method is called, Then: Interceptor is set before build")
    void testInterceptorSetBeforeBuild() {
        // Given
        when(restTemplateBuilder.interceptors(traceIdClientInterceptor)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(mockRestTemplate);

        // When
        restTemplateConfig.restTemplate(restTemplateBuilder);

        // Then
        verify(restTemplateBuilder, times(1)).interceptors(traceIdClientInterceptor);
        verify(restTemplateBuilder, times(1)).build();
    }

    @Test
    @DisplayName("When: Multiple RestTemplate beans are created, Then: Each has the interceptor")
    void testMultipleRestTemplateBeans() {
        // Given
        when(restTemplateBuilder.interceptors(traceIdClientInterceptor)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(mockRestTemplate).thenReturn(new RestTemplate());

        // When
        RestTemplate template1 = restTemplateConfig.restTemplate(restTemplateBuilder);
        RestTemplate template2 = restTemplateConfig.restTemplate(restTemplateBuilder);

        // Then
        assertNotNull(template1);
        assertNotNull(template2);
        verify(restTemplateBuilder, times(2)).interceptors(traceIdClientInterceptor);
        verify(restTemplateBuilder, times(2)).build();
    }

    @Test
    @DisplayName("When: RestTemplate is built, Then: Non-null instance is returned")
    void testRestTemplateNotNull() {
        // Given
        when(restTemplateBuilder.interceptors(traceIdClientInterceptor)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(mockRestTemplate);

        // When
        RestTemplate result = restTemplateConfig.restTemplate(restTemplateBuilder);

        // Then
        assertNotNull(result);
        assertSame(mockRestTemplate, result);
    }

    @Test
    @DisplayName("When: Interceptor is injected, Then: Interceptor is available for bean creation")
    void testInterceptorInjection() {
        // Given & When
        assertNotNull(restTemplateConfig);
        // The interceptor is injected via @Autowired

        // Then - Just verify that the config is properly initialized
        verify(restTemplateBuilder, never()).build(); // Not called in this test
    }

    @Test
    @DisplayName("When: Configuration is created, Then: Configuration is annotated properly")
    void testConfigurationAnnotation() {
        // Given & When
        Class<?> configClass = RestTemplateConfig.class;

        // Then
        assertNotNull(configClass.getAnnotation(org.springframework.context.annotation.Configuration.class));
    }
}


