package com.miskevich.core;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class Session {

    public static void main(String[] args) throws IOException {
        System.out.println("Server started...");
        ServerSocket serverSocket = new ServerSocket(3000);

        while (true){
            Socket socket = serverSocket.accept();
            Thread thread = new Thread(new MyServerSocket(socket));
            thread.start();
        }
    }
}
