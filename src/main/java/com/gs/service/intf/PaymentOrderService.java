package com.gs.service.intf;

/**
 * 示例：订单金额校验 + 支付结果幂等更新
 * 实际项目请改为数据库实现，并基于订单表与支付记录表完成幂等、签名校验、金额比对等。
 */
public interface PaymentOrderService {

    /**
     * 校验订单金额并在未处理的情况下将状态置为“已支付”，返回是否本次更新成功。
     * 若已经处理过（幂等），应返回 false。
     */
    boolean validateAmountAndMarkPaidIfUnprocessed(String outTradeNo, long paidTotal, String transactionId);
}




