package com.csse.platform.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.csse.platform.user.repository.UserRepository;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;
	
	public String findUserid(String userid) {
		return userRepository.findUserid(userid);
	}
}
