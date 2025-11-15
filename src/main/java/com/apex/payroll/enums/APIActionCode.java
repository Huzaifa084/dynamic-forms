package com.apex.payroll.enums;

/**
 * Application‐level codes indicating “what happened” and “what to do next.”
 */
public enum APIActionCode {
    // Success‐related
    BH901,  // e.g., proceed with KYC upload
    //    BH902,  // orders retrieved
    //    HMI56,  // order created
    //    HMI57,  // order updated
    //    HMI58,  // order canceled

    // Validation/client errors
    VAL400, // validation failed
    ATH401, // authentication required
    FOR403, // forbidden
    NFD404, // not found
    DUP409, // duplicate
    BAD400, // bad request
    TOO_MANY_429, // too many requests
    UN_AUTH401, // unauthorized, revoked token
    SRV503, // service unavailable
    SRV504, // service timeout

    // Domain‐specific
    NFA568, // insufficient funds

    // Server‐side
    SRV500,  // internal server error
    ERR, // generic error
}