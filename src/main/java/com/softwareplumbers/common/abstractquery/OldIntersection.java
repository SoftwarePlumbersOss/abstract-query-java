package com.softwareplumbers.common.abstractquery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.softwareplumbers.common.abstractquery.Range.Between;
import com.softwareplumbers.common.abstractquery.Range.Equals;
import com.softwareplumbers.common.abstractquery.Range.OpenRange;
import com.softwareplumbers.common.abstractquery.Range.Unbounded;

/** Support a deferred intersection between parametrized ranges.
 *
 * @private 
 */
public class OldIntersection implements Range {

	public static final String OPERATOR = "$and";

	private Range known_bounds;
	private Map<Param,Range> parametrized_bounds;
	private List<Param> parameters;

	/** construct an intersection
	 *
	 * Note: will throw an error if result is logically empty. We should only call this method with
	 * ranges that are known to not to be exclusive, and should only call it with simple unary ranges or
	 * 'between' ranges.
	 * 
	 * Use repeated calls of .intersect to build an intersection without these limitations.
	 */
	@SafeVarargs
	public OldIntersection(Range... ranges) {
		this.known_bounds = UNBOUNDED;
		this.parametrized_bounds = new HashMap<Param,Range>();
		this.parameters = new ArrayList<Param>();
		for (Range range : ranges) {
			if (!this.addRange(range)) throw new IllegalArgumentException("Range excludes previously added ranges");
		}
	}

	public OldIntersection(OldIntersection to_copy) {
		this.known_bounds = to_copy.known_bounds;
		this.parametrized_bounds = new HashMap<Param,Range>(to_copy.parametrized_bounds);
		this.parameters = new ArrayList<Param>(to_copy.parameters);
	}

	/** Add a range to this intersection.
	 *
	 * @returns false if then resulting range would be logically empty.
	 */
	boolean addRange(Range range) {

		if (range instanceof Unbounded) {
			return true;
		}

		if (range instanceof Between) {
			Between between = (Between)range;
			return this.addRange(between.lower_bound) && this.addRange(between.upper_bound);
		}

		if (range instanceof OldIntersection) {
			OldIntersection intersection = (OldIntersection)range;
			boolean result = this.addRange(intersection.known_bounds);
			for (int i = 0; i < intersection.parameters.size() && result; i++)
				result = this.addRange(intersection.parametrized_bounds.get(intersection.parameters.get(i)));
			return result;
		}

		Value rvalue = null;
		if (range instanceof Equals) rvalue = ((Equals)range).value;
		if (range instanceof OpenRange) rvalue = ((OpenRange)range).value;

		if (rvalue.type == Value.Type.PARAM) {
			Param pname = (Param)((Value.Atomic)rvalue).value;

			Range old_param = this.parametrized_bounds.getOrDefault(pname, UNBOUNDED);
			Range new_param = old_param.intersect(range);
			if (new_param == null)  {
				//				console.log('1',new_param, old_param, range);
				return false;
			}
			this.parametrized_bounds.put(pname, new_param);
			if (old_param == UNBOUNDED) this.parameters.add(pname); 
		} else {
			Range new_known_bounds = known_bounds.intersect(range);
			if (new_known_bounds == null) {
				//				console.log('2',known_bounds, this.known_bounds, range);
				return false;
			}
			known_bounds = new_known_bounds;
		}
		return true;
	}


	public Boolean contains(Range range) {
		
		Boolean result = this.known_bounds.contains(range);
		for (int i = 0; i < this.parameters.size() && result != null && result; i++)
			result = this.parametrized_bounds.get(this.parameters.get(i)).contains(range);
		return result;
	}

	public Boolean containsItem(Value.Atomic item) {
		Boolean result = this.known_bounds.containsItem(item);
		for (int i = 0; i < this.parameters.size() && result != null && result; i++)
			result = this.parametrized_bounds.get(this.parameters.get(i)).containsItem(item);
		return result;
	}

