package com.softwareplumbers.common.abstractquery;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.json.JsonValue;

public class RangeFactory extends Factory<JsonValue, Range> {

	@Override
	public Range intersect(List<Range> elements) {
		Iterator<Range> items = elements.iterator();
		if (!items.hasNext()) return Range.EMPTY;
		List<Range> result = new ArrayList<Range>();
		result.add(items.next());
		while (items.hasNext()) {
			Range item = items.next();
			Range intersected = null;
			int i = 0;
			while (i < result.size() && intersected == null) {
				intersected = result.get(i).maybeIntersect(item);
				i++;
			}
			if (intersected == null) 
				result.add(item);
			else if (intersected.isEmpty()) {
				return Range.EMPTY;
			} else {
				result.set(i-1, intersected);
			}
		}
		if (result.size() == 1) return result.get(0);
		
		return new Range.RangeIntersection(result);
	}

	@Override
	public Range union(List<Range> elements) {
		Iterator<Range> items = elements.iterator();
		if (!items.hasNext()) return Range.EMPTY;
		List<Range> result = new ArrayList<Range>();
		result.add(items.next());
		while (items.hasNext()) {
			// TODO: need a loop in here to account for case where list.get(i) is a union
			Range item = items.next();
			Range merged = null;
			for (int j = 0; j < result.size() && merged == null; j++) {
				merged = item.maybeUnion(result.get(j));
				if (merged != null) result.set(j, merged);
			}
			if (merged == null) 
				result.add(item);
			if (merged == Range.UNBOUNDED) return Range.UNBOUNDED;
		}
		if (result.size() == 1) return result.get(0);		
		return new Range.RangeUnion(result);
	}

}
