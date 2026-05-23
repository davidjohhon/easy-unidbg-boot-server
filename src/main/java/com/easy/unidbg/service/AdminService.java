package com.easy.unidbg.service;

import com.easy.unidbg.components.ModuleContainer;
import com.easy.unidbg.entity.AdminUser;
import com.easy.unidbg.entity.ApiKey;
import com.easy.unidbg.entity.ModuleEntity;
import com.easy.unidbg.entity.OperationLog;
import com.easy.unidbg.entity.ResourceFile;
import com.easy.unidbg.repository.AdminUserRepository;
import com.easy.unidbg.repository.ApiKeyRepository;
import com.easy.unidbg.repository.ModuleEntityRepository;
import com.easy.unidbg.repository.OperationLogRepository;
import com.easy.unidbg.repository.ResourceFileRepository;
import com.easy.unidbg.utils.Md5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Core business logic for the admin panel.
 * Manages the complete lifecycle of service modules: upload, reload,
 * offline/online, delete, resource files, API keys, users, and passwords.
 * All module operations are transactional and log to OperationLog.
 */
@Slf4j
@Service
public class AdminService {

    private static final String TASKS_DIR = "tasks";
    private static final String ASSETS_DIR = "assets";

    @Autowired
    private ModuleEntityRepository moduleEntityRepository;

    @Autowired
    private ResourceFileRepository resourceFileRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModuleContainer moduleContainer;

    public List<ModuleEntity> getAllModules() {
        return moduleEntityRepository.findAll();
    }

