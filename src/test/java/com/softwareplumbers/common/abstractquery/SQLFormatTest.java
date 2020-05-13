/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.abstractquery;

import com.softwareplumbers.common.immutablelist.QualifiedName;
import com.softwareplumbers.common.abstractquery.visitor.Formatter;
import com.softwareplumbers.common.abstractquery.visitor.Visitors;
import com.softwareplumbers.common.abstractquery.visitor.Visitors.Relationship;
import com.softwareplumbers.common.abstractquery.visitor.Visitors.ParameterizedSQL;
import java.util.Map;
import javax.json.JsonValue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author jonathan
 */
public class SQLFormatTest {
    
    private static final QualifiedName STUDENTS = QualifiedName.of("student");
    private static final QualifiedName Y_BETA = QualifiedName.of("y","beta");
    
    public static String genericNameMapper(QualifiedName name) {
        if (name.isEmpty()) return "THINGS";
        if (name.equals(Y_BETA)) return "BETATHINGS";
        if (name.startsWith(Y_BETA)) return name.rightFromStart(2).join("_");
        return name.join("_");
    }
    
    public static String gradesNameMapper(QualifiedName name) {
        if (name.isEmpty()) return "GRADES";
        if (name.equals(STUDENTS)) return "STUDENTS";
        if (name.startsWith(STUDENTS)) return name.rightFromStart(1).join("_");
        return name.join("_");
    }
    
    public static String betaRelationship(Map<QualifiedName, String> aliases) {
        return aliases.get(QualifiedName.ROOT) + ".ID = " + aliases.get(Y_BETA) + ".ID";
    }
    
    public static String studentsRelationship(Map<QualifiedName, String> aliases) {
        return aliases.get(QualifiedName.ROOT) + ".ID = " + aliases.get(STUDENTS) + ".ID";
    }
      
    public static Relationship genericRelationships(QualifiedName name) {
        if (name.equals(Y_BETA)) return SQLFormatTest::betaRelationship;
        return null;
    }
    
    public static Relationship gradesRelationships(QualifiedName name) {
        if (name.equals(STUDENTS)) return SQLFormatTest::studentsRelationship;
        return null;
    }
    
    public static Formatter<String> GENERIC_FORMATTER = Visitors.SQL(SQLFormatTest::genericNameMapper, SQLFormatTest::genericRelationships);
    public static Formatter<ParameterizedSQL> GENERIC_FORMATTER_WITH_PARAMS = Visitors.ParameterizedSQL(SQLFormatTest::genericNameMapper, SQLFormatTest::genericRelationships);
    public static Formatter<String> GRADES_FORMATTER = Visitors.SQL(SQLFormatTest::gradesNameMapper, SQLFormatTest::gradesRelationships);
    
    @Test
    public void createsExpression() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(Query.fromJson("{ 'z': 5}"))
    		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z':99}"));

    	assertEquals("FROM THINGS T0 WHERE (T0.x<2 AND T0.y=4 AND T0.z=5 OR T0.x>=6 AND T0.x<8 AND T0.y=3 AND T0.z=99)", query.toExpression(GENERIC_FORMATTER));
    }  
    
    @Test
    public void createsParameterizedExpression() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': { '$': 'yparam'}}")
    		.intersect(Query.fromJson("{ 'z': 5}"))
    		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z': { '$': 'zparam'}}"));

        ParameterizedSQL result = query.toExpression(GENERIC_FORMATTER_WITH_PARAMS);
    	assertEquals("FROM THINGS T0 WHERE (T0.x<2 AND T0.y=? AND T0.z=5 OR T0.x>=6 AND T0.x<8 AND T0.y=3 AND T0.z=?)", result.sql);
        assertEquals("yparam", result.parameters.get(0));
        assertEquals("zparam", result.parameters.get(1));
    }  

    @Test
    public void createsExpressionWithNull() {
        Query query = Query.from("x", Range.equals(JsonValue.NULL));
    	assertEquals("FROM THINGS T0 WHERE T0.x IS NULL", query.toExpression(GENERIC_FORMATTER));
    }
    
    @Test
    public void createsExpressionWithOr() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': 4}")
                
    		.intersect(Query.fromJson("{ 'z': 5}").union(Query.fromJson("{'z' : 8}")));

    	assertEquals("FROM THINGS T0 WHERE T0.x<2 AND T0.y=4 AND (T0.z=5 OR T0.z=8)", query.toExpression(GENERIC_FORMATTER));
    }

	@Test
    public void createsExpressionWithSubquery() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");

    	String expression = query.toString();

    	assertEquals("FROM THINGS T0 INNER JOIN BETATHINGS T1 ON T0.ID = T1.ID WHERE T0.x<2 AND T0.y_alpha>=2 AND T0.y_alpha<6 AND T1.nuts='brazil'", query.toExpression(GENERIC_FORMATTER));
    }
    
    
    @Test
    public void sampleCodeForReadmeTestsOK() {
    	Query query = Query
    		.fromJson("{ 'course': 'javascript 101', 'student': { 'age' : [21, null] }, 'grade': [null,'C']}")
    		.union("{ 'course': 'medieval French poetry', 'student': { 'age': [40,65]}, 'grade': [null,'C']}");

    	String expr = query.toExpression(Visitors.SIMPLIFY).toExpression(GRADES_FORMATTER);
        
        assertEquals("FROM GRADES T0 INNER JOIN STUDENTS T1 ON T0.ID = T1.ID WHERE (T0.grade<'C' AND (T0.course='javascript 101' AND T1.age>=21 OR T0.course='medieval French poetry' AND T1.age>=40 AND T1.age<65))", expr);
    }
    
    @Test
    public void formatsSQL92Patterns() {
        Query query = Query.fromJson("{ 'course': { '$like': 'java*' } }");
    	String expr = query.toExpression(GRADES_FORMATTER);
        assertEquals("FROM GRADES T0 WHERE T0.course LIKE 'java%'", expr);
    }
}
