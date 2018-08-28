package com.github.netty;

import com.github.netty.core.support.Optimize;
import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.springboot.NettyEmbeddedServletContainerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * jvisualvm  jconsole
 *
 * @author 84215
 */
@RestController
@SpringBootApplication
public class TestApplication {

    @Bean
    NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
        return new NettyEmbeddedServletContainerFactory();
    }

    @RequestMapping("/hello")
    public Object hello(@RequestParam Map o, HttpSession session, HttpServletRequest request, HttpServletResponse response){
//        try {
//            Thread.sleep(10);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        List list = new LinkedList<>();
//        for(int i=0; i<100; i++){
//            Map map = new HashMap<>();
//            StringBuilder sb = new StringBuilder();
//            for(int j=0; j<500;j++){
//                sb.append(j);
//            }
//            map.put("fd1",sb);
//            list.add(map);
//        }
        return "测试返回数据1";
    }

    /**
     * Start
     * @param args vm参数
     * @throws IOException io异常
     */
    public static void main(String[] args) throws IOException {
        preStart();

        ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class, args);
    }

    /**
     * 设置参数
     */
    private static void preStart(){
        //        ResourceLeakDetector -> 关闭内存泄漏检测
        System.setProperty("io.netty.noResourceLeakDetection","true");
//        AbstractByteBuf -> 关闭bytebuf重复释放检查
        System.setProperty("io.netty.buffer.bytebuf.checkAccessible","false");

        //统计任务
        ThreadPoolX.getDefaultInstance().scheduleAtFixedRate(new Optimize.ReportRunning(),5,5, TimeUnit.SECONDS);
    }


}
