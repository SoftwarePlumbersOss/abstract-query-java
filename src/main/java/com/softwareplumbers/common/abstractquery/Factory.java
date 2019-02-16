package com.softwareplumbers.common.abstractquery;

import java.util.Arrays;
import java.util.List;

/** Factory class that allows us to construct new abstract sets of the correct type 
 * 
 * @param <U> type of abstract set
 */
public abstract class Factory<T, U extends AbstractSet<T,U>> {
	
	public abstract U intersect(List<U> elements);
	public abstract U union(List<U> elements);
	
	@SafeVarargs
	public final U intersect(U... elements) { return intersect(Arrays.asList(elements)); }
	@SafeVarargs
	public final U union(U... elements) { return union(Arrays.asList(elements)); }
}
