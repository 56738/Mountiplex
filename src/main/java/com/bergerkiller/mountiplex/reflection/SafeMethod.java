package com.bergerkiller.mountiplex.reflection;

import java.lang.reflect.Method;
import java.util.logging.Level;

import com.bergerkiller.mountiplex.MountiplexUtil;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;

/**
 * Wraps around the java.lang.reflect.Method class to provide an error-free
 * alternative<br>
 * Exceptions are logged, isValid can be used to check if the Method is actually
 * working
 */
public class SafeMethod<T> implements MethodAccessor<T> {

    private Method method;
    //private Class<?>[] parameterTypes;
    //private boolean isStatic = false;

    public SafeMethod(Method method) {
        if (method == null) {
            this.method = null;
            //this.parameterTypes = null;
            //this.isStatic = false;
        } else {
            this.method = method;
            //this.parameterTypes = this.method.getParameterTypes();
            //this.isStatic = Modifier.isStatic(this.method.getModifiers());
        }
    }

    public SafeMethod(String methodPath, Class<?>... parameterTypes) {
        if (methodPath == null || methodPath.isEmpty() || !methodPath.contains(".")) {
            MountiplexUtil.LOGGER.log(Level.SEVERE, "Method path contains no class: " + methodPath);
            return;
        }
        try {
            String className = MountiplexUtil.getLastBefore(methodPath, ".");
            String methodName = methodPath.substring(className.length() + 1);
            Class<?> type = Resolver.loadClass(className, false);
            load(type, methodName, parameterTypes);
        } catch (Throwable t) {
            System.out.println("Failed to load method '" + methodPath + "':");
            t.printStackTrace();
        }
    }

    public SafeMethod(Object value, String name, Class<?>... parameterTypes) {
        load(value == null ? null : value.getClass(), name, parameterTypes);
    }

    public SafeMethod(Class<?> source, String name, Class<?>... parameterTypes) {
        load(source, name, parameterTypes);
    }

    private void load(Class<?> source, String name, Class<?>... parameterTypes) {
        if (source == null) {
            new Exception("Can not load method '" + name + "' because the class is null!").printStackTrace();
            return;
        }
        // Find real name and display name
        String fixedName = Resolver.resolveMethodName(source, name, parameterTypes);
        String dispName = name.equals(fixedName) ? name : (name + "[" + fixedName + "]");
        dispName += "(";
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                dispName += ", ";
            }
            dispName += parameterTypes[i].getSimpleName();
        }
        dispName += ")";

        // try to find the method
        try {
            this.method = findRaw(source, fixedName, parameterTypes);
            if (this.method != null) {
                this.method.setAccessible(true);
                //this.isStatic = Modifier.isStatic(this.method.getModifiers());
                //this.parameterTypes = parameterTypes;
                return;
            }
        } catch (SecurityException ex) {
            new Exception("No permission to access method '" + dispName + "' in class file '" + source.getSimpleName() + "'").printStackTrace();
            return;
        }
        MountiplexUtil.LOGGER.warning("Method '" + dispName + "' could not be found in class " + source.getName());
    }

    /**
     * Gets the name of this method as declared in the Class
     *
     * @return Method name
     */
    public String getName() {
        return method.getName();
    }

    /**
     * Checks whether this method is overrided in the Class specified
     *
     * @param type to check
     * @return True of this method is overrided in the type specified, False if
     * not
     */
    public boolean isOverridedIn(Class<?> type) {
        try {
            Method m = type.getDeclaredMethod(method.getName(), method.getParameterTypes());
            return m.getDeclaringClass() != method.getDeclaringClass();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean isValid() {
        return this.method != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T invoke(Object instance, Object... args) {
        if (this.method != null) {
            try {
                return (T) this.method.invoke(instance, args);
            } catch (Throwable ex) {
                throw ReflectionUtil.fixMethodInvokeException(method, instance, args, ex);
            }
        }
        return null;
    }

    /**
     * Checks whether a certain method is available in a Class
     *
     * @param type of Class
     * @param name of the method
     * @param parameterTypes of the method
     * @return True if available, False if not
     */
    public static boolean contains(Class<?> type, String name, Class<?>... parameterTypes) {
        return findRaw(type, Resolver.resolveMethodName(type, name, parameterTypes), parameterTypes) != null;
    }

    /**
     * Tries to recursively find a method in a Class
     *
     * @param type of Class
     * @param name of the method
     * @param parameterTypes of the method
     * @return the Method, or null if not found
     */
    private static Method findRaw(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> tmp = type;
        // Try to find the method in the current and all Super Classes
        while (tmp != null) {
            try {
                return tmp.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ex) {
                tmp = tmp.getSuperclass();
            }
        }
        // Try to find the method in all implemented Interfaces
        for (Class<?> interfaceClass : type.getInterfaces()) {
            try {
                return interfaceClass.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ex) {
            }
        }
        // Nothing found
        return null;
    }

    @Override
    public boolean isMethod(Method method) {
        // Shortcut
        if (this.method.equals(method)) {
            return true;
        }

        // Check if the signatures of the two methods match up
        if (!this.method.getName().equals(method.getName())) {
            return false;
        }
        if (!this.method.getReturnType().equals(method.getReturnType())) {
            return false;
        }
        Class<?>[] args_a = this.method.getParameterTypes();
        Class<?>[] args_b = method.getParameterTypes();
        if (args_a.length != args_b.length) {
            return false;
        }
        for (int i = 0; i < args_a.length; i++) {
            if (!args_a[i].equals(args_b[i])) {
                return false;
            }
        }

        // Check that the methods are both declared in the same class, or share superclass
        Class<?> declare_a = this.method.getDeclaringClass();
        Class<?> declare_b = method.getDeclaringClass();
        return (declare_a.isAssignableFrom(declare_b) || declare_b.isAssignableFrom(declare_a));
    }
}
