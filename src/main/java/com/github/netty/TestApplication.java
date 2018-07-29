package com.github.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author 84215
 */
@RestController
@SpringBootApplication
public class TestApplication {

    @RequestMapping("/hello")
    public Object hello(@RequestParam Map o, HttpSession session, HttpServletRequest request, HttpServletResponse response){
        List list = new LinkedList<>();
        for(int i=0; i<100; i++){
            Map map = new HashMap<>();
            StringBuilder sb = new StringBuilder();
            for(int j=0; j<500;j++){
                sb.append(j);
            }
            map.put("fd1",sb);
            list.add(map);
        }
        return list;
    }


    /**
     * Start
     * @param args vm参数
     * @throws IOException io异常
     */
    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class, args);
    }



}
