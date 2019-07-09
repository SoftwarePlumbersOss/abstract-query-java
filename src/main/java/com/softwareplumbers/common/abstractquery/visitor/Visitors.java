/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.abstractquery.visitor;

import com.softwareplumbers.common.QualifiedName;
import com.softwareplumbers.common.abstractquery.Param;
import com.softwareplumbers.common.abstractquery.Range;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

/**
 *
 * @author jonathan.local
 */
public class Visitors {
    /** Get the default query formatter
	*/
	public static class DefaultFormat extends ContextualVisitor<String> {
                
        protected Stack<String> elements = new Stack<String>();
        
        public DefaultFormat() { 
        }
        
        public String getResult() {
            return elements.firstElement();
        }
		
        public void beginDimensionExpr(Context context, String dimension) {
            
        }
        
        
		static String printDimension(Context context) {
            switch (context.type) {
                case ROOT: return "";
                case ARRAY: return "$self";
                case DIMENSION: 
                    Context container = context.getContainer();
                    if (container.type == Context.Type.ROOT)
                        return context.dimension;
                    else {
                        Context grandparent = container.getContainer();
                        switch (grandparent.type) {
                            case ROOT:
                            case ARRAY:
                                return context.dimension;
                            case QUERY:
                                if (container.dimension == null)
                                    return context.dimension;
                                else
                                    return printDimension(container) + "." + context.dimension;
                                default:
                                    throw new RuntimeException("Unexpected container type");
                        }
                    }
                default: return printDimension(context.parent);
            }
		}
        
        @Override
        public void value(Context context, JsonValue value) {
            elements.push(formatValue(value));
            context.count++;
        }
               
				
		public String formatValue(JsonValue value) {
            if (Param.isParam(value)) {
                return "$" + ((JsonObject)value).getString("$");
            }
            switch (value.getValueType()) {
                case STRING: return "'" + ((JsonString)value).getString() + "'";
                default: return value.toString();
            }
		}
        
        public List<String> getChildElements(Context context) {           
            return elements.subList(elements.size() - context.count, elements.size());
        }
        
        public void popChildElements(Context context) {
            elements.setSize(elements.size() - context.count);
        }
        
        @Override
    	public void endAndExpr(Context context) { 
    		String result = formatAndExpr(getChildElements(context));
            popChildElements(context);
            elements.push(result);
    	}
        
        public String formatAndExpr(List<String> children) {
            return children.stream().collect(Collectors.joining(" and "));
        }
        
        @Override
    	public void endQueryExpr(Context context) { 
    		String result = formatAndExpr(getChildElements(context));
            popChildElements(context);
            elements.push(result);
    	}

        public String formatQueryExpr(List<String> children) {
            return children.stream().collect(Collectors.joining(" and "));
        }
              
        @Override
    	public void endBetweenExpr(Context context) { 
    		String result = formatAndExpr(getChildElements(context));
            popChildElements(context);
            elements.push(result);
    	}
        
        public String formatBetweenExpr(List<String> children) {
            return children.stream().collect(Collectors.joining(" and "));
        }
        
        @Override
        public void endOrExpr(Context context) {
    		String result = formatOrExpr(getChildElements(context));
            popChildElements(context);
            elements.push(result);            
        }
    	
    	public String formatOrExpr(List<String> ors) { 
    		return "(" + ors.stream().collect(Collectors.joining(" or ")) + ")"; 
    	}
    	
        @Override
        public void endOperExpr(Context context) {
            String value = elements.pop();
            elements.push(formatOperExpr(printDimension(context), context.operator, value));
        }
        
    	public String formatOperExpr(String dimension, String operator, String value) {
    			// null dimension implies that we are in a 'has' clause where the dimension is attached to the
    			// outer 'has' operator 
    			if (operator.equals("match"))
    				return value;
    			if (operator.equals("has"))
    				return dimension + " has(" + value + ")";
    			if (operator.equals("like"))
    				return dimension + " like(" + value + ")";
    			//if (dimension === null) return '$self' + operator + printValue(value) 

    			return dimension + operator + value ;
    	}
        
