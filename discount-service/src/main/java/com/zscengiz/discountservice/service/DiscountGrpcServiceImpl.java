package com.zscengiz.discountservice.service;

import com.zscengiz.discountservice.data.ExcelLogger;
import com.zscengiz.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;

@GrpcService
@Slf4j
public class DiscountGrpcServiceImpl extends DiscountServiceGrpc.DiscountServiceImplBase {

    @Value("${file.upload_dir}")
    private String uploadDir;

    private final ExcelLogger excelLogger;
    private final ThreadPoolExecutor grpcExecutor;
    private final BlockingQueue<String> fileQueue;

    public DiscountGrpcServiceImpl(ExcelLogger excelLogger) {
        this.excelLogger = excelLogger;
        this.fileQueue = new LinkedBlockingQueue<>();

        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;

        this.grpcExecutor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        for (int i = 0; i < corePoolSize; i++) {
            grpcExecutor.submit(this::processFilesFromQueue);
        }
    }

    @Override
    public StreamObserver<FileChunk> uploadFile(StreamObserver<UploadStatus> responseObserver) {
        return new StreamObserver<FileChunk>() {
            private File tempFile;
            private RandomAccessFile randomAccessFile;
            private long startTime;
            private int fileSize = 0;
            private boolean isFileInitialized = false;

            @Override
            public synchronized void onNext(FileChunk chunk) {
                try {
                    if (!isFileInitialized) {
                        startTime = System.currentTimeMillis();
                        String fileName = "grpc_" + UUID.randomUUID() + "_" + chunk.getFileName();
                        tempFile = new File(uploadDir, fileName);
                        randomAccessFile = new RandomAccessFile(tempFile, "rw");
                        isFileInitialized = true;
                        log.info("Yeni dosya oluşturuldu: {}", tempFile.getAbsolutePath());
                    }

                    byte[] data = chunk.getContent().toByteArray();
                    randomAccessFile.seek(randomAccessFile.length());
                    randomAccessFile.write(data);
                    fileSize += data.length;

                } catch (IOException e) {
                    log.error("Dosya yazılırken hata oluştu!", e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC bağlantısı sırasında hata oluştu!", t);
                cleanup();
            }

            @Override
            public void onCompleted() {
                try {
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                        log.info("Dosya kapatıldı: {}", tempFile.getAbsolutePath());
                    }

                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    if (fileSize > 0) {
                        log.info("Dosya başarıyla kaydedildi: {} bytes", fileSize);
                        fileQueue.put(tempFile.getAbsolutePath());
                        grpcExecutor.submit(() -> excelLogger.log(duration, fileSize, grpcExecutor.getPoolSize(), 1));
                    } else {
                        log.error("Dosya içeriği boş, siliniyor! {}");
                        tempFile.delete();
                    }

                    UploadStatus status = UploadStatus.newBuilder()
                            .setSuccess(fileSize > 0)
                            .setMessage(fileSize > 0 ? "File uploaded successfully in " + duration + "ms, Size: " + fileSize + " bytes" : "File upload failed, empty file detected!")
                            .build();
                    responseObserver.onNext(status);
                } catch (IOException | InterruptedException e) {
                    log.error("Dosya kapatma sırasında hata oluştu!", e);
                    responseObserver.onError(e);
                } finally {
                    responseObserver.onCompleted();
                }
            }

            private void cleanup() {
                try {
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    if (tempFile != null && tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (IOException e) {
                    log.error("Cleanup sırasında hata oluştu!", e);
                }
            }
        };
    }

    private void processFilesFromQueue() {
        while (true) {
            try {
                String filePath = fileQueue.take();
                processFile(filePath);
            } catch (InterruptedException e) {
                log.error("Dosya işleme kuyruğunda hata oluştu!", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processFile(String filePath) {
        try {
            log.info("İşleniyor: {}", filePath);
            log.info("Dosya işlendi: {}", filePath);
        } catch (Exception e) {
            log.error("Dosya işleme hatası: " + filePath, e);
        }
    }
}
