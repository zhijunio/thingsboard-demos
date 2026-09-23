package org.example.thingsboard.grpcdemo;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.example.thingsboard.grpcdemo.proto.ConnectResponseCode;
import org.example.thingsboard.grpcdemo.proto.ConnectResponseMsg;
import org.example.thingsboard.grpcdemo.proto.DownlinkMsg;
import org.example.thingsboard.grpcdemo.proto.EdgeConfiguration;
import org.example.thingsboard.grpcdemo.proto.EdgeRpcServiceGrpc;
import org.example.thingsboard.grpcdemo.proto.RequestMsg;
import org.example.thingsboard.grpcdemo.proto.RequestMsgType;
import org.example.thingsboard.grpcdemo.proto.ResponseMsg;
import org.example.thingsboard.grpcdemo.proto.UplinkResponseMsg;

import java.io.IOException;

public final class DemoServer implements AutoCloseable {

    private static final String EDGE_KEY = "demo-edge";
    private static final String EDGE_SECRET = "demo-secret";

    private final Server server;

    public DemoServer(int port) {
        server = NettyServerBuilder.forPort(port)
                .addService(new EdgeService())
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("[server] listening on port " + server.getPort());
    }

    public void awaitTermination() throws InterruptedException {
        server.awaitTermination();
    }

    @Override
    public void close() {
        server.shutdownNow();
    }

    private static final class EdgeService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase {

        @Override
        public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> output) {
            return new StreamObserver<>() {
                private boolean connected;

                @Override
                public void onNext(RequestMsg request) {
                    if (!connected && request.getMsgType() == RequestMsgType.CONNECT_RPC_MESSAGE) {
                        handleConnect(request, output);
                        return;
                    }
                    if (!connected) {
                        output.onError(new IllegalStateException("connect message is required first"));
                        return;
                    }
                    if (request.getMsgType() == RequestMsgType.SYNC_REQUEST_RPC_MESSAGE) {
                        System.out.println("[server] sync request, fullSync="
                                + request.getSyncRequestMsg().getFullSync());
                        output.onNext(ResponseMsg.newBuilder()
                                .setDownlinkMsg(DownlinkMsg.newBuilder()
                                        .setDownlinkMsgId(1)
                                        .setSyncCompletedMsg(
                                                org.example.thingsboard.grpcdemo.proto.SyncCompletedMsg.newBuilder()))
                                .build());
                    } else if (request.getMsgType() == RequestMsgType.UPLINK_RPC_MESSAGE
                            && request.hasUplinkMsg()) {
                        int id = request.getUplinkMsg().getUplinkMsgId();
                        int entityCount = request.getUplinkMsg().getEntityDataCount();
                        System.out.println("[server] uplink id=" + id + ", entities=" + entityCount);
                        output.onNext(ResponseMsg.newBuilder()
                                .setUplinkResponseMsg(UplinkResponseMsg.newBuilder()
                                        .setSuccess(true)
                                        .setUplinkMsgId(id))
                                .build());
                    }
                }

                private void handleConnect(RequestMsg request, StreamObserver<ResponseMsg> output) {
                    var connect = request.getConnectRequestMsg();
                    boolean accepted = EDGE_KEY.equals(connect.getEdgeRoutingKey())
                            && EDGE_SECRET.equals(connect.getEdgeSecret());
                    System.out.println("[server] connect key=" + connect.getEdgeRoutingKey()
                            + ", accepted=" + accepted);
                    ConnectResponseMsg.Builder response = ConnectResponseMsg.newBuilder()
                            .setResponseCode(accepted
                                    ? ConnectResponseCode.ACCEPTED
                                    : ConnectResponseCode.BAD_CREDENTIALS)
                            .setMaxInboundMessageSize(4 * 1024 * 1024);
                    if (accepted) {
                        response.setConfiguration(EdgeConfiguration.newBuilder()
                                .setName("demo-edge")
                                .setType("demo")
                                .setRoutingKey(EDGE_KEY)
                                .setSecret(EDGE_SECRET));
                        connected = true;
                    } else {
                        response.setErrorMsg("invalid edge credentials");
                    }
                    output.onNext(ResponseMsg.newBuilder()
                            .setConnectResponseMsg(response)
                            .build());
                }

                @Override
                public void onError(Throwable error) {
                    System.out.println("[server] stream error: " + error.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("[server] client completed the stream");
                    output.onCompleted();
                }
            };
        }
    }
}
