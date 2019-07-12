package com.softwareplumbers.common.abstractquery.visitor;

import java.util.function.Supplier;

public interface Visitable {
	void visit(Visitor<?> visitor);
    
	default <T> T toExpression(Formatter<T> format) { 
        Visitor<T> visitor = format.getVisitor();
        visit(visitor);
        return visitor.getResult();
    }
}