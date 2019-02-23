package com.dongnao.demo.client;

import org.I0Itec.zkclient.ZkClient;

public class CreateNodeDemo {
	
	public static void main(String[] args) {
		ZkClient client = new ZkClient("192.168.1.111:2181,192.168.1.111:2182,192.168.1.111:2183", 5000);
		String path = "/zk-client/c1";
		// 递归创建节点
		client.createPersistent(path, true); // true 可以递归创建节点
		
		System.out.println("递归创建节点完成");
	}
	
}
