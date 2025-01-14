package com.easy.unidbg.components;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@Component
public class SystemOutCapture {

    private final ThreadLocal<ByteArrayOutputStream> threadLocalOutput = ThreadLocal.withInitial(ByteArrayOutputStream::new);
    private final ThreadLocal<PrintStream> threadLocalPrintStream = ThreadLocal.withInitial(() -> new PrintStream(threadLocalOutput.get()));

    private final PrintStream originalOut = System.out;

    public void startCapture() {
        threadLocalOutput.get().reset();
        System.setOut(threadLocalPrintStream.get());
    }

    public void stopCapture() {
        System.setOut(originalOut);
    }

    public String getCapturedOutput() {
        String output = threadLocalOutput.get().toString();
        if (output.endsWith("\n")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }
}
