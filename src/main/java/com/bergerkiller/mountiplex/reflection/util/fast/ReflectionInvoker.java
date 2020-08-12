package com.bergerkiller.mountiplex.reflection.util.fast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import com.bergerkiller.mountiplex.reflection.util.BoxedType;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;

public class ReflectionInvoker<T> implements Invoker<T> {
    private static final Object[] NO_ARGS = new Object[0];
    protected final java.lang.reflect.Method m;

    protected ReflectionInvoker(java.lang.reflect.Method method) {
        this.m = method;
    }

    private RuntimeException checkInstance(Object instance) {
        // Verify the instance is of the correct type
        if (Modifier.isStatic(m.getModifiers())) {
            if (instance != null) {
                return new IllegalArgumentException("Instance should be null for static fields, but was " +
                        MPLType.getName(instance.getClass()) + " instead");
            }
        } else {
            if (instance == null) {
                return new IllegalArgumentException("Instance can not be null for member fields declared in " +
                        MPLType.getName(m.getDeclaringClass()));
            }
            if (!m.getDeclaringClass().isAssignableFrom(instance.getClass())) {
                return new IllegalArgumentException("Instance of type " + MPLType.getName(instance.getClass()) +
                        " does not contain the field declared in " + MPLType.getName(m.getDeclaringClass()));
            }
        }
        return null;
    }

    protected RuntimeException f(Object instance, Object[] args, Throwable t) {
        // Check instance
        RuntimeException iex = checkInstance(instance);
        if (iex != null) {
            return iex;
        }

        // Check argument count
        Class<?>[] paramTypes = m.getParameterTypes();
        if (paramTypes.length != args.length) {
            return new InvalidArgumentCountException("method", args.length, paramTypes.length);
        }

        // Check argument types
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].isPrimitive()) {
                if (args[i] == null) {
                    return new IllegalArgumentException("Illegal null value used for primitive " +
                            paramTypes[i].getSimpleName() + " method parameter #" + i);
                }
                Class<?> boxed = BoxedType.getBoxedType(paramTypes[i]);
                if (boxed != null && !boxed.isAssignableFrom(args[i].getClass())) {
                    return new IllegalArgumentException("Value of type " + MPLType.getName(args[i].getClass()) +
                            " can not be assigned to primitive " + paramTypes[i].getSimpleName() +
                            " method parameter #" + i);
                }
            } else if (args[i] != null) {
                if (!paramTypes[i].isAssignableFrom(args[i].getClass())) {
                    return new IllegalArgumentException("Value of type " + MPLType.getName(args[i].getClass()) +
                            " can not be assigned to " + MPLType.getName(paramTypes[i]) +
                            " method parameter #" + i);
                }
            }
        }

        // Don't know, then
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException("Failed to invoke method", t);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T invokeVA(Object instance, Object... args) {
        try {
            return (T) m.invoke(instance, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw ((RuntimeException) cause);
            } else {
                throw new RuntimeException("An error occurred in the invoked method", cause);
            }
        } catch (Throwable t) {
            throw f(instance, args, t);
        }
    }

    @Override
    public T invoke(Object instance) {
        return invokeVA(instance, NO_ARGS);
    }

    @Override
    public T invoke(Object instance, Object arg0) {
        return invokeVA(instance, new Object[] {arg0});
    }

    @Override
    public T invoke(Object instance, Object arg0, Object arg1) {
        return invokeVA(instance, new Object[] {arg0, arg1});
    }

    @Override
    public T invoke(Object instance, Object arg0, Object arg1, Object arg2) {
        return invokeVA(instance, new Object[] {arg0, arg1, arg2});
    }

    @Override
    public T invoke(Object instance, Object arg0, Object arg1, Object arg2, Object arg3) {
        return invokeVA(instance, new Object[] {arg0, arg1, arg2, arg3});
    }

    @Override
    public T invoke(Object instance, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
        return invokeVA(instance, new Object[] {arg0, arg1, arg2, arg3, arg4});
    }

    public static <T> Invoker<T> create(java.lang.reflect.Method method) {
        method.setAccessible(true);
        return new ReflectionInvoker<T>(method);
    }
}
