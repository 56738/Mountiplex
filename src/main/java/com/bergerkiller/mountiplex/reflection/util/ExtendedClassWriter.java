package com.bergerkiller.mountiplex.reflection.util;

import static net.sf.cglib.asm.Opcodes.ACC_PUBLIC;
import static net.sf.cglib.asm.Opcodes.V1_5;

import java.util.WeakHashMap;

import net.sf.cglib.asm.ClassWriter;
import net.sf.cglib.asm.Type;

/**
 * A class writer with the sole aim of extending a class and re-implementing certain methods in it
 * 
 * @param <T> type of base class
 */
public class ExtendedClassWriter<T> extends ClassWriter {
    private static final WeakHashMap<ClassLoader, GeneratorClassLoader> loaders = new WeakHashMap<ClassLoader, GeneratorClassLoader>();
    private final String name;
    private final GeneratorClassLoader loader;

    public ExtendedClassWriter(int flags, Class<T> baseClass) {
        super(flags);
        ClassLoader baseClassLoader = baseClass.getClassLoader();
        GeneratorClassLoader theLoader = loaders.get(baseClassLoader);
        if (theLoader == null) {
            theLoader = new GeneratorClassLoader(baseClassLoader);
            loaders.put(baseClassLoader, theLoader);
        }
        this.loader = theLoader;

        String postfix = loader.nextPostfix();
        String baseName = Type.getInternalName(baseClass);
        this.name = baseClass.getName() + postfix;
        this.visit(V1_5, ACC_PUBLIC, baseName + postfix, null, baseName, null);
    }

    /**
     * Gets the name of the class being generated
     * 
     * @return class name
     */
    public final String getName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    public Class<T> generate() {
        this.visitEnd();
        return (Class<T>) loader.defineClass(this.name, this.toByteArray());
    }

    public T generateInstance(Class<?>[] parameterTypes, Object[] initArgs) {
        Class<T> type = this.generate();
        try {
            return (T) type.getConstructor(parameterTypes).newInstance(initArgs);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to generate class", t);
        }
    }

    private static final class GeneratorClassLoader extends ClassLoader {
        private int index = 1;

        public GeneratorClassLoader(ClassLoader base) {
            super(base);
        }

        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }

        public String nextPostfix() {
            return "$mplgen" + Integer.toHexString(hash(++index));
        }

        // turns a sequential integer into an unique hash number
        private static int hash(int x) {
            final int prime = 2147483647;
            int hash = x ^ 0x5bf03635;
            if (hash >= prime)
                return hash;
            int residue = (int) (((long) hash * hash) % prime);
            return ((hash <= prime / 2) ? residue : prime - residue);
        }
    }
}
