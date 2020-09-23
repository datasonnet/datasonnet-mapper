
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package com.datasonnet.spi;
//
//import com.datasonnet.document.Document;
//import ujson.Value;
//
//import java.util.Collections;
//import java.util.Map;
//
//public class TestFormatPlugin implements DataFormatPlugin<String> {
//
//    public static String TEST_PARAM = "TestParam";
//
//    @Override
//    public Value read(String input, Map<String, Object> params) throws PluginException {
//        return UjsonUtil.stringValueOf(params.get(TEST_PARAM).toString());
//    }
//
//    @Override
//    public Document write(Value input, Map<String, Object> params, String mimeType) throws PluginException {
//        return new StringDocument(params.get(TEST_PARAM).toString(), mimeType);
//    }
//
//    @Override
//    public String[] getSupportedIdentifiers() {
//        return new String[] { "application/test.test", "test" };
//    }
//
//    @Override
//    public Map<String, String> getReadParameters() {
//        return Collections.singletonMap(TEST_PARAM, "TestParameter");
//    }
//
//    @Override
//    public Map<String, String> getWriteParameters() {
//        return getReadParameters();
//    }
//
//    public String getPluginId() {
//        return "TEST";
//    }
//}
