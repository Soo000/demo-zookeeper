package com.kkwrite.demo.zookeeper.client.distribute.lock;

import java.util.concurrent.locks.Lock;

/**
 * 使用了分布式锁的订单服务
 * @author Soosky
 *
 */
public class OrderServiceWithDistributeLock implements OrderService {
	
	private static OrderCodeGenerator ocg = new OrderCodeGenerator();
	
	// 分布式锁
	//private Lock lock = new ZkDistributeLock("/mylock");
	// 使用避免惊群效应的分布式锁
	private Lock lock = new ZkDistributeLockImproved("/mylocks");

	/**
	 * 生成订单
	 */
	@Override
	public void createOrder() {
		String orderCode = null;
		
		try {
			lock.lock();
			// 获取订单编号
			orderCode = ocg.getOrderCode();
			System.out.println("生成订单 " + orderCode);
		} finally {
			lock.unlock();
		}
		
	}
	
}
