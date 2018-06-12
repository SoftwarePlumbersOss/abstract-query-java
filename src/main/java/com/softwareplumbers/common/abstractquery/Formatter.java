package com.softwareplumbers.common.abstractquery;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Format a Query
 * 
 * Used to walk the logical structure of a query, building a representation of
 * type T using context data of type U.
 * 
 * @author Jonathan Essex
 *
 * @param <T> Type of formatted representation (typically, but not always, a String)
 * @param <U> Type of context information required
 */
public interface Formatter<T> {
	
	/** Create a representation of a constraint on a dimension */
	T operExpr(String dimension, String operator, Value value);
	/** Create a representation of an intersection of constraints */
	T andExpr(Stream<T> expressions);
	/** Create a representation of a union of constraints */
	T orExpr(Stream<T> expressions);
	
	
	/** Get the default query formatter
	*/
	public static class DefaultFormat implements Formatter<String> {
		
		private DefaultFormat parent;
		private String dimension;
		
		public DefaultFormat(DefaultFormat parent, String dimension) {
			this.parent = parent;
			this.dimension = dimension;
		}

		String printDimension(String name) {
			if (name == null) return "$self";
			if (dimension == null) return name;
			return parent.printDimension(dimension) + "." + name;
		}
		
		String printValue(Value value) {
			return value.type == Value.Type.STRING ? "\"" + value.toString() + "\"" : value.toString();
		}

    	public String andExpr(Stream<String> ands) { 
    		return ands.collect(Collectors.joining(" and ")); 
    	}
    	
    	public String orExpr(Stream<String> ors) { 
    		return "(" + ors.collect(Collectors.joining(" or ")) + ")"; 
    	}
    	
    	public String operExpr(String dimension, String operator, Value value) {
    			// null dimension implies that we are in a 'has' clause where the dimension is attached to the
    			// outer 'has' operator 
    			if (operator.equals("match"))
    				return value.toString();
    			if (operator.equals("has"))
    				return printDimension(dimension) + " has(" + value + ")";
    			//if (dimension === null) return '$self' + operator + printValue(value) 

    			return printDimension(dimension) + operator + printValue(value) ;
    	}
	};
	
	public Formatter<String> DEFAULT = new DefaultFormat(null, null);
}
