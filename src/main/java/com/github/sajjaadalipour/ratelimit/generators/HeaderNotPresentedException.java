package com.github.sajjaadalipour.ratelimit.generators;

/**
 * should be thrown when the {@link #headerKey} not presented in
 * the Http servlet request header while generating the key.
 *
 * @author Sajjad Alipour
 */
public class HeaderNotPresentedException extends RuntimeException {

    /**
     * The header key.
     */
    private String headerKey;

    public HeaderNotPresentedException(String message, String headerKey) {
        super(message);
        this.headerKey = headerKey;
    }

    public String getHeaderKey() {
        return headerKey;
    }
}
