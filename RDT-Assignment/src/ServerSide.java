import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSide {
	private short port;
	private long timeOutInMillis;
	private double plp;
	private ExecutorService executor = Executors.newCachedThreadPool();
	private int maxWnd;
	public ServerSide(short port,int maxSlidingwindow, long seed, double plp, long timeOutInMillis) {
		this.port = port;
		this.timeOutInMillis = timeOutInMillis;
		this.plp = plp;
		this.maxWnd = maxSlidingwindow;
		TCPUtils.setRandSeed(seed);
	}

	public void run() {
		DatagramSocket socket = null;
		try {
			InetAddress addr = InetAddress.getByName(TCPUtils.INADDR_ANY);
			socket = new DatagramSocket(port, addr);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while (true) {
			try {
				byte[] receiveData = new byte[515];
				DatagramPacket requestPacket = new DatagramPacket(receiveData, receiveData.length);
				socket.receive(requestPacket);
				// Debuging
				String sentence = new String(receiveData, 0, requestPacket.getLength());
				System.out.println("RECEIVED: " + TCPUtils.getSrcPort(receiveData)+ " " +TCPUtils.getDestPort(receiveData)+" "
						+TCPUtils.getLength(receiveData)+ " " + TCPUtils.getCheckSum(receiveData)+ " " + TCPUtils.getSeqNum(receiveData)+ " "+ TCPUtils.getData(sentence) );
				/*******************************************/

//				ClientHandler handler = new ClientHandler(receiveData, requestPacket.getLength(),plp,timeOutInMillis,requestPacket.getAddress());
				GBNServerSide handler = new GBNServerSide(receiveData, requestPacket.getLength(),plp,timeOutInMillis,requestPacket.getAddress(), maxWnd);
		//		SRServerSide handler = new SRServerSide(receiveData, requestPacket.getLength(),plp,timeOutInMillis,requestPacket.getAddress(), maxWnd);
				executor.submit(handler);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public int getPort() {
		return port;
	}

	public void setPort(short port) {
		this.port = port;
	}

	public long getTimeOutInMillis() {
		return timeOutInMillis;
	}

	public void setTimeOutInMillis(long timeOutInMillis) {
		this.timeOutInMillis = timeOutInMillis;
	}

	public double getPlp() {
		return plp;
	}

	public void setPlp(double plp) {
		this.plp = plp;
	}

}
