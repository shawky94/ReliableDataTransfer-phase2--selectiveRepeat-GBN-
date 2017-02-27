import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


/**
 * 
 */

/**
 * @author youssef
 *
 */
public class TCPUtils {
	public static final String INADDR_ANY = "0.0.0.0";
	public static final int ANY_AVAIL_PORT = 0;
	private static Random rand;
	public static final int mod = 10000;

	public static void setRandSeed(long seed) {
		rand = new Random(seed);
	}

	public static boolean isToDrop(double plp) {
		return rand.nextDouble() < plp;
	}
	public static byte[] encapsulate(String data,short sPort ,short dPort, int seqNum)
	{
		byte temp[] = new byte[data.length()+12];
		convertTobyte(0,sPort,temp);
		convertTobyte(2,dPort,temp);
		convertTobyte(4,data.length()+12,temp);
		convertTobyte(6 , checkSum(data.getBytes(),sPort,dPort,seqNum),temp);
		convertToInt(8,seqNum,temp);
		byte x[]=data.getBytes();
		for(int i =  0 ; i < x.length;i++)
			temp[i+12] = x[i];
		
		return temp;
	}
	public static byte[] encapsulate(byte[] data,int len,short sPort ,short dPort, int seqNum)
	{
		byte temp[] = new byte[len+12];
		convertTobyte(0,sPort,temp);
		convertTobyte(2,dPort,temp);
		convertTobyte(4,len+12,temp);
		byte toSend[] = new byte[len];
		for(int i = 0 ;i < len;i++)
			toSend[i]=data[i];
		convertTobyte(6 , checkSum(toSend,sPort,dPort,seqNum),temp);
		convertToInt(8,seqNum,temp);
		for(int i =  0 ; i < len;i++)
			temp[i+12] = data[i];
		
		return temp;
	}
	public static void convertToInt(int ind , int toConvert , byte[] temp)
	{
		temp[ind] = (byte)(toConvert & ((1<<8)-1) );
		temp[ind+1] = (byte)((toConvert>>8) & ((1<<8)-1) );
		temp[ind+2] = (byte)((toConvert>>16) & ((1<<8)-1) );
		temp[ind+3] = (byte)((toConvert>>24) & ((1<<8)-1) );
		return;
	}
	public static void convertTobyte(int ind, int toConvert, 	byte[] temp)
	{
		temp[ind] = (byte)(toConvert & ((1<<8)-1) );
		temp[ind+1] = (byte)((toConvert>>8) & ((1<<8)-1) );
		return;
	}
//	public static short checkSum(String data,short sPort ,short dPort, int seqNum)
//	{
//		return 0;
//	}
	public static short checkSum(byte[] data,short sPort ,short dPort, int seqNum) {
		
		//selecting a decoding hash algorithm
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 0;
		}
		//adding all data which we want to validate
		md.update(data);
		
		md.update(convertShortToByteArray(sPort));
		md.update(convertShortToByteArray(dPort));
		md.update(convertIntToByteArray(seqNum));
		
		//hashing the data into 40 char code
		 byte[] mdbytes = md.digest(); 
		 StringBuffer sb = new StringBuffer("");
		 for (int i = 0; i < mdbytes.length; i++) {
		   	sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
		    
		String hashedValue = sb.toString();    
		//System.out.println("Digest(in hex format):: " + sb.toString());
		
		
		//partioning the hashvalue int 2 bytes strings
		ArrayList<String> partitions = new ArrayList<String>();
		for (int i = hashedValue.length(); i > 0  ; i=i-4) {
			partitions.add(hashedValue.substring(i-4, i));
		}

		
		//adding the partitioned strings
		Integer accumilator = 0;
		for (int i = 0; i < partitions.size(); i++) {
			accumilator = accumilator + Integer.parseInt(partitions.get(i),16);
			
		}
		String result = Integer.toHexString(accumilator);
		//in case of over flow
		//wrapping the output sum into 32 bit output
		if(result.length()>4){
			char firstChar= result.charAt(0);
			result=result.substring(1, result.length());
			String finalOutput = String.valueOf(Integer.parseInt(result,16) + Character.getNumericValue(firstChar));
			Integer temp = (Integer.parseInt(finalOutput));
			short res =(short) ~temp ;
			if(res<0){
				res*=-1;
			}
			
			return res;
			
		}
		//in case where no overflow takes place
		else{
			Integer temp = (Integer.parseInt(result,16));
			short res =(short) ~temp ;
			if(res<0){
				res*=-1;
			}
		
		return res;
		}
	}
	static private byte[] convertShortToByteArray(short num){
		byte[] ret = new byte[2];
		ret[0] = (byte)(num & 0xff);
		ret[1] = (byte)((num >> 8) & 0xff);
		return ret;
	}
	static private byte[] convertIntToByteArray(int num){
		byte[] ret = new byte[4];
		ret[0] = (byte)(num & 0xff);
		ret[1] = (byte)((num >> 8) & 0xff);
		ret[2] = (byte)((num >> 16) & 0xff);
		ret[3] = (byte)((num >> 24) & 0xff);
		return ret;
	}
	public static short getSrcPort(byte[] data)
	{
		short x = correct((short)data[0]);
		short y = correct((short)data[1]);
		return (short) ((y<<8)+x);
	}
	public static short getDestPort(byte[] data)
	{
		short x = correct((short)data[2]);
		short y = correct((short)data[3]);
		return (short) ((y<<8)+x);
	}
	public static short getCheckSum(byte[] data)
	{
		short x = correct((short)data[6]);
		short y = correct((short)data[7]);
		return (short) ((y<<8)+x);
	}
	public static short getLength(byte[] data)
	{
		short x = correct((short)data[4]);
		short y = correct((short)data[5]);
		return (short) ((y<<8)+x);
	}
	public static int getSeqNum(byte[] data)
	{
		int x = correct((short)data[8]);
		int y = correct((short)data[9]);
		int z = correct((short)data[10]);
		int t = correct((short)data[11]);
		return (t<<24)+(z<<16)+ (y<<8)+x;
	}
	public static String getData(String data)
	{
		return data.substring(12);
	}
	public static byte[] getData(byte[] data,int len)
	{
		byte temp[] = new byte[len];
		for(int i = 0 ;i < len ;i++)
			temp[i]=data[i+12];
		return temp;
			
	}
	public static short correct(short x)
	{
		if(x < 0)
			x+=256;
		return x;
	}
}
