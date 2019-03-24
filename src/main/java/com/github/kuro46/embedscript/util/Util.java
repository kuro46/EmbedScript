package com.github.kuro46.embedscript.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public final class Util {
    private static final Pattern PATTERN_SPLIT_BY_FIRST_SPACE = Pattern.compile("(?<left>[^ ]+) (?<right>.+)");

    private Util() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pair<String, String> splitByFirstSpace(String string) {
        Matcher splitMatcher = PATTERN_SPLIT_BY_FIRST_SPACE.matcher(string);
        if (!splitMatcher.find()) {
            return null;
        }

        return new Pair<>(splitMatcher.group("left"), splitMatcher.group("right"));
    }

    public static String joinStringSpaceDelimiter(int startIndex, String[] strings) {
        return Arrays.stream(strings)
            .skip(startIndex)
            .collect(Collectors.joining(" "));
    }

    public static String replaceAndUnescape(String source, String target, String replacement) {
        return replaceAndUnescape(source, target, target, replacement, true);
    }

    public static String replaceAndUnescape(String source, String target, String escapeTo, String replacement, boolean quote) {
        Patterns patterns = createPatterns(target, quote);
        Pattern pattern = patterns.pattern;
        Pattern escapedPattern = patterns.escapedPattern;

        source = pattern.matcher(source).replaceAll("$1" + replacement);
        source = escapedPattern.matcher(source).replaceAll(escapeTo);

        return source;
    }

    public static String[] splitAndUnescape(String source, String target) {
        Patterns patterns = createPatterns(target, true);
        Pattern pattern = patterns.pattern;
        Pattern escapedPattern = patterns.escapedPattern;

        String quoteReplacementTarget = Matcher.quoteReplacement(target);
        source = pattern.matcher(source).replaceAll("$1 " + quoteReplacementTarget);
        return Arrays.stream(pattern.split(source))
            .map(s -> escapedPattern.matcher(s).replaceAll(quoteReplacementTarget))
            .toArray(String[]::new);
    }

    private static Patterns createPatterns(String target, boolean quote) {
        if (quote) {
            target = Pattern.quote(target);
        }
        Pattern pattern = Pattern.compile("(^|[^\\\\])" + target);
        Pattern escapedPattern = Pattern.compile("\\\\" + target);

        return new Patterns(pattern, escapedPattern);
    }

    private static class Patterns {
        private final Pattern pattern;
        private final Pattern escapedPattern;

        Patterns(Pattern pattern, Pattern escapedPattern) {
            this.pattern = pattern;
            this.escapedPattern = escapedPattern;
        }
    }
}
