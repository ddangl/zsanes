package com.anes.schedule.service;

import com.anes.schedule.common.BusinessException;
import com.anes.schedule.common.PageResult;
import com.anes.schedule.dto.RuleDtos.RuleQuery;
import com.anes.schedule.dto.RuleDtos.RuleResp;
import com.anes.schedule.dto.RuleDtos.RuleSaveReq;
import com.anes.schedule.dto.RuleDtos.VersionResp;
import com.anes.schedule.entity.RuleDefinition;
import com.anes.schedule.entity.RuleVersion;
import com.anes.schedule.mapper.RuleDefinitionMapper;
import com.anes.schedule.mapper.RuleVersionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 规则中心:CRUD、发布(生成版本)、回读预览 */
@Service
@RequiredArgsConstructor
public class RuleService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 回读预览用的字段中文标签(常用键;未收录的键原样展示) */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("staffName", "人员"), Map.entry("position", "岗位"),
            Map.entry("roomFrom", "起始房号"), Map.entry("roomTo", "结束房号"), Map.entry("area", "区域"),
            Map.entry("persons", "人员名单"), Map.entry("reservedCountDefault", "默认预留间数"),
            Map.entry("reservedCountMax", "最大预留间数"), Map.entry("roomsPerLead", "主麻房间数"),
            Map.entry("specialtyFirst", "亚专科优先"), Map.entry("studentFollowsMentorFirst", "带教优先"),
            Map.entry("othersRandom", "其余随机"), Map.entry("maxRetry", "重试上限"),
            Map.entry("enabled", "启用"), Map.entry("reason", "原因"),
            Map.entry("halfDay", "半天班"), Map.entry("saturdayWork", "周六上班"),
            Map.entry("weeklyAbsent", "每周缺席日"), Map.entry("workdaysOnly", "仅工作日"),
            Map.entry("onlyArea", "限定区域"), Map.entry("location", "外派地点"),
            Map.entry("chief", "老总"), Map.entry("tier2", "二档"), Map.entry("tier3", "三档"),
            Map.entry("tier4", "四档"), Map.entry("cannotRest", "不能休息"), Map.entry("firstPlaced", "最先放入"),
            Map.entry("rotateDaily", "每天轮换"), Map.entry("firstSurgery", "手术第一优先"),
            Map.entry("orderSequential", "按序顺延"), Map.entry("skipIfStandby1Conflict", "备班1冲突跳过"),
            Map.entry("copyPreviousDay", "沿用前日"), Map.entry("resetOn", "重置日"),
            Map.entry("joinNormalScheduling", "正常排班"), Map.entry("restAllowed", "允许休息"),
            Map.entry("excludeTitles", "剔除职位"), Map.entry("joinSchedule", "参与排班"),
            Map.entry("deductLeave", "扣假期"), Map.entry("dayStatus", "日状态"),
            Map.entry("reminder", "提醒"), Map.entry("weekdays", "星期"), Map.entry("note", "备注"));

    private final RuleDefinitionMapper ruleMapper;
    private final RuleVersionMapper versionMapper;

    public PageResult<RuleResp> page(RuleQuery query) {
        LambdaQueryWrapper<RuleDefinition> qw = new LambdaQueryWrapper<RuleDefinition>()
                .eq(StringUtils.hasText(query.category()), RuleDefinition::getCategory, query.category())
                .eq(StringUtils.hasText(query.status()), RuleDefinition::getStatus, query.status())
                .and(StringUtils.hasText(query.keyword()), w -> w
                        .like(RuleDefinition::getName, query.keyword())
                        .or().like(RuleDefinition::getRuleCode, query.keyword()))
                .orderByAsc(RuleDefinition::getPriority);
        Page<RuleDefinition> page = ruleMapper.selectPage(Page.of(query.page(), query.size()), qw);
        List<RuleResp> list = page.getRecords().stream().map(RuleService::toResp).toList();
        return PageResult.of(page.getTotal(), list);
    }

    public void create(RuleSaveReq req) {
        validatePayload(req.payload());
        String code = StringUtils.hasText(req.ruleCode()) ? req.ruleCode().trim()
                : req.category() + "." + System.currentTimeMillis();
        Long count = ruleMapper.selectCount(new LambdaQueryWrapper<RuleDefinition>()
                .eq(RuleDefinition::getRuleCode, code));
        if (count != null && count > 0) {
            throw BusinessException.badRequest("规则编码已存在:" + code);
        }
        RuleDefinition rule = new RuleDefinition();
        rule.setRuleCode(code);
        applySave(rule, req);
        rule.setStatus("DRAFT");
        rule.setVersion(0);
        ruleMapper.insert(rule);
    }

    public void update(Long id, RuleSaveReq req) {
        RuleDefinition rule = requireRule(id);
        validatePayload(req.payload());
        applySave(rule, req);
        ruleMapper.updateById(rule);
    }

    /** 发布:校验 payload → 版本 +1 → 写 rule_version → 置 ACTIVE */
    @Transactional
    public Integer publish(Long id, Long publishedBy) {
        RuleDefinition rule = requireRule(id);
        validatePayload(rule.getPayload());
        int nextVersion = (rule.getVersion() == null ? 0 : rule.getVersion()) + 1;
        RuleVersion snapshot = new RuleVersion();
        snapshot.setRuleId(rule.getId());
        snapshot.setVersion(nextVersion);
        snapshot.setNlText(rule.getNlText());
        snapshot.setPayload(rule.getPayload());
        snapshot.setPublishedBy(publishedBy);
        versionMapper.insert(snapshot);

        rule.setVersion(nextVersion);
        rule.setStatus("ACTIVE");
        rule.setUpdatedBy(publishedBy);
        ruleMapper.updateById(rule);
        return nextVersion;
    }

    /** 退役:保留定义与历史版本,不再参与执行 */
    public void retire(Long id) {
        RuleDefinition rule = requireRule(id);
        rule.setStatus("RETIRED");
        ruleMapper.updateById(rule);
    }

    public List<VersionResp> versions(Long id) {
        requireRule(id);
        return versionMapper.selectList(new LambdaQueryWrapper<RuleVersion>()
                        .eq(RuleVersion::getRuleId, id)
                        .orderByDesc(RuleVersion::getVersion)).stream()
                .map(v -> new VersionResp(v.getVersion(), v.getNlText(), v.getPayload(),
                        v.getPublishedBy(), v.getPublishedAt() == null ? null
                                : v.getPublishedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))))
                .toList();
    }

    /** 中文回读预览:payload 键翻成标签,生成"标签=值"分号串 */
    public String preview(String payloadJson) {
        JsonNode root = validatePayload(payloadJson);
        Map<String, String> flat = new LinkedHashMap<>();
        root.fields().forEachRemaining(e -> flat.put(
                LABELS.getOrDefault(e.getKey(), e.getKey()), nodeToText(e.getValue())));
        return String.join(";", flat.entrySet().stream()
                .map(en -> en.getKey() + "=" + en.getValue()).toList());
    }

    // ------------------------------------------------------------------

    private JsonNode validatePayload(String payloadJson) {
        try {
            return MAPPER.readTree(payloadJson);
        } catch (Exception e) {
            throw BusinessException.badRequest("payload 不是合法 JSON:" + e.getMessage());
        }
    }

    private static String nodeToText(JsonNode node) {
        if (node.isValueNode()) {
            return node.asText();
        }
        StringBuilder sb = new StringBuilder();
        node.elements().forEachRemaining(n -> {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(n.isValueNode() ? n.asText() : n.toString());
        });
        return sb.length() == 0 ? node.toString() : sb.toString();
    }

    private void applySave(RuleDefinition rule, RuleSaveReq req) {
        rule.setName(req.name());
        rule.setCategory(req.category());
        rule.setNlText(req.nlText());
        rule.setPayload(req.payload());
        rule.setRuleType(StringUtils.hasText(req.ruleType()) ? req.ruleType() : "UNSTRUCTURED");
        rule.setPriority(req.priority() == null ? 100 : req.priority());
        rule.setEffectiveFrom(parseDate(req.effectiveFrom()));
        rule.setEffectiveTo(parseDate(req.effectiveTo()));
    }

    private static LocalDate parseDate(String s) {
        return StringUtils.hasText(s) ? LocalDate.parse(s) : null;
    }

    private RuleDefinition requireRule(Long id) {
        RuleDefinition rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw BusinessException.badRequest("规则不存在:id=" + id);
        }
        return rule;
    }

    private static RuleResp toResp(RuleDefinition r) {
        return new RuleResp(r.getId(), r.getRuleCode(), r.getCategory(), r.getName(), r.getNlText(),
                r.getPayload(), r.getRuleType(), r.getStatus(), r.getPriority(), r.getVersion());
    }
}
