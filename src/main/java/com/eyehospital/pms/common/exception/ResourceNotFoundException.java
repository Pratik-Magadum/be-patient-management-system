package com.eyehospital.pms.common.exception;

/**
 * Thrown when a requested resource cannot be found in the data store.
 * Maps to HTTP {@code 404 NOT FOUND} via {@link GlobalExceptionHandler}.
 *
 * <pre>{@code
 * throw new ResourceNotFoundException("Hospital", "subdomain", "apollo-eye");
 * }</pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
