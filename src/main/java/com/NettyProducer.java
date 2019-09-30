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
// public class Application implements CommandLineRunner {
// @Resource
// ValidateClientRedis r1;
//
// public static void main(String[] args) {
// SpringApplication.run(Application.class, args);
// }
//
// @Override
// public void run(String... strings) throws Exception {
// // EchoServer echoServer = new EchoServer(8080);
// // echoServer.openServer();
//
// // echoServer.connect();
// // Thread.sleep(10000);
// // echoServer.sendSms("16655169698", "【科技】我是短信内容", "109002");
// System.out.println("r1.get() = " + r1.get("1"));
// }
// }
