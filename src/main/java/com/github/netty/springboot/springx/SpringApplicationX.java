package com.github.netty.springboot.springx;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ResourceLoader;

/**
 * Created by acer01 on 2018/8/29/029.
 */
public class SpringApplicationX extends SpringApplication {

    public SpringApplicationX(Object... sources) {
        super(sources);
    }

    public SpringApplicationX(ResourceLoader resourceLoader, Object... sources) {
        super(resourceLoader, sources);
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        return new AnnotationConfigEmbeddedWebApplicationContextX();
    }

    public static ConfigurableApplicationContext run(Object source, String... args) {
        return run(new Object[] { source }, args);
    }

    public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
        return new SpringApplicationX(sources).run(args);
    }
}
