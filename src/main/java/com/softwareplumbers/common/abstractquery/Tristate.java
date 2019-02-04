package com.softwareplumbers.common.abstractquery;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;

/** Implementation of yes/no/maybe logic, using a Boolean value where 'null' is maybe.
 * 
 * @author SWPNET\jonessex
 *
 */
public class Tristate {
	
	@FunctionalInterface
	public static interface Predicate<T> {
		Boolean test(T item);
	}

	@FunctionalInterface
	public static interface BiPredicate<T,U> {
		Boolean test(T a, U b);
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
			result = and(result, condition.test(item));
			if (result == Boolean.FALSE) return result;
		}
		return result;
	}
	
	public static <T> Boolean any(Iterator<T> collection, Predicate<T> condition) {
		Boolean result = Boolean.FALSE;
		while (collection.hasNext() && result != Boolean.TRUE) {
			result = or(result, condition.test(collection.next()));
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
				result = and(result, condition.test(ai.next(), bi.next()));
				if (result == Boolean.FALSE) return result;
			}
			return result;
		} else {
			return false;
		}
	}
}
