package com.easy.unidbg.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.LocalDateTime;

/**
 * Scheduled service that automatically cleans up old log entries.
 * Both access logs and operation logs respect the enabled flag and retention period.
 * - Access logs: cleaned daily at 3 AM (configurable via app.cleanup.access-log.cron)
 * - Operation logs: cleaned daily at 4 AM
 */
@Slf4j
@Service
public class LogCleanupService {

    @Autowired
    private EntityManager entityManager;

    @Value("${app.cleanup.access-log.enabled:true}")
    private boolean enabled;

    @Value("${app.cleanup.access-log.keep-days:30}")
    private int keepDays;

    @Scheduled(cron = "${app.cleanup.access-log.cron:0 0 3 * * ?}")
    public void cleanupAccessLogs() {
        int deleted = deleteLogs("AccessLog");
        if (deleted > 0) {
            log.info("Cleaned {} access log entries older than {} days", deleted, keepDays);
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupOperationLogs() {
        int deleted = deleteLogs("OperationLog");
        if (deleted > 0) {
            log.info("Cleaned {} operation log entries older than {} days", deleted, keepDays);
        }
    }

    private int deleteLogs(String entityName) {
        if (!enabled) {
            log.debug("Log cleanup is disabled");
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        Query query = entityManager.createQuery(
                "DELETE FROM " + entityName + " WHERE createdAt < :cutoff");
        query.setParameter("cutoff", cutoff);
        return query.executeUpdate();
    }
}
