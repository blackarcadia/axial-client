/** Exercises the client compatibility helper with an existing trimmed launcher runtime. */
public final class IrisShaderFallbackProbe {
    public static void main(String[] args)throws Exception{
        var helper=Class.forName("com.axial.cosmetics.client.IrisShaderCompatibility");
        boolean enabled=(Boolean)helper.getMethod("fmaEnabled").invoke(null);
        if(ModuleLayer.boot().findModule("jdk.management").isEmpty()&&enabled)
            throw new IllegalStateException("Missing diagnostics must use the non-FMA shadow path");
        System.out.println("Iris shadow compatibility passed (FMA="+enabled+")");
    }
}
