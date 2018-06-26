package com.softwareplumbers.common.abstractquery;

import javax.json.JsonValue;

public interface AbstractSet<T extends Value, U extends AbstractSet<T,U>> {
	
	/** Create a new range that contains only those values contained by both ranges.
	 * 
	 * The intersection of two ranges contains all the values contained by both ranges.
	 * 
	 * @param range Range to intersect with this range.
	 * @return intersection of the two ranges
	 */	
	U intersect(U other);
	
	Boolean intersects(U other);
	
	U union(U other);
	
	/** Check if this range contains a value.
	 * 
	 * A range contains a value if the value meets the implied constraints.Ranges may be 
	 * parameterized, in which case this cannot always be determined. 
	 * 
	 * @param range Range to compare to this range.
	 * @return True if this range contains the given value, False if not, null if this cannot be determined
	 */
	Boolean containsItem(T item);
	
	/** Check if this range contain another range.
	 * 
	 * A range contains another range if every value in the contained range is also contained
	 * by the containing range. Ranges may be parameterized, in which case this cannot always be
	 * determined. 
	 * 
	 * @param range Range to compare to this range.
	 * @return True if this range contains the given range, False if not, null if this cannot be determined
	 */
	Boolean contains(U set);
	Boolean maybeEquals(U other);
	
	<X> X toExpression(Formatter<X> formatter, Formatter.Context context);
	default <X> X toExpression(Formatter<X> formatter) { return toExpression(formatter, Formatter.Context.ROOT); }
	
	JsonValue toJSON();
	
	U bind(Value.MapValue values);
	
	boolean isEmpty();
	boolean isUnconstrained();
}
