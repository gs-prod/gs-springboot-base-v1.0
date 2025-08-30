Page({
  data: {
    amount: 100, // 金额（分）
    description: '测试商品标题'
  },

  // 点击支付按钮
  onPay() {
    wx.showLoading({ title: '正在下单...' });
    
    // 1. 获取用户登录凭证
    wx.login({
      success: async ({ code }) => {
        try {
          // 2. 用 code 换 openid
          const openidRes = await this.request({
            url: 'http://localhost:8081/api/wechat/openid',
            method: 'POST',
            data: { code }
          });
          
          if (openidRes.code !== '10000') {
            throw new Error('获取openid失败: ' + openidRes.message);
          }
          
          // 解析返回的 openid
          const openidData = JSON.parse(openidRes.result);
          const openid = openidData.openid;
          
          if (!openid) {
            throw new Error('openid为空');
          }

          // 3. 调用后端 JSAPI 下单，获取完整支付参数
          const prepayRes = await this.request({
            url: 'http://localhost:8081/api/payments/jsapi/prepay',
            method: 'POST',
            data: {
              description: this.data.description,
              outTradeNo: 'order_' + Date.now(),
              amount: this.data.amount,
              openid: openid,
              notifyUrl: 'https://placeholder.example.com/notify'
            }
          });

          if (prepayRes.code !== '10000') {
            throw new Error('下单失败：' + prepayRes.message);
          }

          const payParams = prepayRes.result;
          
          // 4. 调起支付
          wx.requestPayment({
            timeStamp: payParams.timeStamp,
            nonceStr: payParams.nonceStr,
            package: payParams.packageValue,
            signType: payParams.signType,
            paySign: payParams.paySign,
            success: () => {
              wx.hideLoading();
              wx.showToast({ title: '支付成功', icon: 'success' });
            },
            fail: (err) => {
              wx.hideLoading();
              console.error('支付失败:', err);
              wx.showToast({ 
                title: err.errMsg || '支付失败', 
                icon: 'error' 
              });
            }
          });

        } catch (error) {
          wx.hideLoading();
          console.error('支付流程错误:', error);
          wx.showToast({ 
            title: error.message || '支付异常', 
            icon: 'error' 
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        wx.showToast({ title: '登录失败', icon: 'error' });
      }
    });
  },

  // 封装请求方法
  request(options) {
    return new Promise((resolve, reject) => {
      wx.request({
        ...options,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.data);
          } else {
            reject(new Error(`请求失败: ${res.statusCode}`));
          }
        },
        fail: reject
      });
    });
  }
});


