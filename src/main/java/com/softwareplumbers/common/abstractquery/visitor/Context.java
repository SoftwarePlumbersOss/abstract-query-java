package com.softwareplumbers.common.abstractquery.visitor;

import com.softwareplumbers.common.QualifiedName;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

public class Context {
	
	public enum Type {
		DIMENSION,
		QUERY,
		ARRAY,
		ROOT,
        AND,
        OR,
        SUBEXPR,
        OPERATOR,
        BETWEEN
	}
	
	public final Context parent;
	public final String dimension;
	public final Context.Type type;
    public final ValueType valueType;
    public final String operator;
    public int count;
	
	public Context(Context parent, Context.Type type, String dimension, ValueType valueType, String operator) {
		this.type = type;
		this.dimension = dimension;
		this.parent = parent;
        this.valueType = valueType;
        this.operator = operator;
        this.count = 0;
	}
    
    public Context(Context.Type type) {
        this(null, type, null, null, null);
    }
    	
	public Context dimensionExpr(String dimension) {
		return new Context(this, Type.DIMENSION, dimension, valueType, operator);
	}

    public Context andExpr(ValueType type) {
        return new Context(this, Type.AND, dimension, valueType, operator);
    }

    public Context orExpr(ValueType type) {
        return new Context(this, Type.OR, dimension, valueType, operator);
    }
    
    public Context subExpr(String operator) {
       return new Context(this, Type.SUBEXPR, dimension, valueType, operator); 
    }
    
    public Context operExpr(String operator) {
        return new Context(this, Type.OPERATOR, dimension, valueType, operator);
    }
    
    public Context arrayExpr() {
        return new Context(this, Type.ARRAY, dimension, valueType, operator);        
    }

    public Context queryExpr() {
        return new Context(this, Type.QUERY, dimension, ValueType.OBJECT, operator);        
    }
    
    public Context betweenExpr(ValueType valueType) {
        return new Context(this, Type.BETWEEN, dimension, valueType, operator);
    }
    

	protected static boolean eq(Object a, Object b) { return a == b || (a != null && b!= null && a.equals(b)); }	
	
	public boolean equals(Context other) {
		return eq(type, other.type) 
            && eq(dimension, other.dimension) 
            && eq(parent, other.parent)
            && eq(valueType, other.valueType)
            && eq(operator, other.operator);
	}
	
	public boolean equals(Object other) {
		return other instanceof Context && equals((Context)other);
	}

	public static final Context ROOT = new Context(null, Type.ROOT, null, null, null);
    
    public Context getContainer() {
        if (parent == null) return null;
        switch(parent.type) {
            case ROOT:
            case QUERY:
            case ARRAY:
                return parent;
            default:
                return parent.getContainer();
        }
    }
    
    public QualifiedName getDimension() {
        switch (type) {
            case ROOT: return QualifiedName.ROOT;
            case DIMENSION: return parent.getDimension().add(dimension);
            default: return parent.getDimension();
        }
    }
}