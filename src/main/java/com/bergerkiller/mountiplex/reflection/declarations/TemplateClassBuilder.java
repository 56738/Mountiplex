package com.bergerkiller.mountiplex.reflection.declarations;

import static org.objectweb.asm.Opcodes.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.logging.Level;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import com.bergerkiller.mountiplex.MountiplexUtil;
import com.bergerkiller.mountiplex.reflection.declarations.Template.Handle;
import com.bergerkiller.mountiplex.reflection.resolver.ClassDeclarationResolver;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.reflection.util.ExtendedClassWriter;
import com.bergerkiller.mountiplex.reflection.util.asm.MPLType;
import com.bergerkiller.mountiplex.reflection.util.fast.Invoker;

/**
 * Reads the abstract class information of a Template {@link Template.Class Class} type and generates an appropriate
 * implementation of the abstract methods. Since the Class is a singleton, this builder produces an instance
 * of the Class rather than the Class object itself. Of this instance, all the fields are initialized.
 */
public class TemplateClassBuilder<C extends Template.Class<H>, H extends Handle> {
    public final java.lang.Class<C> classType;
    public final java.lang.Class<H> handleType;
    public final java.lang.Class<?> instanceType;
    public final String instanceClassPath;
    public final boolean isOptional;
    public final ClassDeclaration classDec;

    @SuppressWarnings("unchecked")
    public TemplateClassBuilder(java.lang.Class<C> classType, ClassDeclarationResolver classDeclarationResolver) {
        this.classType = classType;

        // Identify the Handle type used alongside this Class
        // If this is a top-level Class that is not inside a Handle, it creates
        // dummy Handle classes without any special implementation inside.
        java.lang.Class<?> handleType = this.classType.getDeclaringClass();
        if (handleType != null && Handle.class.isAssignableFrom(handleType)) {
            this.handleType = (java.lang.Class<H>) handleType;
        } else {
            this.handleType = (java.lang.Class<H>) Handle.class;
        }

        // Identify what instance type is represented by this Class
        // If we can't deduce it, assume Object for maximum compatibility
        this.instanceClassPath = recurseFindAnnotationValue(classType, Template.InstanceType.class,
                Template.InstanceType::value, "java.lang.Object");

        // Identify whether the Optional annotation is specified
        this.isOptional = recurseFindAnnotationValue(classType, Template.Optional.class,
                (a) -> Boolean.TRUE, Boolean.FALSE);

        // Resolve the Class Path to an actual Class object
        // If not found, and the Class is not meant to be optional, log an error
        this.instanceType = Resolver.loadClass(this.instanceClassPath, false);
        if (this.instanceType == null && !this.isOptional) {
            MountiplexUtil.LOGGER.log(Level.SEVERE, "Class " + this.instanceClassPath + " not found; Template '" +
                    handleType.getSimpleName() + " not initialized.");
        }

        // If found, also resolve the class declaration to use
        // If one is expected but not found, log an error
        if (this.instanceType != null && this.instanceType != Object.class && classDeclarationResolver != null) {
            this.classDec = classDeclarationResolver.resolveClassDeclaration(this.instanceClassPath, this.instanceType);
            if (this.classDec == null) {
                MountiplexUtil.LOGGER.log(Level.SEVERE, "Class Declaration for " + this.instanceClassPath + " not found");
            }
        } else {
            this.classDec = null;
        }
    }

    /**
     * Builds the most appropriate {@link Template.Class Class} instance given the provided information
     * 
     * @return Class
     */
    public C build() {
        // If the class type is abstract then we must extend it, and implement whatever we can
        if (Modifier.isAbstract(this.classType.getModifiers())) {
            return buildExtend();
        }

        // By default, just call the default constructor on the Class type
        return buildDefault();
    }

    /**
     * Builds the Class object by using the original class, no new class is generated
     * 
     * @return constructed Class
     */
    private C buildDefault() {
        try {
            C generatedClass = this.classType.newInstance();
            generatedClass.init(this);
            return generatedClass;
        } catch (Throwable t) {
            throw MountiplexUtil.uncheckedRethrow(t);
        }
    }

