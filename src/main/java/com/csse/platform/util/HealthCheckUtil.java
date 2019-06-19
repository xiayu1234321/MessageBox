package com.csse.platform.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Controller
public class HealthCheckUtil {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Environment env;
    
    @Value("${spring.jpa.database}")
    private String dbname;    
    
    @RequestMapping(value = "/health/check", produces = "application/json;charset=utf-8")
    public ResponseEntity checkHealth() throws IOException, TimeoutException {
        Map<String, Object> map = new HashMap<>(1);
        List list = new ArrayList();
        int code = HttpStatus.OK.value();
        boolean rabbitmq = checkRabbitmq();
        if (rabbitmq) {
            list.add(new ResultObj("rabbitmq", true));
        } else {
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
            list.add(new ResultObj("rabbitmq", false));
        }
        boolean db = checkDB();
        if (db) {
            list.add(new ResultObj(dbname, true));
        } else {
            list.add(new ResultObj(dbname, false));
        }       
        map.put("detail", list);
        return ResponseEntity.status(code).body(map);
    }

    /**
     * 检查rabbitmq
     *
     * @return true: 正常， false: 异常
     * @throws TimeoutException
     * @throws IOException
     */
    private boolean checkRabbitmq() throws IOException, TimeoutException {
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
        int code = HttpStatus.OK.value();

        boolean isHealth = false;
        Connection connection = null;
        try {
            connection = factory.newConnection();
            isHealth = true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            code = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return isHealth;

    }

    /**
     * 检查db
     *
     * @return true: 正常， false: 异常
     */
    private boolean checkDB() {
        String hql = "select 1 from dual";
        Query nativeQuery = null;
        try {
            nativeQuery = entityManager.createNativeQuery(hql);
            List resultList = nativeQuery.getResultList();
            if (null == resultList) {
                System.out.println("null");
                return false;
            }
            if (0 == resultList.size()) {
                System.out.println("size=0");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            entityManager.close();
        }

        return true;
    }
   
}
