package com.datasonnet;

import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ujson.Value;

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

    public static Value regexScan(String expr, String str) {
        Pattern pattern = Pattern.compile(expr);
        Matcher matcher = pattern.matcher(str);

        boolean hasMatch = matcher.find();
        if (!hasMatch) {
            return Value.Null();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode regexMatch = mapper.createObjectNode();
        regexMatch.put("string", str);

        ArrayNode capturesNode = mapper.createArrayNode();
        ArrayNode namedCapturesNode = mapper.createArrayNode();

        do {
            ArrayNode nextFindNode = capturesNode.addArray();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                nextFindNode.add(matcher.group(i));
            }

            Map<String, Integer> namedGroups = getNamedGroupsFromMatcher(matcher);
            if (!namedGroups.isEmpty()) {
                ObjectNode nextCaptureGroup = namedCapturesNode.addObject();
                for (Map.Entry<String, Integer> namedGroup : namedGroups.entrySet()) {
                    nextCaptureGroup.put(namedGroup.getKey(), matcher.group(namedGroup.getValue()));
                }
            }
        } while (matcher.find());

        regexMatch.set("captures", capturesNode);
        regexMatch.set("namedCaptures", namedCapturesNode);

        System.out.println("SCAN IS " + regexMatch.toPrettyString());

        return UjsonUtil.jsonObjectValueOf(regexMatch.toString());
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
        Map<String, Integer> namedGroups = getNamedGroupsFromMatcher(matcher);
        for (Map.Entry<String, Integer> namedGroup : namedGroups.entrySet()) {
            namedCapturesNode.put(namedGroup.getKey(), matcher.group(namedGroup.getValue()));
        }

        regexMatch.set("namedCaptures", namedCapturesNode);

        return UjsonUtil.jsonObjectValueOf(regexMatch.toString());
    }

    private static Map<String, Integer> getNamedGroupsFromMatcher(Matcher matcher) {
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
