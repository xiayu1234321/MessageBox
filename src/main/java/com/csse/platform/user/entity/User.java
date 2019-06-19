package com.csse.platform.user.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USER")
public class User implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 582906167471474858L;
	@Id
	@Column(name = "USERID", nullable = false)
	private String userid;
	
	@Column(name = "USERNAME")
	private String userName; // 用户姓名
	
	@Column(name = "ACCOUNT")
	private String account; // 账号
	
	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	@Column(name = "PASSWORD")
	private String password; // 密码
	@Column(name = "SEX")
	private String sex; // 性别
	@Column(name = "ORDERID")
	private Integer orderId; // 顺序
	
	@Column(name = "TOPICNAM")
	private String topicName; // 顺序
	
	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	@Column(name = "ISMANAGER")
	private Integer isManager; // 是否管理员
	
	@Column(name = "USEREMAIL")
	private String userEmail; //
	
	@Column(name = "MOBILE")
	private String mobile; // mobile phone number
	
	@Column(name = "TIMESTAMP")
	private long timestamp;

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public Integer getOrderId() {
		return orderId;
	}

	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	public Integer getIsManager() {
		return isManager;
	}

	public void setIsManager(Integer isManager) {
		this.isManager = isManager;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}