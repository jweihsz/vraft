package com.vraft.core.utils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author jweihsz
 * @version 2024/2/7 15:33
 **/
public class OtherUtil {
    private OtherUtil() {}

    public static void sleep(long ms) {
        if (ms <= 0) {return;}
        try {Thread.sleep(ms);} catch (Exception ex) {}
    }

    public static int randomTimeout(
        int timeoutMs, int maxTimeout) {
        return ThreadLocalRandom.current()
            .nextInt(timeoutMs, maxTimeout);
    }

    public static void props2Obj(Properties p, Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (!mn.startsWith("set")) {continue;}
            try {
                Object arg = null;
                String key = parsePropsKey(mn);
                String prop = System.getenv(key);
                if (prop == null || prop.isEmpty()) {
                    prop = p.getProperty(key);
                }
                if (prop == null) {continue;}
                Class<?>[] pt = method.getParameterTypes();
                if (pt.length <= 0) {continue;}
                String cn = pt[0].getSimpleName();
                switch (cn) {
                    case "int":
                    case "Integer":
                        arg = Integer.parseInt(prop);
                        break;
                    case "long":
                    case "Long":
                        arg = Long.parseLong(prop);
                        break;
                    case "double":
                    case "Double":
                        arg = Double.parseDouble(prop);
                        break;
                    case "boolean":
                    case "Boolean":
                        arg = Boolean.parseBoolean(prop);
                        break;
                    case "float":
                    case "Float":
                        arg = Float.parseFloat(prop);
                        break;
                    case "String":
                        prop = prop.trim();
                        arg = prop;
                        break;
                    default:
                        continue;
                }
                method.invoke(object, arg);
            } catch (Throwable ignored) {}
        }
    }

    public static String parsePropsKey(String setMethodName) {
        final char[] cs = setMethodName.toCharArray();
        if (cs.length <= 3) {return null;}
        final StringBuilder sb = new StringBuilder();
        for (int i = 3; i < cs.length; i++) {
            if (Character.isUpperCase(cs[i])) {
                if (sb.length() != 0) {sb.append('.');}
                sb.append(Character.toLowerCase(cs[i]));
            } else {
                sb.append(cs[i]);
            }
        }
        return sb.toString();
    }

    public static void newDir(String dir) throws Exception {
        if (dir == null || dir.isEmpty()) {return;}
        final Path path = Paths.get(dir);
        Files.createDirectories(path);
    }

    public static long getSysMs() {
        final long gmt = System.nanoTime();
        return TimeUnit.NANOSECONDS.toMillis(gmt);
    }

    public static void setLong(byte[] memory, int index, long value) {
        memory[index] = (byte)(value >>> 56);
        memory[index + 1] = (byte)(value >>> 48);
        memory[index + 2] = (byte)(value >>> 40);
        memory[index + 3] = (byte)(value >>> 32);
        memory[index + 4] = (byte)(value >>> 24);
        memory[index + 5] = (byte)(value >>> 16);
        memory[index + 6] = (byte)(value >>> 8);
        memory[index + 7] = (byte)value;
    }

    public static void setLongLE(byte[] memory, int index, long value) {
        memory[index] = (byte)value;
        memory[index + 1] = (byte)(value >>> 8);
        memory[index + 2] = (byte)(value >>> 16);
        memory[index + 3] = (byte)(value >>> 24);
        memory[index + 4] = (byte)(value >>> 32);
        memory[index + 5] = (byte)(value >>> 40);
        memory[index + 6] = (byte)(value >>> 48);
        memory[index + 7] = (byte)(value >>> 56);
    }

    public static long getLong(byte[] memory, int index) {
        return ((long)memory[index] & 0xff) << 56 | ((long)memory[index + 1] & 0xff) << 48
            | ((long)memory[index + 2] & 0xff) << 40 | ((long)memory[index + 3] & 0xff) << 32
            | ((long)memory[index + 4] & 0xff) << 24 | ((long)memory[index + 5] & 0xff) << 16
            | ((long)memory[index + 6] & 0xff) << 8 | (long)memory[index + 7] & 0xff;
    }

    public static long getLongLE(byte[] memory, int index) {
        return (long)memory[index] & 0xff | ((long)memory[index + 1] & 0xff) << 8
            | ((long)memory[index + 2] & 0xff) << 16 | ((long)memory[index + 3] & 0xff) << 24
            | ((long)memory[index + 4] & 0xff) << 32 | ((long)memory[index + 5] & 0xff) << 40
            | ((long)memory[index + 6] & 0xff) << 48 | ((long)memory[index + 7] & 0xff) << 56;
    }

    public static void setInt(byte[] memory, int index, int value) {
        memory[index] = (byte)(value >>> 24);
        memory[index + 1] = (byte)(value >>> 16);
        memory[index + 2] = (byte)(value >>> 8);
        memory[index + 3] = (byte)value;
    }

    public static void setIntLE(byte[] memory, int index, int value) {
        memory[index] = (byte)value;
        memory[index + 1] = (byte)(value >>> 8);
        memory[index + 2] = (byte)(value >>> 16);
        memory[index + 3] = (byte)(value >>> 24);
    }

    public static int getInt(byte[] memory, int index) {
        return (memory[index] & 0xff) << 24 | (memory[index + 1] & 0xff) << 16 | (memory[index + 2] & 0xff) << 8
            | memory[index + 3] & 0xff;
    }

    public static int getIntLE(byte[] memory, int index) {
        return memory[index] & 0xff | (memory[index + 1] & 0xff) << 8 | (memory[index + 2] & 0xff) << 16
            | (memory[index + 3] & 0xff) << 24;
    }

    public static void setShort(byte[] memory, int index, int value) {
        memory[index] = (byte)(value >>> 8);
        memory[index + 1] = (byte)value;
    }

    public static void setShortLE(byte[] memory, int index, int value) {
        memory[index] = (byte)value;
        memory[index + 1] = (byte)(value >>> 8);
    }

    public static short getShort(byte[] memory, int index) {
        return (short)(memory[index] << 8 | memory[index + 1] & 0xFF);
    }

    public static short getShortLE(byte[] memory, int index) {
        return (short)(memory[index] & 0xff | memory[index + 1] << 8);
    }

}
