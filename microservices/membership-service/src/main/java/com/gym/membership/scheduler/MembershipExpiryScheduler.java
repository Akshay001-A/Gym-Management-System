
package com.gym.membership.scheduler;

import com.gym.membership.service.MembershipService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MembershipExpiryScheduler {

    private final MembershipService membershipService;

    public MembershipExpiryScheduler(
            MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void expireMemberships() {
        membershipService.updateExpiredMemberships();
    }
}
