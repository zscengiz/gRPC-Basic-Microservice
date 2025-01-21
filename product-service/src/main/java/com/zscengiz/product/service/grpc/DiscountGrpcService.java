package com.zscengiz.product.service.grpc;

import com.zscengiz.grpc.DiscountRequest;
import com.zscengiz.grpc.DiscountResponse;

public interface DiscountGrpcService {

    DiscountResponse getDiscount(DiscountRequest discountRequest);

}
