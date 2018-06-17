package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import com.softwareplumbers.common.abstractquery.Value.Atomic;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;

public class RangeTest {

	@Test
    public void canCreateEquals() {
    	Range range1 = Range.equals(Value.from(37));
    	assertTrue(range1.containsItem(Value.from(37)));
    	assertFalse(range1.containsItem(Value.from(38)));
    	assertFalse(range1.containsItem(Value.from(36)));
    	Range range2 = Range.from("37");
    	assertEquals(range1, range2);
    }

	@Test
    public void canCreateLessThan() {
    	Range range1 = Range.lessThan(Value.from(37));
    	assertFalse(range1.containsItem(Value.from(37)));
    	assertFalse(range1.containsItem(Value.from(38)));
    	assertTrue(range1.containsItem(Value.from(36)));
    	Range range2 = Range.from("{ '<' : 37 }");
    	assertEquals(range1, range2);
    }

	@Test
    public void canCreateGreaterThan() {
    	Range range1 = Range.greaterThan(Value.from(37));
    	assertFalse(range1.containsItem(Value.from(37)));
    	assertTrue(range1.containsItem(Value.from(38)));
    	assertFalse(range1.containsItem(Value.from(36)));
    	Range range2 = Range.from("{ '>' : 37 }");
    	assertEquals(range1, range2);
    }

	@Test
    public void canCreateBetween() {
    	Range range1 = Range.between(Value.from(14), Value.from(37));
    	assertFalse(range1.containsItem(Value.from(11)));
    	assertFalse(range1.containsItem(Value.from(38)));
    	assertTrue(range1.containsItem(Value.from(36)));
    	Range range2 = Range.from("[14,37]");
    	assertEquals(range1, range2);
    }

    /*
    public void canCreatesubquery() {
    	Range range1 = Range.from({ name: 'test'}
    	expect(range1.query.union[0].name).to.deep.equal(Range.equals('test'));
    }

    public void canCreatehas() {
        Range range1 = Range.has('mytag');
        expect(range1.bounds[0]).to.deep.equal(Range.equals('mytag'));
        range1 = Range.has({ ">": 'bound'}
        expect(range1.bounds[0]).to.deep.equal(Range.greaterThan('bound'));
    }
    */

	@Test
    public void canCreateRangesFromBoundsObject() {
    	Range range1 = Range.from("{'=':5}");
    	assertEquals(range1,Range.equals(Value.from(5)));
    	range1 = Range.from("{'<':5}");
    	assertEquals(range1,Range.lessThan(Value.from(5)));
    	range1 = Range.from("{'>':5}");
    	assertEquals(range1,Range.greaterThan(Value.from(5)));
    	range1 = Range.from("{'<=':5}");
    	assertEquals(range1,Range.lessThanOrEqual(Value.from(5)));
    	range1 = Range.from("{'>=':5}");
    	assertEquals(range1,Range.greaterThanOrEqual(Value.from(5)));
        //range1 = Range.fromBounds({$has: {'=':5}}
        //assertEquals(range1,Range.has(Range.equals(Value.from(5))));
    }

	@Test
    public void toJSONCreatesJSON() {
    	Range range1 = Range.from("5");
    	assertEquals(range1.toJSON(), JsonUtil.parseValue("5"));
    	range1 = Range.from("{'<':5}");
    	assertEquals(range1.toJSON(), JsonUtil.parseObject("{'<':5}"));
    }

	@Test
    public void canCompareRangesWithEquals() {
		Range range1 = Range.equals(Value.from(37));
		Range range2 = Range.equals(Value.from(37));
		assertEquals(range1,range2);
		Range range3 = Range.equals(Value.from(14));
		assertNotEquals(range1,range3);
		Range range4 = Range.lessThan(Value.from(37));
		assertNotEquals(range1,range4);
		Range range5 = Range.lessThan(Value.from(37));
		assertEquals(range5,range4);
		Range range6 = Range.from("[14,37]");
		Range range7 = Range.from("[14,37]");
		assertEquals(range6,range7);
		Range range8 = Range.from("[15,37]");
		assertNotEquals(range6,range8);
		/*
		Range range9 = Range.from("{ 'name': 'morgan'}");
		Range range10 = Range.from("{ 'name': 'freeman'}");
		
		Range range11 = Range.from("{ 'name': 'morgan'}");
		assertNotEquals(range9,range10);
		assertEquals(range9,range11);
		*/
    }
	