        @Override
        public void endSubExpr(Context context) {
            String value = elements.pop();
            elements.push(formatSubExpr(printDimension(context), context.operator, value));            
        }
    	
    	public String formatSubExpr(String dimension, String operator, String sub) {
    		return dimension + " has (" +  sub + ")";
    	}

		@Override
		public void unbounded(Context context) {
            context.count++;
            elements.push(formatUnboundedExpr());
		}
        
        public String formatUnboundedExpr() {
            return "<unbounded>";
        }
	};

	/** Get the default query formatter
	*/
	public static class JsonFormat extends ContextualVisitor<JsonValue> {
        
        private Stack<JsonValue> elements = new Stack<JsonValue>();
        
        public JsonFormat() { }
        
        public JsonValue getResult() {
            return elements.firstElement();
        }
        
        public List<JsonValue> getChildElements(Context context) {           
            return elements.subList(elements.size() - context.count, elements.size());
        }
        
        public void popChildElements(Context context) {
            elements.setSize(elements.size() - context.count);
        }
        
        @Override
    	public void endAndExpr(Context context) { 
    		JsonValue result = buildAndExpr(context.valueType, getChildElements(context));
            popChildElements(context);
            elements.push(result);
    	}

		
    	public JsonValue buildAndExpr(JsonValue.ValueType type, List<JsonValue> ands) {
    		if (type == JsonValue.ValueType.OBJECT) {
    			JsonObjectBuilder object = Json.createObjectBuilder();
    			ands.forEach(value-> {
    				if (value instanceof JsonObject)
    					object.addAll(Json.createObjectBuilder((JsonObject)value));
    				else
    					throw new RuntimeException("Not excpecting a Cube to have member of type:" + value.getValueType());
    			});
    			return object.build();
    		} else {
    			JsonArrayBuilder array = Json.createArrayBuilder();
    			ands.forEach(value->array.add(value));
    			JsonObjectBuilder object = Json.createObjectBuilder();
    			object.add("$and", array);
    			return object.build();
    		}
    	}
        
        @Override
    	public void endQueryExpr(Context context) { 
    		JsonValue result = buildAndExpr(context.valueType, getChildElements(context));
            popChildElements(context);
            elements.push(result);
    	}

		
    	public JsonValue buildQueryExpr(JsonValue.ValueType type, List<JsonValue> ands) {
    			JsonObjectBuilder object = Json.createObjectBuilder();
    			ands.forEach(value-> {
    				if (value instanceof JsonObject)
    					object.addAll(Json.createObjectBuilder((JsonObject)value));
    				else
    					throw new RuntimeException("Not excpecting a Cube to have member of type:" + value.getValueType());
    			});
    			return object.build();
    	}
        
        @Override
        public void endOrExpr(Context context) {
            JsonValue result = buildOrExpr(context.valueType, getChildElements(context));
            popChildElements(context);
            elements.push(result);        
        }
    	
    	public JsonValue buildOrExpr(JsonValue.ValueType type, List<JsonValue> ors) { 
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		ors.forEach(value->array.add(value));
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		object.add("$or", array);
    		return object.build();
    	}
    	    	
    	private static String getFirstProperty(JsonValue value) {
    		return value.asJsonObject().keySet().iterator().next();
    	}
        
        @Override
        public void endBetweenExpr(Context context) {
            JsonValue upper_bound = elements.pop();
            JsonValue lower_bound = elements.pop();
            JsonValue result = buildBetweenExpr(context.valueType, lower_bound, upper_bound);
            elements.push(result);        
        }
    	
    	public JsonValue buildBetweenExpr(JsonValue.ValueType type, JsonValue lower_bound, JsonValue upper_bound) {
    		JsonArrayBuilder array = Json.createArrayBuilder();
    		String lbp = getFirstProperty(lower_bound);
    		String ubp = getFirstProperty(upper_bound);
    		if (!lbp.equals(ubp)) { throw new RuntimeException("Bad between"); }
    		JsonObject lb = lower_bound.asJsonObject().getJsonObject(lbp);
    		JsonObject ub = upper_bound.asJsonObject().getJsonObject(ubp);
    		String lbo = getFirstProperty(lb);
    		String ubo = getFirstProperty(ub);
    		array.add(lbo.equals(Range.GreaterThanOrEqual.OPERATOR) ? lb.get(lbo) : lb);
    		array.add(ubo.equals(Range.LessThan.OPERATOR) ? ub.get(ubo) : ub);
    		return Json.createObjectBuilder().add(lbp, array).build();
    	}
        
