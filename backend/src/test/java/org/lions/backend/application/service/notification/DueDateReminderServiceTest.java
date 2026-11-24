package org.lions.backend.application.service.notification;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DueDateReminderServiceTest {

    @ParameterizedTest
    @CsvSource({"0,3,false", "1,3,true", "2,3,false", "3,3,false", "4,3,true", "7,3,true", "8,3,false",
            "2,1,true", "5,0,true"})
    void deveAvisarNoPrimeiroDiaEACadaIntervaloQuandoEmAtraso(long daysOverdue, int interval, boolean expected) {
        assertThat(DueDateReminderService.isOverdueReminderDay(daysOverdue, interval)).isEqualTo(expected);
    }
}
