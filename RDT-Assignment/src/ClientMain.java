import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ClientMain {
	public static void main(String[] args) throws FileNotFoundException {

		Scanner sc = new Scanner(new FileInputStream(new File("client.in")));
		// System.out.printf("%s %d %d %s",sc.nextLine(),sc.nextInt(),
		// sc.nextInt(),sc.nextLine()+sc.nextLine());
		String ip = sc.nextLine();
		short sport = sc.nextShort();
		short port = sc.nextShort();
		String fn = sc.nextLine() + sc.nextLine();
		int wndSz = sc.nextInt();
		long seed = sc.nextLong();
		double plp = sc.nextDouble();
//		SRClientSide srcs = new SRClientSide(ip, sport, port, wndSz,seed,plp);
//		srcs.start(fn);
		GBNClientSide srcs = new GBNClientSide(ip, sport, port,wndSz);
		srcs.start(fn);
//		ClientSide cs = new ClientSide(ip, sport, port);
//		cs.start(fn);
		sc.close();
	}

}
