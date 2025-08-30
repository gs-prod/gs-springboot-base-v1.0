package com.gs.model.dto.response;

import lombok.Data;

@Data
public class WeChatJsapiPayResponseDTO {
    private String timeStamp;
    private String nonceStr;
    private String packageValue;
    private String signType;
    private String paySign;
}


