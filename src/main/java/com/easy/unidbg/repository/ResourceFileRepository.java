package com.easy.unidbg.repository;

import com.easy.unidbg.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Spring Data JPA repository for module resource files (.so libraries). */
@Repository
public interface ResourceFileRepository extends JpaRepository<ResourceFile, Long> {
    List<ResourceFile> findByModuleId(Long moduleId);
    void deleteByModuleId(Long moduleId);
}
