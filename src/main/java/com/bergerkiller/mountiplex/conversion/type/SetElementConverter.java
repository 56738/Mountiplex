package com.bergerkiller.mountiplex.conversion.type;

import java.util.Set;

import com.bergerkiller.mountiplex.conversion.Converter;
import com.bergerkiller.mountiplex.conversion.ConverterPair;
import com.bergerkiller.mountiplex.conversion.util.ConvertingSet;

/**
 * A generic converter that converts the elements back and forth inside a Set
 */
public class SetElementConverter<A, B> extends Converter<Set<B>> {
	private final ConverterPair<A, B> pair;

	private SetElementConverter(ConverterPair<A, B> converterPair) {
	    super(Set.class);
		this.pair = converterPair;
	}

	@Override
	public Set<B> convert(Object value, Set<B> def) {
		Set<B> result = convert(value);
		if (result == null) {
			return def;
		} else {
			return result;
		}
	}

	@Override
	public Set<B> convert(Object value) {
		Set<?> inputSet = CollectionConverter.toSet.convert(value);
		if (inputSet == null) {
			return null;
		} else {
			return new ConvertingSet<B>(inputSet, pair);
		}
	}

	@Override
	public boolean isCastingSupported() {
		return false;
	}
	
    @Override
    public boolean isRegisterSupported() {
        return false;
    }
    /**
     * Reverses the element conversion
     *
     * @return new Set Element Converter with element types swapped
     */
    public SetElementConverter<B, A> reverse() {
    	return create(pair.reverse());
    }

    /**
     * Creates a new set element converter
     * 
     * @param converterPair to use during conversion
     * @return Set element converter
     */
    public static <A, B> SetElementConverter<A, B> create(ConverterPair<A, B> converterPair) {
    	return new SetElementConverter<A, B>(converterPair);
    }

    /**
     * Creates a new set element converter
     * 
     * @param convA converter in one direction
     * @param convB converter in the other direction
     * @return Set element converter
     */
    public static <A, B> SetElementConverter<A, B> create(Converter<A> convA, Converter<B> convB) {
    	return new SetElementConverter<A, B>(convA.formPair(convB));
    }
}
