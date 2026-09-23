package org.example.thingsboard.grpcdemo;

public final class DemoMain {

    private DemoMain() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 7070 : Integer.parseInt(args[0]);
        try (DemoServer server = new DemoServer(port)) {
            server.start();
            try (DemoClient client = new DemoClient("127.0.0.1", port)) {
                client.run();
            }
        }
    }
}
