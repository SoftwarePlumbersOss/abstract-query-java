package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import com.softwareplumbers.common.abstractquery.Value.MapValue;
import com.softwareplumbers.common.abstractquery.formatter.Formatter;

import org.junit.Test;

import static org.junit.Assert.*;

public class CubeTest {

	@Test
    public void canCreateCube() {
    	ObjectConstraint cube1 = ObjectConstraint.fromJson("{ 'x': 43, 'y': 33 }");
    	assertEquals(Range.equals(Value.from(43)), cube1.getConstraint("x"));
    	assertEquals(Range.equals(Value.from(33)), cube1.getConstraint("y"));
    }

	@Test
    public void hasWorkingContainsMethodIn2d() {
    	// Test with equals
    	ObjectConstraint cube1 = ObjectConstraint.fromJson("{ 'x': 43, 'y': 33 }");
    	ObjectConstraint cube2 = ObjectConstraint.fromJson("{ 'x': 33, 'y': 43 }");
    	ObjectConstraint cube3 = ObjectConstraint.fromJson("{ 'x': 43, 'y': 99 }");
    	ObjectConstraint cube4 = ObjectConstraint.fromJson("{ 'x': 22, 'y': 33 }");
    	ObjectConstraint cube5 = ObjectConstraint.fromJson("{ 'x': 43, 'y': 33 }");

    	assertFalse(cube1.contains(cube2));
    	assertFalse(cube1.contains(cube3));
    	assertFalse(cube1.contains(cube4));
    	assertTrue(cube1.contains(cube5));

    	// Expand as we add other range types
    }

	@Test
    public void hasWorkingRemoveContainsMethodIn2dd() {
    	ObjectConstraint.Impl cube1 = (ObjectConstraint.Impl)ObjectConstraint.fromJson("{ 'x': 43, 'y': 33 }");

		ObjectConstraint cube2 = cube1.removeConstraints(ObjectConstraint.fromJson("{'x':43}")); 
		assertEquals(ObjectConstraint.fromJson("{'y':33}"), cube2);

		boolean ok = true;
		
    	try {
    		cube1.removeConstraints(ObjectConstraint.fromJson("{'x':33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// Nothing
    	}
    	
    	assertTrue(ok);

    	try {
    		cube1.removeConstraints(ObjectConstraint.fromJson("{'z' : 33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// ok
    	}
    	
    	assertTrue(ok);
    }
	
	@Test public void canProgramaticallyCreateSubquery() {
		ObjectConstraint x = ObjectConstraint.from("x", Range.lessThan(Value.from(19)));
		ObjectConstraint y = ObjectConstraint.from("y", Range.greaterThan(Value.from(21)));
		ObjectConstraint sub = x.intersect(y);
		ObjectConstraint tags = ObjectConstraint.from("tags", ArrayConstraint.matchAny(Range.equals(Value.from("a")), Range.equals(Value.from("c"))));
		ObjectConstraint location = ObjectConstraint.from("location", sub);
		ObjectConstraint object = tags.intersect(location);
		
		Value.MapValue value1 = MapValue.fromJson("{ 'location': { 'x': 17, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		Value.MapValue value2 = MapValue.fromJson("{ 'location': { 'x': 21, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		assertTrue(object.containsItem(value1));
		assertFalse(object.containsItem(value2));
	}
	
	@Test public void canCreateSubqueryFromJson() {
		ObjectConstraint x = ObjectConstraint.from("x", Range.lessThan(Value.from(19)));
		ObjectConstraint y = ObjectConstraint.from("y", Range.greaterThan(Value.from(21)));
		ObjectConstraint sub = x.intersect(y);
		ObjectConstraint tags = ObjectConstraint.from("tags", ArrayConstraint.matchAny(Range.equals(Value.from("a")), Range.equals(Value.from("c"))));
		ObjectConstraint location = ObjectConstraint.from("location", sub);
		ObjectConstraint object1 = tags.intersect(location);
		
		
		ObjectConstraint object2 = ObjectConstraint.fromJson("{'location': { 'x': { '<':19 }, 'y' : { '>':21 } }, 'tags': { '$has': ['a','c'] } }");

		assertEquals(object1,object2);
	}
	
	@Test
    public void canBindParameters(){
        ObjectConstraint cube1 = ObjectConstraint.fromJson("{ 'x': [22, {'$':'param1' } ], 'y' : [ {'$':'param2'}, {'$':'param3'} ]}" );
        ObjectConstraint cube2 = cube1.bind("{ 'param5': 'slartibartfast'}");
        assertEquals(cube2, cube2);
        ObjectConstraint cube3 = cube1.bind("{ 'param1': 44, 'param3': 66}");
        assertEquals(ObjectConstraint.fromJson("{ 'x': [22, 44], 'y': [ { '$': 'param2'}, 66] }"), cube3);
        ObjectConstraint cube4 = cube1.bind("{ 'param1': 20, 'param3': 66}");
        assertNull(cube4);
        //todo: put back in a 'has' test
        //let cube5 = cube1.bind({ param1: 44, param2: 11, param3: 66, param4: 'idiocy'}
        //expect(cube5).to.deep.equal(new Cube( { x: [22, 44], y: [11, 66], z: { type: 'idiocy' } }));
    }

    @Test public void canCreateaAQueryOnAnArrayOfObjects() { 

    	ObjectConstraint query = ObjectConstraint.fromJson("{ 'a': '23', 'b': { '$has' : { 'drumkit': 'bongo' } } }");
    	

    	assertEquals("a='23' and b has (drumkit='bongo')", query.toExpression(Formatter.DEFAULT)); 
    }
}