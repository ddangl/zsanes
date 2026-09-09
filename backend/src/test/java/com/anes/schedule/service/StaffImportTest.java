package com.anes.schedule.service;

import com.alibaba.excel.EasyExcel;
import com.anes.schedule.dto.StaffImportRow;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用科室 2026年8月总人员信息表.xlsx(仓库根目录,gitignore 不入库)实测导入解析。
 * 文件不存在时跳过(如 CI 环境),不影响构建。
 */
class StaffImportTest {

    private static final Path AUGUST_FILE = Path.of("../2026年8月总人员信息表.xlsx");

    private List<StaffImportRow> readAugustRows() {
        return EasyExcel.read(AUGUST_FILE.toFile(), StaffImportRow.class, null)
                .sheet().doReadSync();
    }

    @Test
    void parseAugustTable() {
        Assumptions.assumeTrue(Files.exists(AUGUST_FILE), "8月人员表不在仓库根目录,跳过实测");
        List<StaffImportRow> rows = readAugustRows();

        assertEquals(418, rows.size(), "8月表数据行数应为 418");

        // 职称分布(与 docs/业务规则.md 2.1 核对一致)
        Map<String, Long> roleCount = rows.stream().collect(Collectors.groupingBy(
                r -> StaffService.normalizeEmpNo(r.getJobRole()), Collectors.counting()));
        assertEquals(82, roleCount.get("主麻"));
        assertEquals(6, roleCount.get("总值班"));
        assertEquals(100, roleCount.get("本院住院"));
        assertEquals(40, roleCount.get("麻护"));
        assertEquals(93, roleCount.get("规培"));
        assertEquals(32, roleCount.get("进修"));
        assertEquals(65, roleCount.get("轮转"));

        // 首行:苏子敏(工号10257,主麻,特殊说明 3、5来)
        StaffImportRow first = rows.get(0);
        assertEquals("10257", StaffService.normalizeEmpNo(first.getEmpNo()));
        assertEquals("苏子敏", first.getName());
        assertEquals("主麻", first.getJobRole());
        assertTrue(first.getNote().contains("3"));

        // 特殊说明 6 条;陆珠凤"只在肝科"
        List<StaffImportRow> noted = rows.stream().filter(r -> r.getNote() != null && !r.getNote().isBlank()).toList();
        assertEquals(6, noted.size());
        assertTrue(noted.stream().anyMatch(r -> "陆珠凤".equals(r.getName()) && r.getNote().contains("肝科")));
    }

    @Test
    void empNoNormalization() {
        assertEquals("10257", StaffService.normalizeEmpNo("10257.0"));
        assertEquals("8250864", StaffService.normalizeEmpNo("8250864"));
        assertEquals(null, StaffService.normalizeEmpNo(null));
        assertEquals("12", StaffService.normalizeEmpNo(" 12 "));
    }
}