    public ModuleEntity getModuleById(Long id) {
        return moduleEntityRepository.findById(id).orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ModuleEntity uploadAndLoadModule(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".class") && !fileName.endsWith(".jar"))) {
            throw new IllegalArgumentException("Only .class and .jar files are supported");
        }

        File tasksDir = new File(TASKS_DIR);
        if (!tasksDir.exists()) {
            tasksDir.mkdirs();
        }

        if (fileName.endsWith(".jar")) {
            Path jarPath = Paths.get(TASKS_DIR, fileName);
            Files.copy(file.getInputStream(), jarPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            List<ModuleEntity> loaded = loadClassesFromJar(jarPath);
            return loaded.isEmpty() ? null : loaded.get(0);
        }

        Path destPath = Paths.get(TASKS_DIR, fileName);
        Files.copy(file.getInputStream(), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        byte[] classData = Files.readAllBytes(destPath);
        String md5 = Md5Utils.getMD5(classData);
        MyClassLoader cl = new MyClassLoader();
        Class<?> loadedClass = cl.defineClassFromBytes(classData);
        String moduleName = loadedClass.getName();

        ModuleEntity entity = moduleEntityRepository.findByModuleName(moduleName).orElse(new ModuleEntity());
        entity.setModuleName(moduleName);
        entity.setFileName(fileName);
        entity.setFileMd5(md5);
        entity.setStatus("ONLINE");
        entity.setErrorMessage(null);
        moduleEntityRepository.save(entity);

        moduleContainer.addModule(moduleName, loadedClass, md5);
        logOp("UPLOAD", moduleName, "file=" + fileName, "success");
        log.info("Module loaded via admin: {} -> {}", fileName, moduleName);
        return entity;
    }

    private List<ModuleEntity> loadClassesFromJar(Path jarPath) throws Exception {
        List<ModuleEntity> loaded = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    String simpleName = entry.getName().replace('/', '.').replace(".class", "");
                    simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1) + ".class";

                    try (InputStream is = jar.getInputStream(entry)) {
                        byte[] classData = readAllBytes(is);
                        String md5 = Md5Utils.getMD5(classData);

                        Path classPath = Paths.get(TASKS_DIR, simpleName);
                        Files.write(classPath, classData);

                        MyClassLoader cl = new MyClassLoader();
                        Class<?> loadedClass = cl.defineClassFromBytes(classData);
                        String moduleName = loadedClass.getName();

                        ModuleEntity entity = moduleEntityRepository.findByModuleName(moduleName).orElse(new ModuleEntity());
                        entity.setModuleName(moduleName);
                        entity.setFileName(simpleName);
                        entity.setFileMd5(md5);
                        entity.setStatus("ONLINE");
                        entity.setErrorMessage(null);
                        moduleEntityRepository.save(entity);

                        moduleContainer.addModule(moduleName, loadedClass, md5);
                        log.info("Module loaded from JAR: {} -> {}", simpleName, moduleName);
                        loaded.add(entity);
                    }
                }
            }
        }
        return loaded;
    }

    private byte[] readAllBytes(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModule(Long id) {
        ModuleEntity entity = moduleEntityRepository.findById(id).orElse(null);
        if (entity == null) return;

        if ("ONLINE".equals(entity.getStatus())) {
            logOp("DELETE", entity.getModuleName(), "blocked: module is ONLINE", "failed");
            throw new IllegalStateException("Cannot delete an ONLINE module. Take it offline first.");
        }

        deleteResourceFilesByModule(id);
        moduleEntityRepository.delete(entity);
        moduleContainer.deleteModule(entity.getModuleName());

        Path classPath = Paths.get(TASKS_DIR, entity.getFileName());
        try {
            Files.deleteIfExists(classPath);
        } catch (Exception e) {
            log.warn("Failed to delete file: {}", classPath, e);
        }

        logOp("DELETE", entity.getModuleName(), "file=" + entity.getFileName(), "success");
        log.info("Module deleted: {}", entity.getModuleName());
    }

    @Transactional(rollbackFor = Exception.class)
    public ModuleEntity reloadModule(Long id) {
        ModuleEntity entity = moduleEntityRepository.findById(id).orElse(null);
        if (entity == null) return null;

        Path classPath = Paths.get(TASKS_DIR, entity.getFileName());
        if (!Files.exists(classPath)) {
            entity.setStatus("ERROR");
            entity.setErrorMessage("File not found: " + entity.getFileName());
            logOp("RELOAD", entity.getModuleName(), "file not found", "failed");
            return moduleEntityRepository.save(entity);
        }

        try {
            byte[] classData = Files.readAllBytes(classPath);
            String md5 = Md5Utils.getMD5(classData);
            MyClassLoader cl = new MyClassLoader();
            Class<?> loadedClass = cl.defineClassFromBytes(classData);

            entity.setStatus("ONLINE");
            entity.setFileMd5(md5);
            entity.setErrorMessage(null);
            moduleEntityRepository.save(entity);

            moduleContainer.addModule(entity.getModuleName(), loadedClass, md5);
            logOp("RELOAD", entity.getModuleName(), "", "success");
            log.info("Module reloaded: {}", entity.getModuleName());
        } catch (Exception e) {
            entity.setStatus("ERROR");
            entity.setErrorMessage(e.getMessage());
            moduleEntityRepository.save(entity);
            logOp("RELOAD", entity.getModuleName(), e.getMessage(), "failed");
            log.error("Failed to reload module: {}", entity.getModuleName(), e);
        }

        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public ModuleEntity takeOffline(Long id) {
        ModuleEntity entity = moduleEntityRepository.findById(id).orElse(null);
        if (entity == null) return null;

        entity.setStatus("OFFLINE");
        entity.setErrorMessage(null);
        moduleEntityRepository.save(entity);

        moduleContainer.deleteModule(entity.getModuleName());

        logOp("OFFLINE", entity.getModuleName(), "", "success");
        log.info("Module taken offline: {} (files retained)", entity.getModuleName());
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public ModuleEntity bringOnline(Long id) {
        ModuleEntity entity = moduleEntityRepository.findById(id).orElse(null);
        if (entity == null) return null;

        Path classPath = Paths.get(TASKS_DIR, entity.getFileName());
        if (!Files.exists(classPath)) {
            entity.setStatus("ERROR");
            entity.setErrorMessage("File not found: " + entity.getFileName());
            logOp("ONLINE", entity.getModuleName(), "file not found", "failed");
            return moduleEntityRepository.save(entity);
        }

        try {
            byte[] classData = Files.readAllBytes(classPath);
            String md5 = Md5Utils.getMD5(classData);
            MyClassLoader cl = new MyClassLoader();
            Class<?> loadedClass = cl.defineClassFromBytes(classData);

            entity.setStatus("ONLINE");
            entity.setFileMd5(md5);
            entity.setErrorMessage(null);
            moduleEntityRepository.save(entity);

            moduleContainer.addModule(entity.getModuleName(), loadedClass, md5);
            logOp("ONLINE", entity.getModuleName(), "", "success");
            log.info("Module brought online: {}", entity.getModuleName());
        } catch (Exception e) {
            entity.setStatus("ERROR");
            entity.setErrorMessage(e.getMessage());
            moduleEntityRepository.save(entity);
            logOp("ONLINE", entity.getModuleName(), e.getMessage(), "failed");
            log.error("Failed to bring module online: {}", entity.getModuleName(), e);
        }

        return entity;
    }

    // ==================== Resource Files ====================

    public List<ResourceFile> getResourceFiles(Long moduleId) {
        return resourceFileRepository.findByModuleId(moduleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadResourceFiles(Long moduleId, String subDir, MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0) return;

        File assetsSubDir = new File(ASSETS_DIR, subDir != null ? subDir : "");
        if (!assetsSubDir.exists()) {
            assetsSubDir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isEmpty()) continue;

            Path destPath = Paths.get(assetsSubDir.getAbsolutePath(), originalName);
            Files.copy(file.getInputStream(), destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            ResourceFile rf = new ResourceFile();
            rf.setModuleId(moduleId);
            rf.setFileName(originalName);
            rf.setOriginalName(originalName);
            rf.setSubDirectory(subDir != null ? subDir : "");
            resourceFileRepository.save(rf);

            logOp("UPLOAD_RESOURCE", "module=" + moduleId, "assets/" + subDir + "/" + originalName, "success");
        }
    }

    public void deleteResourceFile(Long fileId) {
        ResourceFile rf = resourceFileRepository.findById(fileId).orElse(null);
        if (rf == null) return;

        Path filePath = Paths.get(ASSETS_DIR, rf.getSubDirectory() != null ? rf.getSubDirectory() : "", rf.getFileName());
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete resource file: {}", filePath, e);
        }
        resourceFileRepository.delete(rf);
        logOp("DELETE_RESOURCE", rf.getOriginalName(), "", "success");
    }

    private void deleteResourceFilesByModule(Long moduleId) {
        List<ResourceFile> files = resourceFileRepository.findByModuleId(moduleId);
        for (ResourceFile rf : files) {
            Path filePath = Paths.get(ASSETS_DIR, rf.getSubDirectory() != null ? rf.getSubDirectory() : "", rf.getFileName());
            try {
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.warn("Failed to delete resource file: {}", filePath, e);
            }
        }
        resourceFileRepository.deleteByModuleId(moduleId);
    }

    // ==================== Operation Logs ====================

    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public Page<OperationLog> getOperationLogs(int page, int size) {
        return operationLogRepository.findAll(
                PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
    }

    public void deleteOperationLog(Long id) {
        operationLogRepository.deleteById(id);
    }

    public void clearOperationLogs() {
        operationLogRepository.deleteAll();
    }

    private void logOp(String action, String target, String detail, String result) {
        OperationLog op = new OperationLog();
        String operator = SecurityContextHolder.getContext().getAuthentication().getName();
        op.setOperator(operator);
        op.setAction(action);
        op.setTarget(target);
        op.setDetail(detail);
        op.setResult(result);
        operationLogRepository.save(op);
    }

    // ==================== User Management ====================

    public List<AdminUser> getAllUsers() {
        return adminUserRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String username, String oldPassword, String newPassword) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("admin".equals(username) && !"admin".equals(currentUser)) {
            throw new IllegalStateException("Only admin can change the admin password");
        }
        AdminUser user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
        logOp("CHANGE_PWD", username, "", "success");
    }

    @Transactional(rollbackFor = Exception.class)
    public void addUser(String username, String password) {
        if (adminUserRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        adminUserRepository.save(user);
        logOp("ADD_USER", username, "", "success");
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        AdminUser user = adminUserRepository.findById(id).orElse(null);
        if (user == null) return;
        if ("admin".equals(user.getUsername())) {
            throw new IllegalStateException("Cannot delete the default admin user");
        }
        long count = adminUserRepository.count();
        if (count <= 1) {
            throw new IllegalStateException("Cannot delete the last admin user");
        }
        adminUserRepository.delete(user);
        logOp("DELETE_USER", user.getUsername(), "", "success");
    }

    // ==================== API Key Management ====================

    public String generateApiKey() {
        String rawKey = "sk-" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String hash = sha256(rawKey);
        ApiKey ak = new ApiKey();
        ak.setKeyPrefix(rawKey.substring(0, 8));
        ak.setKeyHash(hash);
        apiKeyRepository.save(ak);
        logOp("CREATE_APIKEY", ak.getKeyPrefix(), "", "success");
        return rawKey;
    }

    public Page<ApiKey> getAllApiKeys(int page, int size) {
        return apiKeyRepository.findAll(
                PageRequest.of(page, size, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteApiKey(Long id) {
        ApiKey ak = apiKeyRepository.findById(id).orElse(null);
        if (ak == null) return;
        apiKeyRepository.delete(ak);
        logOp("DELETE_APIKEY", ak.getKeyPrefix(), "", "success");
    }

    public boolean validateApiKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) return false;
        String hash = sha256(rawKey);
        return apiKeyRepository.findByKeyHash(hash).isPresent();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ==================== Startup Recovery ====================

    /**
     * Loads all modules marked as ONLINE from the database into ModuleContainer.
     * Called at application startup. Modules whose files are missing are marked ERROR.
     */
    public void reloadAllFromDb() {
        List<ModuleEntity> modules = moduleEntityRepository.findAll();
        for (ModuleEntity entity : modules) {
            if ("ONLINE".equals(entity.getStatus())) {
                Path classPath = Paths.get(TASKS_DIR, entity.getFileName());
                if (Files.exists(classPath)) {
                    try {
                        byte[] classData = Files.readAllBytes(classPath);
                        String md5 = Md5Utils.getMD5(classData);
                        MyClassLoader cl = new MyClassLoader();
                        Class<?> loadedClass = cl.defineClassFromBytes(classData);
                        moduleContainer.addModule(entity.getModuleName(), loadedClass, md5);
                        log.info("DB module loaded: {} -> {}", entity.getFileName(), entity.getModuleName());
                    } catch (Exception e) {
                        log.error("Failed to load module from DB: {}", entity.getModuleName(), e);
                        entity.setStatus("ERROR");
                        entity.setErrorMessage(e.getMessage());
                        moduleEntityRepository.save(entity);
                    }
                } else {
                    entity.setStatus("ERROR");
                    entity.setErrorMessage("File not found");
                    moduleEntityRepository.save(entity);
                }
            }
        }
    }
}

class MyClassLoader extends ClassLoader {
    public Class<?> defineClassFromBytes(byte[] classData) {
        return defineClass(null, classData, 0, classData.length);
    }
}
