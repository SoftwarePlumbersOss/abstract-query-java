package com.softwareplumbers.common.abstractquery;

import javax.json.JsonValue;

import com.softwareplumbers.common.abstractquery.formatter.CanFormat;
import com.softwareplumbers.common.abstractquery.formatter.Context;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

/** Interface representing an object on which fundamental set operations can be performed.
 * 
 * @author SWPNET\jonessex
 *
 * @param <T> The value type of the set
 * @param <U> The implementing class on which operations are performed.
 */
public interface AbstractSet<T, U extends AbstractSet<T,U>> extends CanFormat {
	
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
	
	<X,V> X toExpression(Formatter<X,V> formatter, Context context);
	
	JsonValue toJSON();
	
	U bind(Value.MapValue values);
	
	boolean isEmpty();
	boolean isUnconstrained();
}
