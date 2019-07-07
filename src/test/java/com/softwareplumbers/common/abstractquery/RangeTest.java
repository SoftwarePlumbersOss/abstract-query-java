package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;

public class RangeTest {

	@Test
    public void canCreateEquals() {
    	Range range1 = Range.equals(Json.createValue(37));
    	assertTrue(range1.containsItem(Json.createValue(37)));
    	assertFalse(range1.containsItem(Json.createValue(38)));
    	assertFalse(range1.containsItem(Json.createValue(36)));
    	Range range2 = Range.from("37");
    	assertEquals(range1, range2);
    }
	
	@Test
    public void canCreateEqualsBoolean() {
    	Range range1 = Range.equals(JsonValue.FALSE);
    	assertTrue(range1.containsItem(JsonValue.FALSE));
    	assertFalse(range1.containsItem(JsonValue.TRUE));
    	Range range2 = Range.from("false");
    	assertEquals(range1, range2);
    }

	@Test
    public void canCreateLessThan() {
    	Range range1 = Range.lessThan(Json.createValue(37));
    	assertFalse(range1.containsItem(Json.createValue(37)));
    	assertFalse(range1.containsItem(Json.createValue(38)));
    	assertTrue(range1.containsItem(Json.createValue(36)));
    	Range range2 = Range.from("{ '<' : 37 }");
    	assertEquals(range1, range2);
    }

	@Test
    public void canCreateLessThanBoolean() {
    	Range range1 = Range.lessThan(JsonValue.FALSE);
    	assertFalse(range1.containsItem(JsonValue.FALSE));
    	assertFalse(range1.containsItem(JsonValue.TRUE));
    }

	@Test
    public void canCreateGreaterThan() {
    	Range range1 = Range.greaterThan(Json.createValue(37));
    	assertFalse(range1.containsItem(Json.createValue(37)));
    	assertTrue(range1.containsItem(Json.createValue(38)));
    	assertFalse(range1.containsItem(Json.createValue(36)));
    	Range range2 = Range.from("{ '>' : 37 }");
    	assertEquals(range1, range2);
    }

	@Test
    public void canGreaterThanBoolean() {
    	Range range1 = Range.greaterThan(JsonValue.FALSE);
    	assertFalse(range1.containsItem(JsonValue.FALSE));
    	assertTrue(range1.containsItem(JsonValue.TRUE));
    }

	@Test
    public void canCreateBetween() {
    	Range range1 = Range.between(Json.createValue(14), Json.createValue(37));
    	assertFalse(range1.containsItem(Json.createValue(11)));
    	assertFalse(range1.containsItem(Json.createValue(38)));
    	assertTrue(range1.containsItem(Json.createValue(36)));
    	Range range2 = Range.from("[14,37]");
    	assertEquals(range1, range2);
    }

    public void canCreateHas() {
        ArrayConstraint<JsonValue,Range> range1 = ArrayConstraint.match(Range.equals(Json.createValue("mytag")));
        
        JsonArray val1 = Json.createArrayBuilder().add("one").add("two").add("mytag").add("arkensaw").build();
        JsonArray val2 = Json.createArrayBuilder().add("one").add("two").add("arkensaw").build();
        assertTrue(range1.containsItem(val1));
        assertTrue(range1.containsItem(val2));
    }
    

