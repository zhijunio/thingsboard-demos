package org.example.thingsboard.grpcdemo;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.example.thingsboard.grpcdemo.proto.ConnectRequestMsg;
import org.example.thingsboard.grpcdemo.proto.ConnectResponseCode;
import org.example.thingsboard.grpcdemo.proto.EdgeRpcServiceGrpc;
import org.example.thingsboard.grpcdemo.proto.EdgeVersion;
import org.example.thingsboard.grpcdemo.proto.EntityDataProto;
import org.example.thingsboard.grpcdemo.proto.KeyValueProto;
import org.example.thingsboard.grpcdemo.proto.KeyValueType;
import org.example.thingsboard.grpcdemo.proto.PostTelemetryMsg;
import org.example.thingsboard.grpcdemo.proto.RequestMsg;
import org.example.thingsboard.grpcdemo.proto.RequestMsgType;
import org.example.thingsboard.grpcdemo.proto.ResponseMsg;
import org.example.thingsboard.grpcdemo.proto.TsKvListProto;
import org.example.thingsboard.grpcdemo.proto.UplinkMsg;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class DemoClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final CountDownLatch connected = new CountDownLatch(1);
    private final CountDownLatch uplinkCompleted = new CountDownLatch(1);
    private StreamObserver<RequestMsg> requestStream;

    public DemoClient(String host, int port) {
        channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(10, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();
    }

    public void run() throws InterruptedException {
        StreamObserver<ResponseMsg> responseStream = new StreamObserver<>() {
            @Override
            public void onNext(ResponseMsg response) {
                if (response.hasConnectResponseMsg()) {
                    var connect = response.getConnectResponseMsg();
                    System.out.println("[client] connect response=" + connect.getResponseCode());
                    if (connect.getResponseCode() == ConnectResponseCode.ACCEPTED) {
                        connected.countDown();
                    }
                }
                if (response.hasDownlinkMsg()) {
                    System.out.println("[client] downlink id="
                            + response.getDownlinkMsg().getDownlinkMsgId());
                }
                if (response.hasUplinkResponseMsg()) {
                    var ack = response.getUplinkResponseMsg();
                    System.out.println("[client] uplink ack id=" + ack.getUplinkMsgId()
                            + ", success=" + ack.getSuccess());
                    uplinkCompleted.countDown();
                }
            }

            @Override
            public void onError(Throwable error) {
                System.out.println("[client] stream error: " + error.getMessage());
                connected.countDown();
                uplinkCompleted.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("[client] server completed the stream");
            }
        };

        requestStream = EdgeRpcServiceGrpc.newStub(channel)
                .withCompression("gzip")
                .handleMsgs(responseStream);

        requestStream.onNext(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.CONNECT_RPC_MESSAGE)
                .setConnectRequestMsg(ConnectRequestMsg.newBuilder()
                        .setEdgeRoutingKey("demo-edge")
                        .setEdgeSecret("demo-secret")
                        .setEdgeVersion(EdgeVersion.V_LATEST)
                        .setMaxInboundMessageSize(4 * 1024 * 1024))
                .build());

        if (!connected.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("gRPC connect handshake timed out");
        }

        requestStream.onNext(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.SYNC_REQUEST_RPC_MESSAGE)
                .setSyncRequestMsg(org.example.thingsboard.grpcdemo.proto.SyncRequestMsg.newBuilder()
                        .setFullSync(true))
                .build());

        PostTelemetryMsg telemetry = PostTelemetryMsg.newBuilder()
                .addTsKvList(TsKvListProto.newBuilder()
                        .setTs(System.currentTimeMillis())
                        .addKv(KeyValueProto.newBuilder()
                                .setKey("temperature")
                                .setType(KeyValueType.DOUBLE_V)
                                .setDoubleV(23.5)))
                .build();
        requestStream.onNext(RequestMsg.newBuilder()
                .setMsgType(RequestMsgType.UPLINK_RPC_MESSAGE)
                .setUplinkMsg(UplinkMsg.newBuilder()
                        .setUplinkMsgId(1001)
                        .addEntityData(EntityDataProto.newBuilder()
                                .setEntityIdMSB(0x1234)
                                .setEntityIdLSB(0x5678)
                                .setEntityType("DEVICE")
                                .setPostTelemetryMsg(telemetry)))
                .build());

        if (!uplinkCompleted.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("uplink response timed out");
        }
        requestStream.onCompleted();
    }

    @Override
    public void close() throws InterruptedException {
        channel.shutdown();
        channel.awaitTermination(5, TimeUnit.SECONDS);
    }
}
