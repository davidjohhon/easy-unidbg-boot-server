package com.easy.unidbg.service;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class DcWtf extends AbstractJni {

    private final AndroidEmulator emulator;

    private final DvmClass dvmClass;
    private final VM vm;

    private static final String processName = "com.dachuan.news";
    private static final String filePath = "assets/dcgc/libwtf.so";
    private static final String classPath = "cn/thecover/lib/common/manager/SignManager";

    public DcWtf() {
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName(processName).addBackendFactory(new Unicorn2Factory(true)).build(); // 创建模拟器实例，要模拟32位或者64位，在这里区分
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        vm = emulator.createDalvikVM();
        vm.setJni(this);
        vm.setVerbose(false);
        DalvikModule dm = vm.loadLibrary(new File(filePath), false);
        dm.callJNI_OnLoad(emulator);
        dvmClass = vm.resolveClass(classPath);
    }


    public String getSign(String p1, String p2, String p3) {
        String methodSign = "getSign(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        StringObject obj = dvmClass.callStaticJniMethodObject(emulator, methodSign, new StringObject(vm, p1), new StringObject(vm, p2), new StringObject(vm, p3));
        return obj.getValue();
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "cn/thecover/lib/common/utils/LogShutDown->getAppSign()Ljava/lang/String;":
                return new StringObject(vm, "A262FA6ED59F60E49FD49E205F68426D");
        }

        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }


    public void destroy() throws IOException {
        emulator.close();
    }

    public static void main(String[] args) throws Exception {
        DcWtf dcWtf = new DcWtf();
        String str = "552ff1b3-0b9c-4bef-b7eb-b121119067f6";
        String str2 = "";
        String str3 = "1736135309751";
        String sign = dcWtf.getSign(str, str2, str3);
        System.out.println("sign=" + sign);
        dcWtf.destroy();
    }
}
