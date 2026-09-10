package com.anes.schedule.service.roster;

import java.util.List;

/** 校验结果:问题清单 + 是否存在阻断项(hasBlock=true 时不得入库) */
public record RosterValidationReport(List<RosterIssue> issues) {

    public boolean hasBlock() {
        return issues.stream().anyMatch(i -> i.level() == RosterIssue.Level.BLOCK);
    }
}
