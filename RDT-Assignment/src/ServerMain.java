import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ServerMain {
	
	public static void main(String[] args) throws FileNotFoundException{
		Scanner sc = new Scanner(new FileInputStream(new File("server.in")));
		new ServerSide(sc.nextShort(), sc.nextInt(), sc.nextLong(), sc.nextDouble(), 20).run();
		sc.close();
	}

}
