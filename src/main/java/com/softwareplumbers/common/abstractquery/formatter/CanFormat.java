package com.softwareplumbers.common.abstractquery.formatter;

public interface CanFormat {
	<T> T toExpression(Formatter<T> format, Context ctx);
	default <T> T toExpression(Formatter<T> format) { return format.build(toExpression(format, Context.ROOT)); }
}