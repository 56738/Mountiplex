package com.bergerkiller.mountiplex.reflection.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.bergerkiller.mountiplex.MountiplexUtil;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;
import com.bergerkiller.mountiplex.reflection.util.fast.GeneratedCodeInvoker;
import com.bergerkiller.mountiplex.reflection.util.fast.GeneratedConstructor;
import com.bergerkiller.mountiplex.reflection.util.fast.GeneratedMethodInvoker;

/**
 * ClassLoader used to generate new classes at runtime in various areas
 * of the library. Class loaders are re-used for the different base class
 * loaders in use. The generator is written in a way that class name
 * resolving works properly.
 */
public class GeneratorClassLoader extends ClassLoader {
    private static final Map<String, Class<?>> staticClasses = new HashMap<String, Class<?>>();
    private static WeakHashMap<ClassLoader, GeneratorClassLoader> loaders = new WeakHashMap<ClassLoader, GeneratorClassLoader>();
    private static final java.lang.reflect.Method defineClassMethod;

    private static void registerStaticClass(Class<?> type) {
        staticClasses.put(type.getName(), type);
    }

    static {
        MountiplexUtil.registerUnloader(new Runnable() {
            @Override
            public void run() {
                loaders = new WeakHashMap<ClassLoader, GeneratorClassLoader>(0);
            }
        });

        // Note: wrapped in a Method invoke() to prevent remappers from altering how
        // getDeclaredMethod() works. A class remapper altering this can seriously
        // break it.
        try {
            Method tmp = Class.class.getMethod(String.join("", "get", "Declared", "Method"), String.class, Class[].class);
            defineClassMethod = (Method) tmp.invoke(ClassLoader.class, String.join("", "define", "Class"),
                    new Class[] { String.class, byte[].class, int.class, int.class });
        } catch (Throwable t) {
            throw MountiplexUtil.uncheckedRethrow(t);
        }

        // These classes are often used to generate method bodies at runtime.
        // However, a Class Loader might be specified that has no access to these
        // class types. We register these special types to avoid that problem.
        registerStaticClass(GeneratedMethodInvoker.class);
        registerStaticClass(GeneratedCodeInvoker.class);
        registerStaticClass(GeneratedConstructor.class);
    }

    /**
     * Gets the class loader used to generate classes.
     * This method is thread-safe.
     * 
     * @param baseClassLoader The base class loader used by the caller
     * @return generator class loader
     */
    public static GeneratorClassLoader get(ClassLoader baseClassLoader) {
        synchronized (loaders) {
            return loaders.computeIfAbsent(baseClassLoader, GeneratorClassLoader::create);
        }
    }

    private static GeneratorClassLoader create(ClassLoader base) {
        if (base instanceof GeneratorClassLoader) {
            return (GeneratorClassLoader) base;
        } else {
            return new GeneratorClassLoader(base);
        }
    }

    private GeneratorClassLoader(ClassLoader base) {
        super(base);
    }

    private Class<?> superFindClass(String name) {
        Class<?> loaded = this.findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        try {
            return MPLType.getClassByName(name, false, this.getParent());
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    /*
     * We have to avoid asking class loaders for certain class types, because
     * it causes problems. For example, the generated invoker classes are provided
     * by this library, but a class loader might be used with no access to it.
     */
    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> foundInStaticClasses = staticClasses.get(name);
        if (foundInStaticClasses != null) {
            return foundInStaticClasses;
        } else {
            return super.loadClass(name, resolve);
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        // Ask the other GeneratorClassLoaders what this Class is
        // This fixes a problem that it cannot find classes generated by other base classloaders
        synchronized (loaders) {
            // Ask class loader of mountiplex first, as this is the most likely case
            // For example, when accessing mountiplex types extended at runtime
            Class<?> loaded;
            GeneratorClassLoader mountiplexLoader = loaders.computeIfAbsent(ExtendedClassWriter.class.getClassLoader(), GeneratorClassLoader::create);
            if (mountiplexLoader != this && ((loaded = mountiplexLoader.superFindClass(name)) != null)) {
                return loaded;
            }

            // Try all other loaders we have used to generate classes
            for (GeneratorClassLoader otherLoader : loaders.values()) {
                if (otherLoader != mountiplexLoader && otherLoader != this && ((loaded = otherLoader.superFindClass(name)) != null)) {
                    return loaded;
                }
            }
        }

        // Failed to find it
        throw new ClassNotFoundException(name);
    }

    /**
     * Defines a new class name using the bytecode specified
     * 
     * @param name Name of the class to generate
     * @param b Bytecode for the Class
     * @return defined class
     */
    public Class<?> createClassFromBytecode(String name, byte[] b) {
        try {
            return (Class<?>) defineClassMethod.invoke(this, name, b, Integer.valueOf(0), Integer.valueOf(b.length));
        } catch (Throwable t) {
            throw MountiplexUtil.uncheckedRethrow(t);
        }
    }
}
