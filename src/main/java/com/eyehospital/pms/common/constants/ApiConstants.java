package com.eyehospital.pms.common.constants;

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
    // Auth module
    // -----------------------------------------------------------------------
    public static final String AUTH                  = API_BASE + V1 + "/auth";
    public static final String AUTH_LOGIN            = AUTH + "/login";
    public static final String AUTH_REFRESH          = AUTH + "/refresh";
    public static final String AUTH_LOGOUT           = AUTH + "/logout";

    // -----------------------------------------------------------------------
    // Patient module
    // -----------------------------------------------------------------------
    public static final String PATIENTS              = API_BASE + V1 + "/patients";
    public static final String PATIENT_BY_ID         = "/{patientId}";
    public static final String PATIENT_BY_DATES      = "/by-dates";
    public static final String PATIENT_SEARCH_BY_NAME_PHONE = "/search/by-name-phone";
    public static final String PATIENT_DELETE         = "/{patientId}";
    public static final String PATIENT_DASHBOARD_TODAY = "/dashboard/today";

    // -----------------------------------------------------------------------
    // Appointment module
    // -----------------------------------------------------------------------
    public static final String APPOINTMENTS          = API_BASE + V1 + "/appointments";
    public static final String APPOINTMENT_FOLLOW_UP  = "/follow-up";

    // -----------------------------------------------------------------------
    // Consultation module
    // -----------------------------------------------------------------------
    public static final String CONSULTATIONS         = API_BASE + V1 + "/consultations";

    // -----------------------------------------------------------------------
    // Diagnostic module
    // -----------------------------------------------------------------------
    public static final String DIAGNOSTICS           = API_BASE + V1 + "/diagnostics";

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------
    private ApiConstants() {
        // Utility class — not to be instantiated.
    }
}
