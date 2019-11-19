package com.softwareplumbers.common.abstractquery;

import com.softwareplumbers.common.abstractquery.Tristate.CompareResult;
import java.io.StringReader;
import java.io.IOException;
import java.io.Reader;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

public class JsonUtil {
	
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
		
		public QuoteFutzingReader(Reader reader) {
			this.base = reader;
		}
		
		public QuoteFutzingReader(String string) {
			this(new StringReader(string));
		}
	}
	
	public static JsonValue parseValue(String json) {
		try (Reader rs = new QuoteFutzingReader(json); JsonReader reader = Json.createReader(rs)) {
			return reader.readValue();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static JsonObject parseObject(String json) {
		try (Reader rs = new QuoteFutzingReader(json); JsonReader reader = Json.createReader(rs)) {
			return reader.readObject();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
