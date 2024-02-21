package com.vraft.core.utils;

import java.lang.reflect.Method;
import java.util.Properties;

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

    public static void props2Obj(Properties p, Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            String mn = method.getName();
            if (!mn.startsWith("set")) {continue;}
            try {
                Object arg = null;
                String key = parsePropsKey(mn);
                String property = p.getProperty(key);
                if (property == null) {continue;}
                Class<?>[] pt = method.getParameterTypes();
                if (pt.length <= 0) {continue;}
                String cn = pt[0].getSimpleName();
                switch (cn) {
                    case "int":
                    case "Integer":
                        arg = Integer.parseInt(property);
                        break;
                    case "long":
                    case "Long":
                        arg = Long.parseLong(property);
                        break;
                    case "double":
                    case "Double":
                        arg = Double.parseDouble(property);
                        break;
                    case "boolean":
                    case "Boolean":
                        arg = Boolean.parseBoolean(property);
                        break;
                    case "float":
                    case "Float":
                        arg = Float.parseFloat(property);
                        break;
                    case "String":
                        property = property.trim();
                        arg = property;
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
}
