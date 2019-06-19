package com.csse.platform.message.controller;

import static com.csse.platform.message.controller.MessageController.servicepath;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csse.platform.topic.entity.Topic;
import com.csse.platform.topic.repository.TopicRepository;
import com.csse.platform.topic.service.TopicService;
import com.csse.platform.user.entity.User;
import com.csse.platform.user.repository.UserRepository;
import com.csse.platform.user.service.UserService;
import com.csse.platform.util.ResponseUtil;

import javassist.expr.NewArray;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

@RestController
@RequestMapping(value = "/message")
public class MessageController {
	private static final int    UNKNOW_ERROR_CODE = 10000;
	private static final String UNKNOW_ERROR_INFO ="消息服务未知异常";
	private static final int    AMQP_CONNECT_ERROR_CODE = 10000;
	private static final String AMQP_CONNECT_ERROR_INFO ="消息队列连接异常";
	private static final int    ID_INVALID_CODE = 30000;
	private static final String ID_INVALID_INFO = "id均无效";
	private static final int    NOT_UTF_8_CODE = 30000;
	private static final String NOT_UTF_8_INFO = "消息内容不是utf-8编码";

    public static String servicepath;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    Channel channel;
    
    @Autowired
    TopicRepository topicRepository;
    
    @Autowired
    UserRepository userRepository;
    
    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;
    
    @Value("${spring.rabbitmq.username}")
    private String rabbitUserName;
    
    @Value("${spring.rabbitmq.password}")
    private String rabbitPasswd;
    @Autowired
    TopicService topicService;
    @Autowired
    UserService userService;
    
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    /**
     * 初始化队列、交换机及绑定
     *
     * @return
     * @throws IOException 
     * @throws TimeoutException 
     */
    @RequestMapping(value = "init", method = RequestMethod.GET)
    public ResponseEntity<?> init() throws IOException, TimeoutException  {
    	log.info("**starting initialize  message queue **");
        try {
            //获取主题列表
            List<Topic> topicList =  topicRepository.findAll();
    		//创建机构FanoutExchange
    		for (Topic topic : topicList) {
    			//获取主题姓名
    			String topicName = topic.getTopicName();
    			//创建主题FanoutExchange
    			channel.exchangeDeclare(topicName,"fanout", true, false, null);
    		}
    		//获取人员列表
            List<User> userList = userRepository.findAll();
    		for (User user : userList) {
    			//获取主题姓名
    			String topicName = user.getTopicName();
    			//获取用户姓名
    			String userName = user.getUserName();
    			//创建用户队列
    			channel.queueDeclare(userName, true, false, false, null);
    			//绑定用户至主题FanoutExchange
    			channel.exchangeDeclare(topicName, "fanout", true, false ,null);
    			channel.queueBind(userName, topicName, "");
    		}
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseUtil.NOT_FOUND;
        }
        log.info("**Initialization is complete **");
        return ResponseUtil.INITOK;
    }



