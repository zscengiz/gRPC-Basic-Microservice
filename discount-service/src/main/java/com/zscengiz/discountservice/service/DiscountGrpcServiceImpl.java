package com.zscengiz.discountservice.service;

import com.zscengiz.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class DiscountGrpcServiceImpl extends DiscountServiceGrpc.DiscountServiceImplBase {

    @Value("${file.upload_dir}")
    private String uploadDir;

    private static final String LOG_DIR = "D:\\log\\upload_times.log";

    @Override
    public StreamObserver<FileChunk> uploadFile(StreamObserver<UploadStatus> responseObserver) {
        return new StreamObserver<FileChunk>() {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String fileName = "uploaded_file.pdf"; // Varsayılan, ilk chunk'ta değiştirilecek
            long startTime;

            @Override
            public void onNext(FileChunk chunk) {
                if (chunk.getIsFirst()) {
                    fileName = chunk.getFileName();
                    log.info("Receiving file: {}", fileName);
                    startTime = System.currentTimeMillis(); // Yükleme süresi başlangıcı
                }
                try {
                    bos.write(chunk.getContent().toByteArray());
                } catch (IOException e) {
                    log.error("Error writing file chunk", e);
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error receiving file", t);
            }

            @Override
            public void onCompleted() {
                try {
                    long endTime = System.currentTimeMillis(); // Bitiş zamanı
                    long duration = endTime - startTime; // Geçen süre (ms cinsinden)

                    String uploadFileName = "grpc_" + UUID.randomUUID() + "_" + fileName;
                    File outputFile = new File(uploadDir + File.separator + uploadFileName);
                    outputFile.getParentFile().mkdirs(); // Klasör yoksa oluştur
                    Files.write(outputFile.toPath(), bos.toByteArray());
                    log.info("File {} saved successfully", uploadFileName);

                    // Log dosyasına ekleme yap
                    Files.createDirectories(Paths.get("D:\\log"));
                    String logEntry = "File: " + uploadFileName + " - Upload Time: " + duration + "ms\n";
                    Files.write(Paths.get(LOG_DIR), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    UploadStatus status = UploadStatus.newBuilder()
                            .setSuccess(true)
                            .setMessage("File uploaded successfully in " + duration + "ms")
                            .build();
                    responseObserver.onNext(status);
                } catch (IOException e) {
                    log.error("Error saving file", e);
                    UploadStatus status = UploadStatus.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to save file: " + e.getMessage())
                            .build();
                    responseObserver.onNext(status);
                } finally {
                    responseObserver.onCompleted();
                }
            }
        };
    }
}
