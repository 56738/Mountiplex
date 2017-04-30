package com.bergerkiller.mountiplex.conversion2.type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.bergerkiller.mountiplex.conversion2.Converter;
import com.bergerkiller.mountiplex.reflection.declarations.TypeDeclaration;
import com.bergerkiller.mountiplex.reflection.util.BoxedType;

/**
 * Special type of converter that can also perform the same conversion in reverse,
 * turning an output type into an input type.
 * 
 * @param <A> type A
 * @param <B> type B
 */
public abstract class DuplexConverter<A, B> extends Converter<A, B> {
    private DuplexConverter<B, A> reverse;

    public DuplexConverter(Class<A> typeA, Class<B> typeB) {
        super(typeA, typeB);
        this.reverse = new ReverseDuplexConverter();
    }

    public DuplexConverter(TypeDeclaration typeA, TypeDeclaration typeB) {
        super(typeA, typeB);
        this.reverse = new ReverseDuplexConverter();
    }

    private DuplexConverter(TypeDeclaration typeA, TypeDeclaration typeB, DuplexConverter<B, A> reverse) {
        super(typeA, typeB);
        this.reverse = reverse;
    }

    /**
     * Performs the conversion from input type A to output type B
     * 
     * @param value to be converted (A)
     * @return converted value (B)
     */
    @Override
    public abstract B convertInput(A value);

    /**
     * Performs the conversion from output type B to input type A.
     * 
     * @param value to be converted (B)
     * @return reverse conversed value (A)
     */
    public abstract A convertOutput(B value);

    /**
     * Performs the reversed conversion of {@link #convert(value))}
     * 
     * @param value to be converted (B)
     * @return converted value (A)
     */
    @SuppressWarnings("unchecked")
    public final A convertReverse(B value) {
        A result = null;
        if (value != null && this.output.type.isAssignableFrom(value.getClass())) {
            result = convertOutput(value);
        }
        if (result == null && this.input.type.isPrimitive()) {
            result = (A) BoxedType.getDefaultValue(this.input.type);
        }
        return result;
    }

    /**
     * Gets the reversed version of this Duplex Converter
     * 
     * @return reversed duplex converter
     */
    public final DuplexConverter<B, A> reverse() {
        return this.reverse;
    }

    /**
     * Creates a Duplex Converter by combining a Converter with its reversed version
     * 
     * @param converter for converting from A to B
     * @param reverse converter for converting from B back to A
     * @return duplex converter
     */
    public static <A, B> DuplexConverter<A, B> create(Converter<A, B> converter, Converter<B, A> reverse) {
        // Verify the converters are not null
        if (converter == null || reverse == null) {
            return null;
        }

        // Verify that the output of one converter can be assigned to the other, and vice-versa
        // This check is very important, because an out-of-control duplex converter can wreak havoc
        if (!converter.output.isInstanceOf(reverse.input)) {
            throw new RuntimeException("Converter output of " + converter.toString() +
                    " can not be assigned to the input of " + reverse.toString());
        }
        if (!reverse.output.isInstanceOf(converter.input)) {
            throw new RuntimeException("Reverse converter output of " + converter.toString() +
                    " can not be assigned to the input of " + reverse.toString());
        }

        // If the converter already is a duplex converter, do not create a new one
        if (converter instanceof DuplexConverter) {
            DuplexConverter<A, B> dupl = (DuplexConverter<A, B>) converter;
            if (dupl.reverse() == reverse) {
                return dupl;
            }
        }

        // If both converters are annotated, create a new custom type that prevents the converter in between
        if (converter instanceof AnnotatedConverter && reverse instanceof AnnotatedConverter) {
            AnnotatedConverter a = (AnnotatedConverter) converter;
            AnnotatedConverter b = (AnnotatedConverter) reverse;
            return new DuplexAnnotatedConverter<A, B>(a, b);
        }

        // Fallback: An adapter that calls the convert() method on the converters
        return new DuplexAdapter<A, B>(converter, reverse);
    }

    private final class ReverseDuplexConverter extends DuplexConverter<B, A> {

        public ReverseDuplexConverter() {
            super(DuplexConverter.this.output, DuplexConverter.this.input, DuplexConverter.this);
        }

        @Override
        public A convertInput(B value) {
            return DuplexConverter.this.convertOutput(value);
        }

        @Override
        public B convertOutput(A value) {
            return DuplexConverter.this.convertInput(value);
        }
    }

    private static final class DuplexAdapter<A, B> extends DuplexConverter<A, B> {
        private final Converter<A, B> converter;
        private final Converter<B, A> reverse;

        public DuplexAdapter(Converter<A, B> converter, Converter<B, A> reverse) {
            super(reverse.output, converter.output);
            this.converter = converter;
            this.reverse = reverse;
        }

        @Override
        public B convertInput(A value) {
            return converter.convertInput(value);
        }

        @Override
        public A convertOutput(B value) {
            return reverse.convertInput(value);
        }
    }

    private static final class DuplexAnnotatedConverter<A, B> extends DuplexConverter<A, B> {
        private final Method method;

        public DuplexAnnotatedConverter(AnnotatedConverter converter, AnnotatedConverter reverse) {
            super(reverse.output, converter.output);
            this.method = converter.method;
            super.reverse = new DuplexAnnotatedConverter<B, A>(reverse, converter, this);
        }

        private DuplexAnnotatedConverter(AnnotatedConverter converter, AnnotatedConverter reverse, DuplexAnnotatedConverter<B, A> reverseConv) {
            super(reverse.output, converter.output, null);
            this.method = converter.method;
            super.reverse = reverseConv;
        }

        @Override
        @SuppressWarnings("unchecked")
        public B convertInput(A value) {
            try {
                return (B) this.method.invoke(null, value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public A convertOutput(B value) {
            return super.reverse.convertInput(value);
        }

    }

}
