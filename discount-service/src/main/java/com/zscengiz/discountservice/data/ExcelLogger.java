package com.zscengiz.discountservice.data;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ExcelLogger {

    private static final String FILE_PATH = "D:/grpcLogs/grpcLog.xlsx";
    private final Lock fileLock = new ReentrantLock();

    @Async
    public void log(long duration, int fileSize, int thread, int rampUp) {
        try {
            writeLog(duration, fileSize, thread, rampUp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLog(long duration, int fileSize, int thread, int rampUp) throws IOException {
        fileLock.lock();
        try {
            File file = new File(FILE_PATH);
            Workbook workbook;
            Sheet sheet;

            if (!file.exists()) {
                Files.createDirectories(Paths.get("D:/grpcLogs/"));
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("Logs");
                createHeader(sheet);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(fis);
                    sheet = workbook.getSheetAt(0);
                }
            }

            int lastRow = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(lastRow);

            String threadId = "GRPC_" + fileSize + "_" + thread + "_" + rampUp;

            row.createCell(0).setCellValue(getCurrentTimestamp());
            row.createCell(1).setCellValue(threadId);
            row.createCell(2).setCellValue(fileSize);
            row.createCell(3).setCellValue(thread);
            row.createCell(4).setCellValue(rampUp);
            row.createCell(5).setCellValue(duration < 0 ? -1 : duration);
            row.createCell(6).setCellValue(getJvmHeap());
            row.createCell(7).setCellValue(getCpuUsage());
            row.createCell(8).setCellValue(getRamUsage());



            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            workbook.close();
        } finally {
            fileLock.unlock();
        }
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] headers = {"Date","ThreadId", "File Size", "Thread", "Ramp-up", "Duration (ms)", "JVM Heap (MB)", "CPU (%)", "RAM (GB)"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = sheet.getWorkbook().createCellStyle();
            Font font = sheet.getWorkbook().createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 5000);
        }
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private double getJvmHeap() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024.0 * 1024);
    }

    private double getCpuUsage() {
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemCpuLoad() * 100;
    }

    private double getRamUsage() {
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        return (osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize()) / (1024.0 * 1024 * 1024);
    }
}