package com.ejada.admin.service.impl;

import com.ejada.admin.domain.SuperadminAuditLog;
import com.ejada.admin.repository.SuperadminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@Slf4j
@RequiredArgsConstructor
public class SuperadminAuditService {

    private final SuperadminAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuperadminAction(String action, Long superadminId, String details) {
        SuperadminAuditLog entry = SuperadminAuditLog.builder()
            .action(action)
            .superadminId(superadminId)
            .details(details)
            .ipAddress(resolveClientIp())
            .userAgent(resolveUserAgent())
            .build();

        auditLogRepository.save(entry);
        log.debug("Persisted superadmin audit entry: action={}, superadminId={}", action, superadminId);
    }

    private String resolveClientIp() {
        return currentRequest()
            .map(req -> Optional.ofNullable(req.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",")[0].trim())
                .filter(ip -> !ip.isBlank())
                .orElse(req.getRemoteAddr()))
            .orElse(null);
    }

    private String resolveUserAgent() {
        return currentRequest()
            .map(req -> req.getHeader("User-Agent"))
            .orElse(null);
    }

    private Optional<HttpServletRequest> currentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest);
    }
}
