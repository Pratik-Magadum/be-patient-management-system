package com.eyehospital.pms.common.constant;

/**
 * Centralized API path constants.
 *
 * <p>All controller mappings reference these constants so that versioning
 * changes are made in a single place — eliminating magic strings throughout
 * the codebase (DRY principle).</p>
 */
public final class ApiConstants {

    /** Base prefix for all API endpoints. */
    public static final String API_BASE = "/api";

    /** Version 1 prefix. */
    public static final String V1 = "/v1";

    // -----------------------------------------------------------------------
    // Hospital / Tenant module
    // -----------------------------------------------------------------------
    public static final String HOSPITALS             = API_BASE + V1 + "/hospitals";
    public static final String HOSPITAL_BY_SUBDOMAIN = "/{subdomain}";

    // -----------------------------------------------------------------------
    // Patient module
    // -----------------------------------------------------------------------
    public static final String PATIENTS       = API_BASE + V1 + "/patients";
    public static final String PATIENT_BY_ID  = "/{patientId}";
    public static final String PATIENT_SEARCH = "/search";

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------
    private ApiConstants() {
        // Utility class — not to be instantiated.
    }
}
