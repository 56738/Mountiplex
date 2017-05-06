package com.bergerkiller.mountiplex;

import static org.junit.Assert.*;

import org.junit.Test;

import com.bergerkiller.mountiplex.reflection.declarations.ClassDeclaration;
import com.bergerkiller.mountiplex.reflection.declarations.SourceDeclaration;
import com.bergerkiller.mountiplex.reflection.resolver.ClassDeclarationResolver;
import com.bergerkiller.mountiplex.reflection.resolver.Resolver;
import com.bergerkiller.mountiplex.types.TestObject;
import com.bergerkiller.mountiplex.types.TestObjectHandle;

// tests the correct working of Template elements
public class TemplateTest {

    static {
        Resolver.registerClassDeclarationResolver(new ClassDeclarationResolver() {
            @Override
            public ClassDeclaration resolveClassDeclaration(Class<?> classType) {
                if (classType.equals(TestObject.class)) {
                    String template = "package com.bergerkiller.mountiplex.types;\n" +
                                      "\n" +
                                      "public class TestObject {\n" +
                                      "    private static String staticField:a;\n"+
                                      "    private String localField:b;\n" +
                                      "    private (String) int intConvField:c;\n" +
                                      "    \n" +
                                      "    private int testFunc:d(int k, int l);\n" +
                                      "    private (String) int testConvFunc1:e(int k, int l);\n" +
                                      "    private int testConvFunc2:f((String) int k, (String) int l);\n" +
                                      "    private static (long) int testing2:g(int a, (String) int b);\n" +
                                      "}\n";

                    return SourceDeclaration.parse(template).classes[0];
                }
                return null;
            }
        });
    }

    @Test
    public void testTemplate() {
        TestObject object = new TestObject();
        assertEquals("static_test", TestObjectHandle.CONSTANT);
        assertEquals("static_test", TestObjectHandle.T.staticField.get());
        assertEquals("local_test", TestObjectHandle.T.localField.get(object));
        TestObjectHandle.T.staticField.set("static_changed");
        TestObjectHandle.T.localField.set(object, "local_changed");
        assertEquals("static_changed", TestObjectHandle.T.staticField.get());
        assertEquals("local_changed", TestObjectHandle.T.localField.get(object));
        assertEquals("12", TestObjectHandle.T.intConvField.get(object));
        assertEquals(57, TestObjectHandle.T.testFunc.invoke(object, 12, 45).intValue());
        assertEquals("77", TestObjectHandle.T.testConvFunc1.invoke(object, 43, 33));
        assertEquals(68, TestObjectHandle.T.testConvFunc2.invoke(object, "22", "44").intValue());
        assertEquals(Long.valueOf(288), TestObjectHandle.T.testing2.invoke(12, "24"));
    }

}
