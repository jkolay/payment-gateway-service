package com.payment.util;

/**
 * Utility for masking sensitive data in logs and responses.
 */
public final class MaskingUtil {

    private MaskingUtil() {
    }

    /**
     * Masks a tracking ID for log output, showing only the last 8 characters.
     */
    public static String maskTrackingId(String trackingId) {
        if (trackingId == null || trackingId.length() <= 8) {
            return "****";
        }
        return "****" + trackingId.substring(trackingId.length() - 8);
    }

    /**
     * Masks a description, showing only the first 20 characters.
     */
    public static String maskDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        if (description.length() <= 20) {
            return description;
        }
        return description.substring(0, 20) + "...";
    }

    /**
     * Trims and normalizes a string, returning null if blank.
     */
    public static String trimToNullSafe(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