    /**
	 * 发送消息至一个或多个人   TEST
	 *
	 * @param userids
	 * @param days
	 * @param content
	 * @return
     * @throws IOException 
     * @throws TimeoutException 
	 */
	@RequestMapping(value = "user/{userids}", method = RequestMethod.POST)
	public ResponseEntity<?> sendUsers(@PathVariable("userids") String userids, @RequestParam String content) throws IOException, TimeoutException {
		log.info("the param content is :" + content);
		// 校验消息编码和json格式
		JSONObject msgContent = messageFormValidate(content);
		if (null == msgContent) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "error encodding or not json format !");
		}
		// 校验消息格式
		boolean isMessageFormat = messageValidate(msgContent);
		if (!isMessageFormat) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "Error Message Format");
		}
		String messageString = msgContent.toJSONString();
		log.info("message: " + messageString);
		// 校验id的有效性
		Map map = this.checkUserIds("user", userids);
		boolean checkResult = (boolean) map.get("checkResult");
		ArrayList<String> validUserIds = (ArrayList<String>) map.get("validUserIds"); // 有效id
		ArrayList<String> invalidUserIds = (ArrayList<String>) map.get("invalidUserIds");// 无效id
		// id全部无效
		if (!checkResult) {
			JSONObject result1 = new JSONObject();
			result1.put("rsltcode", ID_INVALID_CODE);
			result1.put("rsltmsg", ID_INVALID_INFO);
			log.info("无效 id:" + invalidUserIds.toString());
			return new ResponseEntity<JSONObject>(result1, HttpStatus.METHOD_NOT_ALLOWED);
		}
		BasicProperties props = new Builder().contentType("application/json").priority(5).contentEncoding("UTF-8").timestamp(new Date()).build();
		int number = validUserIds.size();
		// 发送给20人以上，建立一次性FanoutExchange路由
		if (number > 20) {
			String uuid = UUID.randomUUID().toString();
			channel.exchangeDeclare(uuid, "fanout");
			try {
				for (String id : validUserIds) {
					channel.queueBind(id,uuid,"");
				}
				channel.basicPublish(uuid, uuid,props, messageString.getBytes());
				channel.exchangeDelete(uuid);
			} catch (AmqpConnectException e) {
				log.info(AMQP_CONNECT_ERROR_INFO);
				e.printStackTrace();
				e.printStackTrace();
				JSONObject result = new JSONObject();
				result.put("rsltcode", AMQP_CONNECT_ERROR_CODE);
				result.put("rsltmsg", AMQP_CONNECT_ERROR_INFO);
				channel.close();
				return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (AmqpException e2) {
				log.error(UNKNOW_ERROR_INFO);
				e2.printStackTrace();
				JSONObject result = new JSONObject();
				result.put("rsltcode", UNKNOW_ERROR_CODE);
				result.put("rsltmsg", UNKNOW_ERROR_INFO);
				channel.close();
				return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else { // 20人以下，遍历发送
			for (String id : validUserIds) {
				try {
					channel.basicPublish("", id,props, messageString.getBytes());
					Thread.sleep(5);
				} catch (AmqpConnectException e) {
					log.error(AMQP_CONNECT_ERROR_INFO);
					e.printStackTrace();
					JSONObject result = new JSONObject();
					result.put("rsltcode", AMQP_CONNECT_ERROR_CODE);
					result.put("rsltmsg", AMQP_CONNECT_ERROR_INFO);
					channel.close();
					return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
				} catch (AmqpException e2) {
					log.error(UNKNOW_ERROR_INFO);
					e2.printStackTrace();
					JSONObject result = new JSONObject();
					result.put("rsltcode", UNKNOW_ERROR_CODE);
					result.put("rsltmsg", UNKNOW_ERROR_INFO);
					channel.close();
					return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
				} catch (Exception e3) {
					log.error(UNKNOW_ERROR_INFO);
					e3.printStackTrace();
					JSONObject result = new JSONObject();
					result.put("rsltcode", UNKNOW_ERROR_CODE);
					result.put("rsltmsg", UNKNOW_ERROR_INFO);
					channel.close();
					return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
		}
		if (invalidUserIds.size() > 0) {
			JSONObject result = new JSONObject();
			String invalid = invalidUserIds.toString();
			result.put("rsltcode", 200);
			result.put("rsltmsg", "部分发送成功，无效id:" + invalid);
			result.put("invalid", invalid);
			log.info("部分发送成功，无效id为:" + invalid + ",有效id:" + validUserIds.toString());
			channel.close();
			return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
		} else {
			JSONObject result = new JSONObject();
			result.put("rsltcode", 200);
			result.put("rsltmsg", "发送成功");
			log.info("发送成功, id:" + validUserIds.toString());
			channel.close();
			return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
		}
	}

    /**
	 * 发送消息至所有人   
	 *
	 * @param content
	 * @return
     * @throws IOException 
     * @throws TimeoutException 
	 */
	@RequestMapping(value = "alluser", method = RequestMethod.POST)
	public ResponseEntity<?> sendAllUsers(@RequestParam String content) throws IOException, TimeoutException {
		log.info("the param content is :" + content);
		// 校验消息编码和json格式
		JSONObject msgContent = messageFormValidate(content);
		if (null == msgContent) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "error encodding or not json format !");
		}
		// 校验消息格式
		boolean isMessageFormat = messageValidate(msgContent);
		if (!isMessageFormat) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "Error Message Format");
		}
		String messageString = msgContent.toJSONString();
		BasicProperties props = new Builder().contentType("application/json").priority(5).contentEncoding("UTF-8").timestamp(new Date()).build();
		log.info("message: " + messageString);
        List<User> userlist = userRepository.findAll();
        try {
        	for (User user : userlist) {
    			String username = user.getUserName();
    			channel.basicPublish("", username,props, messageString.getBytes());
    		}
        	JSONObject result = new JSONObject();
    		result.put("rsltcode", 200);
    		result.put("rsltmsg", "发送成功");
    		log.info("发送成功");
    		return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
		} catch (Exception e) {
			log.error(UNKNOW_ERROR_INFO);
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", UNKNOW_ERROR_INFO);
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}	
	}
	
	/**
	 * 创建主题   
	 *
	 * @return
     * @throws IOException 
	 */
	@RequestMapping(value = "/topic/{topicName}", method = RequestMethod.POST)
	public ResponseEntity<?> createTopicName(@PathVariable("topicName") String topicName) throws IOException{
	    if(topicName == null || topicName.trim().length()== 0) {
	    	log.error("主题名为空");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题名不能为空！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    if(checkExchange(topicName)) {
        	log.error("主题已存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题已存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }
	    channel.exchangeDeclare(topicName,"fanout", true, false, null);
    	JSONObject result = new JSONObject();
		result.put("rsltcode", 200);
		result.put("rsltmsg", "创建成功");
		log.info("创建主题成功");
		return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
	}
	
    /**
	 * 订阅主题   
     * @throws IOException 
	 */
	@RequestMapping(value = "/sbTopic/{topicName}", method = RequestMethod.POST)
	public ResponseEntity<?> subTopic(@PathVariable("topicName") String topicName , @RequestParam String userid) throws IOException{
	    if(topicName == null || topicName.trim().length()== 0) {
	    	log.error("主题名为空");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题名不能为空！");
			log.info("主题名不能为空！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    if(!checkExchange(topicName)) {
			log.error("主题不存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题不存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(!checkQueue(userid)) {
			log.error("人员不存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "人员不存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		channel.queueBind(userid, topicName, "");
		JSONObject result = new JSONObject();
		result.put("rsltcode", 200);
		result.put("rsltmsg", "订阅成功");
		log.info("订阅主题成功");
		return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
	}
	
	/**
	 * 取消订阅   
	 *
	 * @return
     * @throws IOException  
	 */
	@RequestMapping(value = "/rmTopic/{topicName}", method = RequestMethod.POST)
	public ResponseEntity<?> rmTopic(@PathVariable("topicName") String topicName , @RequestParam String userid) throws IOException{
	    if(topicName == null || topicName.trim().length()== 0) {
	    	log.error("主题名为空");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题名不能为空！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	    if(!checkExchange(topicName)) {
			log.error("主题不存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题不存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(!checkQueue(userid)) {
			log.error("人员不存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "人员不存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	    channel.queueUnbind(userid, topicName, "");
    	JSONObject result = new JSONObject();
		result.put("rsltcode", 200);
		result.put("rsltmsg", "取消订阅成功");
		log.info("取消订阅成功");
		return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
	}
		
	/**
	 * 发送消息至主题
	 *
	 * @param topicName
	 * @param content
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value = "sendTopic/{topicName}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<?> sendTopic(@PathVariable("topicName") String topicName,
			@RequestParam String content) throws IOException{
		log.info("the param content is :" + content);
		// 校验消息编码和json格式
		JSONObject msgContent = messageFormValidate(content);
		if (null == msgContent) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "error encodding or not json format !");
		}
		// 校验消息格式
		boolean isMessageFormat = messageValidate(msgContent);
		if (!isMessageFormat) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "Error Message Content Format");
		}
		String messageString = msgContent.toJSONString();
		log.info("message: " + messageString);
		// 设置消息属性
		BasicProperties props = new Builder().contentType("application/json").priority(5).contentEncoding("UTF-8").timestamp(new Date()).build();
		if(topicName == null || topicName.trim().length()== 0) {
			log.error("主题名为空");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题名不能为空！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		if(!checkExchange(topicName)) {
			log.error("主题不存在");
			JSONObject result = new JSONObject();
			result.put("rsltcode", UNKNOW_ERROR_CODE);
			result.put("rsltmsg", "主题不存在！");
			return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		// 向主题发送消息
		channel.basicPublish(topicName, "", props, messageString.getBytes());
		JSONObject result = new JSONObject();
		result.put("rsltcode", 200);
		result.put("rsltmsg", "发送成功");
		log.info("发送成功");
		return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
	}

	/**
	 * 发送消息至一个或多个组织机构
	 *
	 * @param departmentids
	 * @param days
	 * @param sublevel
	 *            //20170524删除该参数
	 * @param content
	 * @return
	 * @throws IOException 
	 * @throws TimeoutException 
	 */
	@RequestMapping(value = "department/{departmentids}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<?> sendDepartments(@PathVariable("departmentids") String departmentids,
			@RequestParam String content) throws IOException, TimeoutException {
		log.info("the param content is :" + content);
		// 校验消息编码和json格式
		JSONObject msgContent = messageFormValidate(content);
		if (null == msgContent) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "error encodding or not json format !");
		}
		// 校验消息格式
		boolean isMessageFormat = messageValidate(msgContent);
		if (!isMessageFormat) {
			return ResponseUtil.ResponseEntity(HttpStatus.METHOD_NOT_ALLOWED, "Error Message Content Format");
		}
		String messageString = msgContent.toJSONString();
		log.info("message: " + messageString);
		// 校验id的有效性
		Map map = this.checkUserIds("organ", departmentids);
		boolean checkResult = (boolean) map.get("checkResult");
		ArrayList<String> validUserIds = (ArrayList<String>) map.get("validUserIds"); // 有效id
		ArrayList<String> invalidUserIds = (ArrayList<String>) map.get("invalidUserIds");// 无效id
		// id全部无效
		if (!checkResult) {
			JSONObject result1 = new JSONObject();
			result1.put("rsltcode", ID_INVALID_CODE);
			result1.put("rsltmsg", ID_INVALID_INFO);
			log.info("无效 id:" + invalidUserIds.toString());
			return new ResponseEntity<JSONObject>(result1, HttpStatus.METHOD_NOT_ALLOWED);
		}
		// 设置消息属性
		BasicProperties props = new Builder().contentType("application/json").priority(5).contentEncoding("UTF-8").timestamp(new Date()).build();
		// 向每个主题发送消息
		for (String deptid : validUserIds) {
			// 发送消息
			try {
				//rabbitTemplate.convertAndSend(deptid, deptid, message);
				channel.basicPublish(deptid, deptid, props, messageString.getBytes());
				Thread.sleep(5);
			} catch (AmqpConnectException e) {
				log.error(AMQP_CONNECT_ERROR_INFO);
				e.printStackTrace();
				JSONObject result = new JSONObject();
				result.put("rsltcode", AMQP_CONNECT_ERROR_CODE);
				result.put("rsltmsg", AMQP_CONNECT_ERROR_INFO);
				return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (AmqpException e2) {
				log.error(UNKNOW_ERROR_INFO);
				e2.printStackTrace();
				JSONObject result = new JSONObject();
				result.put("rsltcode", UNKNOW_ERROR_CODE);
				result.put("rsltmsg", UNKNOW_ERROR_INFO);
				return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (Exception e3) {
				log.error(UNKNOW_ERROR_INFO);
				e3.printStackTrace();
				JSONObject result = new JSONObject();
				result.put("rsltcode", UNKNOW_ERROR_CODE);
				result.put("rsltmsg", UNKNOW_ERROR_INFO);
				return new ResponseEntity<JSONObject>(result, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		if (invalidUserIds.size() > 0) {
			JSONObject result = new JSONObject();
			String invalid = invalidUserIds.toString();
			result.put("rsltcode", 200);
			result.put("rsltmsg", "部分发送成功，无效id:" + invalid);
			result.put("invalid", invalid);
			log.info("部分发送成功，无效id为:" + invalid + ",有效id:" + validUserIds.toString());
			return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
		} else {
			JSONObject result = new JSONObject();
			result.put("rsltcode", 200);
			result.put("rsltmsg", "发送成功");
			log.info("发送成功, id:" + validUserIds.toString());
			return new ResponseEntity<JSONObject>(result, HttpStatus.OK);
		}
	}
	
    /**
     * 接收消息
     * 目前的使用场景仅有桌面接受当前登录人消息
     */
    @RequestMapping(value = "recieve/{userid}", method = RequestMethod.GET)
    public ResponseEntity<?> recieveMessage(@PathVariable("userid") String userid) {
        JSONArray arr = new JSONArray();
        while (true) {
            //接收消息
            Message msg = rabbitTemplate.receive(userid);
            if (msg == null) break;
            JSONObject json = new JSONObject();
            try {
                json.put("content", new String(msg.getBody(), "UTF-8"));
                json.put("timestamp", msg.getMessageProperties().getTimestamp());
                arr.add(json);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (msg.getMessageProperties().getMessageCount() == 0) {
                break;
            }
        }
        return new ResponseEntity<JSONArray>(arr, HttpStatus.OK);
    }

    /**
     * 校验消息内容是否为UTF-8格式和json格式
     */
    public JSONObject messageFormValidate(String message) {
        String contentUTF_8 = null;
        try {
            contentUTF_8 = new String(message.getBytes("utf-8"), "utf-8");  //检查是否为UTF-8编码
            if (!message.equals(contentUTF_8)) {
                log.error("encode error ! please check the message encoding is UTF-8 !");
                return null;
            }
        } catch (UnsupportedEncodingException e2) {
            log.error("encode error ! please check the message encoding is UTF-8 !");
            e2.printStackTrace();
        }
        JSONObject msgContent = null;
        try {
            msgContent = JSONObject.parseObject(contentUTF_8);  //检验是否为json格式
        } catch (Exception e) {
            log.error("message content is not json format!!!!!!!!!");
            e.printStackTrace();
        }
        if (null == msgContent) {
            log.error("message content is not json format!!!!!!!!!");
            return null;
        }
        return msgContent;
    }

    /**
     * 验证消息内容的格式，格式正确返回true,格式错误返回false
     *
     * @param message
     * @return
     */
    public boolean messageValidate(JSONObject message) {
        try {

            //校验消息类型
            String type = message.getString("type");
            boolean isTypeTrue = "document".equals(type) || "announcement".equals(type) || "application".equals(type) || "email".equals(type) || "disk".equals(type) || "im".equals(type) || "store".equals(type) || "other".equals(type);
            if (!isTypeTrue) {
                log.error("消息类型不匹配");
                return false;
            }

            //校验消息标题
            String title = message.getString("title");
            if (title.isEmpty()) {
                log.error("消息标题为空");
                return false;
            }

            //校验消息内容
            String content = message.getString("content");
            if (content.isEmpty()) {
                log.error("消息内容为空");
                return false;
            }

            //添加消息时间戳
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            message.put("timestamp", timestamp);//添加时间戳

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
//	/**
//	 * 校验传过来的ids参数是否合法
//	 * @param checkType 检查类型，值为user/organ ,不可为空
//	 * @param ids
//	 * @return
//	 */
//		public Map checkUserIds(String checkType ,String ids) {
//			Map map = new HashMap<>();
//			String[] idsArray = ids.split(",");
//			String regular = "[A-Za-z0-9-]+";
//			Pattern p = Pattern.compile(regular);
//			//检查是否含有无效id
//			ArrayList<String> validUserIds = new ArrayList<>();//有效id
//			ArrayList<String> invalidUserIds = new ArrayList<>();//无效id
//			for(int i=0;i<idsArray.length;i++) {
//				String id = idsArray[i];
//				//判断id是否有非法字符
//				Matcher m = p.matcher(id);
//				if(!m.matches()) {//该id若含有非法字符，则为无效id
//					invalidUserIds.add(id);
//					continue;
//				}
//				//从数据库查询是否合法
//				String resultid = null ;
//				if("user".equals(checkType)) {
//					 resultid = userService.findUserid(id);
//				}else if("organ".equals(checkType)) {
//					resultid = organService.findOrganId(id);
//				}
//				if(null != resultid && resultid.length()>0 ) {
//					validUserIds.add(id);
//				}else {
//					invalidUserIds.add(id);
//				}
//			}
//			
//			if(validUserIds.size()>0) { //有一个及以上有效id，返回结果设为true，表示需要发送消息
//				map.put("checkResult", true); 
//			}else {
//				map.put("checkResult", false);
//			}
//			map.put("validUserIds", validUserIds);
//			map.put("invalidUserIds", invalidUserIds);
//			return map;
//		}

	/**
	 * 校验交换机是否存在
	 */
	public boolean checkExchange(String topicName) throws ClientProtocolException, IOException {
		String url = "http://" + rabbitHost + ":15672/api/exchanges/%2F/" + topicName; 
		HttpGet httpPost = new HttpGet(url);
        CloseableHttpClient pClient=getBasicHttpClient(rabbitUserName,rabbitPasswd);
        CloseableHttpResponse response = null;
        JSONArray jsonArray =null;
        response = pClient.execute(httpPost);
        StatusLine status = response.getStatusLine();
        int state = status.getStatusCode();
        if(state == 200) {
        	return true;
        }
        return false;
	}
	
	/**
	 * 校验交队列是否存在
	 */
	public boolean checkQueue(String userid) throws ClientProtocolException, IOException {
		String url = "http://" + rabbitHost + ":15672/api/queues/%2F/" + userid; 
		HttpGet httpPost = new HttpGet(url);
        CloseableHttpClient pClient=getBasicHttpClient(rabbitUserName,rabbitPasswd);
        CloseableHttpResponse response = null;
        JSONArray jsonArray =null;
        response = pClient.execute(httpPost);
        StatusLine status = response.getStatusLine();
        int state = status.getStatusCode();
        if(state == 200) {
        	return true;
        }
        return false;
	}
	
	private static CloseableHttpClient getBasicHttpClient(String username,String password) {
        // 创建HttpClientBuilder
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        // 设置BasicAuth
        CredentialsProvider provider = new BasicCredentialsProvider();
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username,password);
        provider.setCredentials(scope, credentials);
        httpClientBuilder.setDefaultCredentialsProvider(provider);
        return httpClientBuilder.build();
    }
	
	/**
	 * 校验传过来的ids参数是否合法
	 * @param checkType 检查类型，值为user/organ ,不可为空
	 * @param ids
	 * @return
	 */
		public Map checkUserIds(String checkType ,String ids) {
			Map map = new HashMap<>();
			String[] idsArray = ids.split(",");
			String regular = "[A-Za-z0-9-]+";
			Pattern p = Pattern.compile(regular);
			//检查是否含有无效id
			ArrayList<String> validUserIds = new ArrayList<>();//有效id
			ArrayList<String> invalidUserIds = new ArrayList<>();//无效id
			for(int i=0;i<idsArray.length;i++) {
				String id = idsArray[i];
				//判断id是否有非法字符
				Matcher m = p.matcher(id);
				if(!m.matches()) {//该id若含有非法字符，则为无效id
					invalidUserIds.add(id);
					continue;
				}
				//从数据库查询是否合法
				String resultid = null ;
				if("user".equals(checkType)) {
					 resultid = userService.findUserid(id);
				}else if("organ".equals(checkType)) {
					resultid = topicService.findTopicid(id);
				}
				if(null != resultid && resultid.length()>0 ) {
					validUserIds.add(id);
				}else {
					invalidUserIds.add(id);
				}
			}
			
			if(validUserIds.size()>0) { //有一个及以上有效id，返回结果设为true，表示需要发送消息
				map.put("checkResult", true); 
			}else {
				map.put("checkResult", false);
			}
			map.put("validUserIds", validUserIds);
			map.put("invalidUserIds", invalidUserIds);
			return map;
		}

}