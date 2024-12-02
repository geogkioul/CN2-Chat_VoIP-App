package com.cn2.communication;

import java.util.concurrent.BlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class VoicePlaybackThread implements Runnable{
    private BlockingQueue<byte[]> playbackQueue;
    private boolean running; // boolean variable to keep track of thread's running state

    public VoicePlaybackThread(BlockingQueue<byte[]> playbackQueue) {
        this.playbackQueue = playbackQueue;
        this.running = true; // true by default until forced to stop
    }

    // A function to stop the thread if needed
    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        AudioFormat AUDIO_FORMAT = getAudioFormat();
        try (SourceDataLine speakers = AudioSystem.getSourceDataLine(AUDIO_FORMAT)) {
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
        }
    }

    // A function to define the audio format for audio capturing
    private AudioFormat getAudioFormat() {
        return new AudioFormat(44100.0f, 16, 1, true, false);
    }
}
