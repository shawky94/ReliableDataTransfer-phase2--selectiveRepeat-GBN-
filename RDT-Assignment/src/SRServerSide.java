import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SRServerSide implements Runnable {

	private int size, wndSize;
	double cwnd, ssthresh;
	private BlockingQueue<Integer> wnd;
	private long timeOut, start, lastTimeOut, lastReply, lastWrite;
	private DatagramSocket socket;
	private byte[] receiveData;
	private double plp;
	private InetAddress clientIP;
	private Timer timer;
	private TimerTask[] taskArr;
	private boolean fileFinished = false, acked[], state;
	private PrintWriter pw, pw2,pw3;

	private class ReceiverWork implements Runnable {

		@Override
		public void run() {
			byte[] toReceive = new byte[515];
			while (!fileFinished || !wnd.isEmpty()) {
				DatagramPacket getack = new DatagramPacket(toReceive, toReceive.length);
				try {
					if (fileFinished)
						socket.setSoTimeout((int) (5 * timeOut));
					socket.receive(getack);
				} catch (SocketTimeoutException e) {
					System.out.println("Here");
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}

				int tempSeq = TCPUtils.getSeqNum(toReceive), tempSum = TCPUtils.checkSum(new byte[0],
						TCPUtils.getSrcPort(toReceive), TCPUtils.getDestPort(toReceive), tempSeq);
				if (tempSum == TCPUtils.getCheckSum(toReceive) && getack.getLength() == 12) {
					pw.println("Ack: Seq # " + tempSeq);
					taskArr[tempSeq].cancel();
					if (wnd.isEmpty()) {
						continue;
					}
					int sndBase = wnd.peek();
					if (!acked[tempSeq]) {
						if (state) {
							cwnd = cwnd + 1.0 / cwnd;
							printCwnd();
						} else {
							cwnd++;
							if (cwnd > ssthresh || Math.abs(cwnd - ssthresh) <= 1e-9)
								state = true;
							printCwnd();
						}
					}
					lastReply = System.currentTimeMillis();
					if (sndBase == tempSeq) {
						try {
							wnd.take();
							sndBase = (sndBase + 1) % TCPUtils.mod;
							while (acked[sndBase]) {
								wnd.take();
								acked[sndBase] = false;
								sndBase++;
								if (sndBase >= TCPUtils.mod)
									sndBase -= TCPUtils.mod;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else
						acked[tempSeq] = true;
				}

			}
			socket.close();
			printCwnd();
			pw.close();
			pw2.close();
			pw3.close();
			System.out.println("Time = " + (System.currentTimeMillis() - start));
		}

	}

	private void printCwnd() {
		if (System.currentTimeMillis() - lastWrite > timeOut / 2) {
			pw2.println((int) cwnd);
			pw3.println((int) ssthresh);
			lastWrite = System.currentTimeMillis();
		}
		return;
	}

	public SRServerSide(byte[] receiveData, int size, double plp, long timeOut, InetAddress clientIP, int wndSz) {
		wnd = new LinkedBlockingQueue<Integer>(wndSz);
		timer = new Timer();
		taskArr = new TimerTask[TCPUtils.mod];
		this.wndSize = wndSz;
		this.timeOut = timeOut;
		this.receiveData = receiveData;
		this.size = size;
		this.plp = plp;
		this.clientIP = clientIP;
		this.acked = new boolean[TCPUtils.mod];
		this.ssthresh = wndSize;
		this.cwnd = 1;
		this.state = false;
	}

	@Override
	public void run() {
		try {
			pw = new PrintWriter(new File("Log"));
			pw2 = new PrintWriter(new File("Window Size"));
			pw3 = new PrintWriter(new File("ssthershold"));
			start = System.currentTimeMillis();
			socket = new DatagramSocket(0, InetAddress.getByName(TCPUtils.INADDR_ANY));
			new Thread(new ReceiverWork()).start();
			senderWork();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void senderWork() throws IOException, InterruptedException {
		String sentence = new String(receiveData, 0, size);
		File f = new File(TCPUtils.getData(sentence));
		long fileSize = f.length();
		short destPort = TCPUtils.getSrcPort(receiveData);
		short srcPort = (short) socket.getLocalPort();
		int seqn = 0, packn = 0;
		sentence = String.valueOf(fileSize);
		byte[] toSend = TCPUtils.encapsulate(sentence, srcPort, destPort, seqn);
		FileInputStream fis = new FileInputStream(f);

		while (true) {
			if (wnd.size() == wndSize || wnd.size() > cwnd || Math.abs(cwnd - wnd.size()) <= 1e-9)
				continue;
			printCwnd();
			DatagramPacket data = new DatagramPacket(toSend, toSend.length, clientIP, destPort);
			wnd.put(seqn);
			taskArr[seqn] = new TimerTask() {
				private DatagramPacket p;
				private int packn;

				public TimerTask setPacket(DatagramPacket p, int pn) {
					this.p = p;
					packn = pn;
					return this;
				}

				@Override
				public void run() {
					try {
						if (socket.isClosed()) {
							cancel();
							return;
						}

						if (System.currentTimeMillis() - lastTimeOut > timeOut) {
							ssthresh = cwnd / 2.0;
							cwnd = 1;
							state = false;
							lastTimeOut = System.currentTimeMillis();
							printCwnd();
						}
						
						if (!TCPUtils.isToDrop(plp)) {
							pw.println("Resend: Packet# " + packn);
							socket.send(p);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.setPacket(data, packn);
			timer.scheduleAtFixedRate(taskArr[seqn], timeOut, timeOut);
			if (!TCPUtils.isToDrop(plp)) {
				pw.println("Send: Packet# " + packn + ", Seq# " + seqn);
				socket.send(data);
			}
			packn++;
			seqn = (seqn + 1);
			if (seqn >= TCPUtils.mod)
				seqn -= TCPUtils.mod;
			toSend = new byte[515];
			int a = fis.read(toSend, 0, 500);
			fileFinished = a == -1;
			if (fileFinished)
				break;
			toSend = TCPUtils.encapsulate(toSend, a, srcPort, destPort, seqn);
		}
		fis.close();

	}

}
