package com.sportssync.app.activities.utils;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import com.sportssync.app.activities.models.AttendanceRequest;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    private Context context;

    public ExcelExporter(Context context) {
        this.context = context;
    }

    public void exportAttendance(List<AttendanceRequest> attendanceList, String period) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Attendance");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Date", "Time", "Student Name", "UUCMS ID", "Status", "Responded By", "Response Time"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            int rowNum = 1;
            for (AttendanceRequest attendance : attendanceList) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dateFormat.format(new Date(attendance.getRequestedAt())));
                row.createCell(1).setCellValue(timeFormat.format(new Date(attendance.getRequestedAt())));
                row.createCell(2).setCellValue(attendance.getStudentName());
                row.createCell(3).setCellValue(attendance.getUucmsId());
                row.createCell(4).setCellValue(attendance.getStatus().substring(0, 1).toUpperCase() +
                        attendance.getStatus().substring(1));
                row.createCell(5).setCellValue(attendance.getRespondedBy() != null ?
                        attendance.getRespondedBy() : "N/A");
                row.createCell(6).setCellValue(attendance.getRespondedAt() > 0 ?
                        dateFormat.format(new Date(attendance.getRespondedAt())) : "N/A");
            }

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String fileName = "Attendance_" + period + "_" + System.currentTimeMillis() + ".xlsx";
            File file = new File(downloadsDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();

            Toast.makeText(context, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}