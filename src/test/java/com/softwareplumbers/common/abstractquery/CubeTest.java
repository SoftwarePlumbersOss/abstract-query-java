package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;
import org.junit.Test;

import static org.junit.Assert.*;

public class CubeTest {

	@Test
    public void canCreateCube() {
    	Cube cube1 = new Cube("{ 'x': 43, 'y': 33 }");
    	assertEquals(Range.equals(Value.from(43)), cube1.getConstraint("x"));
    	assertEquals(Range.equals(Value.from(33)), cube1.getConstraint("y"));
    }

	@Test
    public void hasWorkingContainsMethodIn2d() {
    	// Test with equals
    	Cube cube1 = new Cube("{ 'x': 43, 'y': 33 }");
    	Cube cube2 = new Cube("{ 'x': 33, 'y': 43 }");
    	Cube cube3 = new Cube("{ 'x': 43, 'y': 99 }");
    	Cube cube4 = new Cube("{ 'x': 22, 'y': 33 }");
    	Cube cube5 = new Cube("{ 'x': 43, 'y': 33 }");

    	assertFalse(cube1.contains(cube2));
    	assertFalse(cube1.contains(cube3));
    	assertFalse(cube1.contains(cube4));
    	assertTrue(cube1.contains(cube5));

    	// Expand as we add other range types
    }

	@Test
    public void hasWorkingRemoveContainsMethodIn2dd() {
    	Cube cube1 = new Cube("{ 'x': 43, 'y': 33 }");

		Cube cube2 = cube1.removeConstraints(new Cube("{'x':43}")); 
		assertEquals(new Cube("{'y':33}"), cube2);

		boolean ok = true;
		
    	try {
    		cube1.removeConstraints(new Cube("{'x':33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// Nothing
    	}
    	
    	assertTrue(ok);

    	try {
    		cube1.removeConstraints(new Cube("{'z' : 33}"));
    		ok = false;
    	} catch (RuntimeException err) {
    		// ok
    	}
    	
    	assertTrue(ok);
    }

    public void canBindParameters(){
        Cube cube1 = new Cube( "{ 'x': [22, {'$':'param1' } ], 'y' : [ {'$':'param2'}, {'$':'param3'} ]}" );
        Cube cube2 = cube1.bind("{ 'param5': 'slartibartfast'}");
        assertEquals(cube2, cube2);
        Cube cube3 = cube1.bind("{ 'param1': 44, 'param3': 66}");
        assertEquals(new Cube( "{ 'x': [22, 44], 'y': [ { '$': 'param2'}, 66] }"), cube3);
        Cube cube4 = cube1.bind("{ 'param1': 20, 'param3': 66}");
        assertNull(cube4);
        //todo: put back in a 'has' test
        //let cube5 = cube1.bind({ param1: 44, param2: 11, param3: 66, param4: 'idiocy'}
        //expect(cube5).to.deep.equal(new Cube( { x: [22, 44], y: [11, 66], z: { type: 'idiocy' } }));
    }

}