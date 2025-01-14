package com.easy.unidbg.sum;


import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.BaseVM;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.StringObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.VaList;
import com.github.unidbg.memory.Memory;

import java.io.File;
import java.io.IOException;

public class DcWtf extends AbstractJni {
    private final AndroidEmulator emulator = (AndroidEmulator) AndroidEmulatorBuilder.for64Bit().setProcessName("com.dachuan.news").addBackendFactory(new Unicorn2Factory(true)).build();
    private final DvmClass dvmClass;
    private final VM vm;
    private static final String processName = "com.dachuan.news";
    private static final String filePath = "assets/dcgc/libwtf.so";
    private static final String classPath = "cn/thecover/lib/common/manager/SignManager";

    public DcWtf() {
        Memory memory = this.emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23, new String[0]));
        this.vm = this.emulator.createDalvikVM();
        this.vm.setJni(this);
        this.vm.setVerbose(false);
        DalvikModule dm = this.vm.loadLibrary(new File("assets/dcgc/libwtf.so"), false);
        dm.callJNI_OnLoad(this.emulator);
        this.dvmClass = this.vm.resolveClass("cn/thecover/lib/common/manager/SignManager", new DvmClass[0]);
    }

    public String getSign(String p1, String p2, String p3) {
        String methodSign = "getSign(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
        StringObject obj = (StringObject) this.dvmClass.callStaticJniMethodObject(this.emulator, methodSign, new Object[]{new StringObject(this.vm, p1), new StringObject(this.vm, p2), new StringObject(this.vm, p3)});
        return (String) obj.getValue();
    }

    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "cn/thecover/lib/common/utils/LogShutDown->getAppSign()Ljava/lang/String;":
                return new StringObject(vm, "A262FA6ED59F60E49FD49E205F68426D");
            default:
                return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
        }
    }

    public void destroy() throws IOException {
        this.emulator.close();
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

