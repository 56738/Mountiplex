package com.bergerkiller.mountiplex.types;

import com.bergerkiller.mountiplex.reflection.declarations.ClassDeclaration;
import com.bergerkiller.mountiplex.reflection.declarations.SourceDeclaration;
import com.bergerkiller.mountiplex.reflection.resolver.ClassDeclarationResolver;
import com.bergerkiller.mountiplex.reflection.resolver.FieldNameResolver;
import com.bergerkiller.mountiplex.reflection.resolver.MethodNameResolver;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;

public class TestClassDeclarationResolver implements ClassDeclarationResolver, MethodNameResolver, FieldNameResolver {
    public static final TestClassDeclarationResolver INSTANCE = new TestClassDeclarationResolver();
    static {
        Resolver.registerClassDeclarationResolver(INSTANCE);
        Resolver.registerMethodResolver(INSTANCE);
        Resolver.registerFieldResolver(INSTANCE);
        INSTANCE.parse();
    }

    private SourceDeclaration source;

    public static void bootstrap() {
    }

    public TestClassDeclarationResolver() {
    }

    public void parse() {
        String template = "" +
                "#resolver com.bergerkiller.mountiplex.types.TestClassDeclarationResolver.INSTANCE\n" +
                "#bootstrap {\n" +
                "com.bergerkiller.mountiplex.types.BootstrapState.CALLED_ROOT = true;\n" +
                "}\n" +
                "\n" +
                "package com.bergerkiller.mountiplex.types;\n" +
                "\n" +
                "public class TestObject {\n" +
                "    #bootstrap com.bergerkiller.mountiplex.types.BootstrapState.CALLED_TESTOBJECT = true;\n" +
                "    \n" +
                "    private static String staticField:a;\n" +
                "    private static final String staticFinalField:a_f;\n" +
                "    private String localField:b;\n" +
                "    private final String localFinalField:b_f;\n" +
                "    private (String) int intConvField:c;\n" +
                "    public final (List<String>) List<Integer> testRawField;\n" +
                "    public optional String unusedField:###;\n" +
                "    public readonly final (UniqueType) OneWayConvertableType oneWay;\n" +
                "    public long[][] multiArr;\n" +
                "    \n" +
                "    private int testFunc:d(int k, int l);\n" +
                "    private (String) int testConvFunc1:e(int k, int l);\n" +
                "    private int testConvFunc2:f((String) int k, (String) int l);\n" +
                "    private static (long) int testing2:g(int a, (String) int b);\n" +
                "    public int defaultInterfaceMethod();\n" +
                "    public int inheritedClassMethod();\n" +
                "    public int testGeneratedWithArg(int parameter) {\n" +
                "        return 4 + parameter * 20;\n" +
                "    }\n" +
                "    public optional int testGenerated() {\n" +
                "        return 621;\n" +
                "    }\n" +
                "    public static int staticGenerated(int parameter) {\n" +
                "        return 512 + parameter;\n" +
                "    }\n" +
                "}\n" +
                "package com.bergerkiller.mountiplex.types;\n" +
                "\n" +
                "class PrivateTestObject {\n" +
                "    #bootstrap com.bergerkiller.mountiplex.types.BootstrapState.CALLED_PRIVATETESTOBJECT = true;\n" +
                "    \n" +
                "    public String field;\n" +
                "    public String method();\n" +
                "}\n" +
                "package com.bergerkiller.mountiplex.types;\n" +
                "\n" +
                "public class SpeedTestObject {\n" +
                "    private int i;\n" +
                "    private double d;\n" +
                "    private String s;\n" +
                "    public final int getIMethod();\n" +
                "    public final void setIMethod(int value);\n" +
                "    public final void setSMethod(String value);\n" +
                "    public final String getSMethod();\n" +
                "    public void setLocation(double x, double y, double z, float yaw, float pitch);\n" +
                "    public int lotsOfArgs(int a, int b, int c, int d, int e, int f, int g);\n" +
                "}\n" +
                "package com.bergerkiller.mountiplex.types;\n" +
                "\n" +
                "public class RenameTestObject {\n" +
                "    public int someTestPublicField:originalTestPublicField;\n" +
                "    private int someTestPrivateField:originalTestPrivateField;\n" +
                "    public final int someTestFinalField:originalTestFinalField;\n" +
                "    public int someTestPublicMethod:originalTestPublicMethod();\n" +
                "    private int someTestPrivateMethod:originalTestPrivateMethod();\n" +
                "    \n" +
                "    public int generatedGetPublicFieldUsingRequirements() {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject public int someTestPublicField:originalTestPublicField;\n" +
                "        return instance#someTestPublicField;\n" +
                "    }\n" +
                "    public int generatedGetPrivateFieldUsingRequirements() {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject private int someTestPrivateField:originalTestPrivateField;\n" +
                "        return instance#someTestPrivateField;\n" +
                "    }\n" +
                "    public int generatedGetFinalFieldUsingRequirements() {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject public final int someTestFinalField:originalTestFinalField;\n" +
                "        return instance#someTestFinalField;\n" +
                "    }\n" +
                "    public void generatedSetPublicFieldUsingRequirements(int value) {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject public int someTestPublicField:originalTestPublicField;\n" +
                "        instance#someTestPublicField = value;\n" +
                "    }\n" +
                "    public void generatedSetPrivateFieldUsingRequirements(int value) {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject private int someTestPrivateField:originalTestPrivateField;\n" +
                "        instance#someTestPrivateField = value;\n" +
                "    }\n" +
                "    public void generatedSetFinalFieldUsingRequirements(int value) {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject public final int someTestFinalField:originalTestFinalField;\n" +
                "        instance#someTestFinalField = value;\n" +
                "    }\n" +
                "    public int generatedCallMethodUsingRequirements() {\n" +
                "        #require com.bergerkiller.mountiplex.types.RenameTestObject private int someTestPrivateMethod:originalTestPrivateMethod();\n" +
                "        return instance#someTestPrivateMethod();\n" +
                "    }\n" +
                "}\n";
        long t1 = System.nanoTime();
        this.source = SourceDeclaration.parse(template);
        long t2 = System.nanoTime();
        System.out.println("Parse duration: " + ((double) (t2-t1)/1000000) + " ms");
    }

    @Override
    public ClassDeclaration resolveClassDeclaration(String classPath, Class<?> classType) {
        if (this.source == null) {
            return null;
        } else if (classPath.equals("com.bergerkiller.mountiplex.types.TestObject")) {
            return source.classes[0];
        } else if (classPath.equals("com.bergerkiller.mountiplex.types.PrivateTestObject")) {
            return source.classes[1];
        } else if (classPath.equals("com.bergerkiller.mountiplex.types.SpeedTestObject")) {
            return source.classes[2];
        } else if (classPath.equals("com.bergerkiller.mountiplex.types.RenameTestObject")) {
            return source.classes[3];
        }
        return null;
    }

    @Override
    public String resolveFieldName(Class<?> declaredClass, String fieldName) {
        if (fieldName.contains("original") && declaredClass.equals(RenameTestObject.class)) {
            return fieldName.replace("originalT", "t");
        }

        return fieldName;
    }

    @Override
    public String resolveMethodName(Class<?> declaredClass, String methodName, Class<?>[] parameterTypes) {
        if (methodName.contains("original") && declaredClass.equals(RenameTestObject.class)) {
            return methodName.replace("originalT", "t");
        }

        return methodName;
    }
}
