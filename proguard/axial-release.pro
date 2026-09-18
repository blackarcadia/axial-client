# ============================================================
# AxialClient - Release Obfuscation
# ============================================================
#
# IMPORTANT:
# This configuration operates on the compiled release JAR.
# It does NOT modify anything under src/main/java.
# ============================================================


# ------------------------------------------------------------
# FIRST PASS SAFETY
# ------------------------------------------------------------

# Do not remove unused classes/methods yet.
-dontshrink

# Do not perform bytecode optimisations yet.
-dontoptimize

# We DO want obfuscation.
# Therefore DO NOT add -dontobfuscate.


# ------------------------------------------------------------
# PRESERVE IMPORTANT CLASS INFORMATION
# ------------------------------------------------------------

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod


# ------------------------------------------------------------
# AXIAL LAUNCHER ENTRY POINT
# ------------------------------------------------------------

# jpackage launches this class directly.
-keep public class org.example.Main {
    public static void main(java.lang.String[]);
}

# ------------------------------------------------------------
# LAUNCHER API USED BY FABRIC CLIENT
# ------------------------------------------------------------

# These classes are referenced directly by the non-obfuscated
# Fabric account system, so their class/member names must remain stable.
-keep class org.example.launcher.ClientPaths {
    *;
}

-keep class org.example.launcher.MicrosoftLoginProcess {
    *;
}


# ------------------------------------------------------------
# FABRIC ENTRYPOINT
# ------------------------------------------------------------

# Keep the Fabric mod entrypoint stable for our first pass.
-keep public class com.axial.cosmetics.AxialCosmetics {
    *;
}


# ------------------------------------------------------------
# MIXINS
# ------------------------------------------------------------

# Mixins are referenced indirectly by Fabric/Mixin configuration.
# Keep them completely intact for the initial safe build.
-keep class com.axial.cosmetics.mixin.** {
    *;
}


# ------------------------------------------------------------
# JAVA SERIALIZATION
# ------------------------------------------------------------

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# ------------------------------------------------------------
# ENUM SUPPORT
# ------------------------------------------------------------

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ------------------------------------------------------------
# NATIVE METHODS
# ------------------------------------------------------------

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}


# ------------------------------------------------------------
# ANNOTATIONS
# ------------------------------------------------------------

-keep @interface * {
    *;
}


# ------------------------------------------------------------
# INITIAL WARNING HANDLING
# ------------------------------------------------------------

# Third-party libraries included in the fat JAR can generate a
# significant number of unresolved-reference warnings.
# We will tighten this later once the first release works.
-ignorewarnings


# ------------------------------------------------------------
# MAPPING
# ------------------------------------------------------------

# Keep this PRIVATE. Do not upload it to a public GitHub release.
-printmapping ../build/obfuscation/mapping.txt
-printseeds ../build/obfuscation/seeds.txt