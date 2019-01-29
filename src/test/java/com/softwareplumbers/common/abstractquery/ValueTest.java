package com.softwareplumbers.common.abstractquery;

import org.junit.runner.RunWith;

import com.softwareplumbers.common.abstractquery.Value.Atomic;
import com.softwareplumbers.common.abstractquery.Value.MapValue;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.HashMap;

public class ValueTest {
	
	@Test
	public void canCreateMapValueFromJson() {
		Value.MapValue value1 = MapValue.fromJson("{ 'location': { 'x': 17, 'y': 22 }, 'tags': [ 'a', 'g' ] }");
		Value location = value1.getProperty("location");
		Value tags = value1.getProperty("tags");
		assertThat(location, is(instanceOf(Value.MapValue.class)));
		assertThat(tags, is(instanceOf(Value.ArrayValue.class)));
		assertEquals(Value.from(17), location.toMap().getProperty("x"));
		assertEquals(Value.from(22), location.toMap().getProperty("y"));
		assertEquals(Value.from("a"), tags.toArray().getElement(0));
		assertEquals(Value.from("g"), tags.toArray().getElement(1));
	}
	
	@Test
	public void handleTrueFalseValuesFromJson() {
		Value.MapValue value1 = MapValue.fromJson("{ 'true': true, 'false': false }");
		Value trueValue = value1.getProperty("true");
		Value falseValue = value1.getProperty("false");
		assertEquals(Value.from(true), trueValue);
		assertEquals(Value.from(false), falseValue);
	}
	
}