        @Override
        public void endOperExpr(Context context) {
            JsonValue value = elements.pop();
            elements.push(formatOperExpr(context.dimension, context.operator, value));
        }
    	
    	public JsonValue formatOperExpr(String dimension, String operator, JsonValue value) {
    		JsonObjectBuilder object = Json.createObjectBuilder();
    		if (operator.equals(Range.Equals.OPERATOR))
    			object.add(dimension,  value);
    		else
    			object.add(dimension, Json.createObjectBuilder().add(operator, value));
    		return object.build();
    	}
    	
        @Override
        public void endSubExpr(Context context) {
            JsonValue value = elements.pop();
            elements.push(formatSubExpr(context.dimension, context.operator, value));            
        }
        
    	public JsonValue formatSubExpr(String dimension, String operator, JsonValue sub) {
    		JsonObjectBuilder hasExpr = Json.createObjectBuilder().add(operator, sub);
    		return Json.createObjectBuilder().add(dimension,hasExpr).build();
    	}
        
        @Override
        public void unbounded(Context context) {
            context.count++;
            elements.push(formatUnboundedExpr());                        
        }

		public JsonValue formatUnboundedExpr() {
			return Json.createObjectBuilder().build();
		}
        
        @Override 
        public void value(Context context, JsonValue value) {
            context.count++;
            elements.push(value);
        }
	};
	

    private static final boolean eq(Object a, Object b) {
            return a == b || a != null && b!=null && a.equals(b);
    }

	
	public static interface Node extends List<Node>, Visitable {
	}
	
	public  static class Unbounded extends AbstractList<Node> implements Node {

		@Override public Node get(int index) { throw new IndexOutOfBoundsException(); }
		@Override public int size() { return 0; }
		@Override public void visit(Visitor<?> visitor) { visitor.unbounded(); }
		public Unbounded() {  }
        @Override public boolean equals(Object other) { return other instanceof Unbounded;  }
	}
    
    public static class Dimension extends ArrayList<Node> implements Node {
        public final QualifiedName value;
		@Override public void visit(Visitor<?> visitor) { visitor.dimensionExpr(value.part); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public Dimension(QualifiedName value) { this.value = value;  }
        public boolean equals(Dimension other) { return eq(value, other.value) && super.equals(other);  }
        @Override public boolean equals(Object other) { return other instanceof Dimension && equals((Dimension)other); }
    }

    public  static class Value extends AbstractList<Node> implements Node {

        public final JsonValue value;
        
		@Override public Node get(int index) { throw new IndexOutOfBoundsException();  }
		@Override public int size() { return 0; }
		@Override public void visit(Visitor<?> visitor) { visitor.value(value); }
		public Value(JsonValue value) { this.value = value;  }
        public boolean equals(Value other) { return eq(value, other.value);  }
        @Override public boolean equals(Object other) { return other instanceof Value && equals((Value)other); }
    }

	public  static class Operator extends ArrayList<Node> implements Node {
		
		public final String operator;
		public Operator(String operator) { this.operator = operator; }
        public Operator(String operator, JsonValue value) { this.operator = operator; add(new Value(value)); }
		@Override public void visit(Visitor<?> visitor) { visitor.operExpr(operator); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Operator other) { return eq(operator, other.operator) && super.equals(other); }
        @Override public boolean equals(Object other) { return other instanceof Operator && equals((Operator)other); }
	}
	
