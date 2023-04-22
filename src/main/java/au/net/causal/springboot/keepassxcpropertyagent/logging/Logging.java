package au.net.causal.springboot.keepassxcpropertyagent.logging;

public final class Logging
{
    public static void log(String message)
    {
        System.err.println("spring-boot-keepassxc-property-agent: " + message);
    }

    public static void log(String message, Throwable ex)
    {
        log(message + "- " + ex);
    }
}
