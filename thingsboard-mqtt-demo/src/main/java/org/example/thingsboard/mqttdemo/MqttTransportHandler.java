package org.example.thingsboard.mqttdemo;

import com.google.protobuf.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.ReferenceCountUtil;
import org.example.thingsboard.mqttdemo.adaptors.AdaptorException;
import org.example.thingsboard.mqttdemo.adaptors.MqttTransportAdaptor;
import org.example.thingsboard.mqttdemo.proto.TransportProtos;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 对应 ThingsBoard 的 MqttTransportHandler，保留原 handler 的消息入口和分发方向。
 *
 * 省略 server bootstrap、设备 profile cache、业务 ACK 回调和其他 MQTT
 * 功能分支；CONNECT/PUBLISH 仍通过 Netty MQTT message 进入同样的 session/adaptor/service 边界。
 */
public final class MqttTransportHandler extends ChannelInboundHandlerAdapter {

    private static final String PROVISION_USER = "provision";

    private final MqttTransportContext context;
    private final TransportService transportService;
    private final DeviceSessionCtx deviceSessionCtx;
    private final Queue<MqttMessage> messageQueue = new ArrayDeque<>();
    private final Map<String, MqttQoS> subscriptions = new ConcurrentHashMap<>();
    private final List<MqttMessage> outboundMessages = new CopyOnWriteArrayList<>();
    private final int messageQueueLimit = 100;
    private volatile boolean closed;

    public MqttTransportHandler(MqttTransportContext context, TransportPayloadType payloadType) {
        this(context, new DeviceSessionCtx(
                MqttTopics.DEVICE_TELEMETRY_TOPIC,
                MqttTopics.DEVICE_ATTRIBUTES_TOPIC,
                payloadType));
    }

    public MqttTransportHandler(MqttTransportContext context, DeviceSessionCtx deviceSessionCtx) {
        this.context = context;
        this.transportService = context.getTransportService();
        this.deviceSessionCtx = deviceSessionCtx;
    }

