# netty-container
一个基于netty实现的servlet容器, 可以替代tomcat或jetty. 导包即用,容易与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub

---

####优势:

公司台式[8g内存,4核I5cpu]

1.单体应用,连接复用qps=10000+ , tomcat=4000+

2.单体应用,连接不复用qps达到5000+, tomcat=3700+

3.单体应用,双jvm(1.servlet jvm, 2.session jvm), session会话存储分离, qps达到1300+, 
 
 tomcat底层虽然支持,但非常复杂,大家往往都用springboot-redis, 但redis与spring集成后, 无法发挥其原本的性能

----

### 使用方法

#### 1.添加依赖, 在pom.xml中加入

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>netty-container</artifactId>
      <version>1.2.0</version>
    </dependency>
	
	
#### 2.注册进springboot容器中

    @Configuration
    public class WebAppConfig extends WebMvcConfigurationSupport {
    
        /**
         * 注册netty容器
         * @return
         */
        @Bean
        public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
            NettyEmbeddedServletContainerFactory factory = new NettyEmbeddedServletContainerFactory();
            return factory;
        }
    
        /**
         * 如果(您当前的config类继承了WebMvcConfigurationSupport){
         *      必须要写这个servletContextInitializer方法
         * }否则{
         *     可以省略这个servletContextInitializer方法
         * }
         * @return
         */
        @Bean
        public ServletContextInitializer servletContextInitializer(){
            return this::setServletContext;
        }
     }

#### 3.完成! 快去启动服务看看吧

