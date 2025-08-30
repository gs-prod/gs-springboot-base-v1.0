package com.gs.controller;

import com.gs.constant.enums.CodeEnum;
import com.gs.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Tag(name = "WeChatOpenId")
@RestController
@RequestMapping("api/wechat")
@Validated
@RequiredArgsConstructor
public class WeChatOpenIdController {

    // 简化示例：直接通过小程序 appid/secret 调用 jscode2session
    // 生产应放在配置中，通过配置读取
    private final com.gs.config.WeChatPayConfig weChatPayConfig;

    @Operation(summary = "根据code获取openid（jscode2session）")
    @PostMapping("/openid")
    public R getOpenId(@RequestBody Map<String, String> payload) throws Exception {
        String code = payload.get("code");
        String appid = weChatPayConfig.getAppId();
        String secret = weChatPayConfig.getAppSecret();
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code
        );
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return R.success(resp.body());
    }
}




