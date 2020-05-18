package com.datasonnet.util;

import com.ctc.wstx.dtd.LargePrefixedNameSet;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PropertiesGenerator extends Generator<String> {

    private static final Set<String> NEWLINE_HEX = Stream.of("000a", "000A", "000d", "000D").collect(Collectors.toSet());
    private static final Set<String> WHITESPACE_HEX = Stream.of("0020", "0009", "000C", "000c", "000a", "000A", "000d", "000D").collect(Collectors.toSet());
    private static String[] NEWLINES = {"\n", "\r", "\r\n"};
    private static String INLINE_WHITESPACE = "\f\t ";
    private static String COMMENT_INLINE_CHARACTERS = INLINE_WHITESPACE + "abcdeuzABCDEUZ01359~+-=#\\/>.,ƏΔЩԎअઅ③";

    private static String NOT_SLASH_KEY_CHARACTERS = "abcdeuzABCDEUZ01359~+-#/>.,ƏΔЩअઅ③";
    // actual slash characters and then some others since any _can_ be
    private static String SLASH_KEY_CHARACTERS = "=:\t\f tfrn." + "abcdeuzABCDEUZ01359~+-/>,ƏΔЩԎअઅ③";
    private static String NO_WHITESPACE_SLASH_KEY_CHARACTERS = "=:." + "abcdeuzABCDEUZ01359~+-/>,ƏΔЩԎअઅ③";

    // optionally a slash, followed by a newline of some form, followed by a minimal set of characters, followed by
    // end of input
    private static Pattern LAST_LINE = Pattern.compile("\\\\?(?:\\r\\n|\\r|\\n).*?\\z", Pattern.MULTILINE);

    private StringBuilder builder = new StringBuilder();

    public PropertiesGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus generationStatus) {
        final String newLine = random.choose(NEWLINES);
        range(random.nextInt(10), () -> {
            if(random.nextBoolean()) { // if a property
                // 0+ whitespace followed by a key followed by 0+ whitespace followed by one whitespace or = or :
                // followed by 0+ whitespace followed by a value followed by 0+ backslash, newline, more value
                appendKey(random);
                appendSeparator(random);
                appendValue(random);
                range(random.nextInt(3), () -> {
                    appendContinuedValue(random, newLine);
                });
            } else {
                appendIgnored(random);
            }
            builder.append(newLine);
        });
        String output = builder.toString();
        builder.setLength(0);
        return output;
    }

    private void appendSeparator(SourceOfRandomness random) {
        appendWhitespace(random);
        builder.append(random.choose(" \t\f=:".split("")));
        appendWhitespace(random);
    }

    private void appendKey(SourceOfRandomness random) {
        appendWhitespace(random);
        // one non-whitespace
        if(random.nextInt(10) == 1) { // slash
            builder.append("\\");
            if(random.nextBoolean()) { // unicode
                builder.append("u");
                while(true) {
                    String hex = randomCharacterHex(random);
                    if(!WHITESPACE_HEX.contains(hex)) {
                        builder.append(hex);
                        break;
                    }
                }
            } else {
                builder.append(random.choose(SLASH_KEY_CHARACTERS.split("")));
            }
        } else {
            builder.append(random.choose(NOT_SLASH_KEY_CHARACTERS.split("")));
        }
        range(0, random.nextInt(10), () -> {
            if(random.nextInt(10) == 1) { // slash
                builder.append("\\");
                if(random.nextBoolean()) { // unicode
                    builder.append("u");
                    while(true) {
                        String hex = randomCharacterHex(random);
                        if(!NEWLINE_HEX.contains(hex)) {
                            builder.append(hex);
                            break;
                        }
                    }
                } else {
                    builder.append(random.choose(NO_WHITESPACE_SLASH_KEY_CHARACTERS.split("")));
                }
            } else {
                builder.append(random.choose(NOT_SLASH_KEY_CHARACTERS.split("")));
            }
        });
    }

    private String randomCharacterHex(SourceOfRandomness random) {
        while(true) {
            int codepoint = random.nextInt(65535 + 1);
            if(Character.isBmpCodePoint(codepoint)) {
                return String.format("%04x", codepoint);
            }
        }
    }

    private void appendContinuedValue(SourceOfRandomness random, String newLine) {
        builder.append("\\");
        builder.append(newLine);
        appendValue(random);
    }

    private void appendValue(SourceOfRandomness random) {
        // TODO expand value generation
        builder.append("value");
    }

    private void appendIgnored(SourceOfRandomness random) {
        appendWhitespace(random);
        if(random.nextBoolean()) { // a comment
            builder.append("#");
            appendVariously(random, COMMENT_INLINE_CHARACTERS.split(""));
        }
    }

    private void appendWhitespace(SourceOfRandomness random) {
        appendVariously(random, INLINE_WHITESPACE.split(""));
    }

    private <T> void appendVariously(SourceOfRandomness random, T[] items) {
        range(random.nextInt(10), () -> { // could be just whitespace, could be before a comment
            builder.append(random.choose(items));
        });
    }

    private void range(int max, Runnable function) {
        IntStream.range(0, max).forEach(n -> function.run() );
    }

    private void range(int min, int max, Runnable function) {
        IntStream.range(min, max).forEach(n -> function.run() );
    }

    @Override
    public List<String> doShrink(SourceOfRandomness random, String larger) {
        String reduced = LAST_LINE.matcher(larger).replaceAll("");
        if(reduced.equals(larger) || reduced.equals("")) {
            return Collections.emptyList();
        } else {
            //throw new IllegalArgumentException("REDUCED TO " + reduced);
            return Collections.singletonList(reduced);
        }
    }


}
