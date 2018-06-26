package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import com.softwareplumbers.common.abstractquery.Value.MapValue;

import org.junit.Test;

import static org.junit.Assert.*;

public class CubeTest {

	@Test
    public void canCreateCube() {
    	Cube cube1 = Cube.fromJson("{ 'x': 43, 'y': 33 }");
    	assertEquals(Range.equals(Value.from(43)), cube1.getConstraint("x"));
    	assertEquals(Range.equals(Value.from(33)), cube1.getConstraint("y"));
    }

	@Test
    public void hasWorkingContainsMethodIn2d() {
    	// Test with equals
    	Cube cube1 = Cube.fromJson("{ 'x': 43, 'y': 33 }");
    	Cube cube2 = Cube.fromJson("{ 'x': 33, 'y': 43 }");
    	Cube cube3 = Cube.fromJson("{ 'x': 43, 'y': 99 }");
    	Cube cube4 = Cube.fromJson("{ 'x': 22, 'y': 33 }");
    	Cube cube5 = Cube.fromJson("{ 'x': 43, 'y': 33 }");

    	assertFalse(cube1.contains(cube2));
    	assertFalse(cube1.contains(cube3));
    	assertFalse(cube1.contains(cube4));
    	assertTrue(cube1.contains(cube5));

    	// Expand as we add other range types
    }

	@Test
    public void hasWorkingRemoveContainsMethodIn2dd() {
    	Cube.Impl cube1 = (Cube.Impl)Cube.fromJson("{ 'x': 43, 'y': 33 }");

		Cube cube2 = cube1.removeConstraints(Cube.fromJson("{'x':43}")); 
		assertEquals(Cube.fromJson("{'y':33}"), cube2);

		boolean ok = true;
		
    	try {
    		cube1.removeConstraints(Cube.fromJson("{'x':33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// Nothing
    	}
    	
    	assertTrue(ok);

    	try {
    		cube1.removeConstraints(Cube.fromJson("{'z' : 33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// ok
    	}
    	
    	assertTrue(ok);
    }
	
	@Test public void canProgramaticallyCreateSubquery() {
		Cube x = Cube.from("x", Range.lessThan(Value.from(19)));
		Cube y = Cube.from("y", Range.greaterThan(Value.from(21)));
		Cube sub = x.intersect(y);
		Cube tags = Cube.from("tags", Has.matchAny(Range.equals(Value.from("a")), Range.equals(Value.from("c"))));
		Cube location = Cube.from("location", sub);
		Cube object = tags.intersect(location);
		
		Value.MapValue value1 = MapValue.fromJson("{ 'location': { 'x': 17, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		Value.MapValue value2 = MapValue.fromJson("{ 'location': { 'x': 21, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		assertTrue(object.containsItem(value1));
		assertFalse(object.containsItem(value2));
	}
	
	@Test public void canCreateSubqueryFromJson() {
		Cube x = Cube.from("x", Range.lessThan(Value.from(19)));
		Cube y = Cube.from("y", Range.greaterThan(Value.from(21)));
		Cube sub = x.intersect(y);
		Cube tags = Cube.from("tags", Has.matchAny(Range.equals(Value.from("a")), Range.equals(Value.from("c"))));
		Cube location = Cube.from("location", sub);
		Cube object1 = tags.intersect(location);
		
		
		Cube object2 = Cube.fromJson("{'location': { 'x': { '<':19 }, 'y' : { '>':21 } }, 'tags': { '$has': ['a','c'] } }");

		assertEquals(object1,object2);
	}
	
	@Test
    public void canBindParameters(){
        Cube cube1 = Cube.fromJson("{ 'x': [22, {'$':'param1' } ], 'y' : [ {'$':'param2'}, {'$':'param3'} ]}" );
        Cube cube2 = cube1.bind("{ 'param5': 'slartibartfast'}");
        assertEquals(cube2, cube2);
        Cube cube3 = cube1.bind("{ 'param1': 44, 'param3': 66}");
        assertEquals(Cube.fromJson("{ 'x': [22, 44], 'y': [ { '$': 'param2'}, 66] }"), cube3);
        Cube cube4 = cube1.bind("{ 'param1': 20, 'param3': 66}");
        assertNull(cube4);
        //todo: put back in a 'has' test
        //let cube5 = cube1.bind({ param1: 44, param2: 11, param3: 66, param4: 'idiocy'}
        //expect(cube5).to.deep.equal(new Cube( { x: [22, 44], y: [11, 66], z: { type: 'idiocy' } }));
    }

    @Test public void canCreateaAQueryOnAnArrayOfObjects() { 

    	Cube query = Cube.fromJson("{ 'a': '23', 'b': { '$has' : { 'drumkit': 'bongo' } } }");
    	

    	assertEquals("a='23' and has (b.drumkit='bongo')", query.toExpression(Formatter.DEFAULT)); 
    }
}