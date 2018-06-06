package com.softwareplumbers.common.abstractquery;

import java.io.StringReader;
import java.io.IOException;
import java.io.Reader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

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
}
