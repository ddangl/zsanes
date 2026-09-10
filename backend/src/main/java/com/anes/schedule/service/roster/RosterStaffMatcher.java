package com.anes.schedule.service.roster;

import java.util.List;
import java.util.Optional;

/**
 * 人员匹配链(specs「人员匹配」):单元格按姓名读入;唯一在职名直接解析;
 * 同名出候选待人工选择;纯数字按工号优先;匹配范围仅在职,命中停用按未匹配处理并特殊提示。
 */
public class RosterStaffMatcher {

    /** 人员引用(id/工号/姓名/在职),由宿主服务从 staff 表提供 */
    public record StaffRef(long id, String empNo, String name, boolean active) {
    }

    /** 人员目录查询接口(注入,便于纯单测与复用 staff 查询) */
    public interface StaffDirectory {

        List<StaffRef> byName(String name);

        Optional<StaffRef> byEmpNo(String empNo);
    }

    /**
     * 匹配结果:resolved=已确定;needsChoice=同名候选;inactiveHit=仅命中停用人员(特殊提示);
     * 三者皆否=彻底未匹配(阻断)。
     */
    public record StaffMatch(Long staffId, List<StaffRef> candidates,
                             boolean needsChoice, boolean inactiveHit) {

        public boolean resolved() {
            return staffId != null;
        }

        static StaffMatch resolved(long id) {
            return new StaffMatch(id, List.of(), false, false);
        }

        static StaffMatch unresolved() {
            return new StaffMatch(null, List.of(), false, false);
        }
    }

    private final StaffDirectory directory;

    public RosterStaffMatcher(StaffDirectory directory) {
        this.directory = directory;
    }

    public StaffMatch match(String cellText) {
        if (cellText == null || cellText.isBlank()) {
            return StaffMatch.unresolved();
        }
        String text = cellText.trim();

        // 纯数字按工号优先
        if (text.chars().allMatch(Character::isDigit)) {
            Optional<StaffRef> byEmp = directory.byEmpNo(text);
            if (byEmp.isPresent()) {
                StaffRef ref = byEmp.get();
                return ref.active()
                        ? StaffMatch.resolved(ref.id())
                        : new StaffMatch(null, List.of(), false, true);
            }
            return StaffMatch.unresolved();
        }

        List<StaffRef> sameName = directory.byName(text);
        List<StaffRef> active = sameName.stream().filter(StaffRef::active).toList();
        if (active.size() == 1) {
            return StaffMatch.resolved(active.get(0).id());
        }
        if (active.size() > 1) {
            return new StaffMatch(null, active, true, false);
        }
        // 无在职命中:若存在同名停用人员则标记 inactiveHit
        return new StaffMatch(null, List.of(), false, !sameName.isEmpty());
    }
}
