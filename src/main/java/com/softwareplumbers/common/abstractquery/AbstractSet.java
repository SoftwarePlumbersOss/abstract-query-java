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
	
	/** Factory that can be used to create new unions and intersections.
	 * 
	 * @return Get a factory that can be used to create new unions and intersections
	 */
	Factory<T,U> getFactory();
	
	/** Create a new set that contains only those values contained by both sets.
	 * 
	 * The intersection of two set contains all the values contained by both sets.
	 * 
	 * @param range Set to intersect with this set.
	 * @return intersection of the two sets
	 */	
	@SuppressWarnings("unchecked") // Because every instance of AbstractSet<T,U> is a U
	default U intersect(U other) { return getFactory().intersect((U)this, other); }

	/** Create a new set that contains all values contained by either set.
	 * 
	 * The union of two sets contains all the values in both sets.
	 * 
	 * @param other Set to intersect with this set.
	 * @return intersection of the two sets
	 */	
	@SuppressWarnings("unchecked") // Because every instance of AbstractSet<T,U> is a U
	default U union(U other) { return getFactory().union((U)this, other); }

	/** Check whether this abstract set has a non-empty intersection with another.
	 * 
	 * By convention a null value indicates that we do not have enough information to
	 * determine intersection. This can happen with parameterized sets.
	 * 
	 * @param other
	 * @return true, false, or null
	 */
	Boolean intersects(U other);
	
	/** Check if this set contains a value.
	 * 
	 * A set contains a value if the value meets the implied constraints. Sets may be 
	 * parameterized, in which case this cannot always be determined. 
	 * 
	 * @param range Range to compare to this range.
	 * @return True if this range contains the given value, False if not, null if this cannot be determined
	 */
	Boolean containsItem(T item);
	
	/** Check if this set contain another set.
	 * 
	 * A set contains another set if every value in the contained set is also contained
	 * by the containing set. Sets may be parameterized, in which case this cannot always be
	 * determined. 
	 * 
	 * @param set Set to compare to this set.
	 * @return True if this range contains the given range, False if not, null if this cannot be determined
	 */
	Boolean contains(U set);
	
	/** Check if two sets are equal.
	 * 
	 * A set is equal to another set if there are no values contained in either set which are not
	 * also contained in the other. This can not always be determined.
	 * 
	 * @param other
	 * @return True if this  the given value, False if not, null if this cannot be determined
	 */
	Boolean maybeEquals(U other);
	
	/** Output a description of the set using a Formatter object.
	 * 
	 * @param formatter the object used to create a description of the set
	 * @param context object which passes information about complex sets which may have this set as a component
	 */
	<X,V> X toExpression(Formatter<X,V> formatter, Context context);
	
	/** Output set as Json
	 * 
	 * @return A Json representatio  of the set
	 */
	JsonValue toJSON();
	
	U bind(Value.MapValue values);
	
	boolean isEmpty();
	boolean isUnconstrained();
}