	@Test
    public void canCompareParametrizedRangesWithEquals() {
        Range range1 = Range.equals(Value.from(37));
        Range range2 = Range.equals(Value.param("param1"));
        Range range3 = Range.equals(Value.param("param2"));
        Range range4 = Range.equals(Value.param("param1"));
        assertEquals(null, range2.maybeEquals(range3));
        assertEquals(true, range2.maybeEquals(range4));
        assertEquals(null, range2.maybeEquals(range1));
        assertEquals(null, range1.maybeEquals(range2));

        Range range5 = Range.lessThan(Value.param("param1"));
        Range range6 = Range.lessThan(Value.param("param1"));
        Range range7 = Range.lessThan(Value.param("param2"));
        assertEquals(true, range5.maybeEquals(range6));
        assertEquals(null, range5.maybeEquals(range7));
        assertEquals(false, range1.maybeEquals(range5));

        Range range8 = Range.between(Value.from(5), Value.param("param2"));
        Range range9 = Range.between(Value.from(5), Value.param("param1"));
        Range range10 = Range.between(Value.from(5), Value.param("param2"));
        Range range11 = Range.between(Value.from(4), Value.param("param2"));

        assertEquals(null, range8.maybeEquals(range9));
        assertEquals(true, range8.maybeEquals(range10));
        assertEquals(false, range8.maybeEquals(range11));
    }

