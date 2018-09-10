package com.github.netty;

import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.springboot.NettyEmbeddedServletContainerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * jvisualvm  jconsole
 *
 * @author 84215  (示例)
 */
@RestController
@SpringBootApplication
public class ExampleApplication{

    @Bean
    NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
        ContainerConfig config = new ContainerConfig();
//        config.setSessionRemoteServerAddress(new InetSocketAddress("localhost",8082));
        return new NettyEmbeddedServletContainerFactory(config);
    }

    public static void main(String[] args){
        //开启定时统计任务
        ThreadPoolX.getDefaultInstance().scheduleAtFixedRate(new ReportTask(), 5, 5, TimeUnit.SECONDS);
        SpringApplication.run(ExampleApplication.class, args);
    }

    @RequestMapping("/hello")
    public Object hello(@RequestParam Map query, @RequestBody(required = false) Map body, HttpSession session,
                        HttpServletRequest request, HttpServletResponse response, Principal principal) throws IOException {
//        response.sendRedirect("http://www.baidu.com");
        return "测试返回数据1";
    }

}
