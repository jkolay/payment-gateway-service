package com.payment.util;

import org.junit.jupiter.api.Test;

public class MaskingUtilTest {



    @Test
    public   void testMaskTrackingId() {
        String trackingId = "1234567890abcdef";
        String masked = MaskingUtil.maskTrackingId(trackingId);
        assert masked.equals("****90abcdef") : "maskTrackingId failed";
    }

    @Test
    public  void testMaskDescription() {
        String description = "This is a very long description that should be truncated.";
        String masked = MaskingUtil.maskDescription(description);
        assert masked.equals("This is a very long ...") : "maskDescription failed";
    }

    @Test
    public void testTrimToNullSafe() {
        String value1 = "  some value  ";
        String value2 = "   ";
        String value3 = null;

        assert MaskingUtil.trimToNullSafe(value1).equals("some value") : "trimToNullSafe failed for non-blank";
        assert MaskingUtil.trimToNullSafe(value2) == null : "trimToNullSafe failed for blank";
        assert MaskingUtil.trimToNullSafe(value3) == null : "trimToNullSafe failed for null";
    }
}
