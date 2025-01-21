package com.zscengiz.discountservice.service;


import com.zscengiz.discountservice.entity.Category;
import com.zscengiz.discountservice.entity.Discount;
import com.zscengiz.discountservice.repository.CategoryRepository;
import com.zscengiz.discountservice.repository.DiscountRepository;
import com.zscengiz.grpc.DiscountRequest;
import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.grpc.DiscountServiceGrpc;
import com.zscengiz.grpc.Response;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class DiscountGrpcServiceImpl extends DiscountServiceGrpc.DiscountServiceImplBase{

    private final DiscountRepository discountRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void getDiscount(DiscountRequest request, StreamObserver<DiscountResponse> responseObserver) {

        Category category = categoryRepository.findByExternalId(String.valueOf(request.getExternalCategoryId()))
                .orElseThrow(() -> new RuntimeException("Category not found for external id: " + request.getExternalCategoryId()));


        Optional<Discount> discount = discountRepository.findByCodeAndCategoryId(request.getCode(), category.getId());

        if (discount.isPresent()) {

            //çıkarma işlemi tersten olduğu için - ile çarptık
            BigDecimal newPrice = discount.get().getDiscountPrice().subtract(BigDecimal.valueOf(request.getPrice())).multiply(BigDecimal.valueOf(-1));

            responseObserver.onNext(
                    DiscountResponse.newBuilder()
                            .setCode(discount.get().getCode())
                            .setOldPrice(request.getPrice())
                            .setNewPrice(newPrice.floatValue())
                            .setResponse(Response.newBuilder()
                                    .setStatusCode(true)
                                    .setMessage("Discount has been applied successfuly!").build())
                            .build()
            );


        } else {

            responseObserver.onNext(
                    DiscountResponse.newBuilder()
                            .setOldPrice(request.getPrice())
                            .setNewPrice(request.getPrice())
                            .setCode(discount.get().getCode())
                            .setResponse(Response.newBuilder()
                                    .setMessage("Code and Category are invalid")
                                    .setStatusCode(false)
                                    .build())
                            .build()
            );
        }
        responseObserver.onCompleted();
        }
    }




