package com.softwareplumbers.common.abstractquery;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

/** Implementation of yes/no/maybe logic, using a Boolean value where 'null' is maybe.
 * 
 * @author SWPNET\jonessex
 *
 */
public class Tristate {
	
    @FunctionalInterface
	public static interface Predicate<T> extends java.util.function.Predicate<T> {
		Boolean containsItem(T item);
        @Override
        default boolean test(T item) { return containsItem(item) == Boolean.TRUE; }
        default <U> Predicate<U> on(Function<U,T> map) { return new ComposedPredicate<>(this, map); }
	}

	@FunctionalInterface
	public static interface BiPredicate<T,U> extends java.util.function.BiPredicate<T,U> {
		Boolean containsItem(T a, U b);
        @Override
        default boolean test(T a, U b) { return containsItem(a,b) == Boolean.TRUE; };
	}
    
    public static class ComposedPredicate<U,T> implements Predicate<U> {
        public final Predicate<T> original;
        public final Function<U,T> map;
        public ComposedPredicate(Predicate<T> original, Function<U,T> map) { this.original = original; this.map = map; }
        public Boolean containsItem(U item) { return original.containsItem(map.apply(item)); }
    }

	public static Boolean and(Boolean a, Boolean b) {
		if (a == Boolean.FALSE || b == Boolean.FALSE) return Boolean.FALSE;
		if (a == null || b == null) return null;
		return Boolean.TRUE;
	}
	
	public static Boolean or(Boolean a, Boolean b) {
		if (a == Boolean.TRUE || b == Boolean.TRUE) return Boolean.TRUE;
		if (a == null || b == null) return null;
		return Boolean.FALSE;
	}
	
	public static <T> Boolean every(Collection<T> collection, Predicate<T> condition) {
		Boolean result = Boolean.TRUE;
		for (T item : collection) {
			result = and(result, condition.containsItem(item));
			if (result == Boolean.FALSE) return result;
		}
		return result;
	}
	
	public static <T> Boolean any(Iterator<T> collection, Predicate<T> condition) {
		Boolean result = Boolean.FALSE;
		while (collection.hasNext() && result != Boolean.TRUE) {
			result = or(result, condition.containsItem(collection.next()));
			if (result == Boolean.TRUE) return result;
		}
		return result;
	}
	
	public static <T> Boolean any(Collection<T> collection, Predicate<T> condition) {
		return any(collection.iterator(), condition);
	}
	
	public static <T> Boolean any(Stream<T> stream, Predicate<T> condition) {
		return any(stream.iterator(), condition);
	}
	
	public static <T,U> Boolean compareCollections(Collection<? extends T> a, Collection<? extends U> b, BiPredicate<T,U> condition) {
		if (a.size() == b.size()) {
			Iterator<? extends T> ai = a.iterator();
			Iterator<? extends U> bi = b.iterator();
			Boolean result = Boolean.TRUE;
			while (ai.hasNext() && bi.hasNext()) {
				result = and(result, condition.containsItem(ai.next(), bi.next()));
				if (result == Boolean.FALSE) return result;
			}
			return result;
		} else {
			return false;
		}
	}
    
    public static enum CompareResult {
        LESS, GREATER, EQUAL, UNKNOWN;
        
        public static CompareResult valueOf(int value) {
            if (value < 0) return LESS;
            if (value > 0) return GREATER;
            return EQUAL;
        }
    }
    
    public static Boolean isGreaterThan(CompareResult result) {
        switch (result) {
            case GREATER: 
                return Boolean.TRUE;
            case EQUAL:
            case LESS: 
                return Boolean.FALSE;
            case UNKNOWN:
                return null;
            default:
                throw new RuntimeException("Unknown comparison result");
        }
    }
    
        public static Boolean isGreaterThanOrEqual(CompareResult result) {
        switch (result) {
            case GREATER: 
            case EQUAL:
                return Boolean.TRUE;
            case LESS: 
                return Boolean.FALSE;
            case UNKNOWN:
                return null;
            default:
                throw new RuntimeException("Unknown comparison result");
        }
    }
    
    
    public static Boolean isLessThan(CompareResult result) {
        switch (result) {
            case LESS: 
                return Boolean.TRUE;
            case EQUAL:
            case GREATER: 
                return Boolean.FALSE;
            case UNKNOWN:
                return null;
            default:
                throw new RuntimeException("Unknown comparison result");
        }
    }

    public static Boolean isLessThanOrEqual(CompareResult result) {
        switch (result) {
            case LESS: 
            case EQUAL:
                return Boolean.TRUE;
            case GREATER: 
                return Boolean.FALSE;
            case UNKNOWN:
                return null;
            default:
                throw new RuntimeException("Unknown comparison result");
        }
    }

    public static Boolean isEqual(CompareResult result) {
        switch (result) {
            case EQUAL:
                return Boolean.TRUE;
            case LESS: 
            case GREATER: 
                return Boolean.FALSE;
            case UNKNOWN:
                return null;
            default:
                throw new RuntimeException("Unknown comparison result");
        }
    }
}
