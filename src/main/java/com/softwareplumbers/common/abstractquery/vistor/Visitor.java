package com.softwareplumbers.common.abstractquery.vistor;

import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;


/** Walk the logical structure of a query
 * 
 * @author Jonathan Essex
 *
 */
public interface Visitor<T> {
	
	/** Visit an operator expression */
	void operExpr(String operator);
	/** Visit an and expression */
	void andExpr(ValueType type);
	/** Visit an or expression */
	void orExpr(ValueType type);
	/** Visit a between expression */
	default void betweenExpr(ValueType type) { andExpr(type); }
	/** Visit operation over sub-expressions */
	void subExpr(String operator);
    /** Visit an array expression */
    void arrayExpr();
    /** Visit a query expression */
    void queryExpr();
    /** Visit a dimension expression */
    void dimensionExpr(String name);
    /** Visit an atomic value - no endExpr required */
    void value(JsonValue value);
	/** Visit an unbounded constraint - this is a 'special' value */
	void unbounded();
    /** end an expression */
    void endExpr();
    /** get the end result */
    T getResult();
}
