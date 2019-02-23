package com.dongnao.demo.election;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/** 
 * zk 实现 tomcat 选举
 *
 * @author Soosky Wang
 * @date 2018年8月13日 上午11:20:47 
 * @version 1.0.0
 */
public class ZkTomcatValve extends ValveBase {
	
	private static CuratorFramework client;
	// zk 临时节点（传国玉玺）
	private static final String zkPath = "/tomcat/activelogc";
	
	private static TreeCache cache;

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {
		client = CuratorFrameworkFactory.builder()
				.connectString("192.168.1.113:2181")
				.connectionTimeoutMs(10000)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3))
				.build();
		
		client.start();
		
		try {
			createZKNode(zkPath);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				// 创建节点失败时对事件进行监听
				addZKNodeListner(zkPath);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * 创建节点
	 * @param path
	 */
	private void createZKNode(String path) throws Exception {
		client.create()
			.creatingParentsIfNeeded()
			.withMode(CreateMode.EPHEMERAL)
			.forPath(path);
		
		System.out.println("创建节点成功，节点当选为皇帝（master）");
	}
	
	/**
	 * 对节点进行监听
	 * @param path
	 * @throws Exception
	 */
	private void addZKNodeListner(String path) throws Exception {
		cache = new TreeCache(client, path);
		
		cache.start();
		
		// 添加监听
		cache.getListenable().addListener(new TreeCacheListener() {
			
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				if (event.getData() != null && event.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
					System.out.print("=== 老皇帝挂了，赶紧强玉玺!");
					try {
						createZKNode(zkPath);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				System.out.println("已经派间谍监控玉玺");
			}
		});
		
	}

}
