package com.easy.unidbg.controller;

import com.easy.unidbg.components.SystemOutCapture;
import com.easy.unidbg.dto.ResultDTO;
import com.easy.unidbg.service.DcWtf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class TestController {
    @Autowired
    public SystemOutCapture systemOutCapture;

    @GetMapping("/get")
    public ResultDTO<String> get() {
        String result = null;
        try {
            DcWtf dcWtf = new DcWtf();
            String str = UUID.randomUUID().toString();
            String str2 = "";
            String str3 = System.currentTimeMillis() + "";
            String sign = dcWtf.getSign(str, str2, str3);
            result = sign;
            dcWtf.destroy();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            systemOutCapture.stopCapture();
        }
        return ResultDTO.success(result);
    }
}