	@Test
    public void canCreateRangesFromBoundsObject() {
    	Range range1 = Range.from("{'=':5}");
    	assertEquals(range1,Range.equals(Json.createValue(5)));
    	range1 = Range.from("{'<':5}");
    	assertEquals(range1,Range.lessThan(Json.createValue(5)));
    	range1 = Range.from("{'>':5}");
    	assertEquals(range1,Range.greaterThan(Json.createValue(5)));
    	range1 = Range.from("{'<=':5}");
    	assertEquals(range1,Range.lessThanOrEqual(Json.createValue(5)));
    	range1 = Range.from("{'>=':5}");
    	assertEquals(range1,Range.greaterThanOrEqual(Json.createValue(5)));
        //range1 = Range.fromBounds({$has: {'=':5}}
        //assertEquals(range1,Range.has(Range.equals(Json.createValue(5))));
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
		Range range1 = Range.equals(Json.createValue(37));
		Range range2 = Range.equals(Json.createValue(37));
		assertEquals(range1,range2);
		Range range3 = Range.equals(Json.createValue(14));
		assertNotEquals(range1,range3);
		Range range4 = Range.lessThan(Json.createValue(37));
		assertNotEquals(range1,range4);
		Range range5 = Range.lessThan(Json.createValue(37));
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
        Range range1 = Range.equals(Json.createValue(37));
        Range range2 = Range.equals(Param.from("param1"));
        Range range3 = Range.equals(Param.from("param2"));
        Range range4 = Range.equals(Param.from("param1"));
        assertEquals(null, range2.maybeEquals(range3));
        assertEquals(true, range2.maybeEquals(range4));
        assertEquals(null, range2.maybeEquals(range1));
        assertEquals(null, range1.maybeEquals(range2));

        Range range5 = Range.lessThan(Param.from("param1"));
        Range range6 = Range.lessThan(Param.from("param1"));
        Range range7 = Range.lessThan(Param.from("param2"));
        assertEquals(true, range5.maybeEquals(range6));
        assertEquals(null, range5.maybeEquals(range7));
        assertEquals(false, range1.maybeEquals(range5));

        Range range8 = Range.between(Json.createValue(5), Param.from("param2"));
        Range range9 = Range.between(Json.createValue(5), Param.from("param1"));
        Range range10 = Range.between(Json.createValue(5), Param.from("param2"));
        Range range11 = Range.between(Json.createValue(4), Param.from("param2"));

        assertEquals(null, range8.maybeEquals(range9));
        assertEquals(true, range8.maybeEquals(range10));
        assertEquals(false, range8.maybeEquals(range11));
    }

