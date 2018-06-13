package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;
import org.junit.Test;

import static org.junit.Assert.*;

import javax.json.JsonObject;


public class QueryTest {

	@Test
    public void canCreateQuery() {
    	Query query = Query.from("{ 'x':2, 'y':4}");
    	assertTrue(query.containsItem(Value.fromJson("{ 'x':2, 'y':4}")));
    	assertFalse(query.containsItem(Value.fromJson("{ 'x':3, 'y':4}")));
    	assertFalse(query.containsItem(Value.fromJson("{ 'x':2, 'y':5}")));
    }

	@Test
    public void canUseAndToAddConstraints() {
    	Query query = Query
    		.from("{'x': 2, 'y': 4}")
    		.and("{'z': 5}");

    	assertEquals(Query.from("{'x': 2, 'y': 4, 'z': 5}"),query);
    }

	@Test
    public void canUseOrToAddConstraints() {
    	Query query = Query
    		.from("{'x': 2, 'y': 4}")
    		.or(Query.from("{'z': 5}"));

    	assertTrue(query.containsItem(Value.fromJson("{ 'x':2, 'y':4, 'z':3}")));
    	assertTrue(query.containsItem(Value.fromJson("{ 'x':3, 'y':4, 'z':5}")));
    }

    /*
    public void canCreateSubqueries() {
    	Query query = Query.from("{ currency: 'GBP', branch: { country: 'UK', type: 'accounting'}");
    };
    */

	@Test
    public void redundantConstraintsAreSuppressed() {
    	Query query = Query
    		.from("{'x': 2, 'y': 4}")
    		.or(Query.from("{ 'x': 2}"));
    	
    	assertEquals(Query.from("{'x': 2}"), query);
    } 

	@Test
    public void redundantParametrizedConstraintsAreSuppressed() {
        Query query = Query
            .from("{'x': { '$': 'param1'}, 'y':4}")
            .or(Query.from("{'x': { '$': 'param1'}}"));

        assertEquals(Query.from("{'x':{'$':'param1'}}"), query);

        query = Query
                .from("{'x': { '$': 'param1'}, 'y':4}")
                .and(Query.from("{'x': { '$': 'param1'}}"));

        assertEquals(Query.from("{'x': { '$': 'param1'}, 'y':4}"), query);
    } 

	@Test
    public void createsExpression() {
    	Query query = Query
    		.from("{'x': [null,2], 'y': 4}")
    		.and(Query.from("{ 'z': 5}"))
    		.or(Query.from("{'x':[6,8], 'y':3, 'z':99}"));

    	assertEquals("(x<2 and y=4 and z=5 or x>=6 and x<8 and y=3 and z=99)",query.toString());
    }    

