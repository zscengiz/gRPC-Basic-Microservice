package com.zscengiz.product.service;

import com.zscengiz.grpc.Discount;
import com.zscengiz.grpc.DiscountRequest;
import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.grpc.UploadStatus;
import com.zscengiz.product.entity.Product;
import com.zscengiz.product.service.grpc.DiscountGrpcService;
import com.zscengiz.product.service.grpc.DiscountGrpcServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final ProductService productService;
    private final DiscountGrpcServiceImpl discountGrpcService;

    public DiscountResponse getDiscount(int productId, String code) {

        Product product = productService.getById(productId);
        DiscountRequest discountRequest = DiscountRequest.newBuilder()
                .setCode(code)
                .setPrice(product.getPrice().floatValue())
                .setExternalCategoryId(product.getCategory().getId())
                .build();
        return discountGrpcService.getDiscount(discountRequest);
    }

    public UploadStatus uploadFile(MultipartFile file) {
        try {
            return discountGrpcService.uploadFile(file);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UploadStatus.newBuilder()
                    .setSuccess(false)
                    .setMessage("Upload interrupted: " + e.getMessage())
                    .build();
        }
    }


}
