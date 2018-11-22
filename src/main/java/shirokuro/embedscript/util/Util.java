package shirokuro.embedscript.util;

/**
 * @author shirokuro
 */
public final class Util {
    private Util() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String joinStringSpaceDelimiter(int startIndex, String[] strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = startIndex; i < strings.length; i++) {
            stringBuilder.append(strings[i]).append(' ');
        }
        return stringBuilder.toString().substring(0, stringBuilder.length() - 1);
    }
}
