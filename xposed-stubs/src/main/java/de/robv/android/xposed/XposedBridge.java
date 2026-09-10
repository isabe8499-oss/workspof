package de.robv.android.xposed;

import java.lang.reflect.Member;
import java.util.Set;

public final class XposedBridge {
    public static void log(String text) {}
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) { throw new UnsupportedOperationException("Compile-only stub"); }
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) { throw new UnsupportedOperationException("Compile-only stub"); }
}
