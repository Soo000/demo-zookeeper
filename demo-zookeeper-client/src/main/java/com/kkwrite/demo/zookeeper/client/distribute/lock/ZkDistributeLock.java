package com.kkwrite.demo.zookeeper.client.distribute.lock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

/**
 * 分布式锁 （未处理惊群效应问题）
 * @since 0.1
 */
public class ZkDistributeLock implements Lock {
	
	/**
	 * 锁路径
	 */
	private String lookPath;
	
	private ZkClient zkClient;
	
	public ZkDistributeLock(String lookPath) {
		super();
		this.lookPath = lookPath;
		
		zkClient = new ZkClient("192.168.1.111:2181");
	}

	@Override
	public boolean tryLock() {
		try {
			// 尝试获取锁（创建临时节点）
			zkClient.createEphemeral(lookPath);
			return true;
		} catch (ZkNodeExistsException e) {
			return false;
		}
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return false;
	}
	
	@Override
	public void lock() {
		if (!tryLock()) { // 如果没有获取锁
			// 阻塞自己
			waitForLock();

			// 唤醒之后，再次尝试加锁
			lock();
		}
	}

	@Override
	public void unlock() {
		// 删除临时节点
		zkClient.delete(lookPath);
	}

	@Override
	public Condition newCondition() {
		return null;
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
	}
	
	/**
	 * 阻塞自己
	 */
	private void waitForLock() {
		CountDownLatch cdl = new CountDownLatch(1);
		
		
		// 定义节点监听器
		IZkDataListener listener = new IZkDataListener() {
			
			@Override
			public void handleDataDeleted(String dataPath) throws Exception {
				System.out.println("收到节点被删除通知，唤醒自己");
				cdl.countDown(); // 对计数器减 1；当计数器值为 0时，会唤醒所有调用器await()阻塞的线程
			}
			
			@Override
			public void handleDataChange(String dataPath, Object data) throws Exception {
				
			}
		};
		
		// watcher 组测，监听 lookPath 节点
		zkClient.subscribeDataChanges(lookPath, listener);
		
		// 阻塞自己
		if (zkClient.exists(lookPath)) {
			try {
				cdl.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// 取消 watcher 注册
		zkClient.unsubscribeDataChanges(lookPath, listener);
	}
}
