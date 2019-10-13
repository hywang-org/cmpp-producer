package com.i.server.handler;

import com.alibaba.fastjson.JSONObject;
import com.i.server.consts.Consts;
import com.i.server.consts.RedisConsts;
import com.i.server.data.mysql.entity.ProOrder;
import com.i.server.data.mysql.service.dao.SmsDao;
import com.i.server.rabbitmq.service.RabbitmqService;
import com.i.server.util.DateUtil;
import com.zx.sms.codec.cmpp.msg.*;
import com.zx.sms.common.util.CachedMillisecondClock;
import com.zx.sms.common.util.DefaultSequenceNumberUtil;
import com.zx.sms.connect.manager.EndpointManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CMPPMessageReceiveHandlerAsServer extends MessageReceiveHandler {

	private final EndpointManager manager = EndpointManager.INS;

	private RabbitmqService rabbitmqService;

	private SmsDao smsDao;

	String appId;

	String channelId;

	public CMPPMessageReceiveHandlerAsServer(RabbitmqService rabbitmqService, SmsDao smsDao, String appId, String channelId) {
		this.rabbitmqService = rabbitmqService;
		this.smsDao = smsDao;
		this.appId = appId;
		this.channelId = channelId;
	}

	@Override
	protected ChannelFuture reponse(final ChannelHandlerContext ctx, Object msg) {
		System.out.println("CMPPMessageReceiveHandlerAsServer channeId = " + ctx.channel().id().asLongText());
		int result = RandomUtils.nextInt(0, 100) > 97 ? 0 : 0;
		if (msg instanceof CmppDeliverRequestMessage) {
			System.out.println("ly 接收到 CmppDeliverRequestMessage 消息");
			CmppDeliverRequestMessage e = (CmppDeliverRequestMessage) msg;

			if (e.getFragments() != null) {
				// 长短信会带有片断
				for (CmppDeliverRequestMessage frag : e.getFragments()) {
					CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(
							frag.getHeader().getSequenceId());
					responseMessage.setResult(result);
					responseMessage.setMsgId(frag.getMsgId());
					ctx.channel().write(responseMessage);
				}
			}

			CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(e.getHeader().getSequenceId());
			responseMessage.setResult(result);
			responseMessage.setMsgId(e.getMsgId());
			return ctx.channel().writeAndFlush(responseMessage);

		} else if (msg instanceof CmppSubmitRequestMessage) {
			ChannelFuture future = null;
			// 接收到 CmppSubmitRequestMessage 消息
			System.out.println("ly 接收到 CmppSubmitRequestMessage 消息," + ((CmppSubmitRequestMessage) msg));
			System.out.println(
					"ly 接收到 CmppSubmitRequestMessage contetnt 消息," + ((CmppSubmitRequestMessage) msg).getMsgContent()
							+ ", length = " + ((CmppSubmitRequestMessage) msg).getMsgContent().length());
			System.out.println("ly 接收到 CmppSubmitRequestMessage 消息," + ((CmppSubmitRequestMessage) msg).getMsgsrc());
			System.out.println("ly 接收到 CmppSubmitRequestMessage pk消息," + ((CmppSubmitRequestMessage) msg).getPktotal()
					+ ", " + ((CmppSubmitRequestMessage) msg).getPknumber());
			CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;

			final List<CmppDeliverRequestMessage> reportlist = new ArrayList<CmppDeliverRequestMessage>();

			final CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage(e.getHeader().getSequenceId());
			resp.setResult(result);
			System.out.println("resp.getMsgId() = " + resp.getMsgId());

			String ownMsgId = resp.getMsgId().toString();
			long ownSequenceId = DefaultSequenceNumberUtil.getSequenceNo();
			long clientSequenceId = e.getHeader().getSequenceId();
			System.out.println("ownSequenceId = " + ownSequenceId + ", ownMsgId = " + ownMsgId
					+ ", clientSequenceId = " + clientSequenceId + ", channelId = " + channelId);
			JSONObject object = new JSONObject();
			object.put("ownMsgId", ownMsgId);
			object.put("clientSequenceId", clientSequenceId);
			object.put("appId", appId);
			object.put("channelId", channelId);
			object.put("needDeliver", e.getRegisteredDelivery());
			object.put("serverId", manager.getServerId());
			manager.getRedisOperationSetsMap().get(RedisConsts.REDIS_PRODUCER).set(String.valueOf(ownSequenceId),
					object.toJSONString());

			future = ctx.channel().writeAndFlush(resp);

			try {
				rabbitmqService.publishMq(appId, channelId, ownSequenceId, Consts.CMPP_SUBMIT_REQUEST_MESSAGE, e);
			} catch (IOException | TimeoutException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ProOrder pro = new ProOrder();
			pro.setAppId(appId);
			pro.setClientSeqId(clientSequenceId + "");
			pro.setDesId(e.getDestterminalId()[0]);
			pro.setChannelId(channelId);
			pro.setOwnSeqId(ownSequenceId + "");
			pro.setOwnMsgId(ownMsgId);
			pro.setProtocol("cmpp");
			pro.setShareDate(DateUtil.LocalDateToUdate());
			smsDao.save(pro);

			// 回复状态报告
			if (e.getRegisteredDelivery() == 1) {

				final CmppDeliverRequestMessage deliver = new CmppDeliverRequestMessage();
				deliver.setDestId(e.getSrcId());
				deliver.setSrcterminalId(e.getDestterminalId()[0]);
				CmppReportRequestMessage report = new CmppReportRequestMessage();
				report.setDestterminalId(deliver.getSrcterminalId());
				report.setMsgId(resp.getMsgId());
				String t = DateFormatUtils.format(CachedMillisecondClock.INS.now(), "yyMMddHHmm");
				report.setSubmitTime(t);
				report.setDoneTime(t);
				report.setStat("DELIVRD");
				report.setSmscSequence(0);
				deliver.setReportRequestMessage(report);
				reportlist.add(deliver);

				ctx.executor().submit(new Runnable() {
					public void run() {
						for (CmppDeliverRequestMessage t : reportlist)
							ctx.channel().writeAndFlush(t);
					}
				});
			}
//			}

			// try {
			// rabbitmqService.publishMq(appId, channelId, ownSequenceId,
			// Consts.CMPP_SUBMIT_REQUEST_MESSAGE, msg);
			// } catch (IOException | TimeoutException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// }

			// EchoServer.sendSms("16655169698", "【信内】我是短信内容2", "109002");
			// EchoServer.sendSms2((CmppSubmitRequestMessage) msg);

			// if (e.getFragments() != null) {
			// // 长短信会可能带有片断，每个片断都要回复一个response
			// for (CmppSubmitRequestMessage frag : e.getFragments()) {
			// CmppSubmitResponseMessage responseMessage = new
			// CmppSubmitResponseMessage(
			// frag.getHeader().getSequenceId());
			// responseMessage.setResult(result);
			// ctx.channel().write(responseMessage);
			//
			// CmppDeliverRequestMessage deliver = new
			// CmppDeliverRequestMessage();
			// deliver.setDestId(e.getSrcId());
			// deliver.setSrcterminalId(e.getDestterminalId()[0]);
			// CmppReportRequestMessage report = new CmppReportRequestMessage();
			// report.setDestterminalId(deliver.getSrcterminalId());
			// report.setMsgId(responseMessage.getMsgId());
			// String t =
			// DateFormatUtils.format(CachedMillisecondClock.INS.now(),
			// "yyMMddHHmm");
			// report.setSubmitTime(t);
			// report.setDoneTime(t);
			// report.setStat("DELIVRD");
			// report.setSmscSequence(0);
			// deliver.setReportRequestMessage(report);
			// reportlist.add(deliver);
			// }
			// }
			return result == 0 ? future : null;
		} else if (msg instanceof CmppQueryRequestMessage) {
			CmppQueryRequestMessage e = (CmppQueryRequestMessage) msg;
			CmppQueryResponseMessage res = new CmppQueryResponseMessage(e.getHeader().getSequenceId());
			return ctx.channel().writeAndFlush(res);
		}
		return null;
	}
}
