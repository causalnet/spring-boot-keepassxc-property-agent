package au.net.causal.springboot.keepassxcpropertyagent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;

public class KeePassXCPropertyAgent
{
    private static AgentConfiguration config;

    public static void premain(String agentArgs, Instrumentation inst)
    {
        config = AgentConfiguration.parse(agentArgs);
        KeePassXCPropertyAgent agent = new KeePassXCPropertyAgent();
        agent.run(inst);
    }

    public void run(Instrumentation inst)
    {
        inst.addTransformer(new ClassFileTransformer()
        {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException
            {
                try
                {
                    if ("org/springframework/boot/env/EnvironmentPostProcessorApplicationListener".equals(className))
                    {
                        ClassPool classPool = new ClassPool(null);
                        classPool.appendClassPath(new LoaderClassPath(loader));

                        CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

                        CtMethod onApplicationEventMethod = ctClass.getDeclaredMethod("onApplicationEvent");
                        transformOnApplicationEventMethod(onApplicationEventMethod);

                        classfileBuffer = ctClass.toBytecode();
                        ctClass.detach();
                    }
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
                return classfileBuffer;
            }
        });
    }

    private void transformOnApplicationEventMethod(CtMethod m)
    throws CannotCompileException
    {
        m.insertBefore("""
            if ($1 instanceof org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent)
            {
                org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent event = (org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent)$1;
                org.springframework.core.env.MutablePropertySources sources = event.getEnvironment().getPropertySources();
                java.util.Map map = new java.util.LinkedHashMap();
                au.net.causal.springboot.keepassxcpropertyagent.KeePassXCPropertyAgent.doKeepass(map);
                if (sources.contains(org.springframework.core.env.CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME))
                {
                    sources.addAfter(org.springframework.core.env.CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME, new org.springframework.core.env.MapPropertySource("keepassxc", map));
                }
                else
                {
                    sources.addFirst(new org.springframework.core.env.MapPropertySource("keepassxc", map));
                }
            }
        """);
    }

    public static void doKeepass(Map<String, Object> map)
    {
        KeepassXCPropertyReader reader = new KeepassXCPropertyReader();

        try
        {
            for (String entryUri : config.getEntryUris())
            {
                reader.readProperties(entryUri, map);
            }
        }
        catch (IOException e)
        {
            System.err.println("Failed to read values from KeepassXC: " + e);
        }
    }

}
