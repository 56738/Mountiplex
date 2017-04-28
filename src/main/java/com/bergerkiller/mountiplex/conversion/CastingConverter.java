package com.bergerkiller.mountiplex.conversion;

/**
 * Extends another converter and tries to cast the returned type
 *
 * @param <T> - type to cast the output to
 */
public class CastingConverter<T> extends Converter<T> {

    private final Converter<?> baseConvertor;

    public CastingConverter(Class<T> outputType, Converter<?> baseConvertor) {
        super(outputType);
        this.baseConvertor = baseConvertor;
    }

    @Override
    public T convert(Object value, T def) {
        try {
            return this.getOutputType().cast(baseConvertor.convert(value));
        } catch (ClassCastException ex) {
            return def;
        }
    }

    @Override
    public boolean isCastingSupported() {
        return false;
    }

    @Override
    public boolean isRegisterSupported() {
        return true;
    }

    @Override
    public <K> ConverterPair<T, K> formPair(Converter<K> converterB) {
        return new ConverterPair<T, K>(this, converterB);
    }

    @Override
    public <K> Converter<K> cast(Class<K> type) {
        return new CastingConverter<K>(type, this);
    }
}
