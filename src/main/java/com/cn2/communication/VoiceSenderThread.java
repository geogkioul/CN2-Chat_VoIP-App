package com.cn2.communication;

import java.io.IOException;
import java.net.*;
import javax.sound.sampled.*;

public class VoiceSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    private volatile boolean running; // boolean variable to safely stop the thread when needed
    private AudioFormat AUDIO_FORMAT; // The audio format that will be used for sound data packets

    // Define the class constructor
    public VoiceSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort, AudioFormat AUDIO_FORMAT) {
        this.socket = socket; // Assign local socket of thread
        this.peerAddress = peerAddress; // Assign peer's IP address
        this.peerPort = peerPort; // Assign peer's port number
        this.running = true; // true by default
        this.AUDIO_FORMAT = AUDIO_FORMAT;
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        TargetDataLine microphone = null;
        try {
            microphone = AudioSystem.getTargetDataLine(AUDIO_FORMAT);
            // Open the microphone
            microphone.open(AUDIO_FORMAT);
            microphone.start();

            byte[] audioBuffer = new byte[1024]; // Create a buffer to store voice data recorded
            while(running) {
                // Read voice data from microphone into buffer, return number of bytes read
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length); 
                if(bytesRead > 0) {
                    byte[] data = new byte[bytesRead + 3];
                    System.arraycopy("VOI".getBytes(), 0, data, 0, 3); // Add the header to the voice data
                    System.arraycopy(audioBuffer, 0, data, 3, bytesRead); // Add the actual data to the data bytes array
                    
                    // Create a packet with the voice data to send to specific IP and Port of peer
                    DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
                    socket.send(packet); // Send the created packet through the local socket
                    
                }
            }
        } catch (LineUnavailableException e) {
            System.err.println("Audio Line Unavailable: " + e.getMessage());
            stopRunning();
        } catch (IOException e) {
            System.err.println("Network Error Occured: " + e.getMessage());
        } finally {
            if (microphone != null && microphone.isOpen()) {
                microphone.close();
            }
        }
    }

    // A function to stop the thread
    public void stopRunning() {
        running = false;
    }
 }
