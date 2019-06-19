package com.csse.platform.topic.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "TOPIC")
public class Topic implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2571414414427308092L;
	/**
	 * 
	 */
	@Id
	@Column(name = "TOPICID", nullable = false)
	private String topicId;
	@Column(name = "TOPICNAME")
	private String topicName; // 全名

	@Column(name = "ORDERID")
	private Integer orderId; // 顺序
	
	@Column(name = "TIMESTAMP")
	private long timestamp;

	public String getTopicId() {
		return topicId;
	}

	public void setTopicId(String topicId) {
		this.topicId = topicId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
