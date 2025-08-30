package com.gs.service.impl;

import com.gs.config.WeChatPayConfig;
import com.gs.model.dto.request.WeChatJsapiPrepayRequestDTO;
import com.gs.model.dto.request.WeChatPaymentRequestDTO;
import com.gs.model.dto.response.WeChatJsapiPayResponseDTO;
import com.gs.service.intf.WeChatPaymentService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class WeChatPaymentServiceImpl implements WeChatPaymentService {
    private final NativePayService nativePayService;
    private final WeChatPayConfig weChatPayConfig;
    private final JsapiService jsapiService;
    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;

    public WeChatPaymentServiceImpl(NativePayService nativePayService, WeChatPayConfig weChatPayConfig, JsapiService jsapiService, RSAAutoCertificateConfig rsaAutoCertificateConfig) {
        this.nativePayService = nativePayService;
        this.weChatPayConfig = weChatPayConfig;
        this.jsapiService = jsapiService;
        this.rsaAutoCertificateConfig = rsaAutoCertificateConfig;
    }

    @Override
    public String prepay(WeChatPaymentRequestDTO dto) {
        com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest request = new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        com.wechat.pay.java.service.payments.nativepay.model.Amount amount = new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal(dto.getAmount());
        request.setAmount(amount);
        request.setAppid(weChatPayConfig.getAppId());
        request.setMchid(weChatPayConfig.getMerchantId());
        request.setDescription(dto.getDescription());
        request.setNotifyUrl(dto.getNotifyUrl());
        request.setOutTradeNo(dto.getOutTradeNo());
        PrepayResponse response = nativePayService.prepay(request);
        return response.getCodeUrl();
    }

    @Override
    public WeChatJsapiPayResponseDTO jsapiPrepay(WeChatJsapiPrepayRequestDTO dto) {
        // 1. 调用微信下单接口
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(dto.getAmount());
        request.setAmount(amount);
        request.setAppid(weChatPayConfig.getAppId());
        request.setMchid(weChatPayConfig.getMerchantId());
        request.setDescription(dto.getDescription());
        request.setNotifyUrl(dto.getNotifyUrl());
        request.setOutTradeNo(dto.getOutTradeNo());
        Payer payer = new Payer();
        payer.setOpenid(dto.getOpenid());
        request.setPayer(payer);
        
        String prepayId = jsapiService.prepay(request).getPrepayId();
        
        // 2. 生成小程序支付参数
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonceStr = generateNonceStr();
        String packageValue = "prepay_id=" + prepayId;
        String signType = "RSA";
        
        // 3. 生成签名
        String paySign = generatePaySign(weChatPayConfig.getAppId(), timeStamp, nonceStr, packageValue);
        
        // 4. 返回完整支付参数
        WeChatJsapiPayResponseDTO response = new WeChatJsapiPayResponseDTO();
        response.setTimeStamp(timeStamp);
        response.setNonceStr(nonceStr);
        response.setPackageValue(packageValue);
        response.setSignType(signType);
        response.setPaySign(paySign);
        
        return response;
    }
    
    private String generateNonceStr() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private String generatePaySign(String appId, String timeStamp, String nonceStr, String packageValue) {
        // 构造签名串
        String signStr = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n";
        
        try {
            // 使用注入的RSAAutoCertificateConfig生成签名
            return rsaAutoCertificateConfig.createSigner().sign(signStr).getSign();
        } catch (Exception e) {
            throw new RuntimeException("生成支付签名失败", e);
        }
    }
}
