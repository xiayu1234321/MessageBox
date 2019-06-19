package com.csse.platform.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csse.platform.user.entity.User;
import com.csse.platform.user.repository.UserRepository;

@Controller
public class UserController {
    @Autowired
	private UserRepository userRepository;
    
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> saveUser(@RequestBody User user){
    	userRepository.save(user);
    	JSONObject result = new JSONObject();
    	result.put("rsltcode", 200);
		result.put("rsltmsg", "保存成功！");
    	return new ResponseEntity<JSONObject>(result,HttpStatus.OK);
    }
}
