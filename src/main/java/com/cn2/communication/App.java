package com.cn2.communication;

import java.net.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.sound.sampled.AudioFormat;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.*;
import java.awt.event.*;

import java.lang.Thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	/* 
	 * Definition of network related variables
	 */
	private static DatagramSocket socket; // the local socket through which the local host will send packets
	private static InetAddress peerAddress; // the IP address of the peer
	private static int localPort; // the transport layer port of local host
	private static int peerPort; // the transport layer port of peer host

	/*
	 * Definition of the threads that will be used for concurrency
	 * We will use 4 threads
	 */
	// This thread is responsible for sending out messages written in the text area by the user
	private static MessageSenderThread messageSenderThread; 
	// This thread is responsible for receiving all messages from the local socket (text, control and voice) and pass them to the appropriate handlers
	private static ReceiverThread receiverThread;
	// This is a thread to handle voice data received
	private static VoicePlaybackThread voicePlaybackThread;
	// This is a thread that records, formats and sends out the voice of the user
	private static VoiceSenderThread voiceSenderThread;
	
	/*
	 * Definition of the queues that will handle data transfer between threads
	 * We use shared queues among threads for coordinating communication flow
	 * in a thread-safe and efficient way
	 */
	// This queue holds all the messages that are send out by the user
	private static BlockingQueue<String> outgoingMessages;
	// This queue holds all the messages that are received from the socket
	private static BlockingQueue<String> incomingMessages;
	// This queue holds the control commands received from the socket
	private static BlockingQueue<String> incomingControl;
	// This queue holds the audio data that needs to be played back
	private static BlockingQueue<byte[]> playbackQueue;
	
	// Define a boolean variable to keep track of the user being on call or not
	private boolean isOnCall;

	// Define the Audio Format that will be used
	private static AudioFormat AUDIO_FORMAT;

	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		
		/*
		 * 1. Defining the components of the GUI
		 */
		
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField(); // This is where the user will write messages
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40); // This is where messages will appear	
		textArea.setLineWrap(true);				
		textArea.setEditable(false);			
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");			
		
		/*
		* 2. Adding the components to the GUI
		*/
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		
		/*
		* 3. Linking the buttons and textInputField to the ActionListener
		*/
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		inputTextField.addActionListener(this);
		
		// Initialize the queues
		outgoingMessages = new LinkedBlockingQueue<>();
		incomingMessages = new LinkedBlockingQueue<>();
		incomingControl = new LinkedBlockingQueue<>();
		playbackQueue = new LinkedBlockingQueue<>();

		// Initialize the AUDIO_FORMAT
		AUDIO_FORMAT = new AudioFormat(8000.0f, 16, 1, true, false);

		// Start the helper threads that check for new incoming messages/commands/voice
		checkIncomingThread();
		checkCommandsThread();
		isOnCall = false; // false by default
	}
	
	/**
	 * The main method of the application. It prompts the user for the network parameters
	 * and starts the threads that will be needed for communication
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Chat & VoIP App");																  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		// Prompt the user to define network parameters
		try {
			
			peerAddress = InetAddress.getByName(JOptionPane.showInputDialog("Please enter the IP address of the peer:")); 
			localPort = Integer.parseInt(JOptionPane.showInputDialog("Please enter the local port you want to use:"));
			peerPort = Integer.parseInt(JOptionPane.showInputDialog("Please specify the peer's port:"));
			
			// Create the local socket. IP of socket set to wildcard address 0.0.0.0 which binds the users to the IPs of every local interface
			socket = new DatagramSocket(localPort);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// Initialize some of the threads using the socket info, the voice threads will activate only during calls
		messageSenderThread = new MessageSenderThread(socket, peerAddress, peerPort, outgoingMessages); // The thread that will handle message sending
		receiverThread = new ReceiverThread(socket, incomingMessages, incomingControl, playbackQueue); // The thread that will handle receiving packets

		// Start the threads, except from the voice threads that will be used only during calls
		new Thread(messageSenderThread).start();
		new Thread(receiverThread).start();
	}


	@Override
	public void actionPerformed(ActionEvent event) {
		/*
		 * Check which button was clicked.
		 */
		if (event.getSource() == sendButton){
			// The "Send" button was clicked
			sendMessage();
		} else if(event.getSource() == callButton){
			// The "Call" button was clicked
			if (!isOnCall) {
				isOnCall = true;
				updateCallButton();
				startCall();
			} else {
				isOnCall = false;
				updateCallButton();
				endCall();
			}
		} else if(event.getSource() == inputTextField){
			// Enter was pressed in the textInputField
			sendMessage();
		}
	}
	// A function that will set the text of the Call button based on the boolean isOnCall
	private void updateCallButton() {
		if (isOnCall) {
			callButton.setText("End Call");
		} else {
			callButton.setText("Call");
		}
	}



	/* MESSAGES LOGIC */



	// The function sendMessage will take the message from inputTextField and put it in the outgoingMessages queue
	// the messageSender Thread will take on from that point by overwatching the contents of that queue
	// Also it will display the message in the text area
	private void sendMessage() {
		String message = inputTextField.getText();
			if(!message.isEmpty()){ // send only if there is something written in input field
				try {
					outgoingMessages.put("MSG" + message); // put message to the queue, added message header
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				inputTextField.setText(""); // Clear the text input field			
				// Display the message in the local text area
				displayMessage("You: " + message);
			}
			
	}

	// This is a helper thread that will check the incomingMessages queue for new messages received to display them
	private void checkIncomingThread() {
		new Thread(() -> {
			while (true) {
				try {
					// Check queue for new messages
					String newMessage = incomingMessages.take(); // This blocks until a message is available
					displayMessage("Peer: " + newMessage); // Display the received message
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).start();
	}
	// A function that displays the given message on the textArea
	public static void displayMessage(String message) {
		textArea.append(message + newline); // Show the message in the text area
	}




	/* CALL CONTROL LOGIC */



	// This is a helper thread that will check the incomingControl queue for new call commands
	private void checkCommandsThread() {
		new Thread(() -> {
			while (true) {
				try {
					// Check queue for new messages
					String newCommand = incomingControl.take(); // This blocks until a message is available
					handleCommands(newCommand);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}).start();
	}
	
	// This function will handle the call commands based on the command message
	private void handleCommands(String command) {
		switch (command) {
			case "CALL_REQUEST":
				if (!isOnCall) { // only if he isn't already in a call when he received the call request
					callRequested(); // call the appropriate function
				}
				break;
			case "CALL_ACCEPT": // the peer accepted the call request we sent them
				// Start the voice threads
				displayMessage("The peer accepted your call.");
				isOnCall = true;
				voiceThreads();
				break;
			case "CALL_DENY": // the peer denied the call request we sent them
				displayMessage("The peer denied your call.");
				isOnCall = false; // change call status
				updateCallButton();
				break;
			case "CALL_END":
				displayMessage("The peer ended the call");
				isOnCall = false;
				updateCallButton();
				voiceThreads();
				break;
			default:
				break;
		}
	}

	



	/* CALLING LOGIC */
	/* THE CALL CONTROL PROCESS
	 * 1) A peer starts a call by pressing the Call button. The app sends a command CALL_REQUEST to the other peer. 
	 * 2) Upon receival of the CALL_REQUEST, the peer can either accept or deny the call, and he will send a command CALL_ACCEPT or CALL_DENY accordingly
	 * 	If he selects to accept the call he starts sending voice packets to the peer who started the call
	 * 3) When the peer who started the call receives the CALL_ACCEPT command he starts sending voice packets too
	 * 4) At any point during the call any peer can choose to end the call by pressing End Call and then he will send a CALL_END command 
	 * 	and he will stop sending voice packets. The other peer will stop transmitting voice too when he receives the CALL_END command
	 */


	// A function to start/stop voice threads during call start/end
	private void voiceThreads() {
		if (isOnCall) {
			// The thread that will send voice during calls
			voiceSenderThread = new VoiceSenderThread(socket, peerAddress, peerPort, AUDIO_FORMAT);
			new Thread(voiceSenderThread).start();
			// The thread that will playback voice during calls
			voicePlaybackThread = new VoicePlaybackThread(playbackQueue, AUDIO_FORMAT);
			new Thread(voicePlaybackThread).start();
		}
		else {
			// Stop the threads when call ends
			voiceSenderThread.stopRunning();
			voicePlaybackThread.stopRunning();
		}
	}

	private void startCall() {
		String command = "CALL_REQUEST";
		try {
			outgoingMessages.put("CTL" + command); // put command to the queue, added control header
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		isOnCall = true;
		displayMessage("You started a call. Waiting for peer...");
		// Wait for peer to accept in order to start voice sending thread
	}

	private void endCall() {
		String command = "CALL_END";
		try {
			outgoingMessages.put("CTL" + command);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		isOnCall = false;
		displayMessage("You ended the call.");
		voiceThreads();
	}
	
	private void callRequested() {
		String[] options = {"Accept Call", "Deny Call"};
		int choise = JOptionPane.showOptionDialog(null, "Incoming Call Request", "Call Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if(choise == JOptionPane.YES_OPTION) {
			displayMessage("You accepted the call.");
			isOnCall = true;
			updateCallButton();
			voiceThreads();

			String command = "CALL_ACCEPT";
			try {
				outgoingMessages.put("CTL" + command); // put message to the queue, added control header
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		} else if (choise == JOptionPane.NO_OPTION) {
			displayMessage("You denied the call.");
			isOnCall = false;
			updateCallButton();
			String command = "CALL_DENY";
			try {
				outgoingMessages.put("CTL" + command); // put message to the queue, added control header
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// Stop all the threads currently running
		if (messageSenderThread != null) {
			messageSenderThread.stopRunning();
		}
		if (receiverThread != null) {
			receiverThread.stopRunning();
		}
		if (isOnCall) {
			isOnCall = false;
			voiceThreads();
		}
		dispose();
        System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
