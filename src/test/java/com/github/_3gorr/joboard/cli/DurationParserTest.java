package com.github._3gorr.joboard.cli;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurationParserTest {

    @Test
    void parsesSingleUnit() {
        assertThat(DurationParser.parse("30s")).isEqualTo(Duration.ofSeconds(30));
        assertThat(DurationParser.parse("5m")).isEqualTo(Duration.ofMinutes(5));
        assertThat(DurationParser.parse("1h")).isEqualTo(Duration.ofHours(1));
        assertThat(DurationParser.parse("2d")).isEqualTo(Duration.ofDays(2));
    }

    @Test
    void parsesCombinedUnits() {
        assertThat(DurationParser.parse("1h30m")).isEqualTo(Duration.ofMinutes(90));
        assertThat(DurationParser.parse("2h15m45s"))
                .isEqualTo(Duration.ofHours(2).plusMinutes(15).plusSeconds(45));
    }

    @Test
    void isCaseInsensitive() {
        assertThat(DurationParser.parse("1H")).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void rejectsBadFormat() {
        assertThatThrownBy(() -> DurationParser.parse("abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurationParser.parse("10"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurationParser.parse("5x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurationParser.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurationParser.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
