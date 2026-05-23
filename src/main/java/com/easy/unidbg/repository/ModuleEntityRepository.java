package com.easy.unidbg.repository;

import com.easy.unidbg.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Spring Data JPA repository for ModuleEntity persistence and lookup. */
@Repository
public interface ModuleEntityRepository extends JpaRepository<ModuleEntity, Long> {
    Optional<ModuleEntity> findByModuleName(String moduleName);
    boolean existsByModuleName(String moduleName);
}
