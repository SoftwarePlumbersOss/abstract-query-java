package com.softwareplumbers.common.abstractquery.formatter;

public class Context {
	
	public enum Type {
		FIELD,
		OBJECT,
		ARRAY,
		ROOT
	}
	
	public final Context parent;
	public final String dimension;
	public final Context.Type type;
	
	public Context(Context parent, Context.Type type, String dimension) {
		this.type = type;
		this.dimension = dimension;
		this.parent = parent;
	}
	
	public Context in(String dimension) {
		return new Context(this, Type.FIELD, dimension);
	}

	public Context setType(Context.Type type) {
		return new Context(this, type, dimension);
	}
	
	protected static boolean eq(Object a, Object b) { return a == b || (a != null && b!= null && a.equals(b)); }	
	
	public boolean equals(Context other) {
		return eq(type, other.type) && eq(dimension, other.dimension) && eq(parent, other.parent);
	}
	
	public boolean equals(Object other) {
		return other instanceof Context && equals((Context)other);
	}

	public static final Context ROOT = new Context(null, Type.ROOT, null);
}