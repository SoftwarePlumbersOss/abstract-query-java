package com.softwareplumbers.common.abstractquery;

import java.util.stream.Stream;

public interface Formatter<T,U> {
	T operExpr(String dimension, String operator, Value value, U context);
	T andExpr(Stream<T> expressions);
}
