import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SRClientSide {
	private String serverIp;
	private short serverPort, port;
	private DatagramSocket socket;
	private int wndSize;
	private double plp;
	private byte[][] dataArr;

	public SRClientSide(String serverIp, short serverPort, short port, int wndSize, long seed, double plp) {
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		this.port = port;
		this.wndSize = wndSize;
		dataArr = new byte[TCPUtils.mod][];
		this.plp = plp;
		TCPUtils.setRandSeed(seed);

	}

	public void start(String fileName) {
		try {
			socket = new DatagramSocket(port, InetAddress.getByName(TCPUtils.INADDR_ANY));
			byte[] toSend = TCPUtils.encapsulate(fileName, port, serverPort, 0);
			DatagramPacket packet = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp),
					serverPort);
			socket.send(packet);
			receiverWork(fileName);
			socket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void receiverWork(String fileName) throws IOException {
		FileOutputStream out = new FileOutputStream("Client/" + fileName);
		byte[] toReceive = new byte[515];
		DatagramPacket rcvPacket = new DatagramPacket(toReceive, toReceive.length);
		socket.receive(rcvPacket);
		String sentence = new String(TCPUtils.getData(rcvPacket.getData(), rcvPacket.getLength() - 12));
		long sz = Long.valueOf(sentence).longValue();
		int seqn = 0;
		short destPort = TCPUtils.getSrcPort(toReceive);
		int dPort = destPort;
		if (dPort < 0)
			dPort += (1 << 16);
		short srcPort = TCPUtils.getDestPort(toReceive);
		byte[] toSend = TCPUtils.encapsulate("", srcPort, destPort, seqn);
		DatagramPacket data = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), dPort);
		socket.send(data);
		seqn = 1;
		long x = 0;

		while (x < sz) {
			rcvPacket = new DatagramPacket(toReceive, toReceive.length);
			socket.receive(rcvPacket);
			int tempSeq = TCPUtils.getSeqNum(toReceive),
					tempSum = TCPUtils.checkSum(TCPUtils.getData(toReceive, rcvPacket.getLength() - 12),
							TCPUtils.getSrcPort(toReceive), TCPUtils.getDestPort(toReceive), tempSeq);
			boolean isNew = isNewPacket(tempSeq, seqn), isAcked = isAckedPacket(tempSeq, seqn);
			if (tempSum == TCPUtils.getCheckSum(toReceive) && (isAcked || isNew)) {
				if (dataArr[tempSeq] == null && rcvPacket.getLength() == TCPUtils.getLength(toReceive) && isNew) {

					int y = TCPUtils.getLength(toReceive) - 12;
					x += y;
					if (seqn == tempSeq) {
						out.write(TCPUtils.getData(toReceive, y));
						seqn = (seqn + 1) % TCPUtils.mod;
						while (dataArr[seqn] != null) {
							out.write(dataArr[seqn]);
							dataArr[seqn] = null;
							seqn = (seqn + 1) % TCPUtils.mod;
						}
					} else
						dataArr[tempSeq] = TCPUtils.getData(toReceive, y);
				}
				toSend = TCPUtils.encapsulate("", srcPort, destPort, tempSeq);
				data = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), dPort);
				if (!TCPUtils.isToDrop(plp))
					socket.send(data);
				toReceive = new byte[515];
			}
		}
		out.close();
	}

	public boolean isNewPacket(int seqn, int rcvBase) {
		int end = (rcvBase + wndSize) % TCPUtils.mod;
		for (int i = rcvBase; i != end;)
		{
			if (i == seqn)
				return true;
			i++;
			if(i>=TCPUtils.mod)
				i-=TCPUtils.mod;
		}
		return false;
	}

	public boolean isAckedPacket(int seqn, int rcvBase) {
		int start = (rcvBase + wndSize) % TCPUtils.mod;
		for (int i = start; i != rcvBase; )
		{
			if (i == seqn)
				return true;
			i++;
			if(i>=TCPUtils.mod)
				i-=TCPUtils.mod;
		}
		return false;
	}
}
