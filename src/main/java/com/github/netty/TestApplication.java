package com.github.netty;

import com.github.netty.core.support.Optimize;
import com.github.netty.core.support.ThreadPoolX;
import com.github.netty.springboot.NettyEmbeddedServletContainerFactory;
import com.github.netty.springboot.springx.SpringApplicationX;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * jvisualvm  jconsole
 *
 * 1. (2018年8月29日 23:35:06)
 *      很无奈, 现在只能调优spring了, 因为servlet不走DispatchServlet的流程, 连接复用qps能到15000+, 走spring只能到5500+ ,
 *      新建的com.github.netty.springboot.springx 这个包就是为了优化spring而用的
 *
 *
 * @author 84215
 */
@RestController
@SpringBootApplication
public class TestApplication extends WebMvcConfigurationSupport{

    @Bean
    NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
        return new NettyEmbeddedServletContainerFactory();
    }

    @RequestMapping("/hello")
    public Object hello(@RequestParam Map query, HttpSession session, HttpServletRequest request, HttpServletResponse response){
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

    public static final AtomicLong HANDLER_NUM = new AtomicLong();
    public static final AtomicLong HANDLER_TIME = new AtomicLong();

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptorAdapter(){

            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                request.setAttribute("HANDLER_TIME",System.nanoTime());
                return true;
            }

            @Override
            public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
                long nao = System.nanoTime() - (long)request.getAttribute("HANDLER_TIME");
                HANDLER_TIME.addAndGet(nao);
                HANDLER_NUM.incrementAndGet();
            }
        });
        super.addInterceptors(registry);
    }

    @Bean
    public ServletContextInitializer servletContextInitializer(){
        return this::setServletContext;
    }

    /**
     * Start
     * @param args vm参数
     * @throws IOException io异常
     */
    public static void main(String[] args) throws IOException {
        preStart();

        ConfigurableApplicationContext context = SpringApplicationX.run(TestApplication.class, args);
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
