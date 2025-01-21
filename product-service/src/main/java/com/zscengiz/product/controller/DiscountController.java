package com.zscengiz.product.controller;

import com.zscengiz.grpc.DiscountResponse;
import com.zscengiz.product.entity.dto.DiscountResponseDTO;
import com.zscengiz.product.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    }}