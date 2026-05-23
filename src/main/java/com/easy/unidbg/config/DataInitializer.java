package com.easy.unidbg.config;

import com.easy.unidbg.entity.AdminUser;
import com.easy.unidbg.repository.AdminUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Creates a default admin user (admin/admin123) on first startup
 * when the admin_user table is empty.
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(Md5PasswordEncoder.md5("admin123"));
            adminUserRepository.save(admin);
            log.info("Default admin user created: admin / admin123 (MD5)");
        }
    }
}
