/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softwareplumbers.common.abstractquery;

import com.softwareplumbers.common.abstractquery.Tristate.CompareResult;
import javax.json.JsonValue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author jonathan
 */
public class JsonUtilTest {
    @Test
    public void testCompare() {
        assertThat(JsonUtil.maybeCompare(null, JsonValue.TRUE), equalTo(CompareResult.LESS));
        assertThat(JsonUtil.maybeCompare(null, JsonValue.FALSE), equalTo(CompareResult.LESS));
        assertThat(JsonUtil.maybeCompare(JsonValue.FALSE, null), equalTo(CompareResult.GREATER));
        assertThat(JsonUtil.maybeCompare(JsonValue.TRUE, null), equalTo(CompareResult.GREATER));
    }
}
