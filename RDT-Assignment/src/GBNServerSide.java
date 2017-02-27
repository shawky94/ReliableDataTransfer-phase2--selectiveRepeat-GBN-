import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class GBNServerSide implements Runnable {
	private int size, wndSize;
	private BlockingQueue<DatagramPacket> wnd;
	private long timeOut, start;
	private DatagramSocket socket;
	private byte[] receiveData;
	private double plp;
	private InetAddress clientIP;
	private Timer timer;
	private TimerTask taskArr;
	private boolean fileFinished = false, acked[];
	private boolean begin = true;
	private PrintWriter pw;

	public GBNServerSide(byte[] receiveData, int size, double plp, long timeOut, InetAddress clientIP, int wndSz) {
		wnd = new LinkedBlockingQueue<DatagramPacket>(wndSz);
		timer = new Timer();
		//taskArr = new TimerTask();
		this.wndSize = wndSz;
		this.timeOut = timeOut;
		this.receiveData = receiveData;
		this.size = size;
		this.plp = plp;
		this.clientIP = clientIP;
		this.acked = new boolean[TCPUtils.mod];
		
	}
	
	private class ReceiverWork implements Runnable {

		@Override
		public void run() {
			byte[] toReceive = new byte[550];
			while (!fileFinished || !wnd.isEmpty()) {
				DatagramPacket getack = new DatagramPacket(toReceive, toReceive.length);
				try {
					
					socket.receive(getack);
					
				} catch (IOException e) {
					e.printStackTrace();
				}

				int tempSeq = TCPUtils.getSeqNum(toReceive), tempSum = TCPUtils.checkSum(new byte[0],
				TCPUtils.getSrcPort(toReceive), TCPUtils.getDestPort(toReceive), tempSeq);
				
				if (tempSum == TCPUtils.getCheckSum(toReceive) && getack.getLength() == 12) {
					
					if(wnd.peek()==null){
						continue;
					}
					int sndBase = TCPUtils.getSeqNum(wnd.peek().getData());
					
					
					if (sndBase == tempSeq) {
						
						
						try {
							wnd.take();
							taskArr.cancel();
							reintializeTimerTask();
							
							while (acked[(sndBase = (sndBase + 1) % TCPUtils.mod)]) {
								wnd.take();
								acked[sndBase] = false;
								
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} 
					else
						acked[tempSeq] = true;
				}

			}
			socket.close();
			System.out.println("Time = "+(System.currentTimeMillis() - start));
			taskArr.cancel();
			pw.close();
		}

	}
	@Override
	public void run(){

		
		try {
			pw = new PrintWriter(new File("Log"));
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

	private void resend() throws IOException {
		
		for (DatagramPacket dp : wnd) {
			if (!TCPUtils.isToDrop(plp)){
				pw.println("resend");
				socket.send(dp);
				
			}
		
		}
	}
	
	private void senderWork() throws IOException, InterruptedException{
		String sentence = new String(receiveData, 0, size);
		File f = new File(TCPUtils.getData(sentence));
		long fileSize = f.length();
		short destPort = TCPUtils.getSrcPort(receiveData);
		short srcPort = (short) socket.getLocalPort();
		int seqn = 0;
		sentence = String.valueOf(fileSize);
		byte[] toSend = TCPUtils.encapsulate(sentence, srcPort, destPort, seqn);
		FileInputStream fis = new FileInputStream(f);
		while (true) {
			DatagramPacket data = new DatagramPacket(toSend, toSend.length, clientIP, destPort);
			wnd.put(data);
			
			
			if(begin){
				begin=false;
			reintializeTimerTask();
			}
			
			
			if (!TCPUtils.isToDrop(plp)){
				
				socket.send(data);
				
			}
			
			seqn = (seqn + 1) % TCPUtils.mod;
			toSend = new byte[550];
			int a = fis.read(toSend, 0, 500);
			fileFinished = a == -1;
			if (fileFinished)
				break;
			toSend = TCPUtils.encapsulate(toSend, a, srcPort, destPort, seqn);
			
		}
		
		fis.close();
	}
	private void reintializeTimerTask(){
		taskArr = new TimerTask() {
			public TimerTask setSeq() {
				
				return this;
			}

			@Override
			public void run() {
				try {
					resend();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.setSeq();
		timer.scheduleAtFixedRate(taskArr, timeOut, timeOut);
	}

}
