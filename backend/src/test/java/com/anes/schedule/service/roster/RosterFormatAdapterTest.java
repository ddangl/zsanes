package com.anes.schedule.service.roster;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.anes.schedule.service.roster.RosterStaffMatcher.StaffDirectory;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 解析适配层(specs「两阶段解析与导入」「校验分级·静默丢弃禁止」)。
 * 布局假设(design D2,常量集中,真实样本到位后校准):
 * A1=月份;第2行=日期表头(A="日期",B起=1..31);第3行起 A=岗位标签、B起=人名;
 * 值班/备班/长接班/副麻行按列=日期;短接班行列=顺序;合并单元格=同一人跨列展开。
 */
class RosterFormatAdapterTest {

    private static final StaffDirectory DIRECTORY = new StaffDirectory() {
        @Override
        public List<StaffRef> byName(String name) {
            return switch (name) {
                case "仓静" -> List.of(new StaffRef(1L, "10495", "仓静", true));
                case "李四" -> List.of(new StaffRef(2L, "10002", "李四", true));
                case "张三" -> List.of(new StaffRef(3L, "10001", "张三", true));
                default -> List.of();
            };
        }

        @Override
        public Optional<StaffRef> byEmpNo(String empNo) {
            return Optional.empty();
        }
    };

    /** 构造测试工作簿的工具 */
    private static WorkbookBuilder workbook() {
        return new WorkbookBuilder();
    }

    static final class WorkbookBuilder {
        private final XSSFWorkbook wb = new XSSFWorkbook();

        Sheet sheet(String name) {
            return wb.createSheet(name);
        }

        byte[] bytes() throws Exception {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                wb.write(out);
                return out.toByteArray();
            }
        }
    }

    private static void set(Sheet sheet, int rowIdx, int colIdx, String value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        Cell cell = row.createCell(colIdx, CellType.STRING);
        cell.setCellValue(value);
    }

    private static void setNum(Sheet sheet, int rowIdx, int colIdx, int value) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        row.createCell(colIdx, CellType.NUMERIC).setCellValue(value);
    }

    private RosterFormatAdapter adapter() {
        return new RosterFormatAdapter(new RosterStaffMatcher(DIRECTORY));
    }

    private RosterParseResult parse(byte[] bytes, String targetMonth) throws Exception {
        return adapter().parse(new ByteArrayInputStream(bytes), targetMonth);
    }

    private static void standardHeader(Sheet sheet) {
        set(sheet, 0, 0, "2026-09");
        set(sheet, 1, 0, "日期");
        setNum(sheet, 1, 1, 1);
        setNum(sheet, 1, 2, 2);
        setNum(sheet, 1, 3, 3);
    }

    @Test
    void dateMatrixRowsParseWithDayColumns() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "值班老总");
        set(sheet, 2, 1, "仓静");
        set(sheet, 2, 2, "李四");
        set(sheet, 2, 3, "张三");

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        assertEquals("2026-09", result.fileMonth());
        assertEquals(3, result.rows().size());
        RosterRow day1 = result.rows().get(0);
        assertEquals(RosterPositionType.ON_CALL_CHIEF, day1.type());
        assertEquals(1, day1.day());
        assertEquals(1L, day1.match().staffId());
        assertEquals(3, result.rows().get(2).day());
    }

    @Test
    void mergedCellExpandsAcrossCoveredDays() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "值班老总");
        set(sheet, 2, 1, "仓静");
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 2, 1, 3));

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        // 合并覆盖 day1-3 → 展开为 3 行同人
        assertEquals(3, result.rows().size());
        assertTrue(result.rows().stream().allMatch(r -> r.match().staffId() == 1L));
        assertEquals(List.of(1, 2, 3),
                result.rows().stream().map(RosterRow::day).toList());
    }

    @Test
    void shortReliefRowBecomesSequencePool() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "短接班");
        set(sheet, 2, 1, "张三");
        set(sheet, 2, 2, "李四");
        set(sheet, 2, 3, "仓静");

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        assertEquals(3, result.rows().size());
        assertTrue(result.rows().stream()
                .allMatch(r -> r.type() == RosterPositionType.SHORT_RELIEF));
        assertEquals(List.of(1, 2, 3), result.rows().stream().map(RosterRow::seq).toList());
        assertTrue(result.rows().stream().allMatch(r -> r.day() == null));
    }

    @Test
    void unresolvableMergedRegionProducesBlockIssueNoSilentDrop() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "值班老总");
        set(sheet, 3, 0, "二档");
        set(sheet, 2, 1, "仓静");
        // B3:B4 跨行合并(同一人跨两个岗位行)→ 语义无法判定,必须报问题行
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(2, 3, 1, 1));

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        assertTrue(result.issues().stream().anyMatch(
                i -> i.level() == RosterIssue.Level.BLOCK && i.message().contains("无法解析")),
                "跨行合并必须产生无法解析阻断问题,实际:" + result.issues());
        // 且不产生任何行(被合并纠缠的两格都不出数据)
        assertTrue(result.rows().stream().noneMatch(r -> r.match() != null && r.match().resolved()));
    }

    @Test
    void unknownPositionRowProducesPositionBlockIssue() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "神秘岗位");
        set(sheet, 2, 1, "仓静");

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        assertEquals(0, result.rows().size());
        assertTrue(result.issues().stream().anyMatch(
                i -> i.level() == RosterIssue.Level.BLOCK && i.fixType() == RosterIssue.FixType.POSITION));
    }

    @Test
    void blankCellsAreSkippedAsEmpty() throws Exception {
        WorkbookBuilder wb = workbook();
        Sheet sheet = wb.sheet("月表");
        standardHeader(sheet);
        set(sheet, 2, 0, "值班老总");
        set(sheet, 2, 1, "仓静");
        // day2、day3 留空 = 当日无人,不产生行(空格不是数据,四档缺人由校验器警告)

        RosterParseResult result = parse(wb.bytes(), "2026-09");

        assertEquals(1, result.rows().size());
        assertNull(result.rows().get(0).seq());
    }
}
