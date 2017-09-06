/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coderun1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author peiran
 */
public class LBServer implements Runnable {

    public final static String host = "127.0.0.1";
    public final static int port = 9999;
    public final static String upHost = "127.0.0.1";
    public final static int upPort = 9090;
    public final static long timeout = 1000;
    private static Map<SocketChannel, byte[]> dataTracking =
            new HashMap<SocketChannel, byte[]>();
    private ServerSocketChannel serverChannel;
    private Selector selector;

    public static void main(String[] args) throws IOException {
        LBServer server = new LBServer();
        Thread thread = new Thread(server);
        thread.start();
    }

    public LBServer() {
        init();
    }
    private void init() {
        System.out.println("Load Balancer");
        if (selector != null) return;
        if (serverChannel != null) return;
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            InetSocketAddress hostAddr = new InetSocketAddress(host, port);
            serverChannel.socket().bind(hostAddr);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int noOfKeys = selector.select(timeout);
                System.out.println("Number of selected keys are:" + noOfKeys);
                Set selectedKeys = selector.selectedKeys();
                Iterator itr = selectedKeys.iterator();
                while (itr.hasNext()) {
                    SelectionKey key = (SelectionKey)itr.next();
                    itr.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        System.out.println("Accepting conn");
                        accept(key);
                    } else if (key.isReadable()) {
                        System.out.println("Reading from Client");
                        read(key);
                    } else if (key.isWritable()) {
                        System.out.println ("Writing to Client");
                        write(key);
                    }
                    
                }
            }  
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
        System.out.println("Closing server down");
        if (selector != null) {
            try {
                selector.close();
                serverChannel.socket().close();
                serverChannel.close();  
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void accept(SelectionKey key) throws IOException { 
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        byte[] data = dataTracking.get(channel);
        dataTracking.remove(channel);
        channel.write(ByteBuffer.wrap(data));
        key.interestOps(SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException { 
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        readBuffer.clear();
        int read = -1;
        try {
            read = channel.read(readBuffer);
        } catch (Exception e) {
            System.out.println("Reading error, close conn");
            key.cancel();
            channel.close();
            return;
        }
        if (read == -1) {
            System.out.println("Nothing was read, close conn");
            channel.close();
            key.cancel();
            return;
        }        
        readBuffer.flip();
        byte[] data = new byte[1000];
        readBuffer.get(data, 0, read);
        System.out.println("CLIENT: " + new String(data));
        Dispatcher dp = new Dispatcher();
        dp.dispatch(key, data);
    }
    
    private static class Dispatcher {
        private Selector dpSelector = null;
        private boolean dpDone = false;

        public Dispatcher() {
        }

        public void dispatch(SelectionKey clientKey, byte[] data) {
            SocketChannel channel;
            dpDone = false;
            try {
                dpSelector = Selector.open();
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.register(dpSelector, SelectionKey.OP_CONNECT);
                channel.connect(new InetSocketAddress(upHost, upPort));
                while (!Thread.interrupted() && !dpDone) {
                    dpSelector.select(1000);
                    Iterator<SelectionKey> keys = dpSelector.selectedKeys().iterator();
                    while(keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();
                        if (!key.isValid())
                            continue;
                        if (key.isConnectable()) {
                            System.out.println ("Connecting to upstream");
                            connect(key);
                        }
                        if (key.isWritable()) {
                            System.out.println ("Writing to upstream");
                            write(key, data);
                        }
                        if (key.isReadable()) {
                            System.out.println ("Reading from upstream");
                            read(key, clientKey);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (dpSelector != null) {
                        dpSelector.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        private void read(SelectionKey key, SelectionKey clientKey) throws IOException {
            SocketChannel channel = (SocketChannel)key.channel();
            ByteBuffer readBuffer = ByteBuffer.allocate(1000);
            readBuffer.clear();
            int length = 0;
            try {
                length = channel.read(readBuffer);

            } catch (IOException e) {
                e.printStackTrace();
                key.cancel();
                channel.close();
                return;
            }
            if (length == -1) {
                channel.close();
                key.cancel();
                dpDone = true;
                return;
            }
            readBuffer.flip();
            byte[] buff = new byte[length];
            readBuffer.get(buff, 0, length);
            System.out.println("Downstream: " + new String(buff));
            SocketChannel clientChannel = (SocketChannel)clientKey.channel();
            if (dataTracking.containsKey(clientChannel)) {
                byte[] prevBuff = dataTracking.get(clientChannel);
                byte[] combined = new byte[prevBuff.length + buff.length];
                System.arraycopy(prevBuff, 0, combined, 0, prevBuff.length);
                System.arraycopy(buff, 0, combined, prevBuff.length, buff.length);
                dataTracking.put(clientChannel, combined);
            } else {
                dataTracking.put(clientChannel, buff);
            }
            clientKey.interestOps(SelectionKey.OP_WRITE);

        }
        private void write(SelectionKey key, byte[] data) throws IOException {
            SocketChannel channel = (SocketChannel)key.channel();
            System.out.println("Upstream: " + new String(data));
            channel.write(ByteBuffer.wrap(data));
            key.interestOps(SelectionKey.OP_READ);

        }
        private void connect(SelectionKey key) throws IOException {
            SocketChannel channel = (SocketChannel)key.channel();
            if (channel.isConnectionPending()) {
                channel.finishConnect();
            }
            channel.configureBlocking(false);
            channel.register(dpSelector, SelectionKey.OP_WRITE);
        }
    }

}
