package com.cn2.communication;

import java.net.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
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
	
	private static DatagramSocket socket; // the local socket through which the local host will send packets
	private static InetAddress peerAddress; // the IP address of the peer
	private static int localPort; // the transport layer port of local host
	private static int peerPort; // the transport layer port of peer host

	// Define the threads that we will use
	private static MessageSenderThread messageSenderThread;
	private static ReceiverThread receiverThread;
	private static VoicePlaybackThread voicePlaybackThread;
	private static VoiceSenderThread voiceSenderThread;
	
	// We will also define some queues that will ensure safe data exchange between threads
	
	private static BlockingQueue<String> outgoingMessages;
	private static BlockingQueue<String> incomingMessages;	
	private static BlockingQueue<String> incomingControl;
	private static BlockingQueue<byte[]> playbackQueue;
	
	// Define a boolean variable to keep track of the user being on call
	private boolean isOnCall;

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
		* 3. Linking the buttons to the ActionListener
		*/
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
		inputTextField.addActionListener(this);
		
		// Initialize the queues
		outgoingMessages = new LinkedBlockingQueue<>();
		incomingMessages = new LinkedBlockingQueue<>();
		incomingControl = new LinkedBlockingQueue<>();
		playbackQueue = new LinkedBlockingQueue<>();

		// Start the helper threads that checks for new incoming messages/commands/voice
		checkIncomingThread();
		checkCommandsThread();
		isOnCall = false; // false by default
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
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
			String username = JOptionPane.showInputDialog("are you dolby or gkioul: ");
			switch (username) {
				case "dolby":
					peerAddress = InetAddress.getByName("25.9.63.44");
					localPort = 50000;
					peerPort = 50001;
					break;
				case "gkioul":
					peerAddress = InetAddress.getByName("25.8.217.160");
					localPort = 50001;
					peerPort = 50000;
					break;
				default:
					peerAddress = InetAddress.getByName(JOptionPane.showInputDialog("Please enter the IP address of the peer:")); 
					localPort = Integer.parseInt(JOptionPane.showInputDialog("Please enter the local port you want to use:"));
					peerPort = Integer.parseInt(JOptionPane.showInputDialog("Please specify the peer's port:"));
					break;
			}
			// Create the local socket. IP of socket set to wildcard address 0.0.0.0 which binds the users to the IPs of every local interface
			socket = new DatagramSocket(localPort);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// after creating the socket it's time to initialize the threads with the socket info
		messageSenderThread = new MessageSenderThread(socket, peerAddress, peerPort, outgoingMessages); // The thread that will handle message sending
		receiverThread = new ReceiverThread(socket, incomingMessages, incomingControl, playbackQueue); // The thread that will handle receiving packets
		voicePlaybackThread = new VoicePlaybackThread(playbackQueue); // The thread that will playback the voice data received

		// and then start them all besides the voiceSenderThread which will activate only during calls
		new Thread(messageSenderThread).start();
		new Thread(receiverThread).start();
		new Thread(voicePlaybackThread).start();
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
				callButton.setText("End Call");
				startCall();
			} else {
				isOnCall = false;
				callButton.setText("Call");
				endCall();
			}
		} else if(event.getSource() == inputTextField){
			// Enter was pressed
			sendMessage();
		}
	}



	/* MESSAGES LOGIC */



	// The function sendMessage will take the message from inputTextField and put it in the outgoing queue
	// the message sender will take on from that point.
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

	// This is a helper thread that will check the incomingMessages queue for new messages
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

	public static void displayMessage(String message) {
		textArea.append(message + newline); // Show the message in the text area
	}




	/* CONTROL LOGIC */




	// This is a helper thread that will check the incomingControl queue for new commands
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
	
	// This function will handle the commands based on the command message
	private void handleCommands(String command) {
		switch (command) {
			case "CALL_REQUEST":
				callRequested();
				break;
			case "CALL_ACCEPT":
				// Start the voice thread only if the caller is still on call at the time the acceptance command arrives
				if (isOnCall) {
					// Start the voice sending thread
					voiceSenderThread = new VoiceSenderThread(socket, peerAddress, peerPort); // The thread that will handle voice sending during calls
					new Thread(voiceSenderThread).start();
				}
				break;
			case "CALL_DENY":
				isOnCall = false;
				callButton.setText("Call");
				break;
			case "CALL_END":
				isOnCall = false;
				callButton.setText("Call");
				voiceSenderThread.stopRunning();
			default:
				break;
		}
	}

	



	/* CALLING LOGIC */




	private void startCall() {
		String command = "CALL_REQUEST";
		try {
			outgoingMessages.put("CTL" + command); // put command to the queue, added control header
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		isOnCall = true;
		// Start the voice sending thread
	}

	private void endCall() {
		String command = "CALL_END";
		try {
			outgoingMessages.put("CTL" + command);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		isOnCall = false;
		// Stop the voice sending thread if it's currently running (it will be null otherwise)
		if(voiceSenderThread != null){
			voiceSenderThread.stopRunning();
		}
	}
	
	private void callRequested() {
		String[] options = {"Accept Call", "Deny Call"};
		int choise = JOptionPane.showOptionDialog(null, "Incoming Call Request", "Call Request", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
		if(choise == JOptionPane.YES_OPTION) {
			displayMessage("Call Accepted");
			isOnCall = true;
			callButton.setText("End Call");
			// Start voice thread
			new Thread(voiceSenderThread).start();

			String command = "CALL_ACCEPT";
			try {
				outgoingMessages.put("CTL" + command); // put message to the queue, added control header
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		} else if (choise == JOptionPane.NO_OPTION) {
			displayMessage("Call Denied");
			isOnCall = false;
			callButton.setText("Call");
			String command = "CALL_DENY";
			try {
				outgoingMessages.put("CTL" + command); // put message to the queue, added control header
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

		/*THE CALLING PROCESS
	 * 1) The peer initiating the call sends a speciall call initiation message "CALL_REQUEST" to the other peer
	 * 	  when call is initiaded the voiceSendingThread is activated. However the peer will receive the voice packets only
	 * 2) The receiver responds with a call acknowledgment message to confirm they are ready "CALL_ACCEPT"
	 * 	  or else they can send a call rejection message if they don't want to accept the call "CALL_DENY"
	 * 3) Once the call is accepted both peers start the voice sender thread to transmit audio
	 * 4) Both peers start exchanging audio through the UDP socket, while they can still send messages
	 * 5) Call ending: when either peer decides to end the call they send a call termination message "CALL_END"
	 *    to notify the other peer
	 * 	  Both peers stop their voice sending threads and release audio resources
	*/	
	
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
		// TODO Auto-generated method stub
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
