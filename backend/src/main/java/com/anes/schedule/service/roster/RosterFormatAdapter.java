package com.anes.schedule.service.roster;

import com.anes.schedule.common.BusinessException;
import com.anes.schedule.service.roster.RosterStaffMatcher.StaffMatch;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 解析适配层(design D2):POI 直读 + 合并区域坐标→语义重建。
 * 布局假设(常量集中,真实样本校准点):
 * A1=月份;第2行=日期表头(1..N);第3行起 A=岗位标签、B起=人名;
 * 按日岗位列=日期,短接班列=顺序;单行合并=同一人跨列展开;跨行合并=无法解析(阻断,禁止静默丢弃)。
 */
public class RosterFormatAdapter {

    private static final int MONTH_ROW = 0;
    private static final int MONTH_COL = 0;
    private static final int HEADER_ROW = 1;
    private static final int LABEL_COL = 0;
    private static final int DATA_START_COL = 1;
    private static final int DATA_START_ROW = 2;

    private final RosterStaffMatcher matcher;

    public RosterFormatAdapter(RosterStaffMatcher matcher) {
        this.matcher = matcher;
    }

    public RosterParseResult parse(InputStream in, String targetMonth) {
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            String sheetName = wb.getSheetName(0);
            String fileMonth = stringAt(sheet, MONTH_ROW, MONTH_COL);

            Map<Integer, Integer> dayByCol = dayColumns(sheet);
            // 网格宽度以表头日期列为准:合并覆盖而未建 cell 的列也在网格内
            int lastCol = dayByCol.keySet().stream().max(Integer::compare)
                    .orElse(DATA_START_COL - 1);
            List<CellRangeAddress> regions = sheet.getMergedRegions();
            Set<String> reportedRegions = new HashSet<>();
            List<RosterRow> rows = new ArrayList<>();
            List<RosterIssue> issues = new ArrayList<>();

            for (int r = DATA_START_ROW; r <= sheet.getLastRowNum(); r++) {
                String label = stringAt(sheet, r, LABEL_COL);
                if (label == null || label.isBlank()) {
                    continue;
                }
                String loc = sheetName + ":第" + (r + 1) + "行";
                RosterPositionType type = RosterPositionType.fromLabel(label);
                if (type == null) {
                    issues.add(RosterIssue.block(loc, "岗位无法识别:" + label,
                            RosterIssue.FixType.POSITION));
                    continue;
                }
                Row row = sheet.getRow(r);
                for (int c = DATA_START_COL; c <= lastCol; c++) {
                    CellRangeAddress region = regionAt(regions, r, c);
                    String value;
                    if (region != null && region.getFirstRow() != region.getLastRow()) {
                        // 跨行合并:无法判定归属哪个岗位行 → 阻断问题行,禁止静默丢弃
                        String key = region.formatAsString();
                        if (reportedRegions.add(key)) {
                            issues.add(RosterIssue.block(
                                    sheetName + ":第" + (region.getFirstRow() + 1) + "行",
                                    "合并区域跨行无法解析:" + key, RosterIssue.FixType.NONE));
                        }
                        continue;
                    }
                    value = region != null
                            ? stringAt(sheet, region.getFirstRow(), region.getFirstColumn())
                            : stringAt(sheet, r, c);
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    StaffMatch match = matcher.match(value);
                    if (type.form() == RosterForm.BY_DATE) {
                        rows.add(new RosterRow(sheetName, r + 1, label, type,
                                dayByCol.get(c), null, value, match));
                    } else {
                        rows.add(new RosterRow(sheetName, r + 1, label, type,
                                null, c, value, match));
                    }
                }
            }
            return new RosterParseResult(fileMonth, rows, issues);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.badRequest("解析失败:" + e.getMessage());
        }
    }

    /** 第2行表头:B列起的数字即日期号 → 列→日映射 */
    private static Map<Integer, Integer> dayColumns(Sheet sheet) {
        Map<Integer, Integer> dayByCol = new HashMap<>();
        Row header = sheet.getRow(HEADER_ROW);
        if (header == null) {
            return dayByCol;
        }
        for (int c = DATA_START_COL; c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                dayByCol.put(c, (int) cell.getNumericCellValue());
            }
        }
        return dayByCol;
    }

    private static CellRangeAddress regionAt(List<CellRangeAddress> regions, int rowIdx, int colIdx) {
        for (CellRangeAddress region : regions) {
            if (region.isInRange(rowIdx, colIdx)) {
                return region;
            }
        }
        return null;
    }

    private static String stringAt(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colIdx);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            default -> null;
        };
    }
}
