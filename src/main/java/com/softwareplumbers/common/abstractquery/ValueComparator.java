package com.softwareplumbers.common.abstractquery;

import java.util.Comparator;

import com.softwareplumbers.common.abstractquery.Value.Type;

public class ValueComparator implements Comparator<Value> {
	
	/** @returns true if a or b is a parameter */
	static boolean params(Value a, Value b) 		{ 
		return a.type == Type.PARAM || b.type == Type.PARAM;
	}
	
	/** @returns true if a and b are both the same parameter */
	static boolean paramsEqual(Value a, Value b) { 
		return a.type == Type.PARAM && b.type == Type.PARAM && a.value.equals(b.value); 
	}

	public boolean order(Value a, Value b) {
		if (a.type == Type.PARAM || b.type == Type.PARAM) {
			throw new IllegalArgumentException("Can't compare unbound parameters");
		}
		if (a.type == Type.NUMBER || b.type == Type.NUMBER) {
			return a.toNumber().compareTo(b.toNumber()) < 0;
		}
		return a.toString().compareTo(b.toString()) < 0;
	}
	
	/** @returns true if a = b or a and b are both the same parameter, null if either is a parameter and they are not equal, false otherwise */ 
	Boolean equals(Value a, Value b)		{ 
		if (paramsEqual(a,b)) return true;
		return (params(a,b) ? null : !order(a,b) && !order(b,a)); 
	}
	
	/** @returns true if a < b or null if a or b is a parameter */
	Boolean lessThan(Value a, Value b) 	{ 
		if (params(a,b)) return paramsEqual(a,b) ? Boolean.FALSE : null;
		return order(a,b); 
	}
	
	/** @returns {boolean} true if a > b or null if a or b is a parameter */
	Boolean greaterThan(Value a, Value b) { 
		if (params(a,b)) return paramsEqual(a,b) ? Boolean.FALSE : null;
		return order(b,a); 
	}
	
	/** @returns {boolean} true if a <= b or a and b are both the same parameter, null if either is a parameter and they are not equal, false otherwise */
	Boolean greaterThanOrEqual(Value a, Value b) { 
		if (params(a,b)) return paramsEqual(a,b) ? Boolean.TRUE : null;
		return !this.order(a,b); 
	}
	
	/** @returns {boolean} true if a >= b or a and b are both the same parameter, null if either is a parameter and they are not equal, false otherwise */
	Boolean lessThanOrEqual(Value a, Value b) 	{ 
		if (params(a,b)) return paramsEqual(a,b) ? Boolean.TRUE : null;
		return !this.order(b,a); 
	}
	
	public static ValueComparator getInstance() {
		return new ValueComparator();
	}
	
	@Override
	public int compare(Value a, Value b) {
		if (params(a,b)) throw new IllegalArgumentException("Can't compare unbound parameters");
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
 		if (order(a,b)) return -1;
		if (order(b,a)) return 1;
		return 0;
	}
}
