package com.csse.platform.config;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

//import org.springframework.boot.context.embedded.FilterRegistrationBean;

@Configuration
public class MsgConfig {
	@Autowired
	private Environment env;

	@Bean
	public Channel getRabbitConnection() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
        String host = env.getProperty("spring.rabbitmq.host");
        String port = env.getProperty("spring.rabbitmq.port");
        String username = env.getProperty("spring.rabbitmq.username");
        String password = env.getProperty("spring.rabbitmq.password");
        factory.setHost(host);
        factory.setVirtualHost("/");
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setPort(Integer.parseInt(port));
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
		return channel;
	}

}