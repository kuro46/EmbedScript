package com.github.kuro46.embedscript.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author shirokuro
 */
public final class Util {
    private Util() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String joinStringSpaceDelimiter(int startIndex, String[] strings) {
        return Arrays.stream(strings)
            .skip(startIndex)
            .collect(Collectors.joining(" "));
    }
}
