package com.cn2.communication;

import java.net.*;
import javax.sound.sampled.*;

public class VoiceSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 16, 1, true, true); // Default audio format
    
    // Define the class constructor
    public VoiceSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort) throws LineUnavailableException {
        this.socket = socket; // Assign local socket of thread
        this.peerAddress = peerAddress; // Assign peer's IP address
        this.peerPort = peerPort; // Assign peer's port number
        if (!AudioSystem.isLineSupported(new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT))) {
            throw new LineUnavailableException("Audio format not supported");
        }
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        TargetDataLine microphone = null; // Define the line from which voice will be recorded
        try {
            // Open the microphone
            microphone = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT));
            microphone.open(AUDIO_FORMAT);
            microphone.start();

            byte[] buffer = new byte[1024]; // Create a buffer to store voice data recorded
            while(running) {
                // Read voice data from microphone into buffer, return number of bytes read
                int bytesRead = microphone.read(buffer, 0, buffer.length); 
                if(bytesRead > 0) {
                    byte[] data = new byte[bytesRead + 3];
                    System.arraycopy("VOI".getBytes(), 0, data, 0, 3); // Add the header to the voice data
                    System.arraycopy(buffer, 0, data, 3, bytesRead); // Add the actual data to the data bytes array
                    
                    // Check that the socket is available
                    if(!socket.isClosed()) {
                        // Create a packet with the voice data to send to specific IP and Port of peer
                        DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
                        socket.send(packet); // Send the created packet through the local socket
                    }
                }
            }
        } catch (LineUnavailableException e) {
            System.err.println("Microphone Line not available: " + e.getMessage());
        } catch (Exception e) {
            if(Thread.currentThread().isInterrupted()) {
                System.out.println("Voice sender thread interrupted");
            } else {
                e.printStackTrace();
            }
        } finally {
            // Ensure the microphone is closed
            if(microphone != null){
                try {
                    microphone.stop();
                    microphone.close();
                } catch (Exception e) {
                    System.err.println("Error closing microphone: " + e.getMessage());
                }
            }
        }
    }
    // Implement a function to stop the thread, when the user is no more on call
    // This function can be used by main app
    public void stop() {
        running = false;
        Thread.currentThread().interrupt(); // Interrupt the thread if it's blocked
    }
 }
