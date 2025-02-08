package com.zscengiz.product.controller;

import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.grpc.UploadStatus;
import com.zscengiz.product.entity.dto.DiscountResponseDTO;
import com.zscengiz.product.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping
    public ResponseEntity<DiscountResponseDTO> getDiscount(@RequestParam("code") String code, @RequestParam("productId") int productId) {

        DiscountResponse discountResponse = discountService.getDiscount(productId,code);
        return ResponseEntity.ok(
                DiscountResponseDTO.builder()
                        .newPrice(discountResponse.getNewPrice())
                        .oldPrice(discountResponse.getOldPrice())
                        .code(discountResponse.getCode())
                        .build()
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        UploadStatus status = discountService.uploadFile(file);
        if (status.getSuccess()) {
            return ResponseEntity.ok(status.getMessage());
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(status.getMessage());
        }
    }

}