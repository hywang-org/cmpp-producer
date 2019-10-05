package com.zx.sms.codec.cmpp.wap;


import com.i.server.data.redis.service.RedisService;
import com.i.server.tabooword.core.TabooWordChecker;
import com.zx.sms.BaseMessage;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.EndpointEntity.SupportLongMessage;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.marre.sms.SmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLongMessageExtractContentHandler<T extends BaseMessage>
		extends MessageToMessageCodec<T, T> {
	private final Logger logger = LoggerFactory.getLogger(AbstractLongMessageExtractContentHandler.class);

	private EndpointEntity entity;

	public AbstractLongMessageExtractContentHandler(EndpointEntity entity) {
		this.entity = entity;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, T msg, List<Object> out) throws Exception {
		if ((entity == null || entity.getSupportLongmsg() == SupportLongMessage.BOTH
				|| entity.getSupportLongmsg() == SupportLongMessage.RECV) && msg instanceof LongSMSMessage
				&& needHandleLongMessage(msg)) {
			System.out.println("deal long handler");
			String key = generateFrameKey(msg);
			try {
				SmsMessageHolder hoder = LongMessageFrameHolder.INS.putAndget(key, ((LongSMSMessage) msg));

				List<LongSMSMessage> longMsgList = (List<LongSMSMessage>) LongMessageFrameHolder.INS.msgHashMap
						.get(key);
				if (longMsgList != null) {
					longMsgList.add((LongSMSMessage) msg);
					LongMessageFrameHolder.INS.msgHashMap.putIfAbsent(key, longMsgList);
				} else {
					longMsgList = new ArrayList<LongSMSMessage>();
					longMsgList.add((LongSMSMessage) msg);
					LongMessageFrameHolder.INS.msgHashMap.putIfAbsent(key, longMsgList);
				}
				if (hoder != null) {

					// resetMessageContent((T) hoder.msg, hoder.smsMessage);

					// 长短信合并完成，返回的这个msg里已经包含了所有的短信短断。后边的handler响应response时要包含这些片断。
					// out.add(hoder.msg);
					List<LongSMSMessage> longMsgList2 = (List<LongSMSMessage>) LongMessageFrameHolder.INS.msgHashMap
							.get(key);
					System.out.println("longMsgList size = " + longMsgList.size());
					if(!TabooWordChecker.containTabooWord(String.valueOf(hoder.smsMessage))) {
						long msgListSize = longMsgList2.size();
						System.out.println("longMsgList2 size = " + msgListSize);
						logger.info("deduct appId = {}, deductNo = {}", entity.getId(),msgListSize);
						boolean result = RedisService.deduct(entity.getId(), String.valueOf(msgListSize));
						if(!result) {
							logger.info("insufficient number of message to deduct by appId = {}", entity.getId());
						}
						for (LongSMSMessage s : longMsgList2) {
							out.add(s);
						}
					} else {
						logger.info("Not able to send, since appId {} sent taboo words",entity.getId());
					}
				}
			} catch (Exception ex) {
				logger.error("", ex);
				// 长短信解析失败，直接给网关回复 resp . 并丢弃这个短信
				logger.error("Decode Message Error ,msg dump :{}",
						ByteBufUtil.hexDump(((LongSMSMessage) msg).generateFrame().getMsgContentBytes()));
				BaseMessage res = response(msg);
				res.setRequest(msg);
				ctx.writeAndFlush(res);
			}
		} else {
			out.add(msg);
		}
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, T requestMessage, List<Object> out) throws Exception {
		if ((entity == null || entity.getSupportLongmsg() == SupportLongMessage.BOTH
				|| entity.getSupportLongmsg() == SupportLongMessage.SEND) && requestMessage instanceof LongSMSMessage
				&& needHandleLongMessage(requestMessage)) {
			SmsMessage msgcontent = ((LongSMSMessage) requestMessage).getSmsMessage();
			List<LongMessageFrame> frameList = LongMessageFrameHolder.INS.splitmsgcontent(msgcontent);
			boolean first = true;
			LongSMSMessage lmsg = (LongSMSMessage) requestMessage;
			for (LongMessageFrame frame : frameList) {
				T t = (T) lmsg.generateMessage(frame);
				out.add(t);
			}
		} else {
			out.add(requestMessage);
		}
	}

	protected abstract BaseMessage response(T msg);

	protected abstract boolean needHandleLongMessage(T msg);

	protected abstract String generateFrameKey(T msg) throws Exception;

	protected abstract void resetMessageContent(T t, SmsMessage content);

}
