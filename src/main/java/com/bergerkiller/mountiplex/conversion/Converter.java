package com.bergerkiller.mountiplex.conversion;

import com.bergerkiller.mountiplex.conversion.type.ArrayElementConverter;


/**
 * Represents a data type converter
 *
 * @param <T> output type
 */
public abstract class Converter<T> {
    private final Class<T> _outputType;

    @SuppressWarnings("unchecked")
    public Converter(Class<?> outputType) {
        this._outputType = (Class<T>) outputType;
    }

    /**
     * Converts the input value to the output type
     *
     * @param value to convert
     * @param def value to return when conversion fails
     * @return converted output type
     */
    public abstract T convert(Object value, T def);

    /**
     * Converts the input value to the output type<br>
     * If conversion fails, null is returned instead
     *
     * @param value to convert
     * @return converted output type
     */
    public T convert(Object value) {
        return convert(value, null);
    }

    /**
     * Gets the Class type returned by convert
     *
     * @return output Class type
     */
    public Class<T> getOutputType() {
        return this._outputType;
    }

    /**
     * Checks whether the returned output value can be casted to another
     * type<br>
     * This should only be supported when the returned type can be an extension
     * of the output type<br>
     * Typically, interfaces do not support this, as they can conflict with
     * other converters<br><br>
     * <p/>
     * <b>Do not give a converter for multipurpose types this property! For
     * example, an Object converter would end up being used for all cases,
     * rendering isCastingSupported unusable globally.</b>
     *
     * @return True if casting is supported, False if not
     */
    public abstract boolean isCastingSupported();

    /**
     * Checks whether this converter supports registering in the Conversion
     * look-up table. If this converter produces something that has to do with
     * reading a field or method, and not actual conversion, this should be set
     * to False. If an unique type is produced on the output, not bound to generics,
     * this should be set to True to allow for automatic conversion to that type.
     *
     * @return True if Conversion table registration is enabled, False if not
     */
    public abstract boolean isRegisterSupported();

    /**
     * Creates a new ConverterPair with this converter as A and the specified
     * converter as B
     *
     * @param converterB to form a pair with
     * @return new ConverterPair
     */
    public <K> ConverterPair<T, K> formPair(Converter<K> converterB) {
    	return new ConverterPair<T, K>(this, converterB);
    }

    /**
     * Creates a new Converter that uses this base converter, but attempts to
     * cast the result to the type specified
     *
     * @param type to cast to
     * @return new Casting Converter
     */
    public <K> Converter<K> cast(Class<K> type) {
    	return new CastingConverter<K>(type, this);
    }

    /**
     * Applies this Conversion to a collection of elements and converts each element
     * 
     * @return new Array element converter
     */
    public Converter<T[]> toArray() {
    	return ArrayElementConverter.create(this);
    }
}
