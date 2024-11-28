package com.cn2.communication;
import java.net.*;
import javax.sound.sampled.*;

public class VoiceSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    private TargetDataLine microphone; // The source from which audio data will be read

    // Define the class constructor
    public VoiceSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort) throws LineUnavailableException {
        this.socket = socket; // Assign local socket of thread
        this.peerAddress = peerAddress; // Assign peer's IP address
        this.peerPort = peerPort; // Assign peer's port number

        // Select the format that will be used to sample voice
        AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        this.microphone = (TargetDataLine) AudioSystem.getLine(info);
        this.microphone.open(format);
        this.microphone.start();
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        try {
            byte[] buffer = new byte[1024]; // Create a buffer to store voice data recorded
            while(true) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 }
