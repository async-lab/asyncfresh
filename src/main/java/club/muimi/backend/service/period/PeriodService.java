package club.muimi.backend.service.period;

import club.muimi.backend.common.enums.PeriodType;
import club.muimi.backend.entity.RecruitmentPeriod;
import club.muimi.backend.exception.PeriodNotAllowedException;
import club.muimi.backend.repository.RecruitmentPeriodRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PeriodService {

    private final RecruitmentPeriodRepository recruitmentPeriodRepository;

    public PeriodService(RecruitmentPeriodRepository recruitmentPeriodRepository) {
        this.recruitmentPeriodRepository = recruitmentPeriodRepository;
    }

    public boolean isRegistrationOpen() {
        return isPeriodOpen(PeriodType.REGISTRATION, LocalDateTime.now());
    }

    public void ensureRegistrationOpen() {
        if (!isRegistrationOpen()) {
            throw new PeriodNotAllowedException("当前不是报名期，暂不允许该操作");
        }
    }

    private boolean isPeriodOpen(PeriodType periodType, LocalDateTime now) {
        return recruitmentPeriodRepository.findByPeriodTypeAndEnabledTrue(periodType)
                .filter(period -> !now.isBefore(period.getStartTime()) && !now.isAfter(period.getEndTime()))
                .map(RecruitmentPeriod::getEnabled)
                .orElse(false);
    }
}
