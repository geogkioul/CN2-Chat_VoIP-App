package com.cn2.communication;

import java.net.*;
import java.util.concurrent.BlockingQueue;

class MessageSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    // Use a queue for messages from the gui, rather than directly accessing the text field
    private BlockingQueue<String> outgoingMessages; // The queue that holds the messages forwarded by the app
    private volatile boolean running; // a boolean to keep track of thread running
    
    // Define the class constructor
    public MessageSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort, BlockingQueue<String> outgoingMessages) {
        this.socket = socket; // Assign the socket of the thread
        this.peerAddress = peerAddress; // Assign the peer IP
        this.peerPort = peerPort; // Assign the peer port
        this.outgoingMessages = outgoingMessages; // Assign the message queue
        this.running = true; // true by default, until forced to stop
    }

    public void stopRunning() {
        running = false;
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        try {
            while(running) {
                // Get the next message from the queue
                String message = outgoingMessages.take(); // Block until next message is available
                sendMessage(message);
            }
        } catch (InterruptedException e) {
            // Thread interrupted, exiting
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) throws Exception {
        try{
            byte[] data = message.getBytes(); // Convert the message string to bytes after adding header
            // Create a packet with the contents of the message to send to specific IP and Port of peer
            DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
            socket.send(packet); // Send the packet through the local socket
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}