	@Test
    public void correctContainmentForEquals() {
    	Range range1 = Range.equals(Value.from(37));
    	Range range2 = Range.equals(Value.from(14));
    	Range range3 = Range.equals(Value.from(37));

    	assertFalse(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForEqualsWithParameters() {
        Range range1 = Range.equals(Value.from(37));
        Range range2 = Range.equals(Value.param("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
    }

	@Test
    public void correctContainmentForLessThan() {
    	Range range1 = Range.lessThan(Value.from(37));
    	Range range2 = Range.lessThan(Value.from(14));
    	Range range3 = Range.lessThan(Value.from(37));

    	assertTrue(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForLessThanWithParameters() {
        Range range1 = Range.lessThan(Value.from(37));
        Range range2 = Range.lessThan(Value.param("param"));
        Range range3 = Range.lessThan(Value.param("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
        assertTrue(range2.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanAndIntersection() {
        Range range1 = Range.lessThan(Value.from(37));
        Range range2 = Range.lessThan(Value.param("param1"));
        Range range3 = Range.lessThan(Value.param("param2"));
        Range range4 = range1.intersect(range2).intersect(range3);
        Range range5 = Range.greaterThan(Value.param("param1"));
        Range range7 = Range.lessThan(Value.from(8));

        assertTrue(range1.contains(range4));
        assertTrue(range2.contains(range4));
        assertTrue(range3.contains(range4));
        assertFalse(range5.contains(range4));
        assertEquals(null,range7.contains(range4));
    }

	@Test
    public void correctContainmentForlessThanOrEqual() {
    	Range range1 = Range.lessThanOrEqual(Value.from(37));
    	Range range2 = Range.lessThanOrEqual(Value.from(14));
    	Range range3 = Range.lessThanOrEqual(Value.from(37));

    	assertTrue(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanOrEqualWithParameters() {
        Range range1 = Range.lessThanOrEqual(Value.from(37));
        Range range2 = Range.lessThanOrEqual(Value.param("param"));
        Range range3 = Range.lessThanOrEqual(Value.param("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
        assertTrue(range2.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanOrEqualLessThanEquals() {
    	Range range1 = Range.lessThanOrEqual(Value.from(37));
    	Range range2 = Range.equals(Value.from(37));
    	Range range3 = Range.lessThan(Value.from(37));

    	assertTrue(range1.contains(range3));
    	assertTrue(range1.contains(range2));
    	assertFalse(range3.contains(range1));
    	assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForlessThanOrEqualLessThanEqualsWithParameters() {
        Range range1 = Range.lessThanOrEqual(Value.param("param"));
        Range range2 = Range.equals(Value.param("param"));
        Range range3 = Range.lessThan(Value.param("param"));


        assertTrue(range1.contains(range3));
        assertTrue(range1.contains(range2));
        assertFalse(range3.contains(range1));
        assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForgreaterThan() {
    	Range range1 = Range.greaterThan(Value.from(37));
    	Range range2 = Range.greaterThan(Value.from(14));
    	Range range3 = Range.greaterThan(Value.from(37));

    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForgreaterThanOrEqual() {
    	Range range1 = Range.greaterThanOrEqual(Value.from(37));
    	Range range2 = Range.greaterThanOrEqual(Value.from(14));
    	Range range3 = Range.greaterThanOrEqual(Value.from(37));

    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForgreaterThanOrEqualGreterThanEquals() {
    	Range range1 = Range.greaterThanOrEqual(Value.from(37));
    	Range range2 = Range.equals(Value.from(37));
    	Range range3 = Range.greaterThan(Value.from(37));

    	assertTrue(range1.contains(range3));
    	assertTrue(range1.contains(range2));
    	assertFalse(range3.contains(range1));
    	assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForgreaterThanLessThan() {
    	Range range1 = Range.greaterThan(Value.from(37));
    	Range range2 = Range.greaterThan(Value.from(14));
    	Range range3 = Range.lessThan(Value.from(37));
    	Range range4 = Range.lessThan(Value.from(14));

    	assertFalse(range1.contains(range3));
    	assertFalse(range1.contains(range4));
    	assertFalse(range2.contains(range3));
    	assertFalse(range2.contains(range4));
    }

	@Test
    public void correctContainmentForgreaterThanLessThanWithParameters(){
        Range range1 = Range.greaterThan(Value.param("param1"));
        Range range2 = Range.greaterThan(Value.param("param2"));
        Range range3 = Range.lessThan(Value.param("param1"));
        Range range4 = Range.lessThan(Value.param("param2"));

        assertFalse(range1.contains(range3));
        assertFalse(range1.contains(range4));
        assertFalse(range2.contains(range3));
        assertFalse(range2.contains(range4));
    }
	
	@Test
    public void correctContainmentForbetween(){
    	Range range1 = Range.between(Value.from(14),Value.from(37));
    	Range range2 = Range.between(Value.from(15),Value.from(36));
    	Range range3 = Range.between(Value.from(13),Value.from(15));
    	Range range4 = Range.between(Value.from(36),Value.from(38));
    	Range range5 = Range.between(Value.from(12),Value.from(13));
    	Range range6 = Range.between(Value.from(38),Value.from(39));

    	assertTrue(range1.contains(range2));
    	assertFalse(range1.contains(range3));
    	assertFalse(range1.contains(range4));
    	assertFalse(range1.contains(range5));
    	assertFalse(range1.contains(range6));
    }

	@Test
    public void correctContainmentForbetweenWithParameters() {
        Range range1 = Range.between(Value.from(14),Value.param("param1"));
        Range range2 = Range.between(Value.from(15),Value.param("param1"));
        Range range3 = Range.between(Value.param("param1"),Value.from(15));
        Range range4 = Range.between(Value.param("param1"),Value.from(14));
        assertTrue(range1.contains(range2));
        assertTrue(range3.contains(range4));
    }

    /*
    public void correctContainmentForsubquery(){
    	Range range1 = Range.from({count: [3,]}
    	Range range2 = Range.from({count: [1,]}
    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1)).to.be.true;
    }

    public void correctContainmentForsubqueryWithParameters(){
        Range range1 = Range.from({count: [Value.param("param")1,]}
        Range range2 = Range.from({count: [Value.param("param2"),]}
        Range range3 = Range.from({count: Value.param("param")1}
        assertEquals(null, range1.contains(range2)); // cant tell, contains if Value.param("param")1 === Value.param("param2")
        assertEquals(null, range2.contains(range1));
        assertTrue(range1.contains(range3)); 
    }
    */

	@Test
    public void CorrectIntersectionForEquals() {
    	Range range1 = Range.equals(Value.from(37));
    	Range range2 = Range.equals(Value.from(14));
    	Range range3 = Range.equals(Value.from(37));

    	assertEquals(null,range1.intersect(range2));
    	assertEquals(null,range2.intersect(range1));
    	assertEquals(range1.intersect(range3),range1);
    }

	@Test
    public void CorrectIntersectionForEqualsWithParameters() {
        Range range1 = Range.equals(Value.param("param1"));
        Range range2 = Range.equals(Value.from(14));
        Range range3 = Range.equals(Value.param("param1"));

        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1.intersect(range3),range1);
    }

	@Test
    public void CorrectIntersectionForlessThan() {
    	Range range1 = Range.lessThan(Value.from(37));
    	Range range2 = Range.lessThan(Value.from(14));
    	Range range3 = Range.lessThan(Value.from(37));

    	assertEquals(range1.intersect(range2), range2);
    	assertEquals(range2.intersect(range1), range2);
    	assertEquals(range1.intersect(range3), range1);
    }

	@Test
    public void CorrectIntersectionForlessThanWithParameters() {
        Range range1 = Range.lessThan(Value.param("param"));
        Range range2 = Range.lessThan(Value.from(14));
        Range range3 = Range.lessThan(Value.param("param"));
        
        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1, range1.intersect(range3));
    }

	@Test
    public void CorrectIntersectionForgreaterThan() {
    	Range range1 = Range.greaterThan(Value.from(37));
    	Range range2 = Range.greaterThan(Value.from(14));
    	Range range3 = Range.greaterThan(Value.from(37));

    	assertEquals(range1.intersect(range2), range1);
    	assertEquals(range2.intersect(range1), range1);
    	assertEquals(range1.intersect(range3), range1);
    }


	@Test
    public void CorrectIntersectionForgreaterThanWithParameters() {
        Range range1 = Range.greaterThan(Value.param("param"));
        Range range2 = Range.greaterThan(Value.from(14));
        Range range3 = Range.greaterThan(Value.param("param"));

        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1, range1.intersect(range3));    
    }


	@Test
    public void CorrectIntersectionForgreaterThanLessThan() {
    	Range range1 = Range.lessThan(Value.from(37));
    	Range range2 = Range.greaterThan(Value.from(14));
    	Range range3 = Range.lessThan(Value.from(14));
    	Range range4 = Range.greaterThan(Value.from(37));

    	assertEquals(range1.intersect(range2), Range.between(range2,range1));
    	assertEquals(range2.intersect(range1), Range.between(range2,range1));
    	assertNull(range3.intersect(range4));
    	assertNull(range4.intersect(range3));
    }

	@Test
    public void CorrectIntersectionForgreaterThanLessThanWithParameters() {
        Range range1 = Range.lessThan(Value.param("param1"));
        Range range2 = Range.greaterThan(Value.param("param2"));
        Range range3 = Range.lessThan(Value.param("param2"));
        Range range4 = Range.greaterThan(Value.param("param1"));

        assertEquals(range1.intersect(range2),Range.between(range2,range1));
        assertEquals(range2.intersect(range1),Range.between(range2,range1));
        assertNull(range1.intersect(range4));
        assertNull(range2.intersect(range3));
        assertEquals(range1.intersect(range3),range3.intersect(range1));
        assertEquals(range2.intersect(range4),range4.intersect(range2));
    }

/*
    public void CorrectIntersectionForsubquery() {
    	Range range1 = Range.from({value: [3,10]}
    	Range range2 = Range.from({value: [5,12]}
    	expect(range1.intersect(range2)).to.deep.equal(Range.from({value: [5,10]}));
    }
*/

	@Test
	public void usedBoundsObjectsInRangeFrom() {
    	Range range1 = Range.from("{ '>':5}");
    	assertEquals(range1,Range.greaterThan(Value.from(5)));
    	Range range2 = Range.from("[2,8]");
    	assertEquals(range2,Range.between(Value.from(2),Value.from(8)));
    	Range range3 = Range.from("[{'>':2}, {'<=':8}]");
    	assertEquals(range3,Range.between(Range.greaterThan(Value.from(2)),Range.lessThanOrEqual(Value.from(8))));
    }
	
	@Test
    public void canCreaterangeWithParameters() {
        Range range1 = Range.lessThan(Value.param("bottom"));
        Range range2 = Range.lessThan(Value.param("top"));
        assertNotEquals(range1, range2);
    }
/*
    public void canCreaterange with and() {
        Range range1 = Range.greaterThanOrEqual($.bottom);
        Range range2 = Range.lessThan($.top);
        Range range3 = Range.and([range1, range2]);
        expect(range3).to.deep.equal(Range.between($.bottom, $.top));
    }
*/
    @Test
    public void correctIntersectionForBetween() {

        Range range1 = Range.between(Value.from(14),Value.from(37));
        Range range2 = Range.between(Value.from(15),Value.from(36));
        Range range3 = range1.intersect(range2);

        assertEquals(range3, range2);

        Range range4 = Range.between(Value.from(14),Value.from(17));
        Range range5 = Range.between(Value.from(29),Value.from(36));
        Range range6 = range4.intersect(range5);

        assertNull(range6);

        Range range7 = Range.between(Value.from(14),Value.from(17));
        Range range8 = Range.between(Value.from(17),Value.from(36));
        Range range9 = range7.intersect(range8);

        assertNull(range9);

    }
/*
    public void CorrectIntersectionForbetweenWithParameters() {

        Range range1 = Range.between(Value.from(14),37);
        Range range2 = Range.between(Value.param("param1"),Value.from(36));
        Range range3 = range1.intersect(range2);

        expect(range3.known_bounds).to.deep.equal(Range.between(Value.from(14),Value.from(36)));
        expect(range3.parametrized_bounds.param1).to.deep.equal(Range.greaterThanOrEqual(Value.param("param1")));

        Range range4 = Range.between(Value.from(14),Value.param("param1"));
        Range range5 = Range.between(Value.from(15),Value.from(36));
        Range range6 = range4.intersect(range5);

        expect(range6.known_bounds).to.deep.equal(Range.between(Value.from(15),Value.from(36)));
        expect(range6.parametrized_bounds.param1).to.deep.equal(Range.lessThan(Value.param("param1")));

        Range range7 = Range.between(Value.from(14),Value.param("param1"));
        Range range8 = Range.between(Value.from(15),Value.param("param1"));
        Range range9 = range7.intersect(range8);

        expect(range9).to.deep.equal(Range.between(Value.from(15), Value.param("param1")));

        Range range10 = Range.between(Value.from(14),Value.param("param1"));
        Range range11 = Range.between(Value.param("param1"),Value.from(36));
        Range range12 = range10.intersect(range11);

        expect(range12).to.be.null;
    }
*/
    @SuppressWarnings("serial")
	@Test
    public void canBindParameters() {
    	
    	final Map<Param, Value> map1 = new HashMap<Param, Value>() {{
    		put(Param.from("param1"), Value.from(34));
    		put(Param.from("param2"), Value.from(55));
    	}};

    	final Map<Param, Value> map2 = new HashMap<Param, Value>() {{
    		put(Param.from("param1"), Value.from(55));
    		put(Param.from("param2"), Value.from(34));
    	}};

    	final Map<Param, Value> map3 = new HashMap<Param, Value>() {{
    		put(Param.from("param2"), Value.from(34));
    	}};

    	final Map<Param, Value> map4 = new HashMap<Param, Value>() {{
    		put(Param.from("param1"), Value.from(55));
    	}};

        Range range1 = Range.between(Value.param("param1"), Value.param("param2")).intersect(Range.lessThan(Value.from(50)));
        assertEquals(range1.bind(map1),Range.between(Value.from(34), Value.from(50)));
        assertNull(range1.bind(map2));
        assertEquals(range1.bind(map3), Range.between(Value.param("param1"), Value.from(34)));
        assertNull(range1.bind(map4));
    }
/*
    public void canBindParametersForHas() {
        Range range1 = Range.hasAll([Value.param("param")1, Value.param("param2")]);
        expect(range1.bind({param1: 34, param2: 55})).to.deep.equal(Range.hasAll([34, 55]));
        expect(range1.bind({param1: 34, param2: 34})).to.deep.equal(Range.has(34));
    }

    public void CorrectIntersectionForhas() {
        Range range1 = Range.from({ $has: 6 }
        Range range2 = Range.from({ $has: 8 }
        Range range3 = range1.intersect(range2);
        expect(range3).to.deep.equal(Range.from({$hasAll: [6,8]}));
    }
*/
    
    @Test
    public void canCreateFromJson() {
    	Range range = Range.from("[2,4]");
    	assertEquals(Range.between(Value.from(2), Value.from(4)), range);
    	range = Range.from("[2,null]");
    	assertEquals(Range.greaterThanOrEqual(Value.from(2)), range);
    	range = Range.from("{ '$': 'param1'}");
    	assertEquals(Range.equals(Value.param("param1")), range);
    }
}
