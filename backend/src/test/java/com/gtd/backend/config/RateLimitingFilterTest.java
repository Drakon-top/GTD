package com.gtd.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter filter;
    private RateLimitProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setMaxRequests(3);
        properties.setWindowMs(60_000);
        filter = new RateLimitingFilter(properties, objectMapper());
        filter.clearBuckets();
        SecurityContextHolder.clearContext();
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Test
    void shouldAllowRequests_withinLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(3)).doFilter(request, response);
    }

    @Test
    void shouldBlock_whenLimitExceeded() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
        verify(response).setStatus(429);
    }

    @Test
    void shouldUseUserId_whenAuthenticated() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        String key = filter.resolveClientKey(request);
        assertThat(key).isEqualTo("user:" + userId);
    }

    @Test
    void shouldUseXForwardedFor_whenPresent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.0.1");

        String key = filter.resolveClientKey(request);
        assertThat(key).isEqualTo("ip:10.0.0.1");
    }

    @Test
    void shouldUseRemoteAddr_whenNoForwardedHeader() {
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        String key = filter.resolveClientKey(request);
        assertThat(key).isEqualTo("ip:192.168.1.100");
    }

    @Test
    void shouldTrackSeparately_forDifferentClients() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(4)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}
