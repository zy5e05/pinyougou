package com.pinyougou.pay.service;

import java.util.Map;

public interface WeixinPayService {

	Map createNative(String out_trade_no, String total_fee);

	Map queryPayStatus(String out_trade_no);

	Map closePay(String out_trade_no);

}
