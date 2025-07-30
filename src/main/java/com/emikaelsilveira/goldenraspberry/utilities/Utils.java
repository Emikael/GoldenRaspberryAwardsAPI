package com.emikaelsilveira.goldenraspberry.utilities;

public class Utils {

    private Utils() {}

    public static boolean normalizeTrue(final String value) {
        return "yes".equals(value) || "true".equals(value) || "1".equals(value) || "y".equals(value);
    }

    public static boolean normalizeFalse(final String value) {
        return "no".equals(value) || "false".equals(value) || "0".equals(value) || "n".equals(value);
    }
}
