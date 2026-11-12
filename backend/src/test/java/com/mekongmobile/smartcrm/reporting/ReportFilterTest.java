package com.mekongmobile.smartcrm.reporting;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReportFilterTest {
    @Test
    void validatesDateAndScoreIntervals() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        assertThrows(ApiException.class, () -> ReportFilter.of(null, null, start, start.minusDays(1), null, null));
        assertThrows(ApiException.class, () -> ReportFilter.of(null, null, start, start.plusDays(366), null, null));
        assertThrows(ApiException.class, () -> ReportFilter.of(null, null, start, start, 4, 2));
        assertThrows(ApiException.class, () -> ReportFilter.of(null, null, start, start, 0, 5));
        assertThrows(ApiException.class, () -> ReportFilter.of(-1L, null, start, start, 1, 5));
        assertDoesNotThrow(() -> ReportFilter.of(null, null, start, start.plusDays(365), 1, 5));
    }

    @Test
    void maskingDoesNotRevealPhone() {
        assertEquals("090****567", ReportingRepository.maskPhone("0901234567"));
        assertEquals("***", ReportingRepository.maskPhone(null));
        assertEquals("***", ReportingRepository.maskPhone("123"));
    }
}
