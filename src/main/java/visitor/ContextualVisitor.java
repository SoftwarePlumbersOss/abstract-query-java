/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package visitor;

import javax.json.JsonValue;

public abstract class ContextualVisitor<T> implements Visitor<T> {

    private Context context = new Context(Context.Type.ROOT);

    @Override
    public void operExpr(String operator) {
        context = context.operExpr(operator);
        beginOperExpr(context, operator);
    }

    public void beginOperExpr(Context context, String operator) {
    }

    ;
        public void endOperExpr(Context context) {
    }

    ;

        @Override
    public void andExpr(JsonValue.ValueType type) {
        context = context.andExpr(type);
        beginAndExpr(context, type);
    }

    public void beginAndExpr(Context context, JsonValue.ValueType type) {
    }

    ;
        public void endAndExpr(Context context) {
    }

    ;
        
        @Override
    public void betweenExpr(JsonValue.ValueType type) {
        context = context.betweenExpr(type);
        beginBetweenExpr(context, type);
    }

    public void beginBetweenExpr(Context context, JsonValue.ValueType type) {
    }

    ;
        public void endBetweenExpr(Context context) {
    }

    ;

        @Override
    public void orExpr(JsonValue.ValueType type) {
        context = context.orExpr(type);
        beginOrExpr(context, type);
    }

    public void beginOrExpr(Context context, JsonValue.ValueType type) {
    }

    ;
        public void endOrExpr(Context context) {
    }

    ;
        
        @Override
    public void subExpr(String operator) {
        context = context.subExpr(operator);
        beginSubExpr(context, operator);
    }

    public void beginSubExpr(Context context, String operator) {
    }

    ;
        public void endSubExpr(Context context) {
    }

    ;

        @Override
    public void unbounded() {
        unbounded(context);
    }

    ;
        public void unbounded(Context context) {
    }

    ;
        
        @Override
    public void value(JsonValue value) {
        value(context, value);
    }

    public void value(Context context, JsonValue value) {
    }

    ;
        
        @Override
    public void arrayExpr() {
        context = context.arrayExpr();
        beginArrayExpr(context);
    }

    public void beginArrayExpr(Context context) {
    }

    ;
        public void endArrayExpr(Context context) {
    }

    ;

        @Override
    public void queryExpr() {
        context = context.queryExpr();
        beginQueryExpr(context);
    }

    public void beginQueryExpr(Context context) {
    }

    ;
        public void endQueryExpr(Context context) {
    }

    ;

        @Override
    public void dimensionExpr(String name) {
        context = context.dimensionExpr(name);
        beginDimensionExpr(context, name);
    }

    public void beginDimensionExpr(Context context, String name) {
    }

    ;
        public void endDimensionExpr(Context context) {
    }

    ;
        
        @Override
    public void endExpr() {
        endExpr(context);
        context = context.parent;
        context.count++;
    }

    public void endExpr(Context context) {
        switch (context.type) {
            case DIMENSION:
                endDimensionExpr(context);
                break;
            case QUERY:
                endQueryExpr(context);
                break;
            case ARRAY:
                endArrayExpr(context);
                break;
            case ROOT:
                break;
            case AND:
                endAndExpr(context);
                break;
            case OR:
                endOrExpr(context);
                break;
            case SUBEXPR:
                endSubExpr(context);
                break;
            case OPERATOR:
                endOperExpr(context);
                break;
            case BETWEEN:
                endBetweenExpr(context);
                break;
        }
    }

}