	@Test
    public void correctContainmentForEquals() {
    	Range range1 = Range.equals(Json.createValue(37));
    	Range range2 = Range.equals(Json.createValue(14));
    	Range range3 = Range.equals(Json.createValue(37));

    	assertFalse(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForEqualsWithParameters() {
        Range range1 = Range.equals(Json.createValue(37));
        Range range2 = Range.equals(Param.from("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
    }

	@Test
    public void correctContainmentForLessThan() {
    	Range range1 = Range.lessThan(Json.createValue(37));
    	Range range2 = Range.lessThan(Json.createValue(14));
    	Range range3 = Range.lessThan(Json.createValue(37));

    	assertTrue(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForLessThanWithParameters() {
        Range range1 = Range.lessThan(Json.createValue(37));
        Range range2 = Range.lessThan(Param.from("param"));
        Range range3 = Range.lessThan(Param.from("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
        assertTrue(range2.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanAndIntersection() {
        Range range1 = Range.lessThan(Json.createValue(37));
        Range range2 = Range.lessThan(Param.from("param1"));
        Range range3 = Range.lessThan(Param.from("param2"));
        Range range4 = range1.intersect(range2).intersect(range3);
        Range range5 = Range.greaterThan(Param.from("param1"));
        Range range7 = Range.lessThan(Json.createValue(8));

        assertTrue(range1.contains(range4));
        assertTrue(range2.contains(range4));
        assertTrue(range3.contains(range4));
        assertFalse(range5.contains(range4));
        assertEquals(null,range7.contains(range4));
    }

	@Test
    public void correctContainmentForlessThanOrEqual() {
    	Range range1 = Range.lessThanOrEqual(Json.createValue(37));
    	Range range2 = Range.lessThanOrEqual(Json.createValue(14));
    	Range range3 = Range.lessThanOrEqual(Json.createValue(37));

    	assertTrue(range1.contains(range2));
    	assertFalse(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanOrEqualWithParameters() {
        Range range1 = Range.lessThanOrEqual(Json.createValue(37));
        Range range2 = Range.lessThanOrEqual(Param.from("param"));
        Range range3 = Range.lessThanOrEqual(Param.from("param"));

        assertEquals(null, range1.contains(range2));
        assertEquals(null, range2.contains(range1));
        assertTrue(range2.contains(range3));
    }

	@Test
    public void correctContainmentForlessThanOrEqualLessThanEquals() {
    	Range range1 = Range.lessThanOrEqual(Json.createValue(37));
    	Range range2 = Range.equals(Json.createValue(37));
    	Range range3 = Range.lessThan(Json.createValue(37));

    	assertTrue(range1.contains(range3));
    	assertTrue(range1.contains(range2));
    	assertFalse(range3.contains(range1));
    	assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForlessThanOrEqualLessThanEqualsWithParameters() {
        Range range1 = Range.lessThanOrEqual(Param.from("param"));
        Range range2 = Range.equals(Param.from("param"));
        Range range3 = Range.lessThan(Param.from("param"));


        assertTrue(range1.contains(range3));
        assertTrue(range1.contains(range2));
        assertFalse(range3.contains(range1));
        assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForgreaterThan() {
    	Range range1 = Range.greaterThan(Json.createValue(37));
    	Range range2 = Range.greaterThan(Json.createValue(14));
    	Range range3 = Range.greaterThan(Json.createValue(37));

    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForgreaterThanOrEqual() {
    	Range range1 = Range.greaterThanOrEqual(Json.createValue(37));
    	Range range2 = Range.greaterThanOrEqual(Json.createValue(14));
    	Range range3 = Range.greaterThanOrEqual(Json.createValue(37));

    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1));
    	assertTrue(range1.contains(range3));
    }

	@Test
    public void correctContainmentForgreaterThanOrEqualGreterThanEquals() {
    	Range range1 = Range.greaterThanOrEqual(Json.createValue(37));
    	Range range2 = Range.equals(Json.createValue(37));
    	Range range3 = Range.greaterThan(Json.createValue(37));

    	assertTrue(range1.contains(range3));
    	assertTrue(range1.contains(range2));
    	assertFalse(range3.contains(range1));
    	assertFalse(range3.contains(range2));
    }

	@Test
    public void correctContainmentForgreaterThanLessThan() {
    	Range range1 = Range.greaterThan(Json.createValue(37));
    	Range range2 = Range.greaterThan(Json.createValue(14));
    	Range range3 = Range.lessThan(Json.createValue(37));
    	Range range4 = Range.lessThan(Json.createValue(14));

    	assertFalse(range1.contains(range3));
    	assertFalse(range1.contains(range4));
    	assertFalse(range2.contains(range3));
    	assertFalse(range2.contains(range4));
    }

	@Test
    public void correctContainmentForgreaterThanLessThanWithParameters(){
        Range range1 = Range.greaterThan(Param.from("param1"));
        Range range2 = Range.greaterThan(Param.from("param2"));
        Range range3 = Range.lessThan(Param.from("param1"));
        Range range4 = Range.lessThan(Param.from("param2"));

        assertFalse(range1.contains(range3));
        assertFalse(range1.contains(range4));
        assertFalse(range2.contains(range3));
        assertFalse(range2.contains(range4));
    }
	
	@Test
    public void correctContainmentForbetween(){
    	Range range1 = Range.between(Json.createValue(14),Json.createValue(37));
    	Range range2 = Range.between(Json.createValue(15),Json.createValue(36));
    	Range range3 = Range.between(Json.createValue(13),Json.createValue(15));
    	Range range4 = Range.between(Json.createValue(36),Json.createValue(38));
    	Range range5 = Range.between(Json.createValue(12),Json.createValue(13));
    	Range range6 = Range.between(Json.createValue(38),Json.createValue(39));

    	assertTrue(range1.contains(range2));
    	assertFalse(range1.contains(range3));
    	assertFalse(range1.contains(range4));
    	assertFalse(range1.contains(range5));
    	assertFalse(range1.contains(range6));
    }

	@Test
    public void correctContainmentForbetweenWithParameters() {
        Range range1 = Range.between(Json.createValue(14),Param.from("param1"));
        Range range2 = Range.between(Json.createValue(15),Param.from("param1"));
        Range range3 = Range.between(Param.from("param1"),Json.createValue(15));
        Range range4 = Range.between(Param.from("param1"),Json.createValue(14));
        assertTrue(range1.contains(range2));
        assertTrue(range3.contains(range4));
    }

    /*
    public void correctContainmentForsubquery(){
    	Range range1 = Range.from({count: [3,]});
    	Range range2 = Range.from({count: [1,]});
    	assertFalse(range1.contains(range2));
    	assertTrue(range2.contains(range1)).to.be.true;
    }

    public void correctContainmentForsubqueryWithParameters(){
        Range range1 = Range.from({count: [Param.from("param")1,]}
        Range range2 = Range.from({count: [Param.from("param2"),]}
        Range range3 = Range.from({count: Param.from("param")1}
        assertEquals(null, range1.contains(range2)); // cant tell, contains if Param.from("param")1 === Param.from("param2")
        assertEquals(null, range2.contains(range1));
        assertTrue(range1.contains(range3)); 
    }
    */

	@Test
    public void CorrectIntersectionForEquals() {
    	Range range1 = Range.equals(Json.createValue(37));
    	Range range2 = Range.equals(Json.createValue(14));
    	Range range3 = Range.equals(Json.createValue(37));

    	assertEquals(Range.EMPTY,range1.intersect(range2));
    	assertEquals(Range.EMPTY,range2.intersect(range1));
    	assertEquals(range1.intersect(range3),range1);
    }

	@Test
    public void CorrectIntersectionForEqualsWithParameters() {
        Range range1 = Range.equals(Param.from("param1"));
        Range range2 = Range.equals(Json.createValue(14));
        Range range3 = Range.equals(Param.from("param1"));

        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1.intersect(range3),range1);
    }

	@Test
    public void CorrectIntersectionForlessThan() {
    	Range range1 = Range.lessThan(Json.createValue(37));
    	Range range2 = Range.lessThan(Json.createValue(14));
    	Range range3 = Range.lessThan(Json.createValue(37));

    	assertEquals(range1.intersect(range2), range2);
    	assertEquals(range2.intersect(range1), range2);
    	assertEquals(range1.intersect(range3), range1);
    }

	@Test
    public void CorrectIntersectionForlessThanWithParameters() {
        Range range1 = Range.lessThan(Param.from("param"));
        Range range2 = Range.lessThan(Json.createValue(14));
        Range range3 = Range.lessThan(Param.from("param"));
        
        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1, range1.intersect(range3));
    }

	@Test
    public void CorrectIntersectionForgreaterThan() {
    	Range range1 = Range.greaterThan(Json.createValue(37));
    	Range range2 = Range.greaterThan(Json.createValue(14));
    	Range range3 = Range.greaterThan(Json.createValue(37));

    	assertEquals(range1.intersect(range2), range1);
    	assertEquals(range2.intersect(range1), range1);
    	assertEquals(range1.intersect(range3), range1);
    }


	@Test
    public void CorrectIntersectionForgreaterThanWithParameters() {
        Range range1 = Range.greaterThan(Param.from("param"));
        Range range2 = Range.greaterThan(Json.createValue(14));
        Range range3 = Range.greaterThan(Param.from("param"));

        assertEquals(range1.intersect(range2), range2.intersect(range1));
        assertEquals(range1, range1.intersect(range3));    
    }


	@Test
    public void CorrectIntersectionForgreaterThanLessThan() {
    	Range range1 = Range.lessThan(Json.createValue(37));
    	Range range2 = Range.greaterThan(Json.createValue(14));
    	Range range3 = Range.lessThan(Json.createValue(14));
    	Range range4 = Range.greaterThan(Json.createValue(37));

    	assertEquals(range1.intersect(range2), Range.between(range2,range1));
    	assertEquals(range2.intersect(range1), Range.between(range2,range1));
    	assertEquals(Range.EMPTY, range3.intersect(range4));
    	assertEquals(Range.EMPTY, range4.intersect(range3));
    }

	@Test
    public void CorrectIntersectionForgreaterThanLessThanWithParameters() {
        Range range1 = Range.lessThan(Param.from("param1"));
        Range range2 = Range.greaterThan(Param.from("param2"));
        Range range3 = Range.lessThan(Param.from("param2"));
        Range range4 = Range.greaterThan(Param.from("param1"));

        assertEquals(range1.intersect(range2),Range.between(range2,range1));
        assertEquals(range2.intersect(range1),Range.between(range2,range1));
        assertEquals(Range.EMPTY,range1.intersect(range4));
        assertEquals(Range.EMPTY,range2.intersect(range3));
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
    	assertEquals(range1,Range.greaterThan(Json.createValue(5)));
    	Range range2 = Range.from("[2,8]");
    	assertEquals(range2,Range.between(Json.createValue(2),Json.createValue(8)));
    	Range range3 = Range.from("[{'>':2}, {'<=':8}]");
    	assertEquals(range3,Range.between(Range.greaterThan(Json.createValue(2)),Range.lessThanOrEqual(Json.createValue(8))));
    }
	
	@Test
    public void canCreaterangeWithParameters() {
        Range range1 = Range.lessThan(Param.from("bottom"));
        Range range2 = Range.lessThan(Param.from("top"));
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

        Range range1 = Range.between(Json.createValue(14),Json.createValue(37));
        Range range2 = Range.between(Json.createValue(15),Json.createValue(36));
        Range range3 = range1.intersect(range2);

        assertEquals(range3, range2);

        Range range4 = Range.between(Json.createValue(14),Json.createValue(17));
        Range range5 = Range.between(Json.createValue(29),Json.createValue(36));
        Range range6 = range4.intersect(range5);

        assertEquals(range6, Range.EMPTY);

        Range range7 = Range.between(Json.createValue(14),Json.createValue(17));
        Range range8 = Range.between(Json.createValue(17),Json.createValue(36));
        Range range9 = range7.intersect(range8);

        assertEquals(range9, Range.EMPTY);

    }
/*
    public void CorrectIntersectionForbetweenWithParameters() {

        Range range1 = Range.between(Json.createValue(14),37);
        Range range2 = Range.between(Param.from("param1"),Json.createValue(36));
        Range range3 = range1.intersect(range2);

        expect(range3.known_bounds).to.deep.equal(Range.between(Json.createValue(14),Json.createValue(36)));
        expect(range3.parametrized_bounds.param1).to.deep.equal(Range.greaterThanOrEqual(Param.from("param1")));

        Range range4 = Range.between(Json.createValue(14),Param.from("param1"));
        Range range5 = Range.between(Json.createValue(15),Json.createValue(36));
        Range range6 = range4.intersect(range5);

        expect(range6.known_bounds).to.deep.equal(Range.between(Json.createValue(15),Json.createValue(36)));
        expect(range6.parametrized_bounds.param1).to.deep.equal(Range.lessThan(Param.from("param1")));

        Range range7 = Range.between(Json.createValue(14),Param.from("param1"));
        Range range8 = Range.between(Json.createValue(15),Param.from("param1"));
        Range range9 = range7.intersect(range8);

        expect(range9).to.deep.equal(Range.between(Json.createValue(15), Param.from("param1")));

        Range range10 = Range.between(Json.createValue(14),Param.from("param1"));
        Range range11 = Range.between(Param.from("param1"),Json.createValue(36));
        Range range12 = range10.intersect(range11);

        expect(range12).to.be.null;
    }
*/
    @SuppressWarnings("serial")
	@Test
    public void canBindParameters() {
    	
    	JsonObject map1 = JsonUtil.parseObject("{'param1':34, 'param2':55}");
    	JsonObject map2 = JsonUtil.parseObject("{'param1':55, 'param2':34}");
    	JsonObject map3 = JsonUtil.parseObject("{'param2':34}");
    	JsonObject map4 = JsonUtil.parseObject("{'param1':55}");

        Range range1 = Range.between(Param.from("param1"), Param.from("param2")).intersect(Range.lessThan(Json.createValue(50)));
        assertEquals(range1.bind(map1), Range.between(Json.createValue(34), Json.createValue(50)));
        assertEquals(Range.EMPTY, range1.bind(map2));
        assertEquals(range1.bind(map3), Range.between(Param.from("param1"), Json.createValue(34)));
        assertEquals(Range.EMPTY,range1.bind(map4));
    }
    
	@Test public void canBindParameterInClosedRange() {
		Range r1 = Range.from("[22, {'$':'param1' } ]");
		Range r2 = r1.bind("{ 'param1': 25}");
		assertEquals(Range.from("[22, 25]"), r2);
	}
/*
    public void canBindParametersForHas() {
        Range range1 = Range.hasAll([Param.from("param")1, Param.from("param2")]);
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
    	assertEquals(Range.between(Json.createValue(2), Json.createValue(4)), range);
    	range = Range.from("[2,null]");
    	assertEquals(Range.greaterThanOrEqual(Json.createValue(2)), range);
    	range = Range.from("{ '$': 'param1'}");
    	assertEquals(Range.equals(Param.from("param1")), range);
    }
    
    @Test public void canUnionLessThan() {
    	Range range1 = Range.lessThan(Json.createValue(27));
       	Range range2 = Range.lessThan(Json.createValue(37));
       	Range range3 = Range.lessThanOrEqual(Json.createValue(27));
       	assertEquals(range2, range1.union(range2));
       	assertEquals(range2, range2.union(range1));
       	assertEquals(range3, range1.union(range3));
       	assertEquals(range3, range3.union(range1));
       	assertEquals(range2, range3.union(range2));
       	assertEquals(range2, range2.union(range3));
    }
    
    @Test public void canUnionGreaterThan() {
    	Range range1 = Range.greaterThan(Json.createValue(27));
       	Range range2 = Range.greaterThan(Json.createValue(37));
       	Range range3 = Range.greaterThanOrEqual(Json.createValue(27));
       	assertEquals(range1, range1.union(range2));
       	assertEquals(range1, range2.union(range1));
       	assertEquals(range3, range1.union(range3));
       	assertEquals(range3, range3.union(range1));
       	assertEquals(range3, range3.union(range2));
       	assertEquals(range3, range2.union(range3));
    }
    
    @Test public void canUnionLessThanAndGreaterThan() {
    	Range range1 = Range.greaterThan(Json.createValue(27));
       	Range range2 = Range.lessThan(Json.createValue(37));
    	Range range3 = Range.greaterThan(Json.createValue(37));
       	Range range4 = Range.lessThan(Json.createValue(27));
       	Range range5 = Range.greaterThanOrEqual(Json.createValue(27));
       	Range range6 = Range.lessThanOrEqual(Json.createValue(27));
       	assertEquals(Range.UNBOUNDED, range1.union(range2));
       	assertTrue(range3.union(range4) instanceof Union);
       	assertTrue(range1.union(range4) instanceof Union);
       	assertEquals(Range.UNBOUNDED, range5.union(range6));
    }

    @Test public void canUnionEquals() {
    	Range range1 = Range.equals(Json.createValue(27));
       	Range range2 = Range.equals(Json.createValue(37));
    	Range range3 = Range.equals(Json.createValue(37));
       	assertTrue(range1.union(range2) instanceof Union);
       	assertEquals(range2, range2.union(range3));
    }
    
    @Test public void canDoComplicatedUnion() {
    	Range range1 = Range.lessThan(Json.createValue(27));
       	Range range2 = Range.greaterThan(Json.createValue(37));
       	Range range3 = Range.greaterThanOrEqual(Json.createValue(36));
    	Range range4 = Range.equals(Json.createValue(29));
    	Range range5 = Range.lessThan(Json.createValue(26));
    	Range range6 = Range.equals(Json.createValue(29));
       	assertEquals(Range.union(range1,range3,range4), Range.union(range1,range2,range3,range4,range5,range6));
    }
    
    @Test public void canCreateSimpleWildcards() {
    	Range range = Range.like("def*jkl");
    	assertTrue(range instanceof Range.Like);
    	range = Range.like("def?jkl");
    	assertTrue(range instanceof Range.Like);
    }
    
    @Test public void canMatchSimpleStringsWithWildcards() {
    	Range range = Range.like("def*");
    	assertTrue(range.containsItem(Json.createValue("defabcjkl")));
    	assertFalse(range.containsItem(Json.createValue("decjkl")));
    	range = Range.like("def*jkl");
    	assertTrue(range.containsItem(Json.createValue("defabcjkl")));
    	assertTrue(range.containsItem(Json.createValue("defjkl")));
    	assertFalse(range.containsItem(Json.createValue("decjkl")));
    	assertFalse(range.containsItem(Json.createValue("defaajl")));
    	range = Range.like("def?");
    	assertTrue(range.containsItem(Json.createValue("defa")));
    	assertFalse(range.containsItem(Json.createValue("deca")));
    	assertFalse(range.containsItem(Json.createValue("defca")));
    	range = Range.like("def?jkl");
    	assertTrue(range.containsItem(Json.createValue("defajkl")));
    	assertFalse(range.containsItem(Json.createValue("defacjkl")));
    	assertFalse(range.containsItem(Json.createValue("defajl")));
    	range = Range.like("def*jkl*xyz");
    	assertTrue(range.containsItem(Json.createValue("defabcjkl123xyz")));
    	assertTrue(range.containsItem(Json.createValue("defjklxyz")));
    	assertFalse(range.containsItem(Json.createValue("defjkl123xyw")));
    }
    
    @Test public void canTestContainmentWithWildcards() {
    	Range range = Range.like("def*");
    	Range box = Range.between(Json.createValue("ab"), Json.createValue("gh"));
    	assertTrue(box.contains(range));
    	assertFalse(range.contains(box));
    	box = Range.between(Json.createValue("defg"), Json.createValue("defz"));
    	assertNull(box.contains(range));
    	assertNull(range.contains(box));
    	// Add more cases
    }

    @Test public void canTestIntersectionWithWildcards() {
    	Range range = Range.like("def*");
    	Range box = Range.between(Json.createValue("ab"), Json.createValue("gh"));
    	assertTrue(box.intersects(range));
    	assertTrue(range.intersects(box));
    	box = Range.between(Json.createValue("defg"), Json.createValue("defz"));
    	assertNull(box.intersects(range));
    	assertNull(range.intersects(box));
    	// Add more cases
    }

    @Test public void canIntersectWithWildcards() {
    	Range range = Range.like("def*");
    	Range box = Range.between(Json.createValue("ab"), Json.createValue("gh"));
    	Range result = box.intersect(range);
    	assertEquals(range, result);
    	box = Range.between(Json.createValue("defg"), Json.createValue("defz"));
    	result = box.intersect(range);
    	assertTrue(result instanceof Intersection);
    	// Add more cases
    }
    
    @Test public void canUnionWithWildcards() {
    	Range range = Range.like("def*");
    	Range box = Range.between(Json.createValue("ab"), Json.createValue("gh"));
    	Range result = box.union(range);
    	assertEquals(box, result);
    	box = Range.between(Json.createValue("defg"), Json.createValue("defz"));
    	result = box.intersect(range);
    	assertTrue(result instanceof Intersection);
    	// Add more cases
    }
    
    @Test public void testLikeWithNoWildcardsIsEquals() {
    	Range range = Range.like("def");
    	Range range1 = Range.equals(Json.createValue("def"));
    	assertEquals(range, range1);
    	
    }
}
