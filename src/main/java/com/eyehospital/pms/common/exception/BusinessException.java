package com.eyehospital.pms.common.exception;

/**
 * Custom exception to represent business rule violations in the application.
 *
 * <p>When thrown, this exception will result in a 400 Bad Request response
 * with a JSON body containing the error code and message.</p>
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }

    public String getErrorCode() { return errorCode; }
}
