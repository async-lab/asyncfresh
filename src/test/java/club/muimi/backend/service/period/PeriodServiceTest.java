package club.muimi.backend.service.period;

import club.muimi.backend.common.enums.PeriodType;
import club.muimi.backend.entity.RecruitmentPeriod;
import club.muimi.backend.exception.PeriodNotAllowedException;
import club.muimi.backend.repository.RecruitmentPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    @Mock
    private RecruitmentPeriodRepository recruitmentPeriodRepository;

    @Test
    void getCurrentPeriodShouldReturnSelectionWhenSelectionIsOpen() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T02:00:00Z"), APP_ZONE);
        LocalDateTime now = LocalDateTime.now(clock);
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.REGISTRATION)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.SELECTION)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.SELECTION)
                .startTime(now.minusHours(1))
                .endTime(now.plusHours(1))
                .enabled(true)
                .build()));

        PeriodService periodService = new PeriodService(recruitmentPeriodRepository, clock);

        assertThat(periodService.getCurrentPeriod()).isEqualTo(PeriodType.SELECTION);
    }

    @Test
    void ensureSelectionOpenForGroupingShouldRejectWhenSelectionClosed() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-27T02:00:00Z"), APP_ZONE);
        LocalDateTime now = LocalDateTime.now(clock);
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.SELECTION)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.SELECTION)
                .startTime(now.minusDays(2))
                .endTime(now.minusDays(1))
                .enabled(true)
                .build()));

        PeriodService periodService = new PeriodService(recruitmentPeriodRepository, clock);

        assertThatThrownBy(periodService::ensureSelectionOpenForGrouping)
                .isInstanceOf(PeriodNotAllowedException.class)
                .hasMessage("当前不是选拔期，暂不允许分组管理");
    }

    @Test
    void getCurrentPeriodShouldReturnNotOpenBeforeAnyPeriodStarts() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), APP_ZONE);
        LocalDateTime now = LocalDateTime.now(clock);
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.REGISTRATION)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.REGISTRATION)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(10))
                .enabled(true)
                .build()));
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.SELECTION)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.INTERVIEW)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findAllByEnabledTrue()).thenReturn(List.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.REGISTRATION)
                .startTime(now.plusDays(1))
                .endTime(now.plusDays(10))
                .enabled(true)
                .build()));

        PeriodService periodService = new PeriodService(recruitmentPeriodRepository, clock);

        assertThat(periodService.getCurrentPeriod()).isEqualTo(PeriodType.NOT_OPEN);
    }

    @Test
    void getCurrentPeriodShouldReturnNotOpenDuringGapBetweenPeriods() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), APP_ZONE);
        LocalDateTime now = LocalDateTime.now(clock);
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.REGISTRATION)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.REGISTRATION)
                .startTime(now.minusDays(10))
                .endTime(now.minusDays(5))
                .enabled(true)
                .build()));
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.SELECTION)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.SELECTION)
                .startTime(now.plusDays(2))
                .endTime(now.plusDays(6))
                .enabled(true)
                .build()));
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.INTERVIEW)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findAllByEnabledTrue()).thenReturn(List.of(
                RecruitmentPeriod.builder()
                        .periodType(PeriodType.REGISTRATION)
                        .startTime(now.minusDays(10))
                        .endTime(now.minusDays(5))
                        .enabled(true)
                        .build(),
                RecruitmentPeriod.builder()
                        .periodType(PeriodType.SELECTION)
                        .startTime(now.plusDays(2))
                        .endTime(now.plusDays(6))
                        .enabled(true)
                        .build()
        ));

        PeriodService periodService = new PeriodService(recruitmentPeriodRepository, clock);

        assertThat(periodService.getCurrentPeriod()).isEqualTo(PeriodType.NOT_OPEN);
    }

    @Test
    void getCurrentPeriodShouldReturnFinishedAfterLatestPeriodEnds() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), APP_ZONE);
        LocalDateTime now = LocalDateTime.now(clock);
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.REGISTRATION)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.SELECTION)).thenReturn(Optional.empty());
        when(recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(PeriodType.INTERVIEW)).thenReturn(Optional.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.INTERVIEW)
                .startTime(now.minusDays(10))
                .endTime(now.minusDays(1))
                .enabled(true)
                .build()));
        when(recruitmentPeriodRepository.findAllByEnabledTrue()).thenReturn(List.of(RecruitmentPeriod.builder()
                .periodType(PeriodType.INTERVIEW)
                .startTime(now.minusDays(10))
                .endTime(now.minusDays(1))
                .enabled(true)
                .build()));

        PeriodService periodService = new PeriodService(recruitmentPeriodRepository, clock);

        assertThat(periodService.getCurrentPeriod()).isEqualTo(PeriodType.FINISHED);
    }
}