	@Test
    public void createsExpressionWithOr() {
    	Query query = Query
    		.from("{'x': [null,2], 'y': 4}")
    		.and(Query.from("{ 'z': 5}").or(Query.from("{'z' : 8}")));


    	assertEquals("x<2 and y=4 and (z=5 or z=8)", query.toString());
    }
/*
    public void createsexpression with subquery() {
    	let query = Query
    		.from({x: [,2], y: { alpha: [2,6], beta: { nuts: 'brazil' }}}

    	let expression = query.toExpression();

    	expect(expression).to.equal('x<2 and y.alpha>=2 and y.alpha<6 and y.beta.nuts="brazil"');
    }

    public void createsexpression with has() {
        let query = Query
            .from({x: [,2], y: { alpha: [2,6], nuts: { $has: 'brazil' }}}

        let expression = query.toExpression();

        expect(expression).to.equal('x<2 and y.alpha>=2 and y.alpha<6 and y.nuts has($self="brazil")');
    }

    public void createsexpression with has and parameters() {
        let query = Query
            .from({x: [,2], y: { alpha: [2,6], nuts: { $has: $.param1 }}})
            .and({ y : {nuts: { $has: $.param2 }}}

        let expression = query.toExpression();

        expect(expression).to.equal('x<2 and y.nuts has($self=$param2) and y.nuts has($self=$param1) and y.alpha>=2 and y.alpha<6');
    }

    public void createsexpression with paramters() {
        let query = Query
            .from({x: [$.param1,2], y: $.param2}

        let expression = query.toExpression();

        expect(expression).to.equal('x>=$param1 and x<2 and y=$param2');
    }

    it('has working equals operation() {
    	let query1 = Query
    		.from({x: [,2], y: { alpha: [2,6], beta: { nuts: 'brazil' }}}
    	let query2 = Query
    		.from({y: { beta: { nuts: 'brazil' }, alpha: [2,6]}, x: [,2]}
    	let query3 = Query
    		.from({x: [,2], y: { alpha: [2,8], beta: { nuts: 'walnut' }}}
    	let query4 = Query
    		.from({x: [1,9], y: { alpha: [2,8], beta: { nuts: 'walnut' }}}
    	expect(query1.equals(query2)).to.be.true;
    	expect(query1.equals(query3)).to.be.false;
    	expect(query1.equals(query4)).to.be.false;
    	expect(query1.and(query3).equals(query3.and(query1))).to.be.true;
    	expect(query1.or(query3).equals(query3.or(query1))).to.be.true;
    }

    it('has working equals operation with parameters() {
        let query1 = Query
            .from({x: [,$.param1], y: { alpha: [2,6], beta: { nuts: $.param2 }}}
        let query2 = Query
            .from({y: { beta: { nuts: $.param2 }, alpha: [2,6]}, x: [,$.param1]}
        let query3 = Query
            .from({x: [,$.param1], y: { alpha: [2,6], beta: { nuts: $.param3 }}}
        expect(query1.equals(query2)).to.be.true;
        expect(query1.equals(query3)).to.be.false;
    }

    it('has working contains operation() {
    	let query1 = Query
    		.from({x: [,2], y: { alpha: [2,6], beta: { nuts: 'brazil' }}}
    	let query2 = Query
    		.from({y: { beta: { nuts: 'brazil' }, alpha: [2,6]}, x: [,2]}
    	let query3 = Query
    		.from({x: [1,2], y: { alpha: [2,8], beta: { nuts: 'walnut' }}}
    	let query4 = Query
    		.from({x: [1,9], y: { alpha: [2,8], beta: { nuts: 'walnut' }}}
    	expect(query1.contains(query2)).to.be.true; // because equal
    	expect(query1.contains(query3)).to.be.false; // walnut != brazil
    	expect(query1.contains(query4)).to.be.false; // [,2] doesn't contain [1,9]
    	expect(query4.contains(query3)).to.be.true; 
    	expect(query1.or(query4).contains(query1)).to.be.true;
    	expect(query1.contains(query1.and(query4))).to.be.true;
   	}

    it('has working contains operation with parameters() {
        let query2 = Query
            .from({x: [$.param1,2], y: { alpha: [2,$.param3], beta: { nuts: $.param2 }}}
        let query3 = Query
            .from({x: [$.param1,2], y: { alpha: [2,8], beta: { nuts: $.param2 }}}
        let query4 = Query
            .from({x: [$.param1,9], y: { alpha: [2,8], beta: { nuts: $.param2 }}}
        expect(query4.contains(query3)).to.be.true; 
        expect(query3.contains(query4)).to.be.false; 
        expect(query3.contains(query2)).to.be.null;
        expect(query2.contains(query3)).to.be.null;
    }

    it('factorizes() {
    	let query = Query
    		.from({x: 2, y : [3,4], z : 8})
    		.or({x:2, y: [,4], z: 7})
    		.or({x:3, y: [3,], z: 7}

    	let factored_part = Query
    		.from({y : [3,4], z : 8})
    		.or({y: [,4], z: 7})

    	let { factored, remainder } = query.factor({ x: 2}

    	expect(remainder).to.deep.equal(Query.from({x:3, y: [3,], z: 7}));
    	expect(factored).to.deep.equal(factored_part);
    }

    it('has sane JSON representation', ()=>{
    	let query = Query
    		.from({x: 2, y : [3,4], z : 8})
    		.or({x:2, y: [,4], z: 7})
    		.or({x:3, y: [3,], z: $.param1}
    	let json = JSON.stringify(query);
    	expect(json).to.equal('{"union":[{"x":2,"y":[3,4],"z":8},{"x":2,"y":[null,4],"z":7},{"x":3,"y":[3,null],"z":{"$":"param1"}}]}');
    }

    it('sample code for README.md tests OK', ()=>{
    	let query = Query
    		.from({ course: 'javascript 101', student: { age : [21,] }, grade: [,'C']})
    		.or({ course: 'medieval French poetry', student: { age: [40,65]}, grade: [,'C']})

    	let expr = query.toExpression();
    	expect(expr).to.equal('grade<"C" and (course="javascript 101" and student.age>=21 or course="medieval French poetry" and student.age>=40 and student.age<65)');
    
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
    }


    it('sample code for README.md with parameters tests OK', ()=>{
        let query = Query
            .from({ course: 'javascript 101', student: { age : [$.min_age,] }, grade: [,'C']})
            .or({ course: 'medieval French poetry', student: { age: [$.min_age, 65]}, grade: [,'C']})

        let expr = query.toExpression();
        expect(expr).to.equal('grade<"C" and (course="javascript 101" and student.age>=$min_age or course="medieval French poetry" and student.age>=$min_age and student.age<65)');

        let expr2 = query.bind({min_age: 27}).toExpression();
        expect(expr2).to.equal('grade<"C" and (course="javascript 101" and student.age>=27 or course="medieval French poetry" and student.age>=27 and student.age<65)');
    }

    it('sample code for README.md with predicate tests OK', ()=>{

        let data = [ 
            { name: 'jonathan', age: 12}, 
            { name: 'cindy', age: 18}, 
            { name: 'ada', age: 21} 
        ];

        let query = Query.from({ age: [,18]}
        let result = data.filter(query.predicate);

        expect(result).to.have.length(1);
    }

    it('sample code for README.md with subquery tests OK', ()=>{

            let data = [ 
                { name: 'jonathan', age: 47, expertise: [ { language:'java', level:'expert' }, { language:'javascript', level:'novice'}] }, 
                { name: 'cindy', age: 34, expertise: [ { language:'java', level:'novice' } ] }, 
                { name: 'ada', age: 32, expertise: [ { language:'javascript', level:'expert'} ] } 
            ];

            let expertise_query = Query.from({ language:'java' }
            let query = Query.from({ age: [,50], expertise: { $has : expertise_query }}

            debug(JSON.stringify(query));

            let result = data.filter(query.predicate);

            expect(result).to.have.length(2);
            expect(query.toExpression()).to.equal('age<50 and expertise has(language="java")');
            expect(query).to.deep.equal(Query.from({ age: [,50], expertise: { $has: { language:'java' }}}));

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

        let query1 = Query.from({ age: [26,]}
        let result = data.filter(query1.predicate);
        expect(result).to.have.length(2);
        expect(result).to.deep.equal(data.filter(item=>item.age>=26));
        let query2 = Query.from({ courses: { $has: { name: 'python'}}}
        let result2 = data.filter(query2.predicate);
        expect(result2).to.have.length(4);
        expect(result2).to.deep.equal(data.filter(item=>item.courses.find(course=>course.name==='python')));
        let query3 = Query.from({ tags: {$has: 'dull'}}
        let result3 = data.filter(query3.predicate);
        expect(result3).to.have.length(2);
        expect(result3).to.deep.equal(data.filter(item=>item.tags.includes('dull')));
        let query4 = Query.from({ tags: {$hasAll: ['old','dull']}}
        let result4 = data.filter(query4.predicate);
        expect(result4).to.have.length(1);
        expect(result4).to.deep.equal(data.filter(item=>item.tags.includes('dull') && item.tags.includes('old')));
       }
       */