	public  static class And extends ArrayList<Node> implements Node { 
		public final JsonValue.ValueType type;
		public And(JsonValue.ValueType type, Node... items) { super(Arrays.asList(items)); this.type = type; }
		public And(JsonValue.ValueType type) { this.type = type; }
		@Override public void visit(Visitor<?> visitor) { visitor.andExpr(type); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(And other) { return eq(type, other.type);  }
        @Override public boolean equals(Object other) { return other instanceof And && equals((And)other); }
	}
	
	public  static class Between extends ArrayList<Node> implements Node { 
		public final JsonValue.ValueType type;
		public Between(JsonValue.ValueType type, Node lower, Node upper) { super(Arrays.asList(lower,upper)); this.type = type;  }
		public Between(JsonValue.ValueType type) { this.type = type; }
		@Override public void visit(Visitor<?> visitor) { visitor.betweenExpr(type); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Between other) { return eq(type, other.type);  }
        @Override public boolean equals(Object other) { return other instanceof Between && equals((Between)other); }
	}
	
		
	public  static class Or extends ArrayList<Node> implements Node { 
		public final JsonValue.ValueType type;
		public Or(JsonValue.ValueType type, List<? extends Node> items) { super(items); this.type = type; }
		public Or(JsonValue.ValueType type) { this.type = type; }		
		@Override public void visit(Visitor<?> visitor) { visitor.orExpr(type); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Or other) { return eq(type, other.type);  }
        @Override public boolean equals(Object other) { return other instanceof Or && equals((Or)other); }
	}
    
	
	public  static class Sub extends ArrayList<Node> implements Node {
		public final String operator;
		public Sub(String operator) { this.operator = operator;  }
		@Override public void visit(Visitor<?> visitor) { visitor.subExpr(operator); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Sub other) { return eq(operator, other.operator);  }
        @Override public boolean equals(Object other) { return other instanceof Sub && equals((Sub)other); }
	}
	
	public  static class Array extends ArrayList<Node> implements Node { 
		public Array(List<? extends Node> items) { super(items); }
		public Array() { super(); }		
		@Override public void visit(Visitor<?> visitor) { visitor.arrayExpr(); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Array other) { return super.equals(other);  }
        @Override public boolean equals(Object other) { return other instanceof Array && equals((Array)other); }
	}
	
	public  static class Query extends ArrayList<Node> implements Node { 
		public Query(List<? extends Node> items) { super(items); }
		public Query() { super(); }		
		@Override public void visit(Visitor<?> visitor) { visitor.queryExpr(); forEach(item->item.visit(visitor)); visitor.endExpr(); }
		public String toString() { return toExpression(DEFAULT); }
        public boolean equals(Query other) { return super.equals(other);  }
        @Override public boolean equals(Object other) { return other instanceof Query && equals((Query)other); }
	}

	public static class TreeFormatter implements Visitor<Node> {
        
        Stack<Node> nodes = new Stack<>();
        QualifiedName scope = QualifiedName.ROOT;
        Node result = null;
		
		@Override
		public Node getResult() {
			return result;
		}
        
        private void beginExpr(Node node) {
            if (!nodes.isEmpty()) nodes.lastElement().add(node);
            nodes.add(node);
        }
        
        @Override 
        public void endExpr() {
            result = nodes.pop();
            if (result instanceof Dimension) scope = scope.parent;
        }
		
		@Override
		public void operExpr(String operator) { beginExpr(new Operator(operator)); }
		@Override
		public void andExpr(JsonValue.ValueType type) { beginExpr(new And(type)); }
		@Override
		public void orExpr(JsonValue.ValueType type) { beginExpr(new Or(type)); }
		@Override
		public void subExpr(String operator) { beginExpr(new Sub(operator)); }
		@Override
		public void betweenExpr(JsonValue.ValueType type) { beginExpr(new Between(type)); }
		@Override
		public void unbounded() { nodes.lastElement().add(new Unbounded()); }
        @Override
        public void dimensionExpr(String dimension) { 
            scope = scope.add(dimension);
            beginExpr(new Dimension(scope)); 
        }
        @Override
        public void value(JsonValue value) { nodes.lastElement().add(new Value(value)); }
        @Override
        public void arrayExpr() { beginExpr(new Array()); }
        @Override
        public void queryExpr() { beginExpr(new Query()); }
	}
	
