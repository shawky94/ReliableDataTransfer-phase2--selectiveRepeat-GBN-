import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientSide {
	String serverIp;
	short serverPort, port;
	public ClientSide(String serverIp, short serverPort, short port) {
		super();
		this.serverIp = serverIp;
		this.serverPort = serverPort;
		this.port = port;
	}
	
	public void start(String fileName){
		try {
			
			DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName(TCPUtils.INADDR_ANY));
			byte[] toSend = TCPUtils.encapsulate(fileName,port,serverPort,0);
			DatagramPacket packet = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), serverPort);
			socket.send(packet);
			//Recieving Code is here 
			receiveSAW(socket,fileName);
			socket.close();
		} catch (SocketException  e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch ( UnknownHostException e )
		{
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void receiveSAW(DatagramSocket socket, String fileName) throws IOException
	{
		
		FileOutputStream out = new FileOutputStream("Client/"+fileName);
		byte[] toReceive = new byte[510];
		DatagramPacket rcvPacket = new DatagramPacket(toReceive, toReceive.length);
        socket.receive(rcvPacket);
		String sentence = new String(TCPUtils.getData(rcvPacket.getData(),rcvPacket.getLength()-9));
		long sz =Long.valueOf(sentence).longValue();
		int seqn =0;
		short destPort = TCPUtils.getSrcPort(toReceive);
		int dPort = destPort;
		if(dPort<0)
			dPort+=(1<<16);
		short srcPort = TCPUtils.getDestPort(toReceive);
		byte[] toSend = TCPUtils.encapsulate("",srcPort,destPort,seqn);
		DatagramPacket data = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), dPort);
		socket.send(data);
		seqn = 1;
		long x = 0;
		while(x < sz)
		{
			rcvPacket = new DatagramPacket(toReceive, toReceive.length);
	        socket.receive(rcvPacket);
	        sentence = new String(toReceive, 0, rcvPacket.getLength());
	        int tempSum= TCPUtils.checkSum(TCPUtils.getData(toReceive, rcvPacket.getLength()-9), TCPUtils.getSrcPort(toReceive), TCPUtils.getDestPort(toReceive),seqn);
	        if(tempSum == TCPUtils.getCheckSum(toReceive) && seqn == TCPUtils.getSeqNum(toReceive)&&rcvPacket.getLength() ==TCPUtils.getLength(toReceive) )
	        {
	        	toSend = TCPUtils.encapsulate("",srcPort,destPort,seqn);
	        	seqn^=1;
	        	int y = TCPUtils.getLength(toReceive)-9;
	        	x+=y;
	        	out.write(TCPUtils.getData(toReceive, y));
	        }
    		data = new DatagramPacket(toSend, toSend.length, InetAddress.getByName(serverIp), dPort);
    		socket.send(data);
		}
		out.close();
		
	}
	
	
	

}
