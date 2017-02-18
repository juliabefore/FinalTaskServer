package com.miskevich.core;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class MyServerSocket implements Runnable{
    private Socket socket;
    private final String INSERT = "INSERT";
    private final String WHERE = "WHERE";

    public MyServerSocket(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        DataBaseService dataBaseService = new DataBaseService(socket);
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String query = bufferedReader.readLine();

            System.out.println("Server read a query: " + query);

            if (INSERT.equalsIgnoreCase(query.substring(0, 6))){
                dataBaseService.save(query);
            }else if(!(query.toLowerCase().contains(WHERE.toLowerCase()))){
                dataBaseService.getAll(query);
            }else{
                dataBaseService.getById(query);
            }

            socket.close();

        }catch (SocketException e){

        }catch (IOException e){
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ServerException e) {
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                bufferedWriter.write("Exception: " + e.getMessage());
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
                throw new RuntimeException(e);
            }
            e.printStackTrace();
        }
    }
}

