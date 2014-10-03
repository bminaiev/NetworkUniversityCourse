import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

class Receiver implements Runnable {
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

	static int genIP(byte[] ip) {
		int result = 0;
		for (int i = 0; i < ip.length; i++) {
			result = result << 8;
			result += ip[i];
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
		if (genIP(ip.getAddress()) == Main.myIP) {
			return;
		}
		byte[] mac = subArray(message, 4, 10);
		int last = 10;
		while (message[last] != 0) {
			last++;
		}
		String name = new String(subArray(message, 10, last));
		String sIp = (ip.toString().indexOf('/') == -1 ? ip.toString() : ip
				.toString().substring(ip.toString().indexOf('/') + 1));
		synchronized (Main.allConnections) {
			for (Connection c : Main.allConnections) {
				if (c.ip.equals(sIp)) {
					c.addMessage(macToString(mac), name, sIp);
					return;
				}
			}
			Connection c = new Connection();
			c.addMessage(macToString(mac), name, sIp);
			Main.allConnections.add(c);
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
}