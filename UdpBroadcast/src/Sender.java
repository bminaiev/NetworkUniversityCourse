import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

class Sender implements Runnable {
	byte[] mac;
	byte[] ip;
	DatagramSocket socket;

	Sender(byte[] mac, byte[] ip, DatagramSocket socket) {
		this.mac = mac;
		this.ip = ip;
		this.socket = socket;
	}
	
	private static byte[] createMessage(byte[] mac, byte[] ip) {
		byte[] result = new byte[4 + 6 + Main.NAME.length() + 1];
		for (int i = 0; i < ip.length; i++) {
			result[i] = ip[i];
		}
		for (int i = 0; i < mac.length; i++) {
			result[ip.length + i] = mac[i];
		}
		for (int i = 0; i < Main.NAME.length(); i++) {
			result[ip.length + mac.length + i] = (byte) Main.NAME.charAt(i);
		}
		return result;
	}


	@Override
	public void run() {
		final byte[] myMessage = createMessage(mac, ip);
		while (true) {
			DatagramPacket packet = null;
			try {
				packet = new DatagramPacket(myMessage, myMessage.length,
						InetAddress.getByName("255.255.255.255"), Main.PORT);
			} catch (UnknownHostException e2) {
				e2.printStackTrace();
			}
			try {
				socket.send(packet);
			} catch (IOException e1) {
				System.out.println("error sending a message");
			}
			try {
				Thread.sleep(Main.WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}