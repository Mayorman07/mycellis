package com.mycelis.user.constant;

public enum Status {
    NEW,          // registered, email not yet verified
    ACTIVE,       // verified, can log in
    INACTIVE,     // dormant — optional, e.g. no login for N months
    DEACTIVATED,  // user-initiated account closure
    BLOCKED       // admin-initiated (ToS violation, fraud)
}