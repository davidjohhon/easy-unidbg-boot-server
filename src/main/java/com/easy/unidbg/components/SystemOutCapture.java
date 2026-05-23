package com.easy.unidbg.components;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Captures System.out output during module main() execution.
 * Uses ThreadLocal for per-thread storage and synchronization on
 * System.setOut() to ensure thread safety under concurrent requests.
 * Only one thread can capture at a time; this is acceptable because
 * module execution (the bottleneck) happens inside the lock.
 */
@Component
public class SystemOutCapture {

    private final ThreadLocal<ByteArrayOutputStream> threadLocalOutput =
            ThreadLocal.withInitial(ByteArrayOutputStream::new);

    private final ThreadLocal<PrintStream> threadLocalPrintStream =
            ThreadLocal.withInitial(() -> new PrintStream(threadLocalOutput.get()));

    private volatile PrintStream originalOut = System.out;

    /** Redirects System.out to a buffered stream, synchronized for thread safety. */
    public synchronized void startCapture() {
        threadLocalOutput.get().reset();
        originalOut = System.out;
        System.setOut(threadLocalPrintStream.get());
    }

    /** Restores the original System.out. */
    public synchronized void stopCapture() {
        System.setOut(originalOut);
    }

    /** Returns the captured output for the current thread, stripping trailing newline/CR. */
    public String getCapturedOutput() {
        String output = threadLocalOutput.get().toString();
        while (output.endsWith("\n") || output.endsWith("\r")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }
}
