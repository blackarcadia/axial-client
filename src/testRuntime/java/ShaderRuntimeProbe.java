import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

/** Run with the actual jlink image: Iris shadows and map allocation need this bean. */
public final class ShaderRuntimeProbe {
    public static void main(String[] args) {
        var diagnostics=ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        if(diagnostics==null)throw new IllegalStateException("HotSpot diagnostics are missing from the game runtime");
        var fma=diagnostics.getVMOption("UseFMA");
        System.out.println("Shader runtime check passed (HotSpot diagnostics, UseFMA="+fma.getValue()+")");
    }
}
