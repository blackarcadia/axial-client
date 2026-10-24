package com.axial.cosmetics.client;

import java.lang.management.ManagementFactory;

/** Optional Iris optimization: older launcher images do not contain jdk.management. */
public final class IrisShaderCompatibility {
    private IrisShaderCompatibility(){}
    public static boolean fmaEnabled(){
        try{
            Class<?> diagnostics=Class.forName("com.sun.management.HotSpotDiagnosticMXBean",false,ClassLoader.getPlatformClassLoader());
            Object bean=ManagementFactory.class.getMethod("getPlatformMXBean",Class.class).invoke(null,diagnostics);
            if(bean==null)return false;
            Object option=diagnostics.getMethod("getVMOption",String.class).invoke(bean,"UseFMA");
            return Boolean.parseBoolean((String)option.getClass().getMethod("getValue").invoke(option));
        }catch(ReflectiveOperationException|LinkageError|RuntimeException unavailable){
            // Iris already has a normal multiply/add path when this optimization is false.
            return false;
        }
    }
}
