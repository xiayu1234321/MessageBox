package com.csse.platform.topic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.csse.platform.topic.entity.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, String>{

	@Query(value="select topicid from topic where topicid = #{topicid}")
	public String findTopicid(String topicid);

	
}
