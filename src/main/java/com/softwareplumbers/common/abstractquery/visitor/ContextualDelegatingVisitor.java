/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.abstractquery.visitor;

import javax.json.JsonValue;

/**
 *
 * @author jonathan.local
 */
public class ContextualDelegatingVisitor<T> implements Visitor<T> {
    
    protected Context context = new Context(Context.Type.ROOT);
    protected Visitor<T> output;
    
    public ContextualDelegatingVisitor(Visitor<T> output) {
        this.output = output;
    }

    @Override
    public T getResult() {
        return output.getResult();
    }

    @Override
    public void operExpr(String operator) {
        context = context.operExpr(operator);
        output.operExpr(operator);
    }

    @Override
    public void andExpr(JsonValue.ValueType type) {
        context = context.andExpr(type);
        output.andExpr(type);
    }

    @Override
    public void orExpr(JsonValue.ValueType type) {
        context = context.orExpr(type);
        output.orExpr(type);
    }

    @Override
    public void betweenExpr(JsonValue.ValueType type) {
        context = context.betweenExpr(type);
        output.betweenExpr(type);
    }

    @Override
    public void subExpr(String operator) {
        context = context.subExpr(operator);
        output.subExpr(operator);
    }

    @Override
    public void arrayExpr() {
        context = context.arrayExpr();
        output.arrayExpr();
    }

    @Override
    public void queryExpr() {
        context = context.queryExpr();
        output.queryExpr();
    }

    @Override
    public void dimensionExpr(String name) {
        context = context.dimensionExpr(name);
        output.dimensionExpr(name);
    }

    @Override
    public void value(JsonValue value) {
        output.value(value);
    }

    @Override
    public void unbounded() {
        output.unbounded();
    }

    @Override
    public void endExpr() {
        output.endExpr();
        context = context.parent;
        context.count++;
    }
    
}
