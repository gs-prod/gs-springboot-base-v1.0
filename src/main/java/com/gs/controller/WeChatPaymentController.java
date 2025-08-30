package com.gs.controller;

import com.gs.constant.enums.CodeEnum;
import com.gs.model.dto.request.WeChatJsapiPrepayRequestDTO;
import com.gs.model.dto.request.WeChatPaymentRequestDTO;
import com.gs.model.dto.response.WeChatJsapiPayResponseDTO;
import com.gs.service.intf.WeChatPaymentService;
import com.gs.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WeChatPayment")
@RestController
@RequestMapping("api/payments")
@Validated
@RequiredArgsConstructor
public class WeChatPaymentController {

    private final WeChatPaymentService weChatPaymentService;

    @Operation(summary = "微信Native下单，返回code_url")
    @PostMapping(value = "/native/prepay")
    public R prepay(@Validated @RequestBody WeChatPaymentRequestDTO requestDTO) {
        try {
            String codeUrl = weChatPaymentService.prepay(requestDTO);
            return R.success(codeUrl);
        } catch (Exception e) {
            return R.error(CodeEnum.IS_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "微信JSAPI下单，返回完整支付参数")
    @PostMapping(value = "/jsapi/prepay")
    public R jsapiPrepay(@Validated @RequestBody WeChatJsapiPrepayRequestDTO requestDTO) {
        try {
            WeChatJsapiPayResponseDTO payParams = weChatPaymentService.jsapiPrepay(requestDTO);
            return R.success(payParams);
        } catch (Exception e) {
            return R.error(CodeEnum.IS_FAIL.getCode(), e.getMessage());
        }
    }
}
