package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;

import static org.junit.Assert.*;
import visitor.Visitor;
import visitor.Visitors;

public class CubeTest {

	@Test
    public void canCreateCube() {
    	Query cube1 = Query.fromJson("{ 'x': 43, 'y': 33 }");
    	assertEquals(Range.equals(Json.createValue(43)), cube1.getConstraint("x"));
    	assertEquals(Range.equals(Json.createValue(33)), cube1.getConstraint("y"));
    }

	@Test
    public void hasWorkingContainsMethodIn2d() {
    	// Test with equals
    	Query cube1 = Query.fromJson("{ 'x': 43, 'y': 33 }");
    	Query cube2 = Query.fromJson("{ 'x': 33, 'y': 43 }");
    	Query cube3 = Query.fromJson("{ 'x': 43, 'y': 99 }");
    	Query cube4 = Query.fromJson("{ 'x': 22, 'y': 33 }");
    	Query cube5 = Query.fromJson("{ 'x': 43, 'y': 33 }");

    	assertFalse(cube1.contains(cube2));
    	assertFalse(cube1.contains(cube3));
    	assertFalse(cube1.contains(cube4));
    	assertTrue(cube1.contains(cube5));

    	// Expand as we add other range types
    }

	@Test
    public void hasWorkingRemoveContainsMethodIn2dd() {
    	Query.Impl cube1 = (Query.Impl)Query.fromJson("{ 'x': 43, 'y': 33 }");

		Query cube2 = cube1.removeConstraints(Query.fromJson("{'x':43}")); 
		assertEquals(Query.fromJson("{'y':33}"), cube2);

		boolean ok = true;
		
    	try {
    		cube1.removeConstraints(Query.fromJson("{'x':33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// Nothing
    	}
    	
    	assertTrue(ok);

    	try {
    		cube1.removeConstraints(Query.fromJson("{'z' : 33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// ok
    	}
    	
    	assertTrue(ok);
    }
	
	@Test public void canProgramaticallyCreateSubquery() {
		Query x = Query.from("x", Range.lessThan(Json.createValue(19)));
		Query y = Query.from("y", Range.greaterThan(Json.createValue(21)));
		Query sub = x.intersect(y);
		Query tags = Query.from("tags", ArrayConstraint.matchAny(Range.equals(Json.createValue("a")), Range.equals(Json.createValue("c"))));
		Query location = Query.from("location", sub);
		Query object = tags.intersect(location);
		
		JsonObject value1 = JsonUtil.parseObject("{ 'location': { 'x': 17, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		JsonObject value2 = JsonUtil.parseObject("{ 'location': { 'x': 21, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		assertTrue(object.containsItem(value1));
		assertFalse(object.containsItem(value2));
	}
	
	@Test public void canCreateSubqueryFromJson() {
		Query x = Query.from("x", Range.lessThan(Json.createValue(19)));
		Query y = Query.from("y", Range.greaterThan(Json.createValue(21)));
		Query sub = x.intersect(y);
		Query tags = Query.from("tags", ArrayConstraint.matchAny(Range.equals(Json.createValue("a")), Range.equals(Json.createValue("c"))));
		Query location = Query.from("location", sub);
		Query object1 = tags.intersect(location);
		
		
		Query object2 = Query.fromJson("{'location': { 'x': { '<':19 }, 'y' : { '>':21 } }, 'tags': { '$has': ['a','c'] } }");

		assertEquals(object1,object2);
	}
	
	@Test
    public void canBindParameters(){
        Query cube1 = Query.fromJson("{ 'x': [22, {'$':'param1' } ], 'y' : [ {'$':'param2'}, {'$':'param3'} ]}" );
        Query cube2 = cube1.bind("{ 'param5': 'slartibartfast'}");
        assertEquals(cube2, cube2);
        Query cube3 = cube1.bind("{ 'param1': 44, 'param3': 66}");
        assertEquals(Query.fromJson("{ 'x': [22, 44], 'y': [ { '$': 'param2'}, 66] }"), cube3);
        Query cube4 = cube1.bind("{ 'param1': 20, 'param3': 66}");
        assertNull(cube4);
        //todo: put back in a 'has' test
        //let cube5 = cube1.bind({ param1: 44, param2: 11, param3: 66, param4: 'idiocy'}
        //expect(cube5).to.deep.equal(new Cube( { x: [22, 44], y: [11, 66], z: { type: 'idiocy' } }));
    }

    @Test public void canCreateaAQueryOnAnArrayOfObjects() { 

    	Query query = Query.fromJson("{ 'a': '23', 'b': { '$has' : { 'drumkit': 'bongo' } } }");
    	

    	assertEquals("a='23' and b has (drumkit='bongo')", query.toExpression(Visitors.DEFAULT)); 
    }
    
    @Test public void canDecodeFromURL() {
        //This generated by javascript implementation
        String urlFormat = "eyJCcmFuY2giOiIxMDIyOCIsIk91clJlZmVyZW5jZSI6eyIkbGlrZSI6IlRFU1QtKiJ9LCJFdmVudFJlZmVyZW5jZSI6eyIkbGlrZSI6IklTUyoifX0=";
        Query result = Query.urlDecode(urlFormat);
        Query reference = Query.fromJson("{ 'Branch': '10228', 'EventReference': { '$like': 'ISS*' }, 'OurReference': { '$like': 'TEST-*'}}");
        assertEquals(reference, result);
    }
}