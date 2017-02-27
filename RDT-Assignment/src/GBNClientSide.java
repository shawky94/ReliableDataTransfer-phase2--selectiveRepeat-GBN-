import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class GBNClientSide {
	private String serverIp;
	private short serverPort, port;
	private DatagramSocket socket;
	private int wndSize;
	

	public GBNClientSide(String serverIp, short serverPort, short port, int wndSize) {
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		this.port = port;
		this.wndSize = wndSize;
		
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
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void receiverWork(String fileName) throws IOException {
		FileOutputStream out = new FileOutputStream("Client/" + fileName);
		byte[] toReceive = new byte[550];
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
			if (tempSum == TCPUtils.getCheckSum(toReceive) && tempSeq == seqn) {
				if ( rcvPacket.getLength() == TCPUtils.getLength(toReceive)) {

					int y = TCPUtils.getLength(toReceive) - 12;
					x += y;
					if (seqn == tempSeq) {
						out.write(TCPUtils.getData(toReceive, y));
						seqn = (seqn + 1) % TCPUtils.mod ;

					}

				}
				toSend = TCPUtils.encapsulate("", srcPort, destPort, tempSeq);
				data = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), dPort);
				socket.send(data);
				toReceive = new byte[550];
			}
		}
		out.close();
	}
}
