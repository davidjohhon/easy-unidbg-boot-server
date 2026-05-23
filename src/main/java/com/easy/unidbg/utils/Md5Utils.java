package com.easy.unidbg.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Utility for computing MD5 checksums, used for hot-reload change detection. */
public class Md5Utils {

    /** Computes the MD5 hex string for a given input string. */
    public static String getMD5(String input) {
        return getMD5(input.getBytes());
    }

    /** Computes the MD5 hex string for a byte array. */
    public static String getMD5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
