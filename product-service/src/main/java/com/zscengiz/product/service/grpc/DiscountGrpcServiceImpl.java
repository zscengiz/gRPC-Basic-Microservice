package com.zscengiz.product.service.grpc;

import com.zscengiz.grpc.DiscountRequest;
import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.grpc.DiscountServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiscountGrpcServiceImpl implements DiscountGrpcService {

    private DiscountServiceGrpc.DiscountServiceBlockingStub discountServiceBlockingStub;

    private ManagedChannel channel; //host ve port için - karşı servisle iletşimde bulunmak için

    public DiscountGrpcServiceImpl(@Value("${discount.grpc.host}") String grpcHost, @Value("${discount.grpc.port}") int grpcPort) {

        System.out.println(" --> Discount gRPC info: " + grpcHost+":"+grpcPort);
        channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

    }

    @Override
    public DiscountResponse getDiscount(DiscountRequest discountRequest) {
        discountServiceBlockingStub = DiscountServiceGrpc.newBlockingStub(channel); //kanal açıp bütün işlem tamamlayıncaya kadar bekliyorum
        DiscountResponse discountResponse = discountServiceBlockingStub.getDiscount(discountRequest);
        return discountResponse;
    }
}
