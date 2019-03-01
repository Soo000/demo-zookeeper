package com.kkwrite.demo.zookeeper.client.distribute.lock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

/**
 * 分布式锁 （解决了惊群效应问题）
 * @since 0.1
 */
public class ZkDistributeLockImproved implements Lock {
	
	/**
	 * 锁路径
	 */
	private String lookPath;
	
	/**
	 * 当前线程的临时顺序节点
	 */
	private String currentPath;
	
	/**
	 * 上一个临时顺序节点
	 */
	private String beforePath;
	
	private ZkClient zkClient;
	
	public ZkDistributeLockImproved(String lookPath) {
		super();
		this.lookPath = lookPath;
		
		zkClient = new ZkClient("192.168.1.111:2181,192.168.1.111:2182,192.168.1.111:2183");
		
		// 创建锁的父节点（这里也会并发创建）
		if (!zkClient.exists(lookPath)) {
			try {
				zkClient.createPersistent(lookPath);
			} catch (Exception e) {
			}
		}
	}

	@Override
	public boolean tryLock() {
		if (currentPath == null) {
			// 尝试获取锁（创建临时顺序节点）
			currentPath = zkClient.createEphemeralSequential(lookPath + "/", "aaa");
		}
		
		// 获取所有子节点，
		List<String> children = zkClient.getChildren(lookPath);
		// 进行排序
		Collections.sort(children);
		// 判断当前节点是否是最小的
		if (currentPath.equals(lookPath + "/" + children.get(0))) {
			return true;
		} else {
			// 获取前一个节点
			int curIndex = children.indexOf(currentPath.substring(lookPath.length() + 1));
			beforePath = lookPath + "/" + children.get(curIndex - 1);
		}
		
		return false;
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
		zkClient.delete(currentPath);
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
		
		// watcher 组测，监听 beforePath 节点
		zkClient.subscribeDataChanges(beforePath, listener);
		
		// 阻塞自己
		if (zkClient.exists(beforePath)) {
			try {
				cdl.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// 取消 watcher 注册
		zkClient.unsubscribeDataChanges(beforePath, listener);
	}
}
