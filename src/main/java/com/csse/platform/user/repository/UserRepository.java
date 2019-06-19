package com.csse.platform.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.csse.platform.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>{

	@Query(value="select userid from user where userid = #{userid}")
	public String findUserId(String userid);

}
