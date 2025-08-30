package com.gs.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wxpay")
@Getter
@Setter
public class WeChatPayConfig {
    private String merchantId;
    private String merchantSerialNumber;
    private String privateKeyPath;
    private String apiV3Key;
    private String appId;
    private String appSecret;

    @Bean
    public RSAAutoCertificateConfig wechatPayRSAAutoConfig() {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();
    }

    @Bean
    public NativePayService nativePayService(RSAAutoCertificateConfig rsaAutoCertificateConfig) {
        Config config = rsaAutoCertificateConfig;
        return new NativePayService.Builder().config(config).build();
    }

    @Bean
    public JsapiService jsapiService(RSAAutoCertificateConfig rsaAutoCertificateConfig) {
        Config config = rsaAutoCertificateConfig;
        return new JsapiService.Builder().config(config).build();
    }
}
