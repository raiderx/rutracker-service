package org.karpukhin.util;

/**
 * @author Pavel Karpukhin
 * @since 17.07.14
 */
public class AssertUtils {

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
