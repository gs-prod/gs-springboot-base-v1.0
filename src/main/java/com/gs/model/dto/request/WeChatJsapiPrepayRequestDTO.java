package com.gs.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeChatJsapiPrepayRequestDTO {

    @NotBlank
    private String description;

    @NotBlank
    private String outTradeNo;

    @NotNull
    @Min(1)
    private Integer amount; // 分

    @NotBlank
    private String openid;

    @NotBlank
    private String notifyUrl;
}




