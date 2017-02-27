import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.*;

public class ClientHandler implements Runnable {
	private byte[] receiveData;
	private double plp;
	private long t;
	private InetAddress clientIP;
	private int size;
	private PrintWriter logger;

	public ClientHandler(byte[] receiveData, int size, double plp, long timeOutInMillis, InetAddress clientIP) {
		this.receiveData = receiveData;
		this.size = size;
		this.plp = plp;
		t = timeOutInMillis;
		this.clientIP = clientIP;
	}

	public void run() {
		try {
			InetAddress addr = InetAddress.getByName(TCPUtils.INADDR_ANY);
			DatagramSocket socket = new DatagramSocket(TCPUtils.ANY_AVAIL_PORT, addr);
			logger = new PrintWriter("Log_" + socket.getLocalPort() + ".txt");
			long start = System.currentTimeMillis();
			sendSAW(socket);
			long end = System.currentTimeMillis();
			logger.println("Time : "+(end - start));
			socket.close();
			logger.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sendSAW(DatagramSocket socket) throws IOException {
		socket.setSoTimeout((int) t);
		String sentence = new String(receiveData, 0, size);
		File f = new File(TCPUtils.getData(sentence));
		long fileSize = f.length();
		short destPort = TCPUtils.getSrcPort(receiveData);
		short srcPort = (short) socket.getLocalPort();
		int seqn = 0;
		sentence = String.valueOf(fileSize);
		byte[] toSend = TCPUtils.encapsulate(sentence, srcPort, destPort, seqn);
		byte[] toReceive = new byte[510];
		InputStream fis = new FileInputStream(f);
		int packetNum = 1;
		int resends = 0;
		while (true) {
			boolean flag = false;
			while (true) {
				DatagramPacket data = new DatagramPacket(toSend, toSend.length, clientIP, destPort);
				if (!TCPUtils.isToDrop(plp)) {
					socket.send(data);
					if (flag) {
						resends++;
						logger.println("Resend: Packet num = " + packetNum);
					} else {
						logger.println("Send: Packet num = " + packetNum);
					}
				} else {
					logger.println("Loss: Packet num = " + packetNum);
				}
				DatagramPacket getack = new DatagramPacket(toReceive, toReceive.length);
				try {
					socket.receive(getack);
					int tempSum = TCPUtils.checkSum(new byte[0], TCPUtils.getSrcPort(toReceive),
							TCPUtils.getDestPort(toReceive), seqn);
					if (tempSum == TCPUtils.getCheckSum(toReceive) && seqn == TCPUtils.getSeqNum(toReceive)
							&& getack.getLength() == 9) {
						seqn ^= 1;
						packetNum++;
						break;
					}
				} catch (SocketTimeoutException e) {
					flag = true;
					logger.println("Time out: Packet num = " + packetNum);
					continue;
				}
			}
			toSend = new byte[510];
			int a = fis.read(toSend, 0, 500);
			if (a == -1)
				break;
			sentence = new String(toSend, 0, a);
			toSend = TCPUtils.encapsulate(toSend, a, srcPort, destPort, seqn);
		}
		logger.println("Number of resends = "+resends);
		fis.close();
	}

}
