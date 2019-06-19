package com.csse.platform.topic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.csse.platform.topic.entity.Topic;
import com.csse.platform.topic.repository.TopicRepository;


@Controller
public class TopicController {
    @Autowired
	private TopicRepository topicRepository;
    
    @RequestMapping(value = "/topic", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> saveUser(@RequestBody Topic topic){
    	topicRepository.save(topic);
    	JSONObject result = new JSONObject();
    	result.put("rsltcode", 200);
		result.put("rsltmsg", "保存成功！");
    	return new ResponseEntity<JSONObject>(result,HttpStatus.OK);
    }
}
