package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class IrisShaderCompatibilityTest {
    @Test void missingManagementModuleUsesIrisNormalShadowPath()throws Exception{
        run("--limit-modules=java.base,java.management");
    }
    @Test void fullRuntimeHonorsAnExplicitlyDisabledFmaOption()throws Exception{
        run("-XX:-UseFMA");
    }
    private void run(String option)throws Exception{
        String javaCommand=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        var process=new ProcessBuilder(javaCommand,option,"-cp",System.getProperty("java.class.path"),getClass().getName(),"probe").redirectErrorStream(true).start();
        try{assertTrue(process.waitFor(15,TimeUnit.SECONDS));String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertEquals(0,process.exitValue(),output);assertTrue(output.contains("fallback passed"),output);}
        finally{process.destroyForcibly();}
    }
    public static void main(String[] args){
        if(IrisShaderCompatibility.fmaEnabled())throw new IllegalStateException("Expected the normal non-FMA path");
        System.out.println("Iris runtime fallback passed");
    }
}
