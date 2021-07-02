package com.datasonnet.modules;

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

import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ujson.Null$;
import ujson.Value;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

public class Regex {

    public static Value regexFullMatch(String expr, String str) throws RegexException {
        return regexMatch(expr, str, true);
    }

    public static Value regexPartialMatch(String expr, String str) throws RegexException {
        return regexMatch(expr, str, false);
    }

    public static Value regexScan(String expr, String str) throws RegexException {
        Pattern pattern = Pattern.compile(expr);
        Matcher matcher = pattern.matcher(str);
        ArrayNode regexMatch = scan(matcher);

        return regexMatch != null ? ujsonUtils.parse(regexMatch.toString()) : Null$.MODULE$;
    }

    public static String regexQuoteMeta(String str) {
        return Pattern.quote(str);
    }

    public static String regexReplace(String str, String pattern, String replace) {
        return replace(str, pattern, replace, false);
    }

    public static String regexGlobalReplace(String str, String pattern, String replace) {
        return replace(str, pattern, replace, true);
    }

    public static String regexGlobalReplace(String str, String pattern, Function<Value, String> replace) throws RegexException {
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(str);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            ObjectNode nextMatch = getRegexMatch(matcher);
            matcher.appendReplacement(sb, replace.apply(ujsonUtils.parse(nextMatch.toString())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String replace(String str, String pattern, String replace, boolean isGlobal) {
        Matcher matcher = Pattern.compile(pattern).matcher(str);
        return isGlobal ? matcher.replaceAll(replace) : matcher.replaceFirst(replace);
    }

    private static Value regexMatch(String expr, String str, boolean isFull) throws RegexException {
        Pattern pattern = Pattern.compile(expr);
        Matcher matcher = pattern.matcher(str);

        boolean hasMatch = isFull ? matcher.matches() : matcher.find();
        if (!hasMatch) {
            return Null$.MODULE$;
        }

        return ujsonUtils.parse(getRegexMatch(matcher).toString());
    }

    private static ArrayNode scan(Matcher matcher) throws RegexException {
        boolean hasMatch = matcher.find();
        if (!hasMatch) {
            return null;
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode matchesNode = mapper.createArrayNode();

        do {
            matchesNode.add(getRegexMatch(matcher));
        } while (matcher.find());

        return matchesNode;
    }

    private static ObjectNode getRegexMatch(Matcher matcher) throws RegexException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode regexMatch = mapper.createObjectNode();
        regexMatch.put("string", matcher.group());

        ArrayNode capturesNode = mapper.createArrayNode();
        ObjectNode namedCapturesNode = mapper.createObjectNode();

        for (int i = 1; i <= matcher.groupCount(); i++) {
            capturesNode.add(matcher.group(i));
        }

        Map<String, Integer> namedGroups = getNamedGroupsFromMatcher(matcher);
        if (!namedGroups.isEmpty()) {
            for (Map.Entry<String, Integer> namedGroup : namedGroups.entrySet()) {
                namedCapturesNode.put(namedGroup.getKey(), matcher.group(namedGroup.getValue()));
            }
        }

        regexMatch.set("captures", capturesNode);
        regexMatch.set("namedCaptures", namedCapturesNode);

        return regexMatch;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> getNamedGroupsFromMatcher(Matcher matcher) throws RegexException {
        try {
            Field namedGroupsMapField = Matcher.class.getDeclaredField("namedGroups");
            namedGroupsMapField.setAccessible(true);
            return (Map<String, Integer>) namedGroupsMapField.get(matcher);
        } catch (NoSuchFieldException e) {
            throw new RegexException("Unable to retrieve named groups", e);
        } catch (IllegalAccessException e) {
            throw new RegexException("Unable to retrieve named groups", e);
        }
    }
}
