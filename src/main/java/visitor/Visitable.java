package visitor;

public interface Visitable {
	void visit(Visitor<?> visitor);
    
	default <T, U extends Visitor<T>> T toExpression(Class<U> format) { 
        try {
            Visitor<T> visitor = format.newInstance();
            visit(visitor);
            return visitor.getResult();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }
}