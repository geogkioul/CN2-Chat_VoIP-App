package com.cn2.communication;

import java.net.*;
class ReceiverThread implements Runnable {
    private DatagramSocket socket; // This is the socket the receiver thread will receive data from
    // Define the class constructor
    public ReceiverThread(DatagramSocket socket) {
        this.socket = socket; // Assign the socket of the thread
    }
    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        try {
            // Define a receiver buffer
            byte[] buffer = new byte[2048]; // A byte array to store the incoming data
            while(true) {
                // Create a packet to hold the data received via the Datagram socket
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length); 
                // Wait for a packet to arrive from peer
                // When received a packet place it into the Datagram Packet
                socket.receive(packet);
                // Extract the header of the packet to find if it's a Message "MSG" or Voice "VOI" packet
                String header = new String(packet.getData(), 0, 3);
                byte[] data = new byte[packet.getLength()-3]; // Create a byte array to hold the actual data
                System.arraycopy(packet.getData(), 3, data, 0, data.length); // Copy the actual data of received packet to data array

                // Handle the packet according to the header which signifies the type of data included
                if(header.equals("MSG")) {
                    handleTextMessage(new String(data)); // call the message handler
                }
                else if (header.equals("VOI")) {
                    handleVoiceData(data); // call the voice data handler
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Define handlers
    
    private void handleTextMessage(String message) {
        // Create an appendMessage(message) function in app that takes the message and shows it to the text frame
    }

    private void handleVoiceData(byte[] audioData) {
        // To be implemented
    }
}