    /**
     * Extends the Class type specified and implements the missing methods
     * 
     * @return constructed Class
     */
    private C buildExtend() {
        // Set up the class writer for the implementation of the class type
        ExtendedClassWriter<C> cw = ExtendedClassWriter.builder(this.classType)
                .setFlags(ClassWriter.COMPUTE_MAXS)
                .setAccess(ACC_FINAL)
                .setPostfix("$impl").build();

        MethodVisitor mv;

        // Add default empty constructor
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, MPLType.getInternalName(this.classType), "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Override getSelfClassType() and return getClass().getSuperClass() instead
        mv = cw.visitMethod(ACC_PUBLIC, "getSelfClassType", "()Ljava/lang/Class;", "()Ljava/lang/Class<*>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSuperclass", "()Ljava/lang/Class;", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        int fieldNameIdx = 1; // Makes sure field names are unique

        ClassResolver resolver = null;
        for (Method method : this.classType.getDeclaredMethods()) {
            Template.Generated generatedAnnot = method.getAnnotation(Template.Generated.class);
            if (generatedAnnot == null) {
                continue; // Skip
            }

            // First-time initialization of ClassResolver used while loading declarations
            if (resolver == null) {
                resolver = new ClassResolver();
                resolver.setDeclaredClass(this.instanceType, this.instanceClassPath);
                resolver.setAllVariables(Resolver.resolveClassVariables(this.instanceClassPath, this.instanceType));

                //TODO: Import declarations on recursive Class level
            }

            // Use the resolver to decode the declaration in the annotation
            Declaration parsedDeclaration = Declaration.parseDeclaration(resolver, generatedAnnot.value());
            if (parsedDeclaration == null || !parsedDeclaration.isValid()) {
                MountiplexUtil.LOGGER.warning("Declaration for method " + method.getName() +
                        " could not be parsed: " + generatedAnnot.value());
                cw.visitMethodUnsupported(method, "Declaration for this generated method could not be parsed");
                continue;
            } else if (!parsedDeclaration.isResolved()) {
                MountiplexUtil.LOGGER.warning("Declaration for method " + method.getName() +
                        " could not be resolved: " + parsedDeclaration);
                cw.visitMethodUnsupported(method, "Declaration for this generated method could not be resolved (missing types)");
                continue;
            }

            // Discover the real method, if possible
            Declaration declaration = parsedDeclaration.discover();
            if (declaration == null) {
                cw.visitMethodUnsupported(method, "Failed to find: " + parsedDeclaration);
                continue;
            }
            if (declaration instanceof FieldDeclaration) {
                cw.visitMethodUnsupported(method, "Field getters/setters not implemented yet");
                continue;
            }
            if (declaration instanceof MethodDeclaration) {
                MethodDeclaration methodDec = (MethodDeclaration) declaration;

                if (!methodDec.modifiers.isStatic()) {
                    // public int() declaration, while only public static int() is supported
                    cw.visitMethodUnsupported(method, "Local methods cannot be called statically");
                    continue;
                }
                if (methodDec.body == null && methodDec.method == null) {
                    // not a generated method, but the method could not be found
                    cw.visitMethodUnsupported(method, "Static method '" + methodDec.name.toString() + "' was not found");
                    continue;
                }

                //TODO: Conversion. For now just fail the method body when used
                boolean hasConversion = methodDec.returnType.cast != null;
                if (!hasConversion) {
                    for (ParameterDeclaration param : methodDec.parameters.parameters) {
                        if (param.type.cast != null) {
                            hasConversion = true;
                            break;
                        }
                    }
                }
                if (hasConversion) {
                    cw.visitMethodUnsupported(method, "Conversion of parameters/return type is not supported yet");
                    continue;
                }
                

                if (methodDec.body == null && Modifier.isPublic(methodDec.method.getModifiers()) && Resolver.isPublic(this.instanceType)) {
                    mv = cw.visitMethod(ACC_PUBLIC, method.getName(), MPLType.getMethodDescriptor(method), null, null);
                    mv.visitCode();

                    // static method can be called from within the method body just fine
                    // load all the parameters onto the stack
                    int varIdx = 1;
                    for (Class<?> param : method.getParameterTypes()) {
                        mv.visitVarInsn(MPLType.getOpcode(param, ILOAD), varIdx++);
                    }

                    // call static method directly and proxy-return the return value
                    mv.visitMethodInsn(INVOKESTATIC, MPLType.getInternalName(this.instanceType), method.getName(),
                            MPLType.getMethodDescriptor(method), false);
                    mv.visitInsn(MPLType.getOpcode(method.getReturnType(), IRETURN));
                    mv.visitMaxs(varIdx, varIdx);
                    mv.visitEnd();
                } else {
                    // we need to use reflection or runtime-code-gen to call this method.
                    // for this we add a static Invoker field to handle initialization/execution for us
                    // the invoker is initialized in such a way that, when first called, it will update the field storing it
                    String invoker_name = methodDec.name.real() + "_invoker_" + (fieldNameIdx++);
                    cw.visitStaticInvokerField(invoker_name, methodDec);

                    // Add a method body that calls the invoker's invoke() method
                    mv = cw.visitMethod(ACC_PUBLIC, method.getName(), MPLType.getMethodDescriptor(method), null, null);
                    mv.visitCode();

                    // Store the static invoker field instance onto the stack
                    mv.visitFieldInsn(GETSTATIC, cw.getInternalName(), invoker_name, MPLType.getDescriptor(Invoker.class));

                    // Null instance onto the stack, because static
                    mv.visitInsn(ACONST_NULL);

                    // Invoke the right virtual method of the Invoker interface with the input parameters
                    if (methodDec.parameters.parameters.length > 5) {
                        // More than five, must pack parameters into an array first
                        ExtendedClassWriter.visitPushInt(mv, methodDec.parameters.parameters.length);
                        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

                        int varIdx = 1;
                        for (ParameterDeclaration param : methodDec.parameters.parameters) {
                            mv.visitInsn(DUP);
                            ExtendedClassWriter.visitPushInt(mv, varIdx-1);
                            mv.visitVarInsn(MPLType.getOpcode(param.type.type, ILOAD), varIdx);
                            ExtendedClassWriter.visitBoxVariable(mv, param.type.type);
                            mv.visitInsn(AASTORE);
                            varIdx++;
                        }

                        // invokeVA
                        mv.visitMethodInsn(INVOKEINTERFACE, MPLType.getInternalName(Invoker.class), "invokeVA",
                                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", true);

                    } else {
                        // Load parameters onto the stack
                        int varIdx = 1;
                        for (ParameterDeclaration param : methodDec.parameters.parameters) {
                            mv.visitVarInsn(MPLType.getOpcode(param.type.type, ILOAD), (varIdx++));
                            ExtendedClassWriter.visitBoxVariable(mv, param.type.type);
                        }

                        // invoke(null, [0], [1], [2], [3], [4])
                        StringBuilder descriptor = new StringBuilder();
                        descriptor.append('(');
                        for (int i = 0; i <= methodDec.parameters.parameters.length; i++) {
                            descriptor.append("Ljava/lang/Object;");
                        }
                        descriptor.append(")Ljava/lang/Object;");

                        mv.visitMethodInsn(INVOKEINTERFACE, MPLType.getInternalName(Invoker.class), "invoke",
                                descriptor.toString(), true);
                    }

                    if (method.getReturnType() == void.class) {
                        // No return value. Pop (ignore) the returned Object and return instantly.
                        mv.visitInsn(POP);
                        mv.visitInsn(RETURN);
                    } else {
                        // Return type is Object, if not already Object, convert to the right return value
                        // We might need to unbox values such as Integer/Long/etc.
                        ExtendedClassWriter.visitUnboxVariable(mv, method.getReturnType());
                        mv.visitInsn(MPLType.getOpcode(method.getReturnType(), IRETURN));
                    }

                    //TODO: Compute these, for now it requires the auto-compute flag to be set
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                continue;
            }

            cw.visitMethodUnsupported(method, "Not supported yet");
        }

        // Generate the final Class instance and further initialize it
        C generatedClass = cw.generateInstance(new Class[0], new Object[0]);
        generatedClass.init(this);
        return generatedClass;
    }

    /**
     * Recursively looks up the Class hierarchy to find the first annotation of a given type.
     * If found, the property of the annotation as specified is returned. If not found, the
     * default value is returned instead.
     * 
     * @param type The Class type from which to recursively look for the annotation
     * @param annotationClass Annotation class type to find
     * @param method Method of the annotation class type to call
     * @param defaultValue Default value to return if the annotation is not found
     * @return value
     */
    private static <A extends Annotation, V> V recurseFindAnnotationValue(java.lang.Class<?> type, java.lang.Class<A> annotationClass, Function<A, V> method, V defaultValue) {
        java.lang.Class<?> currentType = type;
        while (currentType != null) {
            A annotation = currentType.getAnnotation(annotationClass);
            if (annotation != null) {
                return method.apply(annotation);
            } else {
                currentType = currentType.getDeclaringClass();
            }
        }
        return defaultValue;
    }
}
