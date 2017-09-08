/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coderun1;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 *
 * @author peiran
 */
public class LBServerSync {
    public final static String host = "127.0.0.1";
    public final static int port = 9990;
    public final static String upHost = "127.0.0.1";
    public final static int upPort = 9090;
    public final static int timeout = 2000;
    private static Map<SocketChannel, byte[]> dataTracking =
            new HashMap<SocketChannel, byte[]>();
    
    public static void main(String[] args) throws IOException {
        ThreadPoolServer server = new ThreadPoolServer(port);
        new Thread(server).start();
        boolean stop = false;
        while(!stop) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                stop = true;
            }
        }
        server.stop();
    }
    static class ThreadPoolServer implements Runnable {
        protected int serverPort;
        protected ServerSocket serverSocket = null;
        protected boolean isStopped = false;
        protected Thread runningThread = null;
        protected ExecutorService threadPool = Executors.newFixedThreadPool(10);
        
        public ThreadPoolServer(int port) {
            this.serverPort = port;
        }
        public void run() {
            synchronized(this) {
                this.runningThread = Thread.currentThread();
            }
            System.out.println("Server Thread" + this.runningThread.getName());
            openServerSocket();
            while(! isStopped()) {
                Socket clientSocket = null;
                try {
                    clientSocket = this.serverSocket.accept();
                } catch (IOException e) {
                    if (isStopped()) {
                        System.out.println("Server stopped");
                        break;
                    }
                    throw new RuntimeException("Error accepting client connection",e);
                }
                this.threadPool.execute(new WorkerRunnable(clientSocket));
            }
            this.threadPool.shutdown();
            System.out.println("server stopped");
        }
        private void openServerSocket() {
            try {
                this.serverSocket = new ServerSocket(this.serverPort);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open server socket");
            }
        }
        private synchronized boolean isStopped() {
            return this.isStopped;
        }
        public void stop() {
            this.isStopped = true;
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException("Error stop server");
            }
        }
    }
    
    static class WorkerRunnable implements Runnable {
        protected Socket clientSocket = null;
        public WorkerRunnable(Socket clientSocket) {
         this.clientSocket = clientSocket;
        }
        public void run() {
            System.out.println("Worker Thread" + Thread.currentThread().getName());

            try {
                try (InputStream input = clientSocket.getInputStream();
                    OutputStream output = clientSocket.getOutputStream()) {
                    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
                    int n = 0;
                    byte[] data = new byte[2000];
                    n = input.read(data);
                    //while ((n = input.read(data)) != -1) {
                    System.out.println("READ CLIENT " + String.valueOf(n));
                    if (n != -1) {
                        outputBytes.write(data, 0, n);
                    }
                    //}
                    outputBytes.flush();
                    System.out.println(outputBytes.toString());
                    byte[] resp = dispatch(outputBytes.toByteArray());
                    output.write(resp);
                    System.out.println("WRITE CLIENT " + new String(resp));
                }
                System.out.println("Request processed");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        private byte[] dispatch(byte[] data) throws IOException {
            byte[] resp;
            try (Socket socket = openSocket(upHost, upPort)) {
                resp = writeAndRead(socket, data);
            }
            return resp;
        }
        private Socket openSocket(String host, int port) throws IOException {
            Socket socket;
            try {
                InetAddress addr = InetAddress.getByName(host);
                InetSocketAddress socketAddr = new InetSocketAddress(addr, port);
                socket = new Socket();
                socket.connect(socketAddr, timeout);
                return socket;
                
            } catch (SocketTimeoutException | UnknownHostException e) {
                e.printStackTrace();
                throw e;
            } catch (IOException e) {
                e.printStackTrace();
                throw e;
            }
        }
        private byte[] writeAndRead(Socket socket, byte[] writedata) throws IOException {
            try {
                ByteArrayOutputStream outputBytes;
                int n = 0;
                byte[] data = new byte[1000];
                try (InputStream input = socket.getInputStream(); 
                        OutputStream output = socket.getOutputStream()) {
                    outputBytes = new ByteArrayOutputStream();
                    output.write(writedata);
                    while((n = input.read(data)) != -1) {
                        System.out.println("READ UPSTREAM " + String.valueOf(n));
                        outputBytes.write(data, 0, n);
                    }
                    outputBytes.flush();
                    System.out.println(outputBytes.toString());
                }
                return outputBytes.toByteArray();
            } catch(IOException e) {
                e.printStackTrace();
                throw e;
            }            
        }
    }

}
