package com.cn2.communication;

import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class VoicePlaybackThread implements Runnable{
    private BlockingQueue<byte[]> playbackQueue;
    private volatile boolean running; // boolean variable to keep track of thread's running state
    private AudioFormat AUDIO_FORMAT; // The audio format that will be used for sound data packets

    public VoicePlaybackThread(BlockingQueue<byte[]> playbackQueue, AudioFormat AUDIO_FORMAT) {
        this.playbackQueue = playbackQueue;
        this.running = true; // true by default until forced to stop
        this.AUDIO_FORMAT = AUDIO_FORMAT;
    }   

    // A function to stop the thread if needed
    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        SourceDataLine speakers = null;
        try {
            speakers = AudioSystem.getSourceDataLine(AUDIO_FORMAT);
            speakers.open();
            speakers.start();

            while (running) {
                try {
                    byte[] audioData = playbackQueue.take();
                    speakers.write(audioData, 0, audioData.length);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (LineUnavailableException e) {
            System.err.println("Audio Line Unavailable: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error During Audio Playback.");
            e.printStackTrace();
        } finally {
            if(speakers != null && speakers.isOpen()){
                speakers.drain();
                speakers.close();
            }
        }
    }
}
