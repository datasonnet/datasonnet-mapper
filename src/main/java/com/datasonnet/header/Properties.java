package com.datasonnet.header;


import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Properties extends java.util.Properties {

    // zero or an even number of backslashes, not preceded by another backslash
    private static String EVEN_BACKSLASHES = "(?:(?<!\\\\)(?:\\\\\\\\)*)";
    // a backslash preceded by zero or an even number of backslashes, not preceded by another backslash
    private static String ODD_BACKSLASHES = "(?:" + EVEN_BACKSLASHES + "\\\\)";

    private static String INITIAL_WHITESPACE_STRING = "(?:^[ \t\f]*)";
    private static Pattern INITIAL_WHITESPACE = Pattern.compile(INITIAL_WHITESPACE_STRING);

    // as key, all the characters after any initial white space up until the first whitespace, = or : not escaped,
    // note: this means either there is one or more unescaped whitespace and then optional = or :,
    // or there is zero or more unescaped whitespace and definitely = or :
    // or there is the end of the sequence!
    // then, after any following whitespace, as value every remaining character
    private static final Pattern KEY_VALUE = Pattern.compile(INITIAL_WHITESPACE + "(?<key>.+?" + EVEN_BACKSLASHES + ")(?:(?:[ \t\f]+[=:]?)|(?:[ \t\f]*[=:]))[ \t\f]*(?<value>.*)$");

    // if the beginning of the line is 0 or more white space leading to a comment or the end of the line
    private static Pattern IGNORED = Pattern.compile("^[ \t\f]*(?:#|$)");

    // if the line ends with a an odd number of backslashes
    private static Pattern FINAL_BACKSLASH = Pattern.compile(ODD_BACKSLASHES + "$");

    private static Pattern SLASH_THEN = Pattern.compile("\\\\(?<character>.|$)(?<hex>(?<=u)[0-9a-fA-F]{4})?");

    private static Pattern TO_ESCAPE = Pattern.compile("[\n\r\f\t:= ]|[^\\p{ASCII}]", Pattern.MULTILINE);

    private static String replaceAll(String text, Pattern pattern,
                                 Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, replacer.apply(matcher));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static class Line {
        private final String key;
        private final String value;

        public Line(String logical) {
            Matcher matcher = KEY_VALUE.matcher(logical);
            final String keyLocal;
            final String valueLocal;
            if(matcher.find()) {
                keyLocal = matcher.group("key");
                valueLocal = matcher.group("value");
            } else {
                keyLocal = logical;
                valueLocal = "";
            }
            key = unescape(keyLocal, true, true);
            value = unescape(valueLocal, false, false);
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        // TODO why does an exception here NPE the test instead of adding an exception?
        private String unescape(String value, boolean rejectDoubleBackslash, boolean preservePeriodBackslash) {
            return replaceAll(value, SLASH_THEN, matcher -> {
                String character = matcher.group("character");
                switch(character) {
                    case "": throw new IllegalArgumentException("A singleton backslash is not allowed at the end in " + value);
                    case "\\": if(rejectDoubleBackslash) {
                        throw new IllegalArgumentException("escaped (that is, double) backslashes) are not allowed in " + value);
                    } else {
                        return "\\\\";  // replace with one, which needs to be two due to replacement rules that quoteReplacement does not enforce
                    }
                    case "n": return "\n";
                    case "r": return "\r";
                    case "f": return "\f";
                    case "t": return "\t";
                    case ".": if(preservePeriodBackslash) {
                        return "\\\\.";  // our new special case, with escape for appendReplacement
                    } else {
                        return ".";
                    }
                    case "u":
                        String hex = matcher.group("hex");
                        if(hex != null) {
                            int codepoint = Integer.parseInt(hex, 16);
                            return String.valueOf(Character.toChars(codepoint));
                        } else {
                            return "u";  // wasn't a unicode sequence. TODO is that logic right?
                        }
                    default: return character;
                }
            });
        }


    }

    @Override
    public void load(Reader reader) {
        for(String logical : logicalLines(reader)) {
            Line line = new Line(logical);
            setProperty(line.getKey(), line.getValue());
        }
    }

    private Iterable<String> logicalLines(Reader reader) {
        List<String> lines = new ArrayList<>();
        BufferedReader buffered = new BufferedReader(reader);
        StringBuffer current = new StringBuffer();
        buffered.lines().filter(this::notIgnored).forEach(line -> {
            if(isContinuation(current)) {
                current.append(removePrefix(line));
            } else {
                current.append(line);
            }
            if (continues(line)) {
                trimLastSlash(current);
            } else {
                lines.add(current.toString());
                clear(current);
            }
        });
        if(isContinuation(current)) {
            lines.add(current.toString());
        }
        return Collections.unmodifiableList(lines);
    }

    private void clear(StringBuffer current) {
        current.setLength(0);
    }

    private void trimLastSlash(StringBuffer current) {
        current.setLength(current.length() - 1);
    }

    private boolean isContinuation(StringBuffer current) {
        return current.length() > 0;
    }

    private String removePrefix(String line) {
        Matcher matcher = INITIAL_WHITESPACE.matcher(line);
        matcher.find(); // guaranteed to find, as it matches the start + zero or more whitespace
        return line.substring(matcher.end());
    }

    private boolean notIgnored(String s) {
        return !IGNORED.matcher(s).find();
    }

    private boolean continues(String line) {
        return FINAL_BACKSLASH.matcher(line).find();
    }

    @Override
    public void load(InputStream in) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        BufferedWriter buffered = new BufferedWriter(writer);
        // this is created in order to make testing easy
        // also, the comments are ignored since this is only for testing, and there are some nuances to how that works
        for(String key : this.stringPropertyNames()) {
            buffered.write(escapeKey(key) + " = " + escapeValue(this.getProperty(key)));
            buffered.newLine();
        }
        buffered.flush();
    }

    private String escapeValue(String property) {
        // first, escape all slashes, then call escapeKey
        // note that this is not regexes but does replace all. replaceAll is that with regexes (double the slashes, double the fun)
        String doubledSlashes = property.replace("\\", "\\\\");
        return escapeNonSlashes(doubledSlashes);
    }

    private String escapeKey(String key) {
        return escapeNonSlashes(key);
    }

    private String escapeNonSlashes(String value) {
        return replaceAll(value, TO_ESCAPE, matcher -> {
            String character = matcher.group();
            switch(character) {
                case "\n": return "\\\\n";
                case "\r": return "\\\\r";
                case "\t": return "\\\\t";
                case "\f": return "\\\\f";
                case " ": return "\\\\ ";
                default: return "\\\\u" + String.format("%04x", (int) character.charAt(0));
            }
        });
    }

    @Override
    public void store(OutputStream out, String comments) {
        throw new UnsupportedOperationException();
    }
}
