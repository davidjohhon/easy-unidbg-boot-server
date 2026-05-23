package com.easy.unidbg.service;

import com.easy.unidbg.entity.AccessLog;
import com.easy.unidbg.repository.AccessLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for persisting and querying API access logs.
 * Supports dynamic filtering by module name, response status, and date range.
 */
@Service
public class AccessLogService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    public void save(AccessLog log) {
        accessLogRepository.save(log);
    }

    public void clearAll() {
        accessLogRepository.deleteAll();
    }

    /**
     * Queries access logs with optional filters. Results are paginated and sorted by creation time DESC.
     *
     * @param moduleName optional module name filter (LIKE match)
     * @param status     optional response status filter ("ok" or "error")
     * @param startDate  optional start date filter (greater than or equal)
     * @param endDate    optional end date filter (less than or equal)
     * @param page       page number (0-indexed)
     * @param size       page size
     */
    public Page<AccessLog> query(String moduleName, String status, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AccessLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (moduleName != null && !moduleName.isEmpty()) {
                predicates.add(cb.like(root.get("moduleName"), "%" + moduleName + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("responseStatus"), status));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return accessLogRepository.findAll(specification, pageable);
    }
}
