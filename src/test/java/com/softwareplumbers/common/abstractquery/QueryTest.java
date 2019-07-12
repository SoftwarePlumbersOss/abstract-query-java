package com.softwareplumbers.common.abstractquery;

import com.softwareplumbers.common.QualifiedName;
import com.softwareplumbers.common.jsonview.JsonViewFactory;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.json.Json;

import org.junit.Test;

import static org.junit.Assert.*;

import javax.json.JsonObject;
import javax.json.JsonValue;
import com.softwareplumbers.common.abstractquery.visitor.Visitors;


public class QueryTest {

	@Test
    public void canCreateCube() {
    	Query query = Query.fromJson("{ 'x':2, 'y':4}");
    	assertTrue(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y':4}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':3, 'y':4}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y':5}")));
    }

	@Test
    public void canUseAndToAddConstraints() {
    	Query query = Query
    		.fromJson("{'x': 2, 'y': 4}")
    		.intersect("{'z': 5}");

    	assertEquals(Query.fromJson("{'x': 2, 'y': 4, 'z': 5}"),query);
    }

	@Test
    public void canUseOrToAddConstraints() {
    	Query query = Query
    		.fromJson("{'x': 2, 'y': 4}")
    		.union("{'z': 5}");

    	assertTrue(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y':4, 'z':3}")));
    	assertTrue(query.containsItem(JsonUtil.parseObject("{ 'x':3, 'y':4, 'z':5}")));
    }

	@Test
    public void canCreateSubqueries() {
    	Query query = Query.fromJson("{ 'currency': 'GBP', 'branch': { 'country': 'UK', 'type': 'accounting'}}");
    	assertEquals("branch.country='UK' and branch.type='accounting' and currency='GBP'", query.toString());
    };

    private static QualifiedName renameCurrency(QualifiedName name) {
        return QualifiedName.of(name.equals(QualifiedName.of("currency")) ? "ccy" : name.part);
    }

    private static QualifiedName renameBranch(QualifiedName name) {
        return QualifiedName.of(name.equals(QualifiedName.of("branch")) ? "unit" : name.part);
    }
    
    private static QualifiedName renameCountry(QualifiedName name) {
        return QualifiedName.of(name.equals(QualifiedName.of("branch","country")) ? "ctry" : name.part);
    }

    
    @Test
    public void canRenameFields() {
    	Query query = Query.fromJson("{ 'currency': 'GBP', 'branch': { 'country': 'UK', 'type': 'accounting'}}");
    	assertEquals("branch.country='UK' and branch.type='accounting' and ccy='GBP'", 
                query.toExpression(Visitors.DEFAULT.transform(Visitors.rename(QueryTest::renameCurrency))));
    	assertEquals("unit.country='UK' and unit.type='accounting' and currency='GBP'", 
                query.toExpression(Visitors.DEFAULT.transform(Visitors.rename(QueryTest::renameBranch))));
    	assertEquals("branch.ctry='UK' and branch.type='accounting' and currency='GBP'", 
                query.toExpression(Visitors.DEFAULT.transform(Visitors.rename(QueryTest::renameCountry))));
    };
    
    private static QualifiedName pullUpBranch(QualifiedName name) {
        return (name.equals(QualifiedName.of("branch"))) ? QualifiedName.ROOT : QualifiedName.of(name.part);
    }
    
    @Test
    public void canPullUpFields() {
    	Query query = Query.fromJson("{ 'currency': 'GBP', 'branch': { 'country': 'UK', 'type': 'accounting'}}");
    	assertEquals("country='UK' and type='accounting' and currency='GBP'", 
                query.toExpression(Visitors.DEFAULT.transform(Visitors.rename(QueryTest::pullUpBranch))));
    };

    private static QualifiedName pushDownCurrency(QualifiedName name) {
        return (name.equals(QualifiedName.of("currency"))) ? QualifiedName.of("ccy","code") : QualifiedName.of(name.part);
    }

    @Test
    public void canPushDownFields() {
    	Query query = Query.fromJson("{ 'currency': 'GBP', 'branch': { 'country': 'UK', 'type': 'accounting'}}");
    	assertEquals("branch.country='UK' and branch.type='accounting' and ccy.code='GBP'", 
                query.toExpression(Visitors.DEFAULT.transform(Visitors.rename(QueryTest::pushDownCurrency))));
    };

	@Test
    public void redundantConstraintsAreSuppressed() {
    	Query query = Query
    		.fromJson("{'x': 2, 'y': 4}")
    		.union(Query.fromJson("{ 'x': 2}"));
    	
    	assertEquals(Query.fromJson("{'x': 2}"), query);
    } 

	@Test
    public void redundantParametrizedConstraintsAreSuppressed() {
        Query query = Query
            .fromJson("{'x': { '$': 'param1'}, 'y':4}")
            .union(Query.fromJson("{'x': { '$': 'param1'}}"));

        assertEquals(Query.fromJson("{'x':{'$':'param1'}}"), query);

        query = Query
                .fromJson("{'x': { '$': 'param1'}, 'y':4}")
                .intersect(Query.fromJson("{'x': { '$': 'param1'}}"));

        assertEquals(Query.fromJson("{'x': { '$': 'param1'}, 'y':4}"), query);
    } 

	@Test
    public void createsExpression() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(Query.fromJson("{ 'z': 5}"))
    		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z':99}"));

    	assertEquals("(x<2 and y=4 and z=5 or x>=6 and x<8 and y=3 and z=99)",query.toString());
    }    

