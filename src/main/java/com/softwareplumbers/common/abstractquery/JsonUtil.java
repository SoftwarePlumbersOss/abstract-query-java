package com.softwareplumbers.common.abstractquery;

import com.softwareplumbers.common.abstractquery.Tristate.CompareResult;
import com.softwareplumbers.common.jsonview.JsonViewFactory;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParsingException;

/** Utility for Reading Json.
 * 
 * A true Json formatted string is almost unreadable when expressed in Java because
 * of the need to escape all the double quotes. This class essentially switches the
 * roles of single and double quotes in Json, which then allows us to write Json
 * expressions as Java strings much more naturally.
 * 
 * This also provides some functions which are conspicuously missing in javax.json 1.0
 * 
 * @author jonathan
 */
public class JsonUtil {
    
    /** Global empty JSON array.
     *
     * Redundant in later versions of javax.json.
     * 
     */
    public static final JsonArray EMPTY_JSON_ARRAY = Json.createArrayBuilder().build();
    
    /** Copy an exising object into a builder.
     * 
     * Redundant in later versions of javax.json where we have builder.addAll
     *
     * @param builder Builder to add to
     * @param toAdd Adds attributes of this object to builder
     * @return builder
     */
    public static final JsonObjectBuilder addAll(JsonObjectBuilder builder, JsonObject toAdd) {
        for (Map.Entry<String,JsonValue> entry : toAdd.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder;
    }
	
	private static class QuoteFutzingReader extends Reader {
		
		private Reader base;

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			int result = base.read(cbuf, off, len);
			if (result > 0) {
				for (int ix = off; ix < off+result; ix++) {
					if (cbuf[ix] == '\'') cbuf[ix] = '"'; 
				}
			}
			return result;
		}

		@Override
		public void close() throws IOException {
			base.close();
			
		}
        
        @Override
        public void mark(int readaheadLimit) throws IOException {
            base.mark(readaheadLimit);
        }
        
        @Override
        public void reset() throws IOException {
            base.reset();
        }
		
		public QuoteFutzingReader(Reader reader) {
            if (reader.markSupported())
        		this.base = reader;
            else
                this.base = new BufferedReader(reader);
		}
		
		public QuoteFutzingReader(String string) {
			this(new StringReader(string));
		}
	}
	
	public static JsonValue parseValue(String json) {
        json = json.trim();
        if (json.startsWith("{")) return parseStructure(json);
        if (json.startsWith("[")) return parseStructure(json);
        if (json.startsWith("\"")) return parseString(json);
        if (json.startsWith("true")) return JsonValue.TRUE;
        if (json.startsWith("false")) return JsonValue.FALSE;
        if (json.startsWith("null")) return JsonValue.NULL;
        return parseNumber(json);
	}
    
	public static JsonStructure parseStructure(String json) {
		try (Reader rs = new QuoteFutzingReader(json); JsonReader reader = Json.createReader(rs)) {
			return reader.read();
		} catch (IOException e) {
			throw new JsonException("error parsing structure", e);
		}
	}
    
    private static class JsonLocationImpl implements JsonLocation {
        public final long offset;
        @Override public long getLineNumber() { return -1; }
        @Override public long getColumnNumber() { return -1; }
        @Override public long getStreamOffset() { return offset; }
        public JsonLocationImpl(long offset) { this.offset = offset; }
    }
    
    public static JsonObject parseObject(String json) { return (JsonObject)parseStructure(json); }
    public static JsonArray parseArray(String json) { return (JsonArray)parseStructure(json); }
    
    public static JsonString parseString(String json) {
        json = json.trim();
        if (json.endsWith("\'")) {
            return JsonViewFactory.asJson(json.substring(1, json.length()-1));
        }
        throw new JsonParsingException("String must end with quote", new JsonLocationImpl(json.length()));
    }
    
    public static JsonNumber parseNumber(String json) {
        BigDecimal value = new BigDecimal(json);
        return JsonViewFactory.asJson(value);
    }
    
    
    public static boolean isAtomicValue(JsonValue obj) {
        switch (obj.getValueType()) {
            case ARRAY:
                return false;
            case OBJECT:
                return Param.isParam(obj);
            default:
                return true;
        }
	}
    
    
    public static Boolean maybeEquals(JsonValue a, JsonValue b) {
        return Tristate.isEqual(maybeCompare(a,b));
    }
    
    public static int compare(JsonString a, JsonString b) {
        return a.getString().compareTo(b.getString());
    }
    
    public static int compare(JsonNumber a, JsonNumber b) {
        if (a.isIntegral() && b.isIntegral()) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue());
        } else {
            return a.bigDecimalValue().compareTo(b.bigDecimalValue());
        }
    }
    
    public static ValueType getValueType(JsonValue value) {
        return value == null ? ValueType.NULL : value.getValueType();
    }
    
    public static CompareResult maybeCompare(JsonValue a, JsonValue b) {
        boolean isParamA = Param.isParam(a);
        boolean isParamB = Param.isParam(b);
        if (isParamA || isParamB) {
            if (isParamA && isParamB && Param.getKey(a).equals(Param.getKey(b))) 
                return CompareResult.EQUAL;
            else
                return CompareResult.UNKNOWN;
        } 
        ValueType type = getValueType(a);

        if (type == getValueType(b)) {
            switch (type) {
                case STRING: 
                    return CompareResult.valueOf(compare((JsonString)a, (JsonString)b));
                case NUMBER: 
                    return CompareResult.valueOf(compare((JsonNumber)a, (JsonNumber)b));
                case TRUE:
                case FALSE:
                case NULL:
                    return CompareResult.EQUAL;
                case OBJECT:
                case ARRAY:
                    throw new IllegalArgumentException("Can't compare arrays or objects");
            }
        }
        
        switch (type) {
            case NULL: 
                return CompareResult.LESS; // Null deemed less than any value
            case TRUE:
                if (getValueType(b) == ValueType.FALSE || getValueType(b) == ValueType.NULL)
                    return CompareResult.GREATER; // True deemed greater than false or null
                break;
            case FALSE:
                if (getValueType(b) == ValueType.TRUE) // False deemed less than true
                    return CompareResult.LESS;
                if (getValueType(b) == ValueType.NULL) // False deemed greater than null
                    return CompareResult.GREATER;
            default:
                if (getValueType(b) == ValueType.NULL)
                    return CompareResult.GREATER; // Any value deemed greater than null
        }
        
        throw new IllegalArgumentException(String.format("Can't compare %s with %s", type, getValueType(b)));
    }



 }
