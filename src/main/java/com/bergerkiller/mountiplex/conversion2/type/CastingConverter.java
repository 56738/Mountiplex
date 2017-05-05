package com.bergerkiller.mountiplex.conversion2.type;

import com.bergerkiller.mountiplex.conversion2.Converter;
import com.bergerkiller.mountiplex.reflection.declarations.TypeDeclaration;

/**
 * Attempts to cast the input value to the requested output value type
 *
 * @param <T> output type
 */
public class CastingConverter<T> extends Converter<Object, T> {

    public CastingConverter(Class<?> input, Class<?> output) {
        super(input, output);
    }

    public CastingConverter(TypeDeclaration input, TypeDeclaration output) {
        super(input, output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T convertInput(Object value) {
        if (output.isAssignableFrom(value)) {
            return (T) value;
        }
        return null;
    }

}
