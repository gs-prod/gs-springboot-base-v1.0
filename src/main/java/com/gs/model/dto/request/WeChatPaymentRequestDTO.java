package com.gs.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeChatPaymentRequestDTO {

    /**
     * 分为单位的金额，最小1
     */
    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be >= 1")
    private Integer amount;

    /**
     * 商品描述
     */
    @NotBlank(message = "description is required")
    private String description;

    /**
     * 商户系统内的订单号，需唯一
     */
    @NotBlank(message = "outTradeNo is required")
    private String outTradeNo;

    /**
     * 异步通知地址（公网可达）
     */
    @NotBlank(message = "notifyUrl is required")
    private String notifyUrl;
}
