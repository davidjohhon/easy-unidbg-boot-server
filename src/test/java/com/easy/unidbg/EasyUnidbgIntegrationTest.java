package com.easy.unidbg;

import com.easy.unidbg.config.Md5PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EasyUnidbgIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private static MockHttpSession session;
    private static String apiKeyRaw;

    private static final String TASKS_DIR = "tasks";
    private static final String TEST_CLASS = "DcWtf.class";

    // ==================== 1. Login ====================

    @Test
    @Order(1)
    void test01LoginSuccess() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                        .param("username", "admin")
                        .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @Order(2)
    void test02LoginFail() throws Exception {
        mvc.perform(post("/admin/login").session(new MockHttpSession())
                        .param("username", "admin")
                        .param("password", Md5PasswordEncoder.md5("wrong")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login?error=true"));
    }

    @Test
    @Order(3)
    void test03Dashboard() throws Exception {
        String body = mvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("Dashboard"));
    }

    // ==================== 2. User Management ====================

    @Test
    @Order(4)
    void test04AddUser() throws Exception {
        mvc.perform(post("/admin/users/add").session(session)
                        .param("username", "opuser")
                        .param("password", Md5PasswordEncoder.md5("op123"))
                        .param("confirmPassword", Md5PasswordEncoder.md5("op123")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @Order(5)
    void test05ListUsers() throws Exception {
        String body = mvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("admin"));
        assertTrue(body.contains("opuser"));
    }

    @Test
    @Order(6)
    void test06CannotDeleteAdmin() throws Exception {
        mvc.perform(post("/admin/users/delete/1").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/users").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("Cannot delete the default admin user")
                || body.contains("不能删除默认管理员账户"));
    }

    @Test
    @Order(7)
    void test07DeleteOpUser() throws Exception {
        // Re-login to ensure fresh session
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/users/delete/2").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/users").session(session))
                .andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("opuser"));
    }

    // ==================== 3. Password Change ====================

    @Test
    @Order(8)
    void test08ChangePassword() throws Exception {
        mvc.perform(post("/admin/profile/password").session(session)
                        .param("oldPassword", Md5PasswordEncoder.md5("admin123"))
                        .param("newPassword", Md5PasswordEncoder.md5("admin456"))
                        .param("confirmPassword", Md5PasswordEncoder.md5("admin456")))
                .andExpect(status().is3xxRedirection());

        // Login with new password
        MockHttpSession newSession = new MockHttpSession();
        mvc.perform(post("/admin/login").session(newSession)
                        .param("username", "admin")
                        .param("password", Md5PasswordEncoder.md5("admin456")))
                .andExpect(status().is3xxRedirection());

        // Revert to old password
        mvc.perform(post("/admin/profile/password").session(newSession)
                        .param("oldPassword", Md5PasswordEncoder.md5("admin456"))
                        .param("newPassword", Md5PasswordEncoder.md5("admin123"))
                        .param("confirmPassword", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        // Re-login with old password and update session
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                        .param("username", "admin")
                        .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @Order(9)
    void test09WrongOldPassword() throws Exception {
        mvc.perform(post("/admin/profile/password").session(session)
                        .param("oldPassword", Md5PasswordEncoder.md5("wrong"))
                        .param("newPassword", Md5PasswordEncoder.md5("admin123"))
                        .param("confirmPassword", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/profile").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("incorrect") || body.contains("错误"));
    }

    // ==================== 4. Module Upload ====================

    @Test
    @Order(10)
    void test10UploadModule() throws Exception {
        // Re-login to ensure valid session
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        File classFile = new File(TASKS_DIR, TEST_CLASS);
        assertTrue(classFile.exists(), TEST_CLASS + " must exist in tasks/");

        MockMultipartFile file = new MockMultipartFile("file", TEST_CLASS,
                "application/octet-stream", Files.readAllBytes(classFile.toPath()));

        mvc.perform(multipart("/admin/modules/upload").file(file).session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/modules").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("com.sum.dcgc.DcWtf"));
        assertTrue(body.contains("ONLINE"));
    }

    @Test
    @Order(11)
    void test11ModuleFileSaved() throws Exception {
        assertTrue(new File(TASKS_DIR, TEST_CLASS).exists());
    }

    // ==================== 5. API Key ====================

    @Test
    @Order(12)
    void test12GenerateApiKey() throws Exception {
        mvc.perform(post("/admin/apikeys/generate").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/apikeys").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int start = body.indexOf("sk-");
        assertTrue(start > 0, "API key should be displayed");
        int end = body.indexOf("<", start);
        apiKeyRaw = body.substring(start, end).trim();
        assertTrue(apiKeyRaw.startsWith("sk-"));
        assertTrue(apiKeyRaw.length() > 64);
    }

    @Test
    @Order(13)
    void test13ApiKeyListMasked() throws Exception {
        String body = mvc.perform(get("/admin/apikeys").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains(apiKeyRaw.substring(0, 8)), "Prefix visible");
        // Full key should NOT appear in the page body
        assertFalse(body.contains(apiKeyRaw.substring(9, Math.min(20, apiKeyRaw.length()))),
                "Middle of key should not be visible");
    }

    // ==================== 6. API Call ====================

    @Test
    @Order(14)
    void test14ApiCallWithValidKey() throws Exception {
        String body = mvc.perform(get("/api/common/invoke")
                        .param("module", "com.sum.dcgc.DcWtf")
                        .param("args", "test")
                        .param("apikey", apiKeyRaw))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"status\""));
    }

    @Test
    @Order(15)
    void test15ApiCallWithInvalidKey() throws Exception {
        mvc.perform(get("/api/common/invoke")
                        .param("module", "com.sum.dcgc.DcWtf")
                        .param("args", "test")
                        .param("apikey", "sk-invalid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value(401));
    }

    @Test
    @Order(16)
    void test16ApiCallWithoutKey() throws Exception {
        mvc.perform(get("/api/common/invoke")
                        .param("module", "com.sum.dcgc.DcWtf")
                        .param("args", "test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(17)
    void test17ApiCallNonexistentModule() throws Exception {
        String body = mvc.perform(get("/api/common/invoke")
                        .param("module", "com.nonexistent.Foo")
                        .param("args", "test")
                        .param("apikey", apiKeyRaw))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"errorCode\":500"));
        assertTrue(body.contains("\"status\":\"fail\""));
    }

    // ==================== 7. Module Offline/Online ====================

    @Test
    @Order(18)
    void test18TakeOffline() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/modules/offline/1").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("OFFLINE"));
    }

    @Test
    @Order(19)
    void test19ApiCallAfterOffline() throws Exception {
        String body = mvc.perform(get("/api/common/invoke")
                        .param("module", "com.sum.dcgc.DcWtf")
                        .param("args", "test")
                        .param("apikey", apiKeyRaw))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("\"errorCode\":500"));
        assertTrue(body.contains("\"status\":\"fail\""));
    }

    @Test
    @Order(20)
    void test20BringOnline() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/modules/online/1").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("ONLINE"));
    }

    // ==================== 8. Delete Guard ====================

    @Test
    @Order(21)
    void test21CannotDeleteOnlineModule() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/modules/delete/1").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("com.sum.dcgc.DcWtf"));
    }

    @Test
    @Order(22)
    void test22DeleteOfflineModule() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        String listBody = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(listBody.contains("com.sum.dcgc.DcWtf"), "Module must exist");

        // Take offline
        MvcResult off = mvc.perform(post("/admin/modules/offline/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Verify OFFLINE
        listBody = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(listBody.contains("OFFLINE"), "Module should be OFFLINE");

        // Delete
        mvc.perform(post("/admin/modules/delete/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        listBody = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertFalse(listBody.contains("com.sum.dcgc.DcWtf"), "Module should be deleted");
    }

    // ==================== 9. Resource Files ====================

    @Test
    @Order(23)
    void test23UploadModuleWithResource() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        if (!new File(TASKS_DIR, TEST_CLASS).exists()) {
            gitCheckout(TASKS_DIR + "/" + TEST_CLASS);
        }
        File classFile = new File(TASKS_DIR, TEST_CLASS);
        File soFile = new File("assets/dcgc/libwtf.so");
        assertTrue(classFile.exists(), TEST_CLASS + " required");
        assertTrue(soFile.exists(), "libwtf.so required");

        MockMultipartFile classPart = new MockMultipartFile("file", TEST_CLASS,
                "application/octet-stream", Files.readAllBytes(classFile.toPath()));
        MockMultipartFile soPart = new MockMultipartFile("resourceFiles", "libwtf.so",
                "application/octet-stream", Files.readAllBytes(soFile.toPath()));

        mvc.perform(multipart("/admin/modules/upload").file(classPart).file(soPart).session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/modules").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("libwtf.so"));
    }

    private void gitCheckout(String path) {
        try {
            new ProcessBuilder("git", "checkout", "--", path)
                    .directory(new File(".")).start().waitFor();
        } catch (Exception ignored) {}
    }

    // ==================== 10. Access Logs ====================

    @Test
    @Order(24)
    void test24AccessLogExists() throws Exception {
        session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session)
                .param("username", "admin")
                .param("password", Md5PasswordEncoder.md5("admin123")))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/api/common/invoke")
                .param("module", "com.sum.dcgc.DcWtf")
                .param("args", "logtest")
                .param("apikey", apiKeyRaw));

        String body = mvc.perform(get("/admin/logs").session(session))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("logtest"));
    }

    @Test
    @Order(25)
    void test25AccessLogFilterByStatus() throws Exception {
        mvc.perform(get("/api/common/invoke")
                .param("module", "com.nonexistent.Bar")
                .param("args", "x")
                .param("apikey", apiKeyRaw));

        String body = mvc.perform(get("/admin/logs").session(session)
                        .param("status", "error"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("com.nonexistent.Bar"));
    }

    @Test
    @Order(26)
    void test26AccessLogClear() throws Exception {
        mvc.perform(post("/admin/logs/clear").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/logs").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("No logs") || body.contains("暂无日志"));
    }

    // ==================== 11. Operation Logs ====================

    @Test
    @Order(27)
    void test27OperationLogExists() throws Exception {
        String body = mvc.perform(get("/admin/oplogs").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("UPLOAD") || body.contains("OFFLINE")
                || body.contains("DELETE") || body.contains("CREATE_APIKEY"));
    }

    @Test
    @Order(28)
    void test28OperationLogClear() throws Exception {
        mvc.perform(post("/admin/oplogs/clear").session(session))
                .andExpect(status().is3xxRedirection());

        String body = mvc.perform(get("/admin/oplogs").session(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("No operation logs")
                || body.contains("暂无操作日志"));
    }

    // ==================== 12. Internationalization ====================

    @Test
    @Order(29)
    void test29I18nChinese() throws Exception {
        String body = mvc.perform(get("/admin/login").param("lang", "zh"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("后台登录") || body.contains("登录"));
    }

    @Test
    @Order(30)
    void test30I18nEnglishLogin() throws Exception {
        String body = mvc.perform(get("/admin/login").param("lang", "en"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("Login") || body.contains("Username"));
    }

    @Test
    @Order(31)
    void test31I18nApiErrorEnglish() throws Exception {
        MockHttpSession enSession = new MockHttpSession();
        enSession.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, java.util.Locale.ENGLISH);
        String body = mvc.perform(get("/api/common/invoke")
                        .session(enSession)
                        .param("module", "com.nonexistent.Test")
                        .param("args", "test")
                        .param("apikey", apiKeyRaw))
                .andReturn().getResponse().getContentAsString();
        assertTrue(body.contains("Module not found"));
    }

    @Test
    @Order(32)
    void test32I18nApiErrorChinese() throws Exception {
        assertNotNull(apiKeyRaw, "apiKeyRaw must be set by test12");
        MockHttpSession zhSession = new MockHttpSession();
        zhSession.setAttribute(SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME, java.util.Locale.SIMPLIFIED_CHINESE);
        MvcResult result = mvc.perform(get("/api/common/invoke")
                        .session(zhSession)
                        .param("module", "com.nonexistent.Test")
                        .param("args", "test")
                        .param("apikey", apiKeyRaw))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("\"errorMsg\":\"") && body.contains("\"errorCode\":500"),
                "Chinese error response should contain errorMsg and errorCode");
    }

    @Test
    @Order(33)
    void test33RestoreTestFiles() {
        gitCheckout(TASKS_DIR + "/" + TEST_CLASS);
        gitCheckout("assets/dcgc/libwtf.so");
    }
}
