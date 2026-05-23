package com.easy.unidbg.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity tracking uploaded resource files (.so libraries) associated with modules.
 * Files are stored in assets/<subDirectory>/ and cascade-deleted with their parent module.
 */
@Data
@Entity
@Table(name = "resource_file")
public class ResourceFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to module_info.id — the module this resource belongs to */
    @Column(name = "module_id", nullable = false)
    private Long moduleId;

    /** Stored filename on disk (may differ from original on collision) */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Original uploaded filename */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /** Subdirectory within assets/ (auto-derived from class package, e.g. "dcgc") */
    @Column(name = "sub_directory")
    private String subDirectory;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
