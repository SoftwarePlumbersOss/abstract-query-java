package com.softwareplumbers.common.abstractquery.visitor;

import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;


/** Walk the logical structure of a query.
 * 
 * @author Jonathan Essex
 * 
 * @param <T> The type of the final result of visiting all the elements of the query.
 *
 */
public interface Visitor<T> {
	
	/** Visit an operator expression
     * @param operator The operator for the expression ('=','!=',etc) */
	void operExpr(String operator);
	/** Visit an and expression
     * @param type the value type for the expression */
	void andExpr(ValueType type);
	/** Visit an or expression
     * @param type the value type for the expression */
	void orExpr(ValueType type);
	/** Visit a between expression
     * @param type the value type for the expression */
	default void betweenExpr(ValueType type) { andExpr(type); }
	/** Visit operation over sub-expressions
     * @param operator the operator for the expression (usually 'has') */
	void subExpr(String operator);
    /** Visit an array expression */
    void arrayExpr();
    /** Visit a query expression */
    void queryExpr();
    /** Visit a dimension expression
     * @param name the dimension being visited */
    void dimensionExpr(String name);
    /** Visit an atomic value - no endExpr required
     * @param value the value being visited */
    void value(JsonValue value);
	/** Visit an unbounded constraint - this is a 'special' value */
	void unbounded();
    /** end an expression */
    void endExpr();
    /** get the end result
     * @return the end result of visiting all the elements of the query expression */
    T getResult();
}
