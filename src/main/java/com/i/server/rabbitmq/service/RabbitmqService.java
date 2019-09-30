package com.i.server.rabbitmq.service;

import static com.zx.sms.common.util.NettyByteBufUtil.toArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.marre.sms.SmsDcs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.i.server.consts.Consts;
import com.i.server.rabbitmq.consts.RabbitMqConsts;
import com.i.server.util.QueueUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestSelfDefinedMessage;
import com.zx.sms.codec.cmpp.packet.CmppSubmitRequest;
import com.zx.sms.codec.cmpp.wap.LongMessageFrameHolder;
import com.zx.sms.codec.cmpp20.packet.Cmpp20SubmitRequest;
import com.zx.sms.common.GlobalConstance;
import com.zx.sms.common.util.CMPPCommonUtil;
import com.zx.sms.common.util.DefaultMsgIdUtil;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EndpointManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Service
public class RabbitmqService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RabbitmqService.class);

	@Value("${mq_username}")
	private String mqUserName;

	@Value("${mq_password}")
	private String mqPassWord;

	@Value("${mq_host}")
	private String mqHost;

	@Autowired
	private QueueUtils queueUtils;

	private Connection connection = null;

	private final EndpointManager manager = EndpointManager.INS;

	public Connection getConnection() throws IOException, TimeoutException {
		if (connection == null || !connection.isOpen()) {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(mqUserName);
			factory.setPassword(mqPassWord);
			factory.setHost(mqHost);
			factory.setAutomaticRecoveryEnabled(true); // 设置网络异常重连
			factory.setNetworkRecoveryInterval(10000);// 设置每10s重试一次
			factory.setTopologyRecoveryEnabled(true);// 设置重新声明交换器，队列等信息。
			connection = factory.newConnection();
		}
		return connection;
	}

	public Channel getChannel() throws IOException, TimeoutException {
		return getConnection().createChannel();
	}

	public void publishMq(String appId, String channelId, long ownSequenceId, String cmppType, Object obj)
			throws IOException, TimeoutException {
		MqEntity mqEntity = null;
		switch (cmppType) {
		case Consts.CMPP_SUBMIT_REQUEST_MESSAGE:
			CmppSubmitRequestMessage cmppObjs = (CmppSubmitRequestMessage) obj;
			System.out.println("******************************************************start");
			System.out.println("cmppObjs msg lenth = " + cmppObjs.getMsgLength() + ", cmppObjs content = "
					+ cmppObjs.getMsgContent());
			System.out.println("cmppObjs content byte length = " + cmppObjs.getMsgContent().getBytes().length);
			System.out.println("cmppObjs.getHeader() head length = " + cmppObjs.getHeader().getHeadLength()
					+ ",selfDefinedMessage.getHeader() body length = " + cmppObjs.getHeader().getBodyLength()
					+ ", selfDefinedMessage.getBodyBuffer() length = " + cmppObjs.getBodyBuffer().length);

			CmppSubmitRequestSelfDefinedMessage cmppSubmitRequestSelfDefinedMessage = ((CmppSubmitRequestMessage) obj)
					.getCmppSubmitRequestSelfDefinedMessage();
			System.out.println("before selfDefinedMessage.getHeader() head length = "
					+ cmppSubmitRequestSelfDefinedMessage.getHeader().getHeadLength()
					+ ",selfDefinedMessage.getHeader() body length = "
					+ cmppSubmitRequestSelfDefinedMessage.getHeader().getBodyLength()
					+ ", selfDefinedMessage.getBodyBuffer() length = "
					+ cmppSubmitRequestSelfDefinedMessage.getBodyBuffer().length);
			// replace to our own sequence id
			// cmppSubmitRequestSelfDefinedMessage.setHeader(cmppObjs.getHeader());
			// cmppSubmitRequestSelfDefinedMessage.setBodyBuffer(generateBodyBuffer(cmppObjs));

			System.out.println("after selfDefinedMessage.getHeader() head length = "
					+ cmppSubmitRequestSelfDefinedMessage.getHeader().getHeadLength()
					+ ",selfDefinedMessage.getHeader() body length = "
					+ cmppSubmitRequestSelfDefinedMessage.getHeader().getBodyLength()
					+ ", selfDefinedMessage.getBodyBuffer() length = "
					+ cmppSubmitRequestSelfDefinedMessage.getBodyBuffer().length);

			cmppSubmitRequestSelfDefinedMessage.getHeader().setSequenceId(ownSequenceId);

			CmppSubmitRequestMessage cForm = formCmppMessage20(cmppSubmitRequestSelfDefinedMessage,
					((CmppSubmitRequestMessage) obj).getMsgid());
			System.out.println("cForm.getHeader() head length = " + cForm.getHeader().getHeadLength()
					+ ",selfDefinedMessage.getHeader() body length = " + cForm.getHeader().getBodyLength());
			// + ", selfDefinedMessage.getBodyBuffer() length = " +
			// cForm.getBodyBuffer().length);
			System.out.println("cForm content = " + cForm.getMsgContent());

			byte[] b = generateBodyBuffer(cmppObjs);
			System.out.println("generateBodyBuffer length = " + b.length);
			System.out.println("******************************************************end");

			mqEntity = new MqEntity();
			mqEntity.setAppId(appId);
			mqEntity.setChannelId(channelId);
			mqEntity.setOwnSequenceId(ownSequenceId);
			mqEntity.setMsgId(((CmppSubmitRequestMessage) obj).getMsgid());
			// mqEntity.setcObject((CmppSubmitRequestMessage) obj);
			// mqEntity.setCmppSubmitRequestSelfDefinedMessage(cmppSubmitRequestSelfDefinedMessage);
			mqEntity.setObj(cmppSubmitRequestSelfDefinedMessage);
			mqEntity.setCmppMsgType(cmppType);
			mqEntity.setCmppVersion(((CmppSubmitRequestMessage) obj).getCmppVersion());
			mqEntity.setMsgContent(cmppObjs.getMsgContent());
			break;
		default:
			LOGGER.error("wrong cmpp type");
			break;
		}

		if (mqEntity != null) {
			Channel channel = getConnection().createChannel();
			channel.confirmSelect();
			// 声明create_queue和create_consumer
			channel.basicQos(1);
			String queueName = RabbitMqConsts.NETTY_APPID_QUEUE_NAME_PREFIX + appId;
			String exchangeName = RabbitMqConsts.NETTY_APPID_EXCHANGE_NAME_PREFIX + appId;
			if (!isQueueExist(queueName)) {
				channel.exchangeDeclare(exchangeName, "direct", true);
				channel.queueDeclare(queueName, true, false, false, null);
				// 对队列进行绑定
				channel.queueBind(queueName, exchangeName, "consume");
				LOGGER.info("普通用户userId={}的转写任务队列{}创建成功", appId, queueName);
				// 发布到create_queue创建对应的consumer
				channel.basicPublish(RabbitMqConsts.NETTY_CREATE_QUEUE_EXCHANGE_NAME, "create",
						MessageProperties.PERSISTENT_TEXT_PLAIN, queueName.getBytes());
			}
			channel.basicPublish(exchangeName, "consume", MessageProperties.PERSISTENT_TEXT_PLAIN,
					JSON.toJSONBytes(mqEntity, SerializerFeature.WriteClassName));
			LOGGER.info("普通用户appId={}的数据msgId={}发送到消息队列，消息体为{}", appId, mqEntity.getMsgId(),
					JSON.toJSONString(mqEntity));

			channel.close();
		} else {
			LOGGER.info("Not pulish to mq due to empty MqEntity object");
		}
	}

	private byte[] generateBodyBuffer(CmppSubmitRequestMessage requestMessage) {
		assert (requestMessage.getDestUsrtl() > 0);
		ByteBuf bodyBuffer = Unpooled.buffer(CmppSubmitRequest.ATTIME.getBodyLength() + requestMessage.getMsgLength()
				+ (requestMessage.getDestUsrtl() - 1) * CmppSubmitRequest.DESTTERMINALID.getLength());

		bodyBuffer.writeBytes(DefaultMsgIdUtil.msgId2Bytes(requestMessage.getMsgid()));

		bodyBuffer.writeByte(requestMessage.getPktotal());
		bodyBuffer.writeByte(requestMessage.getPknumber());
		bodyBuffer.writeByte(requestMessage.getRegisteredDelivery());
		bodyBuffer.writeByte(requestMessage.getMsglevel());

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getServiceId().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.SERVICEID.getLength(), 0));

		bodyBuffer.writeByte(requestMessage.getFeeUserType());

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getFeeterminalId().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.FEETERMINALID.getLength(), 0));
		bodyBuffer.writeByte(requestMessage.getFeeterminaltype());
		bodyBuffer.writeByte(requestMessage.getTppid());
		bodyBuffer.writeByte(requestMessage.getTpudhi());
		bodyBuffer.writeByte(requestMessage.getMsgfmt().getValue());

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getMsgsrc().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.MSGSRC.getLength(), 0));

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getFeeType().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.FEETYPE.getLength(), 0));

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getFeeCode().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.FEECODE.getLength(), 0));

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getValIdTime().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.VALIDTIME.getLength(), 0));

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getAtTime().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.ATTIME.getLength(), 0));

		bodyBuffer.writeBytes(
				CMPPCommonUtil.ensureLength(requestMessage.getSrcId().getBytes(GlobalConstance.defaultTransportCharset),
						CmppSubmitRequest.SRCID.getLength(), 0));

		bodyBuffer.writeByte(requestMessage.getDestUsrtl());
		for (int i = 0; i < requestMessage.getDestUsrtl(); i++) {
			String[] destTermId = requestMessage.getDestterminalId();
			bodyBuffer.writeBytes(
					CMPPCommonUtil.ensureLength(destTermId[i].getBytes(GlobalConstance.defaultTransportCharset),
							CmppSubmitRequest.DESTTERMINALID.getLength(), 0));
		}
		bodyBuffer.writeByte(requestMessage.getDestterminaltype());

		bodyBuffer.writeByte(requestMessage.getMsgLength());

		bodyBuffer.writeBytes(requestMessage.getMsgContentBytes());

		bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(
				requestMessage.getLinkID().getBytes(GlobalConstance.defaultTransportCharset),
				CmppSubmitRequest.LINKID.getLength(), 0));

		// bodyBuffer.writeBytes(CMPPCommonUtil.ensureLength(requestMessage.getReserve().getBytes(GlobalConstance.defaultTransportCharset),
		// CmppSubmitRequest.RESERVE.getLength(), 0));/**cmpp3.0 无该字段，不进行编解码
		// */
		return toArray(bodyBuffer, bodyBuffer.readableBytes());
	}

	private CmppSubmitRequestMessage formCmppMessage20(CmppSubmitRequestSelfDefinedMessage selfDefinedMessage,
			MsgId msgId) {
		CmppSubmitRequestMessage requestMessage = new CmppSubmitRequestMessage(selfDefinedMessage.getHeader());

		System.out.println("selfDefinedMessage.getHeader() head length = "
				+ selfDefinedMessage.getHeader().getHeadLength() + ",selfDefinedMessage.getHeader() body length = "
				+ selfDefinedMessage.getHeader().getBodyLength() + ", selfDefinedMessage.getBodyBuffer() length = "
				+ selfDefinedMessage.getBodyBuffer().length);

		ByteBuf bodyBuffer = Unpooled.wrappedBuffer(selfDefinedMessage.getBodyBuffer());

		// ly add
		// requestMessage
		// .setMsgid(DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer,
		// Cmpp20SubmitRequest.MSGID.getLength())));
		DefaultMsgIdUtil.bytes2MsgId(toArray(bodyBuffer, Cmpp20SubmitRequest.MSGID.getLength()));
		requestMessage.setMsgid(msgId);

		requestMessage.setPktotal(bodyBuffer.readUnsignedByte());
		requestMessage.setPknumber(bodyBuffer.readUnsignedByte());
		System.out.println("requestMessage getPktotal = " + requestMessage.getPktotal() + ", getPknumber = "
				+ requestMessage.getPknumber());

		// requestMessage.setRegisteredDelivery(bodyBuffer.readUnsignedByte());
		bodyBuffer.readUnsignedByte();
		requestMessage.setRegisteredDelivery((short) 15);
		System.out.println("requestMessage get = " + requestMessage.getRegisteredDelivery());
		requestMessage.setMsglevel(bodyBuffer.readUnsignedByte());
		requestMessage.setServiceId(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.SERVICEID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		requestMessage.setFeeUserType(bodyBuffer.readUnsignedByte());

		requestMessage.setFeeterminalId(bodyBuffer.readCharSequence(Cmpp20SubmitRequest.FEETERMINALID.getLength(),
				GlobalConstance.defaultTransportCharset).toString().trim());
		// requestMessage.setFeeterminaltype(bodyBuffer.readUnsignedByte());//CMPP2.0
		// 无该字段 不进行编解码

		requestMessage.setTppid(bodyBuffer.readUnsignedByte());
		requestMessage.setTpudhi(bodyBuffer.readUnsignedByte());
		requestMessage.setMsgfmt(new SmsDcs((byte) bodyBuffer.readUnsignedByte()));

		requestMessage.setMsgsrc(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.MSGSRC.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setFeeType(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.FEETYPE.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setFeeCode(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.FEECODE.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setValIdTime(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.VALIDTIME.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setAtTime(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.ATTIME.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());

		requestMessage.setSrcId(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.SRCID.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		short destUsrtl = bodyBuffer.readUnsignedByte();
		String[] destTermId = new String[destUsrtl];
		for (int i = 0; i < destUsrtl; i++) {
			destTermId[i] = bodyBuffer.readCharSequence(Cmpp20SubmitRequest.DESTTERMINALID.getLength(),
					GlobalConstance.defaultTransportCharset).toString().trim();
		}
		requestMessage.setDestterminalId(destTermId);

		// requestMessage.setDestterminaltype(bodyBuffer.readUnsignedByte());//CMPP2.0
		// 无该字段 不进行编解码

		short msgLength = (short) ((LongMessageFrameHolder.getPayloadLength(requestMessage.getMsgfmt().getAlphabet(),
				bodyBuffer.readUnsignedByte()) & 0xffff));

		byte[] contentbytes = new byte[msgLength];
		bodyBuffer.readBytes(contentbytes);
		requestMessage.setMsgContentBytes(contentbytes);
		requestMessage.setMsgLength((short) msgLength);
		// // ly add
		// requestMessage.setMsgContent(requestMessage.getMsgContent());
		requestMessage.setReserve(bodyBuffer
				.readCharSequence(Cmpp20SubmitRequest.RESERVE.getLength(), GlobalConstance.defaultTransportCharset)
				.toString().trim());
		requestMessage.setBodyBuffer(selfDefinedMessage.getBodyBuffer());
		return requestMessage;
	}

	/**
	 * 发布转写任务到消息队列
	 * 
	 * @param audioId
	 * @param originalFile
	 * @param userId
	 */
	public void publishMq2(String appId, String msgId, Object obj) throws IOException, TimeoutException {
		// Channel channel = getConnection().createChannel();
		// channel.confirmSelect();
		// // 声明create_queue和create_consumer
		// channel.basicQos(1);
		// String queueName = RabbitMqConsts.NETTY_APPID_QUEUE_NAME_PREFIX +
		// appId;
		// String exchangeName = RabbitMqConsts.NETTY_APPID_EXCHANGE_NAME_PREFIX
		// + appId;
		// if (!isQueueExist(queueName)) {
		// channel.exchangeDeclare(exchangeName, "direct", true);
		// channel.queueDeclare(queueName, true, false, false, null);
		// // 对队列进行绑定
		// channel.queueBind(queueName, exchangeName, "consume");
		// LOGGER.info("普通用户userId={}的转写任务队列{}创建成功", appId, queueName);
		// // 发布到create_queue创建对应的consumer
		// channel.basicPublish(RabbitMqConsts.NETTY_CREATE_QUEUE_NAME,
		// "create",
		// MessageProperties.PERSISTENT_TEXT_PLAIN, queueName.getBytes());
		// }
		//
		// MqEntity mqEntity = new MqEntity();
		// mqEntity.setAppId(appId);
		// mqEntity.setMsgId(msgId);
		// System.out.println("CmppSubmitRequestMessage obj = " +
		// JSON.toJSONString((CmppSubmitRequestMessage) obj));
		// // mqEntity.setS(JSON.toJSONString((CmppSubmitRequestMessage) obj));
		// // mqEntity.setS2(JSON.toJSONString((CmppSubmitRequestMessage) obj));
		// // CmppSubmitRequestMessage obj3 =
		// JSON.parseObject(JSON.toJSONString((CmppSubmitRequestMessage) obj),
		// CmppSubmitRequestMessage.class);
		// System.out.println("yeah!!!");
		//
		// String test = new String(JSON.toJSONBytes(mqEntity), "UTF-8");
		// // System.out.println("test = " + test);
		// // System.out.println("test trim = " + test.replace("\\",
		// // "").replace("\"{", "{").replace("}\"", "}"));
		// // MqEntity mqEntity2 = JSON.parseObject(test.replace("\\",
		// // "").replace("\"{", "{").replace("}\"", "}"),
		// // MqEntity.class);
		// // Object obj2 = JSON.parse(mqEntity2.getS());
		// //
		// // System.out.println("mqEntity2.getS() = " + mqEntity2.getS());
		// // CmppSubmitRequestMessage cmppObj2 = (CmppSubmitRequestMessage)
		// obj2;
		// // CmppSubmitRequestMessage cmppObj = JSON.parseObject(
		// // mqEntity2.getS().replace("\\", "").replace("\"{",
		// "{").replace("}\"",
		// // "}"),
		// // CmppSubmitRequestMessage.class);
		//
		// // JSON json = new JSONObject();
		// // ((JSONObject) json).put("cObj", mqEntity2.getS());
		// // System.out.println("json.toJSONString() = " +
		// json.toJSONString());
		// // MqEntity mqEntity3 = JSON.parseObject(json.toJSONString(),
		// // MqEntity.class);
		//
		// // CmppSubmitRequestMessage cmppObj = (CmppSubmitRequestMessage)
		// // JSON.parse(mqEntity.getS());
		// // mqEntity.setcObj((CmppSubmitRequestMessage) obj);
		// // mqEntity.setObj(JSON.toJSON(obj));
		// // JSONObject task = new JSONObject();
		// // task.put("appId", appId);
		// // task.put("msgId", msgId);
		// // task.put("obj", (CmppSubmitRequestMessage) obj);
		// // 当前转写任务对应的Queue已经存在，可以把转写任务publish到队列中
		// // channel.basicPublish(exchangeName, "consume",
		// // MessageProperties.PERSISTENT_TEXT_PLAIN,
		// // JSON.toJSONString(mqEntity).getBytes());
		// channel.basicPublish(exchangeName, "consume",
		// MessageProperties.PERSISTENT_BASIC, JSON.toJSONBytes(mqEntity));
		// // channel.basicPublish(exchangeName, "consume",
		// // MessageProperties.PERSISTENT_BASIC,
		// // ProtoBufUtil.serializer(mqEntity));
		// // LOGGER.info("普通用户appId={}的数据msgId={}发送到消息队列，消息体为{}", appId, msgId,
		// // task.toJSONString());
		// LOGGER.info("普通用户appId={}的数据msgId={}发送到消息队列，消息体为{}", appId, msgId);
		//
		// channel.close();
	}

	public static byte[] toBytes(Object object) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} finally {
			try {
				oos.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 判断当前MQ中是否存在userId或者appId对应的queue 若存在则返回true，不存在返回false，并把queueName加到集合中
	 * 
	 * @param queueName
	 * @return
	 */
	private boolean isQueueExist(String queueName) {
		Set<String> queueSet = queueUtils.getQueueNameSet();
		if (!queueSet.contains(queueName)) {
			queueSet.add(queueName);
			return false;
		}
		return true;
	}

}
