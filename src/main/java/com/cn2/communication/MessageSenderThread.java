package com.cn2.communication;

import java.io.IOException;
import java.net.*;
class MessageSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    
    // Define the class constructor
    public MessageSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort) {
        this.socket = socket; // Assign the socket of the thread
        this.peerAddress = peerAddress; // Assign the peer IP
        this.peerPort = peerPort; // Assign the peer port
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        try {
            while(true) {
                // Get the input from the user
                String message = App.inputTextField.getText();
                byte[] data = ("MSG" + message).getBytes(); // Convert the message string to bytes after adding header
                // Create a packet with the contents of the message to send to specific IP and Port of peer
                DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
                socket.send(packet); // Send the packet through the local socket
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void initiateCall() throws IOException {
		
	}
}