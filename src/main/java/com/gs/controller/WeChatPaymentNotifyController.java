package com.gs.controller;

import com.gs.constant.enums.CodeEnum;
import com.gs.utils.R;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.gs.service.intf.PaymentOrderService;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/payments/notify")
@Validated
@RequiredArgsConstructor
public class WeChatPaymentNotifyController {

    private final RSAAutoCertificateConfig rsaAutoCertificateConfig;
    private final PaymentOrderService paymentOrderService;

    /**
     * 微信支付异步通知（Native支付）
     * 微信会以POST JSON方式推送，需验签并解析为 Transaction
     */
    @PostMapping(value = "/native", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public R nativeNotify(
            @RequestHeader(name = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(name = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(name = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(name = "Wechatpay-Serial", required = false) String serial,
            @RequestBody String body
    ) {
        try {
            NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .timestamp(timestamp)
                    .signature(signature)
                    .body(body)
                    .build();

            Transaction transaction = parser.parse(requestParam, Transaction.class);

            // 业务处理：幂等 + 金额校验
            String outTradeNo = transaction.getOutTradeNo();
            var tradeState = transaction.getTradeState();
            String transactionId = transaction.getTransactionId();
            Integer total = transaction.getAmount() != null ? transaction.getAmount().getTotal() : null;

            if (tradeState != com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.SUCCESS) {
                return R.error(CodeEnum.IS_FAIL.getCode(), "非成功支付状态");
            }
            if (total == null) {
                return R.error(CodeEnum.IS_FAIL.getCode(), "金额为空");
            }
            boolean updated = paymentOrderService.validateAmountAndMarkPaidIfUnprocessed(outTradeNo, total.longValue(), transactionId);
            if (!updated) {
                // 若金额不一致或已处理过，按需返回成功（防止微信重复通知），同时记录日志
                return R.success("SUCCESS");
            }

            return R.success("SUCCESS");
        } catch (ValidationException e) {
            log.error("微信通知验签失败", e);
            return R.error(CodeEnum.IS_FAIL.getCode(), "验签失败");
        } catch (Exception e) {
            log.error("微信通知处理异常", e);
            return R.error(CodeEnum.IS_FAIL.getCode(), "处理异常");
        }
    }
}


