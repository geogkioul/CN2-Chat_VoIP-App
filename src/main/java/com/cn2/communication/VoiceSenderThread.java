package com.cn2.communication;
import java.net.*;
import javax.sound.sampled.*;

public class VoiceSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    private boolean running = true; // A boolean to keep track if the user is on call, by default set on true
    // Define the class constructor
    public VoiceSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort) throws LineUnavailableException {
        this.socket = socket; // Assign local socket of thread
        this.peerAddress = peerAddress; // Assign peer's IP address
        this.peerPort = peerPort; // Assign peer's port number
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        try {
            // Select the format that will be used to sample voice
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if(!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Audio format not supported");
            }
            // Open the microphone
            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            byte[] buffer = new byte[1024]; // Create a buffer to store voice data recorded
            while(running) {
                // Read voice data from microphone into buffer, return number of bytes read
                int bytesRead = microphone.read(buffer, 0, buffer.length); 
                if(bytesRead > 0) {
                    byte[] data = new byte[bytesRead + 3];
                    System.arraycopy("VOI".getBytes(), 0, data, 0, 3); // Add the header to the voice data
                    System.arraycopy(buffer, 0, data, 3, bytesRead); // Add the actual data to the data bytes array
                    // Create a packet with the voice data to send to specific IP and Port of peer
                    DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
                    socket.send(packet); // Send the created packet through the local socket
                }
            }
            // When the user is no more on call close the mic
            microphone.stop();
            microphone.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Implement a function to stop the thread, when the user is no more on call
    // This function can be used by main
    public void stop() {
        running = false;
    }
 }
