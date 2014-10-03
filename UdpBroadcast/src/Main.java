import java.awt.Font;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Main {

	final static int WAIT_TIME = 2000;
	final static int UPDATE_WAIT_TIME = 100;
	final static int PORT = 7777;
	final static String NAME = "Borys Minaiev";
	final static int SECONDS_WAIT = 20;
	static int myIP = 0;

	static ArrayList<Connection> allConnections = new ArrayList<>();

	private static void printInformation() {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Udp Example");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		JTextArea textArea = new JTextArea();
		frame.add(textArea);
		Font font = new Font("Verdana", Font.PLAIN, 18);
		textArea.setFont(font);
		textArea.setEditable(false);
		while (true) {
			synchronized (allConnections) {
				for (int i = 0; i < allConnections.size(); i++) {
					Connection cur = allConnections.get(i);
					cur.updateLost();
					if (cur.messages.size() == 0) {
						allConnections.remove(i);
						i--;
					}
				}
				Collections.sort(allConnections);
				textArea.setText("some statistics:\n");
				long currentTime = System.currentTimeMillis();
				for (Connection c : allConnections) {
					textArea.append(c.ip + " " + c.mac + " " + c.name + " "
							+ (currentTime - c.messages.getLast())
							+ "ms last; " + c.lost + " lost");
				}
			}

			try {
				Thread.sleep(UPDATE_WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void runServer(byte[] mac, byte[] ip) {
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(PORT);
		} catch (SocketException e1) {
			System.out.println("error creating socket on port: " + PORT);
		}
		new Thread(new Sender(mac, ip, socket)).start();
		new Thread(new Receiver(socket)).start();
		printInformation();
	}

	public static void main(String[] args) {
		InetAddress ip;
		try {
			ip = InetAddress.getLocalHost();
			NetworkInterface network = NetworkInterface.getByInetAddress(ip);
			byte[] mac = network.getHardwareAddress();
			myIP = Receiver.genIP(ip.getAddress());
			runServer(mac, ip.getAddress());
		} catch (UnknownHostException e) {

			e.printStackTrace();

		} catch (SocketException e) {

			e.printStackTrace();

		}
	}
}