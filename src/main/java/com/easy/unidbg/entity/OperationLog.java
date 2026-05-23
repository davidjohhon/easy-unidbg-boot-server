package com.easy.unidbg.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity recording admin panel operations for audit purposes.
 * Every module CRUD, user management, and API key action is logged.
 * Tracks which operator performed what action and whether it succeeded.
 */
@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of the operator who performed the action */
    @Column(name = "operator", nullable = false)
    private String operator;

    /** Action type: UPLOAD, DELETE, OFFLINE, ONLINE, RELOAD, CREATE_APIKEY, etc. */
    @Column(name = "action", nullable = false)
    private String action;

    /** Target of the action (module name, username, etc.) */
    @Column(name = "target")
    private String target;

    /** Additional detail about the action */
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    /** "success" or "failed" */
    @Column(name = "result")
    private String result;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
