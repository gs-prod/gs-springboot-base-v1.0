package com.gs.service.impl;

import com.gs.service.intf.PaymentOrderService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仅用于演示的内存订单服务：
 * - 使用内存 Map 保存：订单金额、是否已支付、微信流水号
 */
@Service
public class PaymentOrderServiceImpl implements PaymentOrderService {

    private final Map<String, Long> orderAmountMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> orderPaidMap = new ConcurrentHashMap<>();
    private final Map<String, String> orderTxnMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 示例：预置一个订单（outTradeNo -> amount）用于演示金额校验
        orderAmountMap.put("out_trade_no_001", 100L);
        orderPaidMap.put("out_trade_no_001", false);
    }

    @Override
    public boolean validateAmountAndMarkPaidIfUnprocessed(String outTradeNo, long paidTotal, String transactionId) {
        Long expected = orderAmountMap.get(outTradeNo);
        if (expected == null) {
            return false; // 订单不存在
        }
        if (!Objects.equals(expected, paidTotal)) {
            return false; // 金额不一致
        }
        // 幂等：仅当未标记支付时更新
        Boolean paid = orderPaidMap.getOrDefault(outTradeNo, false);
        if (Boolean.TRUE.equals(paid)) {
            return false; // 已处理过
        }
        orderPaidMap.put(outTradeNo, true);
        orderTxnMap.put(outTradeNo, transactionId);
        return true;
    }
}




