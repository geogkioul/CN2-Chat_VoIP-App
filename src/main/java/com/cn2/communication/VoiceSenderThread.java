package com.cn2.communication;

import java.io.IOException;
import java.net.*;
import javax.sound.sampled.*;

public class VoiceSenderThread implements Runnable {
    private DatagramSocket socket; // This is the socket the sender thread will send data from
    private InetAddress peerAddress; // This is the IP address of the peer
    private int peerPort; // This is the Transport Layer port the peer listens to
    private boolean running; // boolean variable to safely stop the thread when needed

    // Define the class constructor
    public VoiceSenderThread(DatagramSocket socket, InetAddress peerAddress, int peerPort) {
        this.socket = socket; // Assign local socket of thread
        this.peerAddress = peerAddress; // Assign peer's IP address
        this.peerPort = peerPort; // Assign peer's port number
        this.running = true; // true by default
    }

    // We must implement the inherited abstract method Runnable.run()
    @Override
    public void run() {
        AudioFormat AUDIO_FORMAT = getAudioFormat();
        try (TargetDataLine microphone = AudioSystem.getTargetDataLine(AUDIO_FORMAT)) {

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
        }
    }

    // A function to stop the thread
    public void stopRunning() {
        running = false;
    }
    // A function to define the audio format for audio capturing
    private AudioFormat getAudioFormat() {
        return new AudioFormat(44100.0f, 16, 1, true, false);
    }
 }
