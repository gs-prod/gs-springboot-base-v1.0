package com.gs.service.intf;

import com.gs.model.dto.request.WeChatPaymentRequestDTO;
import com.gs.model.dto.request.WeChatJsapiPrepayRequestDTO;
import com.gs.model.dto.response.WeChatJsapiPayResponseDTO;

public interface WeChatPaymentService {

    /**
     * 微信Native下单，返回code_url
     */
    String prepay(WeChatPaymentRequestDTO requestDTO);

    /**
     * 微信JSAPI下单，返回完整支付参数
     */
    WeChatJsapiPayResponseDTO jsapiPrepay(WeChatJsapiPrepayRequestDTO requestDTO);
}
