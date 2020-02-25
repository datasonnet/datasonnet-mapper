package com.datasonnet;

import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import sjsonnet.Applyer;
import sjsonnet.EvalScope;
import sjsonnet.Val;
import ujson.Value;

import java.lang.reflect.Field;
import java.util.Collections;
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
        ArrayNode regexMatch = scan(expr, str);

        return regexMatch != null ? UjsonUtil.jsonObjectValueOf(regexMatch.toString()) : Value.Null();
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
            matcher.appendReplacement(sb, replace.apply(UjsonUtil.jsonObjectValueOf(nextMatch.toString())));
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
            return Value.Null();
        }

        return UjsonUtil.jsonObjectValueOf(getRegexMatch(matcher).toString());
    }

    private static ArrayNode scan(String expr, String str) throws RegexException {
        Pattern pattern = Pattern.compile(expr);
        Matcher matcher = pattern.matcher(str);

        return scan(matcher);
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
