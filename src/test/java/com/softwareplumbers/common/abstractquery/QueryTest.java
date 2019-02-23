package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import com.softwareplumbers.common.QualifiedName;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

import org.junit.Test;

import static org.junit.Assert.*;

import javax.json.JsonObject;
import javax.json.JsonValue;


public class QueryTest {

	@Test
    public void canCreateCube() {
    	ObjectConstraint query = ObjectConstraint.fromJson("{ 'x':2, 'y':4}");
    	assertTrue(query.containsItem(Value.MapValue.fromJson("{ 'x':2, 'y':4}")));
    	assertFalse(query.containsItem(Value.MapValue.fromJson("{ 'x':3, 'y':4}")));
    	assertFalse(query.containsItem(Value.MapValue.fromJson("{ 'x':2, 'y':5}")));
    }

	@Test
    public void canUseAndToAddConstraints() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': 2, 'y': 4}")
    		.intersect("{'z': 5}");

    	assertEquals(ObjectConstraint.fromJson("{'x': 2, 'y': 4, 'z': 5}"),query);
    }

	@Test
    public void canUseOrToAddConstraints() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': 2, 'y': 4}")
    		.union("{'z': 5}");

    	assertTrue(query.containsItem(Value.MapValue.fromJson("{ 'x':2, 'y':4, 'z':3}")));
    	assertTrue(query.containsItem(Value.MapValue.fromJson("{ 'x':3, 'y':4, 'z':5}")));
    }

	@Test
    public void canCreateSubqueries() {
    	ObjectConstraint query = ObjectConstraint.fromJson("{ 'currency': 'GBP', 'branch': { 'country': 'UK', 'type': 'accounting'}}");
    	assertEquals("branch.country='UK' and branch.type='accounting' and currency='GBP'", query.toString());
    };

	@Test
    public void redundantConstraintsAreSuppressed() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': 2, 'y': 4}")
    		.union(ObjectConstraint.fromJson("{ 'x': 2}"));
    	
    	assertEquals(ObjectConstraint.fromJson("{'x': 2}"), query);
    } 

	@Test
    public void redundantParametrizedConstraintsAreSuppressed() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'x': { '$': 'param1'}, 'y':4}")
            .union(ObjectConstraint.fromJson("{'x': { '$': 'param1'}}"));

        assertEquals(ObjectConstraint.fromJson("{'x':{'$':'param1'}}"), query);

        query = ObjectConstraint
                .fromJson("{'x': { '$': 'param1'}, 'y':4}")
                .intersect(ObjectConstraint.fromJson("{'x': { '$': 'param1'}}"));

        assertEquals(ObjectConstraint.fromJson("{'x': { '$': 'param1'}, 'y':4}"), query);
    } 

	@Test
    public void createsExpression() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(ObjectConstraint.fromJson("{ 'z': 5}"))
    		.union(ObjectConstraint.fromJson("{'x':[6,8], 'y':3, 'z':99}"));

    	assertEquals("(x<2 and y=4 and z=5 or x>=6 and x<8 and y=3 and z=99)",query.toString());
    }    

	@Test
    public void createsExpressionWithOr() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': 4}")
    		.intersect(ObjectConstraint.fromJson("{ 'z': 5}").union(ObjectConstraint.fromJson("{'z' : 8}")));


    	assertEquals("x<2 and y=4 and (z=5 or z=8)", query.toString());
    }

	@Test
    public void createsExpressionWithSubquery() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");

    	String expression = query.toString();

    	assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.beta.nuts='brazil'", expression);
    }

	@Test
    public void createsExpressionWithHas() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'nuts': { '$has': 'brazil' }}}");

        String expression = query.toString();

        assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.nuts has ($self='brazil')", expression);
    }
	
	@Test
    public void createsExpressionWithHasInTopLevel() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'a': [null,2], 'nuts': { '$has': 'brazil' }}");

        String expression = query.toString();

        assertEquals("a<2 and nuts has ($self='brazil')", expression);
    }
	
	@Test
    public void createsExpressionWithHasOnObjects() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'a': [null,2], 'nuts': { '$has': { 'type': 'brazil' }}}");

        String expression = query.toString();
        String json = query.toJSON().toString();

        assertEquals("a<2 and nuts has (type='brazil')", expression);
        assertEquals("{\"a\":{\"<\":2},\"nuts\":{\"has\":{\"type\":\"brazil\"}}}", json);
    }

	@Test
	public void createsExpressionWithHasAndParameters() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'nuts': { '$has': { '$' : 'param1' } }}}")
            .intersect("{ 'y' : {'nuts': { '$has': { '$' : 'param2' } }}}");

        String expression = query.toString();

        assertEquals("x<2 and y.alpha>=2 and y.alpha<6 and y.nuts has ($self=$param1) and y.nuts has ($self=$param2)", expression);
    }

	@Test
	public void createExpressionWithParamters() {
        ObjectConstraint query = ObjectConstraint
            .fromJson("{'x': [{'$':'param1'},2], 'y': {'$':'param2'}}");

        String expression = query.toString();

        assertEquals("x>=$param1 and x<2 and y=$param2", expression);
    }

	@Test
    public void hasWorkingEqualsOperation() {
    	ObjectConstraint query1 = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");
    	ObjectConstraint query2 = ObjectConstraint
    		.fromJson("{'y': { 'beta': { 'nuts': 'brazil' }, 'alpha': [2,6]}, 'x': [null,2]}");
    	ObjectConstraint query3 = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	ObjectConstraint query4 = ObjectConstraint
    		.fromJson("{'x': [1,9], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	assertTrue(query1.equals(query2));
    	assertFalse(query1.equals(query3));
    	assertFalse(query1.equals(query4));
    	assertTrue(query1.intersect(query3).equals(query3.intersect(query1)));
    	assertTrue(query1.union(query3).equals(query3.union(query1)));
    }

	@Test
    public void hasWorkingEqualsOperationWithParameters(){
        ObjectConstraint query1 = ObjectConstraint
            .fromJson("{'x': [null,{'$':'param1'}], 'y': { 'alpha': [2,6], 'beta': { 'nuts': {'$':'param2'} }}}");
        ObjectConstraint query2 = ObjectConstraint
            .fromJson("{'y': { 'beta': { 'nuts': {'$':'param2'} }, 'alpha': [2,6]}, 'x': [null,{'$':'param1'}]}");
        ObjectConstraint query3 = ObjectConstraint
            .fromJson("{'x': [null,{'$':'param1'}], 'y': { 'alpha': [2,6], 'beta': { 'nuts': {'$':'param3'} }}}");
        assertTrue(query1.equals(query2));
        assertFalse(query1.equals(query3));
    }

	@Test
    public void hasWorkingContainsOperator() {
    	ObjectConstraint query1 = ObjectConstraint
    		.fromJson("{'x': [null,2], 'y': { 'alpha': [2,6], 'beta': { 'nuts': 'brazil' }}}");
    	ObjectConstraint query2 = ObjectConstraint
    		.fromJson("{'y': { 'beta': { 'nuts': 'brazil' }, 'alpha': [2,6]}, 'x': [null,2]}");
    	ObjectConstraint query3 = ObjectConstraint
    		.fromJson("{'x': [1,2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': 'walnut' }}}");
    	ObjectConstraint query4 = ObjectConstraint
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
        ObjectConstraint query2 = ObjectConstraint
            .fromJson("{'x': [{'$':'param1'},2], 'y': { 'alpha': [2,{'$':'param3'}], 'beta': { 'nuts': {'$':'param2'} }}}");
        ObjectConstraint query3 = ObjectConstraint
            .fromJson("{'x': [{'$':'param1'},2], 'y': { 'alpha': [2,8], 'beta': { 'nuts': {'$':'param2'} }}}");
        ObjectConstraint query4 = ObjectConstraint
            .fromJson("{'x': [{'$':'param1'},9], 'y': { 'alpha': [2,8], 'beta': { 'nuts': {'$':'param2'} }}}");
        assertTrue(query4.contains(query3)); 
        assertFalse(query3.contains(query4)); 
        assertNull(query3.contains(query2));
        assertNull(query2.contains(query3));
    }

	@Test
    public void factorizes() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': 2, 'y' : [3,4], 'z' : 8}")
    		.union("{'x':2, 'y': [null,4], 'z': 7}")
    		.union("{'x':3, 'y': [3,null], 'z': 7}");

    	// TODO: factor should break out the y<4 but does not because it's hidden in a 'between'.
    	assertEquals("(x=3 and y>=3 and z=7 or x=2 and (y>=3 and y<4 and z=8 or y<4 and z=7))", query.toExpression(Formatter.SIMPLIFY).toExpression(Formatter.DEFAULT));
    }

	@Test
    public void hasSaneJSONRepresentation() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{'x': 2, 'y' : [3,4], 'z' : 8}")
    		.union("{'x':2, 'y': [null,4], 'z': 7}")
    		.union("{'x':3, 'y': [3,null], 'z': {'$':'param1'}}");
    	String json = query.toJSON().toString();
    	assertEquals("{\"$or\":[{\"x\":2,\"y\":[3,4],\"z\":8},{\"x\":2,\"y\":{\"<\":4},\"z\":7},{\"x\":3,\"y\":{\">=\":3},\"z\":{\"$\":\"param1\"}}]}", json);
    }

    @Test
    public void sampleCodeForReadmeTestsOK() {
    	ObjectConstraint query = ObjectConstraint
    		.fromJson("{ 'course': 'javascript 101', 'student': { 'age' : [21, null] }, 'grade': [null,'C']}")
    		.union("{ 'course': 'medieval French poetry', 'student': { 'age': [40,65]}, 'grade': [null,'C']}");

    	String expr = query.toExpression(Formatter.SIMPLIFY).toExpression(Formatter.DEFAULT);
    	assertEquals("(grade<'C' and (course='javascript 101' and student.age>=21 or course='medieval French poetry' and student.age>=40 and student.age<65))", expr);
    /*
		const formatter = {
    		andExpr(...ands) { return ands.join(' and ') }, 
    		orExpr(...ors) { return "(" + ors.join(' or ') + ")"},
    		operExpr(dimension, operator, value, context) { 
    			return (operator === 'match')
    				? dimension + "[" + value + "]"
    				: dimension + operator + '"' + value + '"' 
    		}
    	}

		let expr2 = query.toExpression(formatter);
   		expect(expr2).to.equal('grade<"C" and (course="javascript 101" and student[age>="21"] or course="medieval French poetry" and student[age>="40" and age<"65"])');
   	*/
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

    /*
    it('sample code for README.md with predicate tests OK', ()=>{

        let data = [ 
            { name: 'jonathan', age: 12}, 
            { name: 'cindy', age: 18}, 
            { name: 'ada', age: 21} 
        ];

        Cube query = Cube.fromJson("{ age: [null,18]}
        let result = data.filter(query.predicate);

        expect(result).to.have.length(1);
    }

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
		ObjectConstraint query = ObjectConstraint.fromJson("{ '$and': [ {'x':[2,5]}, {'x':[4,7]} ]}");
		assertEquals("x>=4 and x<5", query.toString());
	}
	
	@Test
	public void canCreateJsonOutput() {
		ObjectConstraint cube1 = ObjectConstraint.fromJson("{'x': [null,2], 'y': 4}");
		JsonValue json1 = cube1.toJSON();
		ObjectConstraint cube2 = ObjectConstraint.fromJson("{ 'z': 5}");
		JsonValue json2 = cube2.toJSON();
		ObjectConstraint cube3 = ObjectConstraint.fromJson("{'x':[6,8], 'y':3, 'z':99}");
		JsonValue json3 = cube3.toJSON();
		
    	ObjectConstraint query = cube1.intersect(cube2).union(cube3);
    	JsonValue json = query.toJSON();
    	assertEquals("{\"$or\":[{\"x\":{\"<\":2},\"y\":4,\"z\":5},{\"x\":[6,8],\"y\":3,\"z\":99}]}", json.toString());
	}
	
	@Test
	public void canRoundtripJsonOutput() {
    	ObjectConstraint query = ObjectConstraint
        		.fromJson("{'x': [null,2], 'y': 4}")
        		.intersect(ObjectConstraint.fromJson("{ 'z': 5}"))
        		.union(ObjectConstraint.fromJson("{'x':[6,8], 'y':3, 'z':99}"));
    	JsonValue json = query.toJSON();
    	ObjectConstraint query2 = ObjectConstraint.from((JsonObject)json);
    	assertEquals(query, query2);
 	}

	@Test
	public void canRoundtripUrlEncodedOutput() {
    	ObjectConstraint query = ObjectConstraint
        		.fromJson("{'x': [null,2], 'y': 4}")
        		.intersect(ObjectConstraint.fromJson("{ 'z': 5}"))
        		.union(ObjectConstraint.fromJson("{'x':[6,8], 'y':3, 'z':99}"));
    	String encoded = query.urlEncode();
    	ObjectConstraint query2 = ObjectConstraint.urlDecode(encoded);
    	assertEquals(query, query2);
 	}
	
	@Test
	public void canCreateObjectConstraintWithLike() {
		ObjectConstraint queryA = ObjectConstraint.fromJson("{'x': { '$like': 'abc*' }}");
		ObjectConstraint queryB = ObjectConstraint.from("x", Range.like("abc*"));
		assertEquals(queryB, queryA);
	}
	
	@Test
	public void canOutputExpressionWithLike() {
		ObjectConstraint query = ObjectConstraint.from("x", Range.like("abc*"));
	}
	
	@Test
	public void testEquivalenceOfQualifedName() {
		ObjectConstraint query1 = ObjectConstraint.from("a", ObjectConstraint.from("b", ObjectConstraint.from("c", Range.equals(Value.from("d")))));
		ObjectConstraint query2 = ObjectConstraint.from(QualifiedName.of("a","b","c"), Range.equals(Value.from("d")));
		assertEquals(query1.toString(), query2.toString());
	}
}