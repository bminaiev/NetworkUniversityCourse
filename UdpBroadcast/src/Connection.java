import java.util.ArrayDeque;
import java.util.Deque;

class Connection implements Comparable<Connection> {
	String mac;
	String name;
	String ip;
	int lost;
	Deque<Long> messages;
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
			long last = messages.getFirst();
			if (last > currentTime - 1000 * Main.SECONDS_WAIT) {
				break;
			} else {
				messages.pollFirst();
			}
		}
	}

	public void updateLost() {
		removeAllUnused();
		int need = (int) Math.min(10, (System.currentTimeMillis() - startTime)
				/ Main.WAIT_TIME);
		lost = Math.max(0, need - messages.size());
	}

	@Override
	public int compareTo(Connection o) {
		updateLost();
		o.updateLost();
		return Integer.compare(lost, o.lost);
	}
}
