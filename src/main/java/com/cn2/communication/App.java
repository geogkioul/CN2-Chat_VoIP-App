package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;

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

	private Thread messageSenderThread; // The thread that will handle message sending
	private Thread receiverThread; // The thread that will handle receiving packets

	private boolean isOnCall = false; // a boolean to keep track of call status: set true if user is on a call
	// When a call starts set isOnCall = true and start the audio sender thread
	// When the call ends set isOnCall = false and stop the audio sender thread
	
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

		
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.
	 */
	public static void main(String[] args){
	
		/*
		 * 1. Create the app's window
		 */
		App app = new App("Computer Networks 2 - Chat & VoIP App");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);				  

		/*
		 * 2. 
		 */
		// Prompt the user to define network parameters
		try {
			String ipInput = JOptionPane.showInputDialog("Please enter the IP address of the peer:");
			app.peerAddress = InetAddress.getByName(ipInput); // assign the peer address as defined by the user
			app.localPort = Integer.parseInt(JOptionPane.showInputDialog("Please enter the local port you want to use:"));
			app.peerPort = Integer.parseInt(JOptionPane.showInputDialog("Please specify the peer's port:"));

			// Create the local socket. IP of socket set to wildcard address 0.0.0.0 which binds the users to the IPs of every local interface
			app.socket = new DatagramSocket(app.localPort);

			// Start the receiver and message sender threads
			new Thread(new ReceiverThread(app.socket)).start();
			new Thread(new MessageSenderThread(app.socket, app.peerAddress, app.peerPort)).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
	

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			
			// The "Send" button was clicked
			
			// TODO: Your code goes here...
		
			
		}else if(e.getSource() == callButton){
			
			// The "Call" button was clicked
			
			// TODO: Your code goes here...
			
			
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
