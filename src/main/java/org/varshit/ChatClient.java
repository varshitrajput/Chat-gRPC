package org.varshit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.varshit.chat.ChatProto.ChatMessage;
import org.varshit.chat.ChatProto.ChatResponse;
import org.varshit.chat.ChatServiceGrpc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ChatClient {
    private final ManagedChannel channel;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final PrintWriter writer;
    private static final String LOG_FILE = "chat_log.txt";

    public ChatClient(String host, int port) throws IOException {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        asyncStub = ChatServiceGrpc.newStub(channel);
        writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);

    }

    public void shutdown() {
        channel.shutdown();
        writer.close();

    }

    public void chat() {
        readAndPrintLogFile();

        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(new StreamObserver<ChatResponse>() {
            @Override
            public void onNext(ChatResponse response) {
                System.out.println(response.getUser() + ": " + response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Chat ended.");
            }
        });

        try (Scanner scanner = new Scanner(System.in)) {
            boolean usernameSet = Boolean.FALSE;
            String username = null;
            while (true) {
                if(!usernameSet) {
                    System.out.println("Enter your username:");
                    username = scanner.nextLine();
                    System.out.print("You can start messaging now, press return to send messages");
                    usernameSet = Boolean.TRUE;
                }
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                ChatMessage chatMessage = ChatMessage.newBuilder()
                        .setUser(username)
                        .setMessage(message)
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                requestObserver.onNext(chatMessage);
            }
        }

        requestObserver.onCompleted();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        ChatClient client = new ChatClient("localhost", 50051);
        client.printAsciiArt();
        client.chat();
        client.shutdown();
    }

    public void printAsciiArt() {
        clearScreen();  // Clear the screen before printing ASCII art
        System.out.println(" .d8888b.  888               888                 8888888b.   .d8888b.  ");
        System.out.println("d88P  Y88b 888               888                 888   Y88b d88P  Y88b ");
        System.out.println("888    888 888               888                 888    888 888    888 ");
        System.out.println("888        88888b.   8888b.  888888      888d888 888   d88P 888        ");
        System.out.println("888        888 88b       88b 888         888P     8888888P  888        ");
        System.out.println("888    888 888  888 .d888888 888         888     888        888    888 ");
        System.out.println("Y88b  d88P 888  888 888  888 Y88b.       888     888        Y88b  d88P ");
        System.out.println("  Y8888P   888  888  Y888888  Y8888888   888     888         Y8888P  ");
        System.out.println("                                                            By Varshit");
    }

    public void readAndPrintLogFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }

    public void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("bash", "-c", "clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            // Handle exception
            e.printStackTrace();
        }
    }
}
