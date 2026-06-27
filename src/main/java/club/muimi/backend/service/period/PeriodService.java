package club.muimi.backend.service.period;

import club.muimi.backend.common.enums.PeriodType;
import club.muimi.backend.entity.RecruitmentPeriod;
import club.muimi.backend.exception.PeriodNotAllowedException;
import club.muimi.backend.repository.RecruitmentPeriodRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PeriodService {

    private final RecruitmentPeriodRepository recruitmentPeriodRepository;
    private final Clock appClock;

    public PeriodService(RecruitmentPeriodRepository recruitmentPeriodRepository, Clock appClock) {
        this.recruitmentPeriodRepository = recruitmentPeriodRepository;
        this.appClock = appClock;
    }

    public boolean isRegistrationOpen() {
        return isPeriodOpen(PeriodType.REGISTRATION, now());
    }

    public boolean isSelectionOpen() {
        return isPeriodOpen(PeriodType.SELECTION, now());
    }

    public boolean isInterviewOpen() {
        return isPeriodOpen(PeriodType.INTERVIEW, now());
    }

    public PeriodType getCurrentPeriod() {
        LocalDateTime currentTime = now();
        if (isPeriodOpen(PeriodType.REGISTRATION, currentTime)) {
            return PeriodType.REGISTRATION;
        }
        if (isPeriodOpen(PeriodType.SELECTION, currentTime)) {
            return PeriodType.SELECTION;
        }
        if (isPeriodOpen(PeriodType.INTERVIEW, currentTime)) {
            return PeriodType.INTERVIEW;
        }

        List<RecruitmentPeriod> enabledPeriods = recruitmentPeriodRepository.findAllByEnabledTrue();
        if (enabledPeriods.isEmpty()) {
            return PeriodType.NOT_OPEN;
        }

        LocalDateTime latestEndTime = enabledPeriods.stream()
                .map(RecruitmentPeriod::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(currentTime);
        if (currentTime.isAfter(latestEndTime)) {
            return PeriodType.FINISHED;
        }
        return PeriodType.NOT_OPEN;
    }

    public void ensureRegistrationOpen() {
        if (!isRegistrationOpen()) {
            throw new PeriodNotAllowedException("当前不是报名期，暂不允许该操作");
        }
    }

    public void ensureSelectionOpenForGrouping() {
        if (!isSelectionOpen()) {
            throw new PeriodNotAllowedException("当前不是选拔期，暂不允许分组管理");
        }
    }

    public void ensureSelectionOpenForTaskSubmit() {
        if (!isSelectionOpen()) {
            throw new PeriodNotAllowedException("当前不是选拔期，暂不允许提交任务");
        }
    }

    private boolean isPeriodOpen(PeriodType periodType, LocalDateTime now) {
        return recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(periodType)
                .filter(period -> !now.isBefore(period.getStartTime()) && !now.isAfter(period.getEndTime()))
                .map(RecruitmentPeriod::getEnabled)
                .orElse(false);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(appClock);
    }
}
