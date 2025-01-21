package com.zscengiz.product.service;

import com.zscengiz.grpc.Discount;
import com.zscengiz.grpc.DiscountRequest;
import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.product.entity.Product;
import com.zscengiz.product.service.grpc.DiscountGrpcService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final ProductService productService;
    private final DiscountGrpcService discountGrpcService;

    public DiscountResponse getDiscount(int productId, String code) {

        Product product = productService.getById(productId);
        DiscountRequest discountRequest = DiscountRequest.newBuilder()
                .setCode(code)
                .setPrice(product.getPrice().floatValue())
                .setExternalCategoryId(product.getCategory().getId())
                .build();
        return discountGrpcService.getDiscount(discountRequest);


    }

}
