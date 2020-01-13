package com.datasonnet;

import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import scala.Function1;
import scala.collection.mutable.ArrayBuffer;
import scala.collection.mutable.LinkedHashMap;
import ujson.Obj;
import ujson.Value;
import upickle.core.Visitor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

public class Regex {

    public static Value regexFullMatch(String expr, String str) {
        return regexMatch(expr, str, true);
    }

    public static Value regexPartialMatch(String expr, String str) {
        return regexMatch(expr, str, false);
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

    private static String replace(String str, String pattern, String replace, boolean isGlobal) {
        Matcher matcher = Pattern.compile(pattern).matcher(str);
        return isGlobal ? matcher.replaceAll(replace) : matcher.replaceFirst(replace);
    }

    private static Value regexMatch(String expr, String str, boolean isFull) {
        Pattern pattern = Pattern.compile(expr);
        Matcher matcher = pattern.matcher(str);

        boolean hasMatch = isFull ? matcher.matches() : matcher.find();
        if (!hasMatch) {
            return Value.Null();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode regexMatch = mapper.createObjectNode();
        regexMatch.put("string", str);

        ArrayNode capturesNode = mapper.createArrayNode();
        for (int i = 1; i <= matcher.groupCount(); i++) {
            capturesNode.add(matcher.group(i));
        }
        regexMatch.set("captures", capturesNode);

        ObjectNode namedCapturesNode = mapper.createObjectNode();
        Map<String, Integer> namedGroups = getNamedGroups(matcher);
        for (Map.Entry<String, Integer> namedGroup : namedGroups.entrySet()) {
            namedCapturesNode.put(namedGroup.getKey(), matcher.group(namedGroup.getValue()));
        }

        regexMatch.set("namedCaptures", namedCapturesNode);

        return UjsonUtil.jsonObjectValueOf(regexMatch.toString());
    }

    private static Map<String, Integer> getNamedGroups(Matcher matcher) {
        try {
            Field namedGroupsMapField = Matcher.class.getDeclaredField("namedGroups");
            namedGroupsMapField.setAccessible(true);
            return (Map<String, Integer>) namedGroupsMapField.get(matcher);
        } catch (Exception e) {
            //TODO log the error?
            return Collections.emptyMap();
        }
    }
}
