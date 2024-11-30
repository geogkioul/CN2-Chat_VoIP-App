package com.cn2.communication;

import java.net.*;
import java.util.concurrent.BlockingQueue;
class ReceiverThread implements Runnable {
    private DatagramSocket socket; // This is the socket the receiver thread will receive data from
    private BlockingQueue<String> incomingMessages; // The queue that the receiver will put the received messages into
    private BlockingQueue<String> incomingControl; // The queue that the receiver will put the control commands into

    private static final int BUFFER_SIZE = 2048; // Maximum buffer size for incoming packets

    // Define the class constructor
    public ReceiverThread(DatagramSocket socket, BlockingQueue<String> incomingMessages, BlockingQueue<String> incomingControl) {
        this.socket = socket; // Assign the socket of the thread
        this.incomingMessages = incomingMessages; // Assign the incoming messages queue
        this.incomingControl = incomingControl; // Assign the incoming messages queue
    }
    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        // Define a receiver buffer
        byte[] buffer = new byte[BUFFER_SIZE]; // A byte array to store the incoming data
        try {
            while(true) {
                // Create a packet to hold the data received via the Datagram socket
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length); 
                // When received a packet place it into the Datagram Packet
                socket.receive(packet);
                
                // Process the packet
                processPacket(packet);
            }
        } catch (Exception e) {
            System.err.println("Error in ReceiverThread: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Packet processing
    private void processPacket(DatagramPacket packet) {
        try {
            // Extract the header of the packet (first 3 bytes) to find if it's a Message "MSG" or Voice "VOI" packet
            String header = new String(packet.getData(), 0, 3);
            byte[] data = new byte[packet.getLength() - 3]; // Create a byte array to hold the actual data
            System.arraycopy(packet.getData(), 3, data, 0, data.length); // Copy the actual data of received packet to data array

            // Handle the packet according to the header which signifies the type of data included
            switch (header) {
                case "MSG":
                    handleTextMessage(new String(data));
                    break;
                case "VOI":
                    // handleVoiceData(data);
                    break;
                case "CTL": 
                    handleControlCommand(new String(data));
                    break;
                default:
                System.err.println("Uknown header received: " + header);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Define handlers
    
    private void handleTextMessage(String message) {
        // Forward the message to the app to display
        try {
            incomingMessages.put(message); // put message to the queue, let main handle it
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleControlCommand(String command) {
        // Forward the message to the app to display
        try {
            incomingControl.put(command); // put command to the queue, let main handle it
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /* To be implemented
    private void handleVoiceData(byte[] audioData) {
        // Forward the audio data to the audio playback system
        App.playVoiceData(audioData);
        // TODO: Implement the playVoiceData function in the App that will forward the audio for playback
    }
    */    
}