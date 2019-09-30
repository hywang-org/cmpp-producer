package com.i.server.rabbitmq.consumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.i.server.rabbitmq.consts.RabbitMqConsts;
import com.i.server.rabbitmq.service.RabbitmqService;
import com.i.server.util.QueueUtils;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@Component
public class DynamicCreateBackConsumer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCreateBackConsumer.class);

	@Autowired
	private RabbitmqService rabbitmqService;

	// @Autowired
	// private RedisService redisService;
	//
	// @Autowired
	// private APIService apiService;
	//
	// @Autowired
	// private YPDao ypDao;

	@Autowired
	private QueueUtils queueUtils;

	@Value("${mq_waitTime}")
	private long waitTime;

	@Value("${numberOfConsumer}")
	private int numberOfConsumer;

	public void createConsumer() throws IOException, TimeoutException {
		// String queueName = RabbitMqConsts.NETTY_APPID_BACK_QUEUE_NAME_PREFIX
		// + 1;
		Channel channel = rabbitmqService.getChannel();
		channel.confirmSelect();
		channel.basicQos(1);
		channel.exchangeDeclare(RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_EXCHANGE_NAME, "direct", true);
		channel.queueDeclare(RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_NAME, true, false, false, null);
		// 对队列进行绑定
		channel.queueBind(RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_NAME,
				RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_EXCHANGE_NAME, "create");
		LOGGER.info("容器启动时成功创建消息队列{}", RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_NAME);
		Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
					byte[] body) throws IOException {
				String queueName = new String(body, "UTF-8");
				LOGGER.info("消息队列{}成功接收到需要创建back Consumer的队列queueName={}", RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_NAME,
						queueName);

				// 创建queueName队列对应的Consumer
				if (queueName.startsWith(RabbitMqConsts.NETTY_APPID_BACK_QUEUE_NAME_PREFIX)) {
					// 创建App用户的consumer
					for (int i = 0; i < numberOfConsumer; i++) {
						Channel channel = null;
						try {
							channel = rabbitmqService.getChannel();
						} catch (TimeoutException e) {
							e.printStackTrace();
						}
						channel.confirmSelect();
						channel.basicQos(1);
						Consumer consumer = new BackMsgConsumer(channel);
						channel.basicConsume(queueName, false, consumer);
						LOGGER.info("系统运行时，动态生成APP用户队列{}对应的back Consumer", queueName);
					}
				}
				channel.basicAck(envelope.getDeliveryTag(), false);
			}
		};
		channel.basicConsume(RabbitMqConsts.NETTY_CREATE_BACK_QUEUE_NAME, false, consumer);
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		try {
			createConsumer();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
}