	/** Determine if this range contained by another.
	 *
	 *
	 */
	public Boolean containedBy(Range range) {

		// range contains intersection if it contains the known bounds, or any of the parameterized bounds
		if (range.contains(this.known_bounds) == Boolean.TRUE) return true;
		// the only way we can know that this range contains a parametrized range is if they have the same
		// parameter. 
		if (range instanceof OpenRange) {
			Value rvalue = ((OpenRange)range).value;
			if (rvalue.type == Value.Type.PARAM) {
				return range.contains(this.parametrized_bounds.getOrDefault((Param)((Value.Atomic)rvalue).value, UNBOUNDED));
			}
		}
		if (range instanceof Equals) {
			Value rvalue = ((Equals)range).value;
			if (rvalue.type == Value.Type.PARAM) {
				return range.contains(this.parametrized_bounds.getOrDefault((Param)((Value.Atomic)rvalue).value, UNBOUNDED));
			}
		}

		//However, we can return a definitive false if all the parametrized bounds return false,
		// which can happen, for example, if a 'less than' is compared to a 'greater than'
		Boolean containedBy = Boolean.FALSE;
		for (int i = 0; i < parameters.size() && containedBy == Boolean.FALSE; i++) {
			containedBy = range.contains(parametrized_bounds.get(parameters.get(i)));
		}

		return containedBy == Boolean.TRUE ? null : containedBy; 
	}

	public Range maybeIntersect(Range range) {
		if (range instanceof Unbounded) return this;

		if (range instanceof OldIntersection || range instanceof Between) {
			Range result = range.intersect(this.known_bounds);
			for (int i = 0; i < this.parameters.size() && result != null; i++)
				result = result.intersect(parametrized_bounds.get(parameters.get(i)));
			return result;
		}

		// essentially, clone this intersection
		OldIntersection result = new OldIntersection(this);

		if (result.addRange(range)) return result;

		return null;
	}

	public Boolean intersects(Range range) {
		if (range == UNBOUNDED) return Boolean.TRUE;
		if (range == EMPTY) return Boolean.FALSE;

		return Tristate.and(
					range.intersects(this.known_bounds),
					Tristate.every(this.parametrized_bounds.values(), bound->bound.intersects(range))
				);
	}
	
	public <U> U toExpression(Formatter<U> formatter)	{ 
		Stream<Range> ranges = Stream.concat(
				Stream.of(known_bounds), 
				parametrized_bounds.values().stream()
				);

		return formatter.andExpr(null,ranges.map(range->range.toExpression(formatter)));
	}

	public Boolean maybeEquals(OldIntersection range) { 
		if (this.known_bounds.equals(range.known_bounds)) {
			if (parameters.size() == range.parameters.size()) {
				Boolean  result = true;
				for (int i = 0; i < parameters.size() && result != null && result; i++) {
					Param param = parameters.get(i);
					Range other_bound = range.parametrized_bounds.get(param);
					result = other_bound != null ? parametrized_bounds.get(param).equals(other_bound) : false;
				}
				return result;
			}
		}
		return false;
	}

	
	public Boolean maybeEquals(Range range) {
		if (range instanceof OldIntersection) return maybeEquals((OldIntersection)range);
		return Boolean.FALSE;
	}

	public boolean equals(Object other) {
		return other instanceof OldIntersection && equals((OldIntersection)other);
	}

	public JsonObject toJSON()	{
		JsonArrayBuilder array = Json.createArrayBuilder();
		Stream.concat(Stream.of(this.known_bounds), this.parametrized_bounds.values().stream())
		.map(range -> range.toJSON())
		.forEach(obj -> array.add(obj));
		JsonObjectBuilder obj  = Json.createObjectBuilder();
		obj.add("$and", array);
		return obj.build();
	}

	public Range bind(Value.MapValue param_map) {
		Range result = known_bounds;
		for (int i = 0; i < parameters.size() && result != null; i++) {
			result = result.intersect(parametrized_bounds.get(parameters.get(i)).bind(param_map));
		}
		return result;
	}

	@Override
	public Range union(Range other) {
		return Range.union(this, other);
		
	}

	@Override
	public Range maybeUnion(Range other) {
		// TODO Auto-generated method stub
		return null;
	}	
	
	public String toString() {
		return toExpression(Formatter.DEFAULT);
	}

	public Value.Type getType() {
		Value.Type type = known_bounds.getType();
		if (type != null) return type;
		type = Range.getType(parametrized_bounds.values());
		return type;
	}
}