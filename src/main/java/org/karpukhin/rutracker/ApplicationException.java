package org.karpukhin.rutracker;

/**
 * @author Pavel Karpukhin
 * @since 14.07.14
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
