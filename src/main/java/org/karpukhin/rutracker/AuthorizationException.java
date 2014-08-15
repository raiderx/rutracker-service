package org.karpukhin.rutracker;

/**
 * @author Pavel Karpukhin
 * @since 15.08.14
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }
}
