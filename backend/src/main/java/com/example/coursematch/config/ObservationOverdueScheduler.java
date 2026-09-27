package com.example.coursematch.config;

import com.example.coursematch.service.ObservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 带教观察逾期扫描：把“确认/回应逾期”“督导长期未复查”推进到独立的逾期状态，
 * 并产生带明确责任人的提醒。状态推进与提醒在服务层去重，定时执行与手动触发都不会重复。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationOverdueScheduler {

    private final ObservationService observationService;

    /** 每小时整点后 10 分钟扫描一次 */
    @Scheduled(cron = "0 10 * * * ?")
    public void scan() {
        try {
            int affected = observationService.scanOverdue();
            if (affected > 0) {
                log.info("带教观察逾期扫描推进 {} 个观察单", affected);
            }
        } catch (Exception e) {
            log.error("带教观察逾期扫描失败", e);
        }
    }
}
