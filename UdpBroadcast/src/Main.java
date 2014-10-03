import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.TreeSet;

public class Main {

	final static int WAIT_TIME = 2000;
	final static int UPDATE_WAIT_TIME = 100;
	final static int PORT = 7777;
	final static String NAME = "Borys Minaiev";
	final static int SECONDS_WAIT = 20;
	static int myIP = 0;

	static class Connection implements Comparable<Connection> {
		String mac;
		String name;
		String ip;
		private int lost;
		private Deque<Long> messages;
		private long startTime;

		Connection() {
			messages = new ArrayDeque<>();
			startTime = System.currentTimeMillis();
		}

		void addMessage(String mac, String name, String ip) {
			long currentTime = System.currentTimeMillis();
			removeAllUnused();
			messages.add(currentTime);
			this.mac = mac;
			this.name = name;
			this.ip = ip;
		}

		private void removeAllUnused() {
			long currentTime = System.currentTimeMillis();
			while (messages.size() > 0) {
				long last = messages.pollFirst();
				if (last > currentTime - 1000 * SECONDS_WAIT) {
					messages.add(last);
					break;
				}
			}
		}

		public void updateLost() {
			removeAllUnused();
			int need = (int) Math.min(10,
					(System.currentTimeMillis() - startTime) / WAIT_TIME);
			lost = Math.max(0, need - messages.size());
		}

		@Override
		public int compareTo(Connection o) {
			updateLost();
			o.updateLost();
			return Integer.compare(lost, o.lost);
		}
	}

	static class Sender implements Runnable {
		byte[] mac;
		byte[] ip;
		DatagramSocket socket;

		Sender(byte[] mac, byte[] ip, DatagramSocket socket) {
			this.mac = mac;
			this.ip = ip;
			this.socket = socket;
		}

		@Override
		public void run() {
			final byte[] myMessage = createMessage(mac, ip);
			while (true) {
				DatagramPacket packet = null;
				try {
					packet = new DatagramPacket(myMessage, myMessage.length,
							InetAddress.getByName("255.255.255.255"), PORT);
				} catch (UnknownHostException e2) {
					e2.printStackTrace();
				}
				try {
					socket.send(packet);
				} catch (IOException e1) {
					System.out.println("error sending a message");
				}
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static class Receiver implements Runnable {
		DatagramSocket socket;

		Receiver(DatagramSocket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			while (true) {
				waitForMessages(socket);
			}
		}

	}

	private static int genIP(byte[] ip) {
		int result = 0;
		for (int i = 0; i < ip.length; i++) {
			result = result << 8;
			result += ip[i];
		}
		return result;
	}

	private static byte[] createMessage(byte[] mac, byte[] ip) {
		byte[] result = new byte[4 + 6 + NAME.length() + 1];
		for (int i = 0; i < ip.length; i++) {
			result[i] = ip[i];
		}
		for (int i = 0; i < mac.length; i++) {
			result[ip.length + i] = mac[i];
		}
		for (int i = 0; i < NAME.length(); i++) {
			result[ip.length + mac.length + i] = (byte) NAME.charAt(i);
		}
		return result;
	}

	private static String macToString(byte[] mac) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
			result.append(String.format("%02X%s", mac[i],
					(i < mac.length - 1) ? ":" : ""));
		}
		return result.toString();
	}

	private static byte[] subArray(byte[] a, int from, int to) {
		byte[] result = new byte[to - from];
		for (int i = 0; i < result.length; i++) {
			result[i] = a[from + i];
		}
		return result;
	}

	private static void decodeMessage(byte[] message) {
		if (message.length < 10)
			return;
		InetAddress ip = null;
		try {
			ip = InetAddress.getByAddress(Arrays.copyOf(message, 4));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (genIP(ip.getAddress()) == myIP) {
			return;
		}
		byte[] mac = subArray(message, 4, 10);
		int last = 10;
		while (message[last] != 0) {
			last++;
		}
		String name = new String(subArray(message, 10, last));
		String sIp = ip.toString().substring(1);
		synchronized (allConnections) {
			for (Connection c : allConnections) {
				if (c.ip.equals(sIp)) {
					c.addMessage(macToString(mac), name, sIp);
					return;
				}
			}
			Connection c = new Connection();
			c.addMessage(macToString(mac), name, sIp);
			allConnections.add(c);
		}

	}

	private static void waitForMessages(DatagramSocket socket) {
		final int bufLength = 1 << 10;
		byte[] buf = new byte[bufLength];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			System.out.println("fail ");
		}
		decodeMessage(packet.getData());
	}

	static ArrayList<Connection> allConnections = new ArrayList<>();

	private static void printInformation() {
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
				System.out.println("---------------------------");
				long currentTime = System.currentTimeMillis();
				for (Connection c : allConnections) {
					System.out.println(c.ip + " " + c.mac + " " + c.name + " "
							+ (currentTime - c.messages.getLast()) + "ms last; "
							+ c.lost + " lost");
				}
				System.out.println("---------------------------");
			}

			try {
				Thread.sleep(UPDATE_WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("resource")
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
			myIP = genIP(ip.getAddress());
			runServer(mac, ip.getAddress());
		} catch (UnknownHostException e) {

			e.printStackTrace();

		} catch (SocketException e) {

			e.printStackTrace();

		}
	}
}