	@Test
    public void createsSameExpressExpressionFromTree() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(Query.fromJson("{ 'z': 5}"))
    		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z':99}"));

    	assertEquals("(x<2 and y=4 and z=5 or x>=6 and x<8 and y=3 and z=99)",query.toExpression(Visitors.TREE).toExpression(Visitors.DEFAULT));
    }    
    
    @Test
    public void createsExpressionWithOr() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(Query.fromJson("{ 'z': 5}").union(Query.fromJson("{'z' : 8}")));


    	assertEquals("x<2 and y=4 and (z=5 or z=8)", query.toString());
    }

	@Test
    public void createsExpressionWithSubquery() {
    	Query query = Query
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");

    	String expression = query.toString();

    	assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.beta.nuts='brazil'", expression);
    }

	@Test
    public void createsExpressionWithHas() {
        Query query = Query
            .fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'nuts': { '$has': 'brazil' }}}");

        String expression = query.toString();

        assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.nuts has ($self='brazil')", expression);
    }
	
	@Test
    public void createsExpressionWithHasInTopLevel() {
        Query query = Query
            .fromJson("{'a': [null,2], 'nuts': { '$has': 'brazil' }}");

        String expression = query.toString();

        assertEquals("a<2 and nuts has ($self='brazil')", expression);
    }
	
	@Test
    public void createsExpressionWithHasOnObjects() {
        Query query = Query
            .fromJson("{'a': [null,2], 'nuts': { '$has': { 'type': 'brazil' }}}");

        String expression = query.toString();
        String json = query.toJSON().toString();

        assertEquals("a<2 and nuts has (type='brazil')", expression);
        assertEquals("{\"a\":{\"<\":2},\"nuts\":{\"has\":{\"type\":\"brazil\"}}}", json);
    }

	@Test
	public void createsExpressionWithHasAndParameters() {
        Query query = Query
            .fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'nuts': { '$has': { '$' : 'param1' } }}}")
            .intersect("{ 'y' : {'nuts': { '$has': { '$' : 'param2' } }}}");

        String expression = query.toString();

        assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.nuts has ($self=$param1) and y.nuts has ($self=$param2)", expression);
    }

	@Test
	public void createExpressionWithParamters() {
        Query query = Query
            .fromJson("{'x': [{'$':'param1'},2], 'y': {'$':'param2'}}");

        String expression = query.toString();

        assertEquals("x>=$param1 and x<2 and y=$param2", expression);
    }

	@Test
    public void hasWorkingEqualsOperation() {
    	Query query1 = Query
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");
    	Query query2 = Query
    		.fromJson("{'y': { 'beta': { 'nuts': 'brazil' }, 'alpha': [2,6]}, 'x': [null,2]}");
    	Query query3 = Query
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	Query query4 = Query
    		.fromJson("{'x': [1,9], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	assertTrue(query1.equals(query2));
    	assertFalse(query1.equals(query3));
    	assertFalse(query1.equals(query4));
    	assertTrue(query1.intersect(query3).equals(query3.intersect(query1)));
    	assertTrue(query1.union(query3).equals(query3.union(query1)));
    }

	@Test
    public void hasWorkingEqualsOperationWithParameters(){
        Query query1 = Query
            .fromJson("{'x': [null,{'$':'param1'}], 'y': { 'alpha': [2,6], 'beta': { 'nuts': {'$':'param2'} }}}");
        Query query2 = Query
            .fromJson("{'y': { 'beta': { 'nuts': {'$':'param2'} }, 'alpha': [2,6]}, 'x': [null,{'$':'param1'}]}");
        Query query3 = Query
            .fromJson("{'x': [null,{'$':'param1'}], 'y': { 'alpha': [2,6], 'beta': { 'nuts': {'$':'param3'} }}}");
        assertTrue(query1.equals(query2));
        assertFalse(query1.equals(query3));
    }

	@Test
    public void hasWorkingContainsOperator() {
    	Query query1 = Query
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");
    	Query query2 = Query
    		.fromJson("{'y': { 'beta': { 'nuts': 'brazil' }, 'alpha': [2,6]}, 'x': [null,2]}");
    	Query query3 = Query
    		.fromJson("{'x': [1,2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	Query query4 = Query
    		.fromJson("{'x': [1,9], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	assertTrue(query1.contains(query2)); // because equal
    	assertFalse(query1.contains(query3)); // walnut != brazil
    	assertFalse(query1.contains(query4)); // [null,2] doesn't contain [1,9]
    	assertTrue(query4.contains(query3)); 
    	assertTrue(query1.union(query4).contains(query1));
    	// TODO: Do all sets contain the empty set? q1 intersect q2 is empty here
    	//assertTrue(query1.contains(query1.intersect(query4)));
   	}
    
	@Test
    public void hasWorkingContainsOperatorWithParameters() {
        Query query2 = Query
            .fromJson("{'x': [{'$':'param1'},2], 'y': { 'alpha': [2,{'$':'param3'}], 'beta': { 'nuts': {'$':'param2'} }}}");
        Query query3 = Query
            .fromJson("{'x': [{'$':'param1'},2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': {'$':'param2'} }}}");
        Query query4 = Query
            .fromJson("{'x': [{'$':'param1'},9], 'y': { 'alpha': [2,8], 'beta': { 'nuts': {'$':'param2'} }}}");
        assertTrue(query4.contains(query3)); 
        assertFalse(query3.contains(query4)); 
        assertNull(query3.contains(query2));
        assertNull(query2.contains(query3));
    }

	@Test
    public void factorizes() {
    	Query query = Query
    		.fromJson("{'x': 2, 'y' : [3,4], 'z' : 8}")
    		.union("{'x':2, 'y': [null,4], 'z': 7}")
    		.union("{'x':3, 'y': [3,null], 'z': 7}");

    	// TODO: factor should break out the y<4 but does not because it's hidden in a 'between'.
    	assertEquals("(x=3 and y>=3 and z=7 or x=2 and (y>=3 and y<4 and z=8 or y<4 and z=7))", query.toExpression(Visitors.SIMPLIFY).toExpression(Visitors.DEFAULT));
    }

	@Test
    public void hasSaneJSONRepresentation() {
    	Query query = Query
    		.fromJson("{'x': 2, 'y' : [3,4], 'z' : 8}")
    		.union("{'x':2, 'y': [null,4], 'z': 7}")
    		.union("{'x':3, 'y': [3,null], 'z': {'$':'param1'}}");
    	String json = query.toJSON().toString();
    	assertEquals("{\"$or\":[{\"x\":2,\"y\":[3,4],\"z\":8},{\"x\":2,\"y\":{\"<\":4},\"z\":7},{\"x\":3,\"y\":{\">=\":3},\"z\":{\"$\":\"param1\"}}]}", json);
    }

    public static class MyVisitor extends Visitors.DefaultFormat {
        public String formatAndExpr(List<String> ands) { return ands.stream().collect(Collectors.joining(" && ")); }
        public String formatOrExpr(List<String> ors) { return "(" + ors.stream().collect(Collectors.joining(" || ")) + ")"; }
    }

    @Test
    public void sampleCodeForReadmeTestsOK() {
    	Query query = Query
    		.fromJson("{ 'course': 'javascript 101', 'student': { 'age' : [21, null] }, 'grade': [null,'C']}")
    		.union("{ 'course': 'medieval French poetry', 'student': { 'age': [40,65]}, 'grade': [null,'C']}");

    	String expr = query.toExpression(Visitors.SIMPLIFY).toExpression(Visitors.DEFAULT);
    	assertEquals("(grade<'C' and (course='javascript 101' and student.age>=21 or course='medieval French poetry' and student.age>=40 and student.age<65))", expr);
    
		String expr2 = query.toExpression(Visitors.SIMPLIFY).toExpression(MyVisitor::new);
   		assertEquals("(grade<'C' && (course='javascript 101' && student.age>=21 || course='medieval French poetry' && student.age>=40 && student.age<65))", expr2);
    }

/*
    it('sample code for README.md with parameters tests OK', ()=>{
        Cube query = Cube
            .fromJson("{ course: 'javascript 101', student: { age : [$.min_age,] }, grade: [null,'C']})
            .union({ course: 'medieval French poetry', student: { age: [$.min_age, 65]}, grade: [null,'C']})

        let expr = query.toString();
        expect(expr).to.equal('grade<"C" and (course="javascript 101" and student.age>=$min_age or course="medieval French poetry" and student.age>=$min_age and student.age<65)');

        let expr2 = query.bind({min_age: 27}).toString();
        expect(expr2).to.equal('grade<"C" and (course="javascript 101" and student.age>=27 or course="medieval French poetry" and student.age>=27 and student.age<65)');
    }

    */
    
    public static class Person {
        public int age;
        public String name;
        
        public int getAge() { return age; }
        public String getName() { return name; }
    }
    
    @Test
    public void sampleCodeForPredicateSearchTestsOK()
    {

        List<Person> data = Arrays.asList( 
            new Person() {{ name="jonathan"; age=14; }}, 
            new Person() {{ name="cindy"; age=18; }}, 
            new Person() {{ name="ada"; age=21; }} 
        );

        Query query = Query.fromJson("{ 'age': [null,18]}");
        List<Person> result = data.stream().filter(query.predicate().compose(JsonViewFactory::asJsonObject)).collect(Collectors.toList());
        assertEquals(1, result.size());
    }
    /*

    it('sample code for README.md with subquery tests OK', ()=>{

            let data = [ 
                { name: 'jonathan', age: 47, expertise: [ { language:'java', level:'expert' }, { language:'javascript', level:'novice'}] }, 
                { name: 'cindy', age: 34, expertise: [ { language:'java', level:'novice' } ] }, 
                { name: 'ada', age: 32, expertise: [ { language:'javascript', level:'expert'} ] } 
            ];

            let expertise_query = Cube.fromJson("{ language:'java' }
            Cube query = Cube.fromJson("{ age: [null,50], expertise: { $has : expertise_query }}

            debug(JSON.stringify(query));

            let result = data.filter(query.predicate);

            expect(result).to.have.length(2);
            expect(query.toString()).to.equal('age<50 and expertise has(language="java")');
            expect(query).to.deep.equal(Cube.fromJson("{ age: [null,50], expertise: { $has: { language:'java' }}}));

    }

    it('can filter a stream', ()=>{
        let data = [
            {   name: 'jonathan', 
                age: '47', 
                courses: [ 
                    { name: 'javascript', grade: 'A' }, 
                    { name: 'python', grade: 'B'} 
                ],
                tags: [ 'old', 'smart'] 
            },
            {   name: 'peter', 
                age: '19', 
                courses: [ 
                    { name: 'javascript', grade: 'C' }, 
                ],
                tags: ['young', 'dull']
            },
            {   name: 'paul', 
                age: '25', 
                courses: [ 
                    { name: 'python', grade: 'B'} 
                ],
                tags: ['young']
            },
            {   name: 'cindy', 
                age: '25', 
                courses: [ 
                    { name: 'javascript', grade: 'A' }, 
                    { name: 'python', grade: 'A'} 
                ], 
                tags: ['young','smart']
            },
            {   name: 'steve', 
                age: '29', 
                courses: [ 
                    { name: 'javascript', grade: 'C' }, 
                    { name: 'python', grade: 'F'} 
                ],
                tags: [ 'old', 'dull']
            }
        ]

        Cube query1 = Cube.fromJson("{ age: [26,]}
        let result = data.filter(query1.predicate);
        expect(result).to.have.length(2);
        expect(result).to.deep.equal(data.filter(item=>item.age>=26));
        Cube query2 = Cube.fromJson("{ courses: { $has: { name: 'python'}}}
        let result2 = data.filter(query2.predicate);
        expect(result2).to.have.length(4);
        expect(result2).to.deep.equal(data.filter(item=>item.courses.find(course=>course.name==='python')));
        Cube query3 = Cube.fromJson("{ tags: {$has: 'dull'}}
        let result3 = data.filter(query3.predicate);
        expect(result3).to.have.length(2);
        expect(result3).to.deep.equal(data.filter(item=>item.tags.includes('dull')));
        Cube query4 = Cube.fromJson("{ tags: {$hasAll: ['old','dull']}}
        let result4 = data.filter(query4.predicate);
        expect(result4).to.have.length(1);
        expect(result4).to.deep.equal(data.filter(item=>item.tags.includes('dull') && item.tags.includes('old')));
       }
       */

	@Test
	public void canCreateCubeWithAnd() {
		Query query = Query.fromJson("{ '$and': [ {'x':[2,5]}, {'x':[4,7]} ]}");
		assertEquals("x>=4 and x<5", query.toString());
	}
	
	@Test
	public void canCreateJsonOutput() {
		Query cube1 = Query.fromJson("{'x': [null,2], 'y': 4}");
		JsonValue json1 = cube1.toJSON();
		Query cube2 = Query.fromJson("{ 'z': 5}");
		JsonValue json2 = cube2.toJSON();
		Query cube3 = Query.fromJson("{'x':[6,8], 'y':3, 'z':99}");
		JsonValue json3 = cube3.toJSON();
		
    	Query query = cube1.intersect(cube2).union(cube3);
    	JsonValue json = query.toJSON();
    	assertEquals("{\"$or\":[{\"x\":{\"<\":2},\"y\":4,\"z\":5},{\"x\":[6,8],\"y\":3,\"z\":99}]}", json.toString());
	}
	
	@Test
	public void canRoundtripJsonOutput() {
    	Query query = Query
        		.fromJson("{'x': [null,2], 'y': 4}")
        		.intersect(Query.fromJson("{ 'z': 5}"))
        		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z':99}"));
    	JsonValue json = query.toJSON();
    	Query query2 = Query.from((JsonObject)json);
    	assertEquals(query, query2);
 	}

	@Test
	public void canRoundtripUrlEncodedOutput() {
    	Query query = Query
        		.fromJson("{'x': [null,2], 'y': 4}")
        		.intersect(Query.fromJson("{ 'z': 5}"))
        		.union(Query.fromJson("{'x':[6,8], 'y':3, 'z':99}"));
    	String encoded = query.urlEncode();
    	Query query2 = Query.urlDecode(encoded);
    	assertEquals(query, query2);
 	}
	
	@Test
	public void canCreateObjectConstraintWithLike() {
		Query queryA = Query.fromJson("{'x': { '$like': 'abc*' }}");
		Query queryB = Query.from("x", Range.like("abc*"));
		assertEquals(queryB, queryA);
	}
	
	@Test
	public void canOutputExpressionWithLike() {
		Query query = Query.from("x", Range.like("abc*"));
	}
	
	@Test
	public void testEquivalenceOfQualifedName() {
		Query query1 = Query.from("a", Query.from("b", Query.from("c", Range.equals(Json.createValue("d")))));
		Query query2 = Query.from(QualifiedName.of("a","b","c"), Range.equals(Json.createValue("d")));
		assertEquals(query1.toString(), query2.toString());
	}
	
	@Test
	public void testOutputUnbounded() {
		assertEquals("<unbounded>", Query.UNBOUNDED.toString());
		assertEquals("<unbounded>", Query.from("test", ArrayConstraint.match(Query.UNBOUNDED)).toString());
	}
    
    @Test
    public void testContainsItemWithSubQuery() {
    	Query query = Query.fromJson("{ 'x':2, 'y': { 'z': 3}}");
    	assertTrue(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y': { 'z': 3}}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':3, 'y': { 'z': 3}}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y': { 'z': 4}}")));
    	query = Query.fromJson("{ 'y': { 'z': 3}, 'x':2 }");
    	assertTrue(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y': { 'z': 3}}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':3, 'y': { 'z': 3}}")));
    	assertFalse(query.containsItem(JsonUtil.parseObject("{ 'x':2, 'y': { 'z': 4}}")));
    }
}