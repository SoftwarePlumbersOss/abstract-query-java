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
public abstract class DelegatingVisitor<T,U> implements Visitor<T> {
    
    protected Visitor<U> output;
    
    public DelegatingVisitor(Visitor<U> output) {
        this.output = output;
    }

    @Override
    public abstract T getResult();
    
    @Override
    public void operExpr(String operator) {
        output.operExpr(operator);
    }

    @Override
    public void andExpr(JsonValue.ValueType type) {
        output.andExpr(type);
    }

    @Override
    public void orExpr(JsonValue.ValueType type) {
        output.orExpr(type);
    }

    @Override
    public void betweenExpr(JsonValue.ValueType type) {
        output.betweenExpr(type);
    }

    @Override
    public void subExpr(String operator) {
        output.subExpr(operator);
    }

    @Override
    public void arrayExpr() {
        output.arrayExpr();
    }

    @Override
    public void queryExpr() {
        output.queryExpr();
    }

    @Override
    public void dimensionExpr(String name) {
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
    }
}