	@Test
	public void canCreateQueryWithAnd() {
		Query query = Query.from("{ '$and': [ {'x':[2,5]}, {'x':[4,7]} ]}");
		assertEquals("x>=4 and x<5", query.toString());
	}
	
	@Test
	public void canCreateJsonOutput() {
    	Query query = Query
        		.from("{'x': [null,2], 'y': 4}")
        		.and(Query.from("{ 'z': 5}"))
        		.or(Query.from("{'x':[6,8], 'y':3, 'z':99}"));
    	JsonObject json = query.toJSON();
    	assertEquals("{\"$or\":[{\"x\":{\"<\":2},\"y\":4,\"z\":5},{\"x\":[6,8],\"y\":3,\"z\":99}]}", json.toString());
	}
	
	@Test
	public void canRoundtripJsonOutput() {
    	Query query = Query
        		.from("{'x': [null,2], 'y': 4}")
        		.and(Query.from("{ 'z': 5}"))
        		.or(Query.from("{'x':[6,8], 'y':3, 'z':99}"));
    	JsonObject json = query.toJSON();
    	Query query2 = Query.from(json);
    	assertEquals(query, query2);
 	}

	@Test
	public void canRoundtripUrlEncodedOutput() {
    	Query query = Query
        		.from("{'x': [null,2], 'y': 4}")
        		.and(Query.from("{ 'z': 5}"))
        		.or(Query.from("{'x':[6,8], 'y':3, 'z':99}"));
    	String encoded = query.urlEncode();
    	Query query2 = Query.urlDecode(encoded);
    	assertEquals(query, query2);
 	}

}