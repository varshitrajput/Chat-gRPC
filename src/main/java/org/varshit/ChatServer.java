package org.varshit;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.varshit.chat.ChatProto.ChatMessage;
import org.varshit.chat.ChatProto.ChatResponse;
import org.varshit.chat.ChatServiceGrpc;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    private final int port;
    private final Server server;
    private final Set<StreamObserver<ChatResponse>> clients = new HashSet<>();
    private final PrintWriter writer;

    public ChatServer(int port) throws IOException {
        this.port = port;
        server = ServerBuilder.forPort(port)
                .addService(new ChatServiceImpl())
                .build();
        writer = new PrintWriter(new FileWriter("chat_log.txt", true), true);

    }

    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            ChatServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        writer.close();
    }

    private void logMessage(ChatMessage message) {
        writer.printf("[%d] %s: %s%n", message.getTimestamp(), message.getUser(), message.getMessage());
    }


    private class ChatServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
        @Override
        public StreamObserver<ChatMessage> chat(StreamObserver<ChatResponse> responseObserver) {
            synchronized (clients) {
                clients.add(responseObserver);
            }

            return new StreamObserver<ChatMessage>() {
                @Override
                public void onNext(ChatMessage message) {
                    ChatResponse response = ChatResponse.newBuilder()
                            .setUser(message.getUser())
                            .setMessage(message.getMessage())
                            .setTimestamp(message.getTimestamp())
                            .build();
                    logMessage(message);


                    synchronized (clients) {
                        for (StreamObserver<ChatResponse> client : clients) {
                            client.onNext(response);
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    synchronized (clients) {
                        clients.remove(responseObserver);
                    }
                }

                @Override
                public void onCompleted() {
                    synchronized (clients) {
                        clients.remove(responseObserver);
                    }
                    responseObserver.onCompleted();
                }
            };
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer server = new ChatServer(50051);
        server.start();
        server.server.awaitTermination();
    }
}
