package com.zscengiz.product.service.grpc;

import com.google.protobuf.ByteString;
import com.zscengiz.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DiscountGrpcServiceImpl implements DiscountGrpcService {

    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceBlockingStub;

    // istek tamamlanıncaya kadar bekleyen stubdır.
    private final DiscountServiceGrpc.DiscountServiceBlockingStub discountStub;
    // Asenkron stub, stream işlemleri için kullanılacak
    private final DiscountServiceGrpc.DiscountServiceStub discountAsyncStub;

    private ManagedChannel channel; //host ve port için - karşı servisle iletşimde bulunmak için

    @Value("${discount.grpc.host}")
    private String grpcHost;

    @Value("${discount.grpc.port}")
    private int grpcPort;

    public DiscountGrpcServiceImpl() {

        System.out.println(" --> Discount gRPC info: " + grpcHost+":"+grpcPort);
        channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        this.discountStub = DiscountServiceGrpc.newBlockingStub(channel);
        this.discountAsyncStub = DiscountServiceGrpc.newStub(channel);

    }



    @Override
    public DiscountResponse getDiscount(DiscountRequest discountRequest) {
        discountServiceBlockingStub = DiscountServiceGrpc.newBlockingStub(channel); //kanal açıp bütün işlem tamamlayıncaya kadar bekliyorum
        DiscountResponse discountResponse = discountServiceBlockingStub.getDiscount(discountRequest);
        return discountResponse;
    }


    public UploadStatus uploadFile(MultipartFile file) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final UploadStatus[] statusResponse = new UploadStatus[1];

        // Response observer: sunucudan gelen sonucu yakalıyoruz.
        StreamObserver<UploadStatus> responseObserver = new StreamObserver<UploadStatus>() {
            @Override
            public void onNext(UploadStatus value) {
                statusResponse[0] = value;
            }

            @Override
            public void onError(Throwable t) {
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        // Sunucuya göndermek üzere request observer'ı oluşturuyoruz.
        StreamObserver<FileChunk> requestObserver = discountAsyncStub.uploadFile(responseObserver);

        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[64 * 1024]; // 64KB'lık parça
            int bytesRead;
            boolean isFirst = true;
            while ((bytesRead = is.read(buffer)) != -1) {
                FileChunk.Builder chunkBuilder = FileChunk.newBuilder()
                        .setContent(ByteString.copyFrom(buffer, 0, bytesRead));
                if (isFirst) {
                    chunkBuilder.setFileName(file.getOriginalFilename())
                            .setIsFirst(true);
                    isFirst = false;
                }
                requestObserver.onNext(chunkBuilder.build());
            }
        } catch (IOException e) {
            requestObserver.onError(e);
        }
        // Dosya gönderiminin tamamlandığını bildiriyoruz.
        requestObserver.onCompleted();

        // Sonucun gelmesi için bekliyoruz (örn. 1 dakika)
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            throw new RuntimeException("Upload timed out");
        }
        return statusResponse[0];
    }

}
