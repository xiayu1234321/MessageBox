package com.csse.platform.topic.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.csse.platform.topic.entity.Topic;
import com.csse.platform.topic.repository.TopicRepository;


@Service
public class TopicService {
	
	@Autowired
	TopicRepository topicRepository;
	
	public String findTopicid(String topicid) {
		return topicRepository.findTopicid(topicid);
	}
}
