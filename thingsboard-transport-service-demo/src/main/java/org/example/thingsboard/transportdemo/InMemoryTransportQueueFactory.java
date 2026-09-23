package org.example.thingsboard.transportdemo;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用三个内存队列替代 ThingsBoard 的 Kafka/InMemory queue provider。
 * 队列方向和原项目一致：Transport API、Rule Engine、Core。
 */
public final class InMemoryTransportQueueFactory {
    private final Queue<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> transportApiRequests = new ConcurrentLinkedQueue<>();
    private final Queue<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> ruleEngineMessages = new ConcurrentLinkedQueue<>();
    private final Queue<TbProtoQueueMsg<TransportProtos.ToCoreMsg>> coreMessages = new ConcurrentLinkedQueue<>();

    public void sendTransportApiRequest(TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> message) {
        transportApiRequests.add(message);
    }

    public TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> pollTransportApiRequest() {
        return transportApiRequests.poll();
    }

    public void sendRuleEngine(TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> message) {
        ruleEngineMessages.add(message);
    }

    public TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> pollRuleEngine() {
        return ruleEngineMessages.poll();
    }

    public void sendCore(TbProtoQueueMsg<TransportProtos.ToCoreMsg> message) {
        coreMessages.add(message);
    }

    public TbProtoQueueMsg<TransportProtos.ToCoreMsg> pollCore() {
        return coreMessages.poll();
    }
}
