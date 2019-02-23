package com.kkwrite.demo.zookeeper.client.distribute.lock;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Main {

	public static void main(String[] args) {
		int currency = 40;
		
		CyclicBarrier cb = new CyclicBarrier(currency);
		
		for (int i = 0; i < currency; i++) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// 模拟分布式集群场景
					OrderService orderService = new OrderServiceWithDistributeLock();
					System.out.println(Thread.currentThread().getName() + " 准备好了...");
					
					
					// 等待一起触发
					try {
						cb.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
					
					// 调用创建订单服务
					orderService.createOrder();
				}
			}).start();
		}
		
	}
	
}
