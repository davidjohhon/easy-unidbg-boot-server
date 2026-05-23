package com.easy.unidbg.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a loaded service module.
 * Tracks the lifecycle state of each uploaded .class module:
 * ONLINE = loaded in container and accessible via API;
 * OFFLINE = unloaded but files retained; ERROR = failed to load.
 */
@Data
@Entity
@Table(name = "module_info")
public class ModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Fully qualified class name, e.g. com.sum.dcgc.DcWtf */
    @Column(name = "module_name", nullable = false, unique = true)
    private String moduleName;

    /** Original uploaded filename, e.g. DcWtf.class */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** ONLINE | OFFLINE | ERROR */
    @Column(nullable = false)
    private String status;

    /** MD5 checksum for change detection during hot reload */
    @Column(name = "file_md5")
    private String fileMd5;

    /** Error details when status is ERROR */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
