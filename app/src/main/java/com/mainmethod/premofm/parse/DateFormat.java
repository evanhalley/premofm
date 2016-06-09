package com.mainmethod.premofm.parse;

/**
 * Created by evan on 7/31/15.
 */
class DateFormat {

    /**
     * EEE - Mon
     * EEEE - Monday
     * d - 1
     * dd - 01
     * M - 4
     * MM - 04
     * MMM - Apr
     * MMMM - April
     * yy - 14
     * yyyy - 2014
     * z - EST
     * Z - -0500
     * zzz - EST
     * a - AM
     * HH - 15
     * h - 1
     * hh - 01
     * m - 1
     * mm - 01
     * s - 1
     * ss - 01
     */
    static final String[] FORMATS = new String[] {
            "EEE d MM yyyy",
            "EEE d MM yyyy HH:mm",
            "EEE d MM yyyy HH:mm:ss",
            "EEE d MM yyyy HH:mm:ss Z",
            "EEE d MM yyyy HH:mm:ss z",
            "EEE d MM yyyy HH:mm:ss zzz",
            "EEE d MMM yyyy HH:mm:ss 'GMT'Z",
            "EEE d MMM yyyy HH:mm:ss 'GMT'Z (z)",
            "EEE, d MMM yyyy",
            "EEE, d MMM yyyy HH:mm Z",
            "EEE, d MMM yyyy HH:mm z",
            "EEE, d MMM yyyy HH:mm zzz",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss z",
            "EEE, d MMM yyyy HH:mm:ss zzz",
            "EEE, d MMMM yyyy HH:mm:ss Z",
            "EEE, d MMMM yyyy HH:mm:ss z",
            "EEE, d MMMM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyy HH:mm:ss Z",
            "EEE, d MMM yyy HH:mm:ss z",
            "EEE, d MMM yyy HH:mm:ss zzz",
            "EEE, d MM yyyy HH:mm:ss Z",
            "EEE, d MM yyyy HH:mm:ss z",
            "EEE, d MM yyyy HH:mm:ss zzz",
            "EEE, d MMM yyyy HH:mm:sss Z",
            "EEE, MMM d, yyyy HH:mm:ss z",
            "EEE, MMM d, yyyy HH:mm:ss zzz",
            "EEE, MMMM d, yyyy HH:mm:ss Z",
            "EEE, MMMM d, yyyy HH:mm:ss z",
            "EEE, MMMM d, yyyy HH:mm:ss zzz",
            "EEEE, d-MMM-yyyy HH:mm:ss Z",
            "EEEE, d-MMM-yyyy HH:mm:ss z",
            "EEEE, d-MMM-yyyy HH:mm:ss zzz",
            "EEEE, d MMM yyyy HH:mm:ss Z",
            "EEEE, d MMM yyyy HH:mm:ss z",
            "EEEE, d MMM yyyy HH:mm:ss zzz",
            "EEEE, MMMM d, yyyy h:mm:ss a",
            "d MMM yyyy",
            "d MMM yyyy HH:mm:ss Z",
            "d MMM yyyy HH:mm:ss z",
            "d MMM yyyy HH:mm:ss zzz",
            "MMMM d yyyy",
            "yyyy-MM-d",
            "yyyy-MM-d'T'HH:mm:ss",
            "yyyy-MM-d'T'HH:mm:ssZ",
            "yyyy-MM-d'T'HH:mm:ss.SSSZ",
            "EEE MMM d yyyy HH:mm:ss z (z)",
            "EEE, d MMM yyyy HH:mm:ss",
            "yyyy-MM-d HH:mm:ss",
            "EEE, d MMM  yyyy HH:mm:ss Z",
            "EEE, d MMM yy HH:mm:ss Z",
            "d MMMM yyyy, HH:mm:ss",
            "EEE, d MMM yyyy HH:mm:ss -Xzzz",
            "EEE dd MM yyyy",
            "EEE dd MM yyyy HH:mm",
            "EEE dd MM yyyy HH:mm:ss",
            "EEE dd MM yyyy HH:mm:ss Z",
            "EEE dd MM yyyy HH:mm:ss z",
            "EEE dd MM yyyy HH:mm:ss zzz",
            "EEE dd MMM yyyy HH:mm:ss 'GMT'Z",
            "EEE dd MMM yyyy HH:mm:ss 'GMT'Z (z)",
            "EEE, dd MMM yyyy",
            "EEE, dd MMM yyyy HH:mm Z",
            "EEE, dd MMM yyyy HH:mm z",
            "EEE, dd MMM yyyy HH:mm zzz",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMMM yyyy HH:mm:ss Z",
            "EEE, dd MMMM yyyy HH:mm:ss z",
            "EEE, dd MMMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyy HH:mm:ss Z",
            "EEE, dd MMM yyy HH:mm:ss z",
            "EEE, dd MMM yyy HH:mm:ss zzz",
            "EEE, dd MM yyyy HH:mm:ss Z",
            "EEE, dd MM yyyy HH:mm:ss z",
            "EEE, dd MM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:sss Z",
            "EEE, MMM dd, yyyy HH:mm:ss z",
            "EEE, MMM dd, yyyy HH:mm:ss zzz",
            "EEE, MMMM dd, yyyy HH:mm:ss Z",
            "EEE, MMMM dd, yyyy HH:mm:ss z",
            "EEE, MMMM dd, yyyy HH:mm:ss zzz",
            "EEEE, dd-MMM-yyyy HH:mm:ss Z",
            "EEEE, dd-MMM-yyyy HH:mm:ss z",
            "EEEE, dd-MMM-yyyy HH:mm:ss zzz",
            "EEEE, dd MMM yyyy HH:mm:ss Z",
            "EEEE, dd MMM yyyy HH:mm:ss z",
            "EEEE, dd MMM yyyy HH:mm:ss zzz",
            "EEEE, MMMM dd, yyyy h:mm:ss a",
            "dd MMM yyyy",
            "dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss zzz",
            "MMMM dd yyyy",
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "EEE MMM dd yyyy HH:mm:ss z (z)",
            "EEE, dd MMM yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "EEE, dd MMM  yyyy HH:mm:ss Z",
            "EEE, dd MMM yyy HH:mm:ss Z",
            "dd MMMM yyyy, HH:mm:ss",
            "EEE, dd MMM yyyy HH:mm:ss -Xzzz"
    };

}