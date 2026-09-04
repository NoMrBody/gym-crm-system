package com.gymcrm.workload.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Publishes the authenticated caller to the logging context. Registered inside the
 * security chain rather than as a bean, so it runs once and only after authentication.
 */
public class MdcUserFilter extends OncePerRequestFilter {
    private static final String USER_MDC_KEY = "user";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        if (authenticated) {
            MDC.put(USER_MDC_KEY, authentication.getName());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (authenticated) {
                MDC.remove(USER_MDC_KEY);
            }
        }
    }
}
