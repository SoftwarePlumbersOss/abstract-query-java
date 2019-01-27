package com.softwareplumbers.common.abstractquery.formatter;

public interface CanFormat {
	<T,U> T toExpression(Formatter<T,U> format, Context ctx);
	default <T,U> U toExpression(Formatter<T,U> format) { return format.build(toExpression(format, Context.ROOT)); }
}