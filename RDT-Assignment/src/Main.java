import java.util.Scanner;


public class Main {
	int x;

	public static void main(String[] args) {
		Scanner scan=new Scanner(System.in);
		int candidate=scan.nextInt();
		int city=scan.nextInt();
		int[][] elections=new int[city][candidate];
		int[] cityResult=new int[city];
		for (int i = 0; i < elections.length; i++) {
			for (int j = 0; j < elections[i].length; j++) {
				elections[i][j]=scan.nextInt();
			}
		}
		
		
		for (int i = 0; i < elections.length; i++) {
			int max=elections[i][0];
			cityResult[i]=1;
			for (int j = 0; j < elections[i].length; j++) {
				if(elections[i][j]>max){
					max=elections[i][j];
					cityResult[i]=j+1;
				}
				
			}
		}
		int []finalResult=new int[candidate];
		// second stage
		int winner=0;
		for (int i = 0; i < cityResult.length; i++) {
			finalResult[cityResult[i]-1]++;
			if(finalResult[cityResult[i]-1]>winner){
				winner=finalResult[cityResult[i]-1];
			}
		}
		// winner
		for (int i = 0; i < finalResult.length; i++) {
			if(finalResult[i]==winner){
				System.out.println(i+1);
				break;
			}
		
		
		}
		
		
	}
}
