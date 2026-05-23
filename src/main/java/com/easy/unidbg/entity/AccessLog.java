package com.easy.unidbg.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity recording every API invocation to /api/common/invoke.
 * Captures request details (module, args, client IP) and response info.
 * Logs are auto-cleaned after a configurable retention period (default 30 days).
 */
@Data
@Entity
@Table(name = "access_log")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Module class name that was invoked */
    @Column(name = "module_name")
    private String moduleName;

    /** Request parameters passed to the module's main() */
    @Column(name = "request_args", columnDefinition = "TEXT")
    private String requestArgs;

    /** Response data returned by the API */
    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    /** Client IP address (supports X-Forwarded-For) */
    @Column(name = "client_ip")
    private String clientIp;

    /** "ok" for successful calls, "error" for failures */
    @Column(name = "response_status")
    private String responseStatus;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
