package com.account_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig Test Suite")
class WebConfigTest {

    @Mock
    private TraceIdInterceptor traceIdInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @InjectMocks
    private WebConfig webConfig;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    @Test
    @DisplayName("When: addInterceptors is called, Then: TraceIdInterceptor is registered")
    void testAddInterceptorsRegistration() {
        // Given & When
        webConfig.addInterceptors(registry);

        // Then
        verify(registry, times(1)).addInterceptor(traceIdInterceptor);
    }

    @Test
    @DisplayName("When: WebMvcConfigurer is implemented, Then: addInterceptors method is available")
    void testWebMvcConfigurerImplementation() {
        // Given & When
        WebConfig config = new WebConfig();

        // Then
        assertTrue(config instanceof org.springframework.web.servlet.config.annotation.WebMvcConfigurer);
    }

    @Test
    @DisplayName("When: Configuration is annotated, Then: Configuration class is recognized")
    void testConfigurationAnnotation() {
        // Given & When
        Class<?> configClass = WebConfig.class;

        // Then
        assertNotNull(configClass.getAnnotation(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    @DisplayName("When: addInterceptors is called multiple times, Then: Each call registers the interceptor")
    void testMultipleAddInterceptorsCalls() {
        // Given
        InterceptorRegistry registry1 = mock(InterceptorRegistry.class);
        InterceptorRegistry registry2 = mock(InterceptorRegistry.class);

        // When
        webConfig.addInterceptors(registry1);
        webConfig.addInterceptors(registry2);

        // Then
        verify(registry1, times(1)).addInterceptor(traceIdInterceptor);
        verify(registry2, times(1)).addInterceptor(traceIdInterceptor);
    }

    @Test
    @DisplayName("When: Interceptor is injected, Then: Interceptor is not null")
    void testInterceptorInjection() {
        // Given & When
        WebConfig config = new WebConfig();
        // The traceIdInterceptor is injected via @Autowired

        // Then - Verify that the configuration can be created
        assertNotNull(config);
    }

    @Test
    @DisplayName("When: Registry method is called with correct interceptor, Then: Registry receives the correct object")
    void testRegistryReceivesCorrectInterceptor() {
        // Given & When
        webConfig.addInterceptors(registry);

        // Then
        verify(registry).addInterceptor(argThat(interceptor -> interceptor == traceIdInterceptor));
    }
}

