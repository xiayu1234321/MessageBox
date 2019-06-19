package com.csse.platform.util;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {
	
	/**
	 * 操作成功
	 */
	public static ResponseEntity<IResult> OK=ResponseEntity(HttpStatus.OK,"发送成功");
	
	/**
	 * 初始化成功
	 */
	public static ResponseEntity<IResult> INITOK=ResponseEntity(HttpStatus.OK,"初始化成功");
	
	/**
	 * 资源不存在
	 */
	public static ResponseEntity<IResult> NOT_FOUND=ResponseEntity(HttpStatus.NOT_FOUND,"资源不存在");
	
	
	/**
	 * 创建ResponseEntity
	 * @param statusCode http响应状态
	 * @param message 响应信息
	 * @return
	 */
	public static ResponseEntity<IResult> ResponseEntity(HttpStatus statusCode,String message){
		return new ResponseEntity<IResult>(new IResult(statusCode,message), statusCode);
	}
	
	
	/**
	 * 创建响应头为BAD_REQUEST的ResponseEntity
	 * @param errmsg 错误信息
	 * @return ResponseEntity
	 */
	public static ResponseEntity<IResult> badRequest(String errmsg){
		return new ResponseEntity<IResult>(new IResult(HttpStatus.BAD_REQUEST,errmsg), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * 创建响应头为NOT_FOUND的ResponseEntity
	 * @param errmsg 错误信息
	 * @return ResponseEntity
	 */
	public static ResponseEntity<IResult> notFound(String errmsg){
		return new ResponseEntity<IResult>(new IResult(HttpStatus.NOT_FOUND,errmsg), HttpStatus.NOT_FOUND);
	}
}
class IResult{
	private int rsltcode;
	private String rsltmsg;
	public IResult(){
		
	}
	public IResult(HttpStatus statusCode,String message){
		rsltcode=statusCode.value();
		rsltmsg=message;
	}
	public int getRsltcode() {
		return rsltcode;
	}
	public void setRsltcode(int rsltcode) {
		this.rsltcode = rsltcode;
	}
	public String getRsltmsg() {
		return rsltmsg;
	}
	public void setRsltmsg(String rsltmsg) {
		this.rsltmsg = rsltmsg;
	}
}
