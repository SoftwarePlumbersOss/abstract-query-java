/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.abstractquery.visitor;

import java.util.function.Function;

/**
 *
 * @author jonathan.local
 */
@FunctionalInterface
public interface Formatter<T> {
    Visitor<T> getVisitor();
    
    default <U> Formatter<U> transform(Function<Visitor<T>, Visitor<U>> transformer) {
        return ()->transformer.apply(getVisitor());
    };
}
