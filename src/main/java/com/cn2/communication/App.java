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
	
	// TODO: Please define and initialize your variables here...
	private DatagramSocket socket; // the local socket through which the local host will send packets
	private InetAddress peerAddress; // the IP address of the peer
	private int localPort; // the transport layer port of local host
	private int peerPort; // the transport layer port of peer host
	
	// We will also define some queues that will ensure safe data exchange between threads
	
	private BlockingQueue<String> outgoingMessages = new LinkedBlockingQueue<>();
	private BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
	
	
	private MessageSenderThread messageSenderThread = new MessageSenderThread(socket, peerAddress, peerPort, outgoingMessages); // The thread that will handle message sending
	private ReceiverThread receiverThread = new ReceiverThread(socket, incomingMessages); // The thread that will handle receiving packets
	
	
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
				
		// Start the threads
		new Thread(messageSenderThread).start();
		new Thread(receiverThread).start();

		// Start the thread that checks for new incoming messages
		checkIncomingThread();
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Chat & VoIP App");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		// Prompt the user to define network parameters
		try {
			app.peerAddress = InetAddress.getByName(JOptionPane.showInputDialog("Please enter the IP address of the peer:")); 
			app.localPort = Integer.parseInt(JOptionPane.showInputDialog("Please enter the local port you want to use:"));
			app.peerPort = Integer.parseInt(JOptionPane.showInputDialog("Please specify the peer's port:"));

			// Create the local socket. IP of socket set to wildcard address 0.0.0.0 which binds the users to the IPs of every local interface
			app.socket = new DatagramSocket(app.localPort);

		} catch (Exception e) {
			e.printStackTrace();
		}
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
			
		} else if(event.getSource() == inputTextField){
			// Enter was pressed
			sendMessage();
		}
	}

	// The function sendMessage will take the message from inputTextField and put it in the outgoing queue
	// the message sender will take on from that point.
	// Also it will display the message in the text area
	private void sendMessage() {
		String message = inputTextField.getText();
			if(!message.isEmpty()){ // send only if there is something written in input field
				try {
					outgoingMessages.put(message); // put message to the queue
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

		/*THE CALLING PROCESS
	 * 1) The peer initiating the call sends a speciall call initiation message "CALL_REQUEST" to the other peer
	 * 2) The receiver responds with a call acknowledgment message to confirm they are ready "CALL_ACCEPTED"
	 * 	  or else they can send a call rejection message if they don't want to accept the call "CALL_REJECTED"
	 * 3) Once the call is accepted both peers start the voice sender thread to transmit audio
	 * 4) Both peers start exchanging audio through the UDP socket, while they can still send messages
	 * 5) Call ending: when either peer decides to end the call they send a call termination message "CALL_END"
	 *    to notify the other peer
	 * 	  Both peers stop their voice sending threads and release audio resources
	*/
	// Sending a call request
	/* To be implemented
	void initiateCall() throws IOException {
		String callRequest = "CALL_REQUEST";
		byte[] data = callRequest.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
		socket.send(packet);
		textArea.append("Calling peer...");
	}

	void acceptCall() throws IOException {
		String callAccept = "CALL_ACCEPT";
		byte[] data = callAccept.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
		socket.send(packet);
		textArea.append("Call accepted.");
		new Thread(new VoiceSenderThread(socket, peerAddress, peerPort)).start();; // Set up audio resources
	}

	void rejectCall() throws IOException {
		String callRejected = "CALL_REJECTED";
		byte[] data = callRejected.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length, peerAddress, peerPort);
		socket.send(packet);
	}
	
	public static void handleIncomingCallRequest() {
		// Show accept/reject buttons
		// Disable the call button
		int response = JOptionPane.showConfirmDialog(frame, "Incoming call request. Accept?", "Call Request", JOptionPane.YES_NO_OPTION);
		if(response == JOptionPane.YES_OPTION) { // if the user accepts the incoming call
			try {
				// Accept call
				textArea.append("Accepted call" + newline);
				acceptCall();
			} catch (IOException e) {
				textArea.append("Error accepting call: " + e.getMessage() + newline);
			}
		} else {
			try {
				// Reject call
				textArea.append("Rejected call" + newline);
				rejectCall();
			} catch (IOException e) {
				textArea.append("Error rejecting call: " + e.getMessage() + newline);
			}
		}
	}

	public static void endCall() {
		// Reset call UI state
		textArea.append("Call ended." + newline);
	}

	public static void playVoiceData(byte[] audioData) {
		// Implement audio playback using SourceDataLine
	}
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
