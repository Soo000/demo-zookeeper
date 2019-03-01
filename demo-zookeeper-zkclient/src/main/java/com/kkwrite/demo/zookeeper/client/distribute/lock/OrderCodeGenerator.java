package com.kkwrite.demo.zookeeper.client.distribute.lock;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 订单编号产生器
 * @author Soosky
 *
 */
public class OrderCodeGenerator {

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private int i = 0;
	
	/**
	 * 获取订单编号
	 */
	public String getOrderCode() {
		String orderCode = sdf.format(new Date())  + "-" + i++;
		return orderCode;
	}

}
