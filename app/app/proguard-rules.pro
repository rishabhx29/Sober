# Sober release rules. Room, Hilt, Compose, WorkManager, Lottie and Coil ship
# their own consumer rules; only the gaps this app creates are covered here.

# Crash reports are only useful if a stack trace maps back to the source.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preferences and the rule catalog persist enum constants by name and read them
# back with valueOf. Keeping the constant fields also stops R8's enum unboxing,
# which would otherwise rewrite the lookup into integer ordinals: a build that
# cannot be smoke-tested on a device should not gamble on that rewrite.
-keepclassmembers enum com.sober.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
