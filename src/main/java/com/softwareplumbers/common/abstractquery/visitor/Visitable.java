package com.softwareplumbers.common.abstractquery.visitor;

import java.util.function.Supplier;

public interface Visitable {
	void visit(Visitor<?> visitor);
    
	default <T, U extends Visitor<T>> T toExpression(Supplier<U> format) { 
        Visitor<T> visitor = format.get();
        visit(visitor);
        return visitor.getResult();
    }
}