	public  static class Factorizer extends TreeFormatter {
		
		private static void triage(List<Node> ands, List<Node> queries, Or atomics, Node node) {
			if (node instanceof And) 		
                ands.add((And)node);
            else if (node instanceof Query) 
                queries.add((Query)node);
            else if (node instanceof Or)	
                ((Or)node).stream().forEach(inner->triage(ands, queries, atomics, inner));
			else 							
                atomics.add(node);			
		}	
		
		public Optional<Node> getCommonestFactor(List<Node> ands) {
			return ands
					.stream()
					.flatMap(List::stream) // Stream of all possible factors
					.collect(Collectors.groupingBy(node->node, Collectors.counting())) // count number of times each factor appears
					.entrySet()
					.stream()
					.filter(entry -> entry.getValue() > 1) // Ignore any factor that appears only once
					.sorted(Comparator.comparing(entry->entry.getKey().toString())) // sort so we factorize in a stable way
					.collect(
						Collectors.maxBy(
							Comparator.comparingLong(entry -> entry.getValue()) // Find the factor that appears the most times
						)
					)
					.map(entry -> entry.getKey());
			
		}
		
		public Optional<Node> factorize(JsonValue.ValueType type, List<Node> ands, Function<JsonValue.ValueType,Node> nodeFactory) {
			
			Optional<Node> factor = getCommonestFactor(ands);
			if (factor.isPresent()) {
				List<Node> factorized = new ArrayList<Node>();
				Or result = new Or(type);
				for (Node and : ands) {
					if (and.remove(factor.get())) 
						factorized.add(and); 
					else result.add(and);
				}
				Optional<Node> inner = factorize(type, factorized, nodeFactory);
				Node factored = null;
				if (inner.isPresent()) {
					// if inner is an 'And' we have no remainder
					if (inner.get() instanceof And || inner.get() instanceof Query) {
						factored = inner.get();
						factored.add(factor.get());
					} else {
						factored = nodeFactory.apply(type);
                        factored.add(factor.get());
                        factored.add(inner.get());
					}
				} else {
					factored = nodeFactory.apply(type);
                    factored.add(factor.get());
                    factored.add(new Or(type, factorized));
				}
				if (result.isEmpty()) 
					return Optional.of(factored);
				else {
					result.add(factored);
					return Optional.of(result);
				}			
			} else {
				return Optional.empty();
			}
		}
		
		@Override
		public void endExpr() {
            
            Node top = nodes.lastElement();
            if (top instanceof Or) {
                ValueType type = ((Or)top).type;
                Stream<Node> expressions = top.stream();

                final List<Node> ands = new ArrayList<>();
                final List<Node> queries = new ArrayList<>();
                final Or atomics = new Or(type);

                expressions.forEach(node -> triage(ands,queries,atomics,node));
                Optional<Node> factorizedAnds = factorize(type, ands, t->new And(t));
                Optional<Node> factorizedQueries = factorize(type, queries, t->new Query());
                
                if (factorizedAnds.isPresent()) {
                    Node node = factorizedAnds.get();
                    if (node instanceof Or) 
                        atomics.addAll(node);
                    else
                        atomics.add(node);
                }

                if (factorizedQueries.isPresent()) {
                    Node node = factorizedQueries.get();
                    if (node instanceof Or) 
                        atomics.addAll(node);
                    else
                        atomics.add(node);
                }
                
                if (factorizedAnds.isPresent() || factorizedQueries.isPresent()) {
                    nodes.pop();
                    nodes.push(atomics);
                }

            
            }
            super.endExpr();
		}
	}
	
	public  static Class<Factorizer> SIMPLIFY = Factorizer.class;	
	public  static Class<TreeFormatter> TREE =  TreeFormatter.class;
	/** Default formatter creates a compact string expression */
	public  static Class<DefaultFormat> DEFAULT = DefaultFormat.class;
	/** Default JSON creates a JSON representation */
	public  static Class<JsonFormat> JSON = JsonFormat.class;

}
