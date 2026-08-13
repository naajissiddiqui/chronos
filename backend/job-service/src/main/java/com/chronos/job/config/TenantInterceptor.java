package com.chronos.job.config;

import com.chronos.job.exception.InvalidTenantException;
import com.chronos.job.util.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Organization-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String orgIdHeader = request.getHeader(HEADER_NAME);
        if (orgIdHeader != null && !orgIdHeader.trim().isEmpty()) {
            try {
                UUID organizationId = UUID.fromString(orgIdHeader.trim());
                TenantContext.setOrganizationId(organizationId);
            } catch (IllegalArgumentException e) {
                throw new InvalidTenantException("Invalid organization ID format in header: " + HEADER_NAME);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
