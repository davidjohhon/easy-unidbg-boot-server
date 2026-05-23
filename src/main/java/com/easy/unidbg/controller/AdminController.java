package com.easy.unidbg.controller;

import com.easy.unidbg.entity.AccessLog;
import com.easy.unidbg.entity.ApiKey;
import com.easy.unidbg.entity.ModuleEntity;
import com.easy.unidbg.entity.OperationLog;
import com.easy.unidbg.entity.ResourceFile;
import com.easy.unidbg.service.AccessLogService;
import com.easy.unidbg.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin panel controller. All routes are under /admin/.
 * Provides full CRUD for modules, users, API keys, and log views.
 * All pages require authentication (enforced by WebSecurityConfig).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AccessLogService accessLogService;

    @GetMapping({"", "/"})
    public String root() {
        return "redirect:/admin/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("moduleCount", adminService.getAllModules().size());
        model.addAttribute("logCount", accessLogService.query(null, null, null, null, 0, 1).getTotalElements());
        return "admin/dashboard";
    }

    // ==================== Module (Service) Management ====================

    @GetMapping("/modules")
    public String modules(Model model) {
        List<ModuleEntity> modules = adminService.getAllModules();
        Map<Long, List<ResourceFile>> resourceMap = new HashMap<>();
        for (ModuleEntity m : modules) {
            resourceMap.put(m.getId(), adminService.getResourceFiles(m.getId()));
        }
        model.addAttribute("modules", modules);
        model.addAttribute("resourceMap", resourceMap);
        return "admin/modules";
    }

    @PostMapping("/modules/upload")
    public String uploadModule(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "resourceFiles", required = false) MultipartFile[] resourceFiles,
                                RedirectAttributes redirectAttributes) {
        try {
            ModuleEntity entity = adminService.uploadAndLoadModule(file);
            if (entity != null) {
                String subDir = deriveSubDir(entity.getModuleName());
                adminService.uploadResourceFiles(entity.getId(), subDir, resourceFiles);
                redirectAttributes.addFlashAttribute("success", "Module uploaded: " + entity.getModuleName());
            } else {
                redirectAttributes.addFlashAttribute("success", "Uploaded, but no class with main() method found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    /** Extracts the last segment of a fully qualified class name's package as the assets subdirectory. */
    private String deriveSubDir(String moduleName) {
        String pkg = moduleName.contains(".") ? moduleName.substring(0, moduleName.lastIndexOf(".")) : "";
        return pkg.contains(".") ? pkg.substring(pkg.lastIndexOf(".") + 1) : pkg;
    }

    @PostMapping("/modules/{id}/resources")
    public String uploadModuleResources(@PathVariable Long id,
                                        @RequestParam("resourceFiles") MultipartFile[] resourceFiles,
                                        RedirectAttributes redirectAttributes) {
        try {
            ModuleEntity entity = adminService.getModuleById(id);
            String subDir = entity != null ? deriveSubDir(entity.getModuleName()) : "";
            adminService.uploadResourceFiles(id, subDir, resourceFiles);
            redirectAttributes.addFlashAttribute("success", "Resource files uploaded");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/resources/delete/{fileId}")
    public String deleteResourceFile(@PathVariable Long fileId, RedirectAttributes redirectAttributes) {
        adminService.deleteResourceFile(fileId);
        redirectAttributes.addFlashAttribute("success", "Resource file deleted");
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/delete/{id}")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteModule(id);
            redirectAttributes.addFlashAttribute("success", "Module deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/reload/{id}")
    public String reloadModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ModuleEntity entity = adminService.reloadModule(id);
        if (entity != null && "ERROR".equals(entity.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Reload failed: " + entity.getErrorMessage());
        } else {
            redirectAttributes.addFlashAttribute("success", "Module reloaded");
        }
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/offline/{id}")
    public String offlineModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ModuleEntity entity = adminService.takeOffline(id);
        if (entity != null) {
            redirectAttributes.addFlashAttribute("success", "Module offline: " + entity.getModuleName());
        }
        return "redirect:/admin/modules";
    }

    @PostMapping("/modules/online/{id}")
    public String onlineModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ModuleEntity entity = adminService.bringOnline(id);
        if (entity != null && "ERROR".equals(entity.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Failed to bring online: " + entity.getErrorMessage());
        } else if (entity != null) {
            redirectAttributes.addFlashAttribute("success", "Module online: " + entity.getModuleName());
        }
        return "redirect:/admin/modules";
    }

    // ==================== Profile & Password ====================

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("username", adminService.getCurrentUsername());
        return "admin/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                  @RequestParam("newPassword") String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/admin/profile";
        }
        try {
            adminService.changePassword(
                SecurityContextHolder.getContext().getAuthentication().getName(),
                oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/profile";
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", adminService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam("username") String username,
                           @RequestParam("password") String password,
                           @RequestParam("confirmPassword") String confirmPassword,
                           RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/admin/users";
        }
        try {
            adminService.addUser(username, password);
            redirectAttributes.addFlashAttribute("success", "User added: " + username);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            adminService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Operation Logs ====================

    @GetMapping("/oplogs")
    public String oplogs(@RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "20") int size,
                          Model model) {
        Page<OperationLog> opPage = adminService.getOperationLogs(page, size);
        model.addAttribute("oplogs", opPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", opPage.getTotalPages());
        model.addAttribute("totalElements", opPage.getTotalElements());
        return "admin/oplogs";
    }

    @PostMapping("/oplogs/delete/{id}")
    public String deleteOplog(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteOperationLog(id);
        redirectAttributes.addFlashAttribute("success", "Operation log deleted");
        return "redirect:/admin/oplogs";
    }

    @PostMapping("/oplogs/clear")
    public String clearOplogs(RedirectAttributes redirectAttributes) {
        adminService.clearOperationLogs();
        redirectAttributes.addFlashAttribute("success", "All operation logs cleared");
        return "redirect:/admin/oplogs";
    }

    // ==================== Access Logs ====================

    @GetMapping("/logs")
    public String logs(
            @RequestParam(value = "moduleName", required = false) String moduleName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDateParam,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDateParam,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {
        LocalDateTime startDate = startDateParam != null ? startDateParam.atStartOfDay() : null;
        LocalDateTime endDate = endDateParam != null ? endDateParam.atTime(LocalTime.MAX) : null;
        Page<AccessLog> logPage = accessLogService.query(moduleName, status, startDate, endDate, page, size);
        model.addAttribute("logs", logPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logPage.getTotalPages());
        model.addAttribute("totalElements", logPage.getTotalElements());
        model.addAttribute("moduleName", moduleName);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDateParam);
        model.addAttribute("endDate", endDateParam);
        model.addAttribute("modules", adminService.getAllModules());
        return "admin/logs";
    }

    @PostMapping("/logs/clear")
    public String clearAccessLogs(RedirectAttributes redirectAttributes) {
        accessLogService.clearAll();
        redirectAttributes.addFlashAttribute("success", "All access logs cleared");
        return "redirect:/admin/logs";
    }

    // ==================== API Key Management ====================

    @GetMapping("/apikeys")
    public String apikeys(@RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "size", defaultValue = "20") int size,
                           Model model) {
        Page<ApiKey> keyPage = adminService.getAllApiKeys(page, size);
        model.addAttribute("apikeys", keyPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", keyPage.getTotalPages());
        model.addAttribute("totalElements", keyPage.getTotalElements());
        return "admin/apikeys";
    }

    @PostMapping("/apikeys/generate")
    public String generateApiKey(RedirectAttributes redirectAttributes) {
        String rawKey = adminService.generateApiKey();
        redirectAttributes.addFlashAttribute("newApiKey", rawKey);
        redirectAttributes.addFlashAttribute("success", "API key generated. Copy it now — it won't be shown again.");
        return "redirect:/admin/apikeys";
    }

    @PostMapping("/apikeys/delete/{id}")
    public String deleteApiKey(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteApiKey(id);
        redirectAttributes.addFlashAttribute("success", "API key deleted");
        return "redirect:/admin/apikeys";
    }
}