    @Override
    public void channelRead(ChannelHandlerContext channel, Object message) throws Exception {
        if (message instanceof MqttMessage mqttMessage) {
            processMqttMsg(channel, mqttMessage);
        } else {
            channel.fireChannelRead(message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext channel) throws Exception {
        doDisconnect();
        super.channelInactive(channel);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext channel) throws Exception {
        doDisconnect();
        super.channelUnregistered(channel);
    }

    /** 对应源码 processMqttMsg 的 CONNECT/PUBLISH 分派。 */
    public void processMqttMsg(MqttMessage message) throws AdaptorException {
        processMqttMsg(null, message);
    }

    public void processMqttMsg(ChannelHandlerContext channel, MqttMessage message) throws AdaptorException {
        if (closed || message.fixedHeader() == null) {
            return;
        }
        switch (message.fixedHeader().messageType()) {
            case CONNECT -> processConnect(channel, (MqttConnectMessage) message);
            default -> enqueueRegularSessionMsg(channel, message);
        }
    }

    /** 对应源码 processConnect 中 basic MQTT credentials 分支。 */
    public void processConnect(MqttConnectMessage connectMessage) {
        processConnect(null, connectMessage);
    }

    public void processConnect(ChannelHandlerContext channel, MqttConnectMessage connectMessage) {
        processConnect(channel, connectMessage, null);
    }

    /**
     * 对应源码 processConnect 的 X.509 分支。
     * certificateHash 是 SSL 层计算出的证书 SHA-3 摘要；真实 handler 从 SslHandler
     * 取得证书并计算摘要，这里通过参数注入，避免引入完整 TLS pipeline。
     */
    public void processConnect(ChannelHandlerContext channel,
                               MqttConnectMessage connectMessage,
                               String certificateHash) {
        String clientId = connectMessage.payload().clientIdentifier();
        String userName = connectMessage.payload().userName();
        byte[] passwordBytes = connectMessage.payload().passwordInBytes();

        if (PROVISION_USER.equals(userName) || PROVISION_USER.equals(clientId)) {
            deviceSessionCtx.setProvisionOnly(true);
            acceptConnection(channel);
            return;
        }

        if (certificateHash != null) {
            TransportProtos.ValidateDeviceX509CertRequestMsg request =
                    TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder()
                            .setHash(certificateHash)
                            .build();
            processCredentials(channel, request);
            return;
        }

        TransportProtos.ValidateBasicMqttCredRequestMsg.Builder request =
                TransportProtos.ValidateBasicMqttCredRequestMsg.newBuilder().setClientId(clientId);
        if (userName != null) {
            request.setUserName(userName);
        }
        if (passwordBytes != null) {
            request.setPassword(new String(passwordBytes, StandardCharsets.UTF_8));
        }

        processCredentials(channel, request.build());
    }

    private void processCredentials(ChannelHandlerContext channel, Message request) {
        transportService.process(DeviceTransportType.MQTT, request,
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse response) {
                        if (response.hasDeviceInfo()) {
                            acceptConnection(channel);
                        } else {
                            doDisconnect();
                            writeConnAck(channel, MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        doDisconnect();
                        writeConnAck(channel, MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                    }
                });
    }

    private void acceptConnection(ChannelHandlerContext channel) {
        closed = false;
        deviceSessionCtx.setConnected(true);
        writeConnAck(channel, MqttConnectReturnCode.CONNECTION_ACCEPTED);
        try {
            processMsgQueue(channel);
        } catch (AdaptorException error) {
            doDisconnect();
        }
    }

    /** 对应源码 processPublish：已认证后才进入设备 topic 分发。 */
    public void processPublish(MqttPublishMessage mqttMessage) throws AdaptorException {
        if (!deviceSessionCtx.isConnected()) {
            return;
        }
        processDevicePublish(mqttMessage, mqttMessage.variableHeader().topicName());
    }

    private void enqueueRegularSessionMsg(ChannelHandlerContext channel, MqttMessage message)
            throws AdaptorException {
        synchronized (messageQueue) {
            if (messageQueue.size() >= messageQueueLimit) {
                doDisconnect();
                return;
            }
            messageQueue.add(message);
        }
        processMsgQueue(channel);
    }

    private void processMsgQueue(ChannelHandlerContext channel) throws AdaptorException {
        if (!deviceSessionCtx.isConnected()) {
            return;
        }
        while (true) {
            MqttMessage message;
            synchronized (messageQueue) {
                message = messageQueue.poll();
            }
            if (message == null) {
                return;
            }
            try {
                processRegularSessionMsg(channel, message);
            } finally {
                ReferenceCountUtil.safeRelease(message);
            }
        }
    }

    private void processRegularSessionMsg(ChannelHandlerContext channel, MqttMessage message)
            throws AdaptorException {
        switch (message.fixedHeader().messageType()) {
            case PUBLISH -> processPublish((MqttPublishMessage) message);
            case SUBSCRIBE -> processSubscribe(channel, (MqttSubscribeMessage) message);
            case UNSUBSCRIBE -> processUnsubscribe(channel, (MqttUnsubscribeMessage) message);
            case PINGREQ -> writeOutbound(channel, new MqttMessage(new MqttFixedHeader(
                    MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0)));
            case DISCONNECT -> doDisconnect();
            case PUBACK -> processPubAck((MqttPubAckMessage) message);
            default -> {
                // CONNECT 已在 processMqttMsg 中处理，其余控制消息不属于本示例主路径。
            }
        }
    }

    private void processSubscribe(ChannelHandlerContext channel, MqttSubscribeMessage message) {
        List<Integer> resultCodes = new ArrayList<>();
        for (MqttTopicSubscription subscription : message.payload().topicSubscriptions()) {
            MqttQoS requestedQos = subscription.qualityOfService();
            if (!isSupportedSubscription(subscription.topicName()) || requestedQos == MqttQoS.FAILURE) {
                resultCodes.add(128);
            } else {
                MqttQoS grantedQos = requestedQos == MqttQoS.EXACTLY_ONCE
                        ? MqttQoS.AT_LEAST_ONCE : requestedQos;
                subscriptions.put(subscription.topicName(), grantedQos);
                resultCodes.add(grantedQos.value());
            }
        }
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttSubAckMessage response = new MqttSubAckMessage(
                fixedHeader,
                MqttMessageIdVariableHeader.from(message.variableHeader().messageId()),
                new MqttSubAckPayload(resultCodes.stream().mapToInt(Integer::intValue).toArray()));
        writeOutbound(channel, response);
    }

    private static boolean isSupportedSubscription(String topic) {
        return MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC.equals(topic)
                || MqttTopics.DEVICE_ATTRIBUTES_TOPIC.equals(topic)
                || topic.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
    }

    private void processUnsubscribe(ChannelHandlerContext channel, MqttUnsubscribeMessage message) {
        List<Short> resultCodes = new ArrayList<>();
        for (String topic : message.payload().topics()) {
            resultCodes.add(subscriptions.remove(topic) == null
                    ? (short) 17
                    : (short) 0);
        }
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttUnsubAckMessage response = new MqttUnsubAckMessage(
                fixedHeader, MqttMessageIdVariableHeader.from(message.variableHeader().messageId()));
        writeOutbound(channel, response);
    }

    private void processPubAck(MqttPubAckMessage message) {
        // 原实现这里确认 RPC downlink；示例没有 downlink awaiting-ack 状态。
    }

    private void writeOutbound(ChannelHandlerContext channel, MqttMessage message) {
        if (channel != null) {
            channel.writeAndFlush(message);
        } else {
            outboundMessages.add(message);
        }
    }

    public List<MqttMessage> outboundMessages() {
        return List.copyOf(outboundMessages);
    }

    public void doDisconnect() {
        deviceSessionCtx.setConnected(false);
        closed = true;
        synchronized (messageQueue) {
            MqttMessage message;
            while ((message = messageQueue.poll()) != null) {
                ReferenceCountUtil.safeRelease(message);
            }
        }
        subscriptions.clear();
    }

    /** 对应源码 processDevicePublish，按 topic 选择 adaptor 和 transport message。 */
    public void processDevicePublish(MqttPublishMessage mqttMessage, String topicName) throws AdaptorException {
        MqttTransportAdaptor payloadAdaptor = deviceSessionCtx.getPayloadAdaptor();
        Message message;
        if (deviceSessionCtx.isDeviceAttributesTopic(topicName)) {
            message = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMessage);
        } else if (deviceSessionCtx.isDeviceTelemetryTopic(topicName)) {
            message = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMessage);
        } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
            message = payloadAdaptor.convertToDeviceRpcResponse(
                    deviceSessionCtx, mqttMessage, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
        } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
            message = payloadAdaptor.convertToServerRpcRequest(
                    deviceSessionCtx, mqttMessage, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC);
        } else {
            return;
        }
        transportService.process(deviceSessionCtx, message, Map.of(
                "topic", topicName,
                "payloadType", deviceSessionCtx.getPayloadType().name()));
    }

    private void writeConnAck(ChannelHandlerContext channel, MqttConnectReturnCode returnCode) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode, false);
        MqttConnAckMessage connAck = new MqttConnAckMessage(fixedHeader, variableHeader);
        writeOutbound(channel, connAck);
    }

    public DeviceSessionCtx getDeviceSessionCtx() {
        return deviceSessionCtx;
    }

    public MqttPublishMessage createPublishMessage(String topic, String payload) {
        return deviceSessionCtx.getPayloadAdaptor().createMqttPublishMsg(deviceSessionCtx, topic, payload);
    }

    public MqttPublishMessage createPublishMessage(String topic, byte[] payload) {
        return deviceSessionCtx.getPayloadAdaptor().createMqttPublishMsg(deviceSessionCtx, topic, payload);
    }
}
