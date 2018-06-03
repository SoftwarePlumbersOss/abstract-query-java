package com.softwareplumbers.common.abstractquery;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class JsonUtil {
	public static JsonValue parseValue(String json) {
		try (StringReader rs = new StringReader(json); JsonReader reader = Json.createReader(rs)) {
			return reader.readValue();
		}
	}
	public static JsonObject parseObject(String json) {
		try (StringReader rs = new StringReader(json); JsonReader reader = Json.createReader(rs)) {
			return reader.readObject();
		}
	}
}
