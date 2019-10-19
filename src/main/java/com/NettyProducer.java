package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.alibaba.fastjson.parser.ParserConfig;

@SpringBootApplication
public class NettyProducer {

	private static ConfigurableApplicationContext context;

	public static ConfigurableApplicationContext getContext() {
		return context;
	}

	public static void setContext(ConfigurableApplicationContext context) {
		NettyProducer.context = context;
	}

	public static void main(String[] args) throws Exception {
		ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
		ConfigurableApplicationContext context = SpringApplication.run(NettyProducer.class, args);
		setContext(context);

	}

}
