package com.easy.unidbg.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for API authentication keys.
 * Keys are generated as "sk-" + 64 hex chars, then SHA-256 hashed for storage.
 * Only the first 8 chars (keyPrefix) are stored in plaintext for display.
 */
@Data
@Entity
@Table(name = "api_key")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** First 8 characters of the raw key, used for masked display in admin UI */
    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix;

    /** SHA-256 hash of the full raw key */
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
