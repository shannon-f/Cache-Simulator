package assign2;

import java.util.List;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Scanner;

public class Main {
    public static int accesses = 0;
    public static CacheEntry associativeCache[][];
    public static int associativity;
    public static String addressInBinary;
    public static String addressInHex;
    public static int blockSize;
    public static int numberOfBlocks;
    public static int blockOffsetBits;
    public static String[] cache;
    public static String cacheHit;
    public static int cacheSize;
    public static String curCache = "";
    public static String currentCache;
    public static String fifo_lru;
    public static String hexNoLeadLowerCase;
    public static int hits = 0;
    public static String index;
    public static int indexBits;
    public static int indexInDecimal;
    public static int m;
    public static int misses = 0;
    public static int n;
    public static String offset;
    public static String tag;
    public static String tracing;
   
   
   public static void main(String[] args){
   	
      if (args.length != 6){
	     System.exit(0);
	   }
	  m = Integer.parseInt(args[0]);
	  n = Integer.parseInt(args[1]);
	  if (n <= 0 || m <= 0){
		     System.exit(0);
     }
	  
	  tracing = args[4];
	  if(!(tracing.equals("on")) && !(tracing.equals("off"))){
		  System.exit(0);
	  }
	  associativity = Integer.parseInt(args[2]);
	  if(associativity < 0 || associativity > m-n){
		  associativity = m - n; //make fully associative here
	  }
	  fifo_lru = args[3];
	  if(!(fifo_lru.equalsIgnoreCase("fifo")) &&
			  !(fifo_lru.equalsIgnoreCase("lru"))){
		  System.exit(0);
	  }
	  cacheSize = (int) Math.pow(2, m);
	  blockSize = (int) Math.pow(2, n);
	  associativity = (int) Math.pow(2, associativity);
	  numberOfBlocks = cacheSize / blockSize / associativity; // make sure to fix later by accounting for both direct mapped and associative
	  blockOffsetBits = n;
	  indexBits = (int) (Math.log(numberOfBlocks) / Math.log(2));
	  cache = new String[numberOfBlocks];
	  associativeCache = new CacheEntry[numberOfBlocks][associativity];
	  if(tracing.equalsIgnoreCase("on")){
		  printHeader();
	  }
	  runCache(args[5]);
	  printFooter(args);
	}
   
   public static void runCache(String file){
   	 Scanner scan = null;
   	 String hexPattern = "(0x)(.+)";
   	 try{
		   scan = new Scanner(new File(file));
		   while(scan.hasNext()){
		      String address = scan.nextLine();
		      address = address.trim();
		      if(address.length() > 2 && !(address.substring(0,2).equals("0x"))){
		      	addressInHex = toHex(address);
		      	
		      } else {
		      	addressInHex = address;
		      }
		      addressInHex.matches(hexPattern);
		      hexNoLeadLowerCase =  addressInHex.replaceAll(hexPattern, "$2");
		      hexNoLeadLowerCase = hexNoLeadLowerCase.toLowerCase();
		      addressInBinary = toBinary(hexNoLeadLowerCase);
		      addressInBinary = to32Bit(addressInBinary);
		      
		      offset = addressInBinary.substring(26, addressInBinary.length());
				index = addressInBinary.substring(addressInBinary.length() 
						- (blockOffsetBits + indexBits),
						addressInBinary.length() - blockOffsetBits);
				if (index.equals("")){
					index = "0";
				}
				indexInDecimal = Integer.parseInt(index, 2);
				ArrayList<CacheEntry> cacheEntries = new ArrayList<CacheEntry>();
				for(CacheEntry cacheEntry : associativeCache[indexInDecimal]){
					if(cacheEntry != null){
						cacheEntries.add(cacheEntry);
					}
				}
				Collections.sort((List<CacheEntry>) cacheEntries,
						new Comparator<CacheEntry>() {
					public int compare(CacheEntry o1, CacheEntry o2) {
						if(Integer.parseInt(o1.getTag(), 2) >
								Integer.parseInt(o2.getTag(), 2)){
							return 1;
						} else if (Integer.parseInt(o1.getTag(), 2) <
								Integer.parseInt(o2.getTag(), 2)){
							return -1;
						} else{
							return 0;
						}
					};
				});
		   		for(CacheEntry entry : cacheEntries){
		   			curCache += toHex(Integer.parseInt(entry.getTag(), 2)+"") + ", ";
		   		}
		   	
				tag = getTag();
				currentCache = cache[Integer.parseInt(index, 2)];
				if(associativity == 0){ // direct-mapped
					if(cache[Integer.parseInt(index, 2)] == null){
						misses++;
						cache[Integer.parseInt(index, 2)] = tag;
						cacheHit = "miss";
					} else {
						if(cache[Integer.parseInt(index, 2)].equals(tag)){
							hits++;
							cacheHit = "hit";
						} else {
							misses++;
							cache[Integer.parseInt(index, 2)] = tag;
							cacheHit = "miss";
						}
					}
					
				} else { // n-way associativity
					boolean replaceFlag = true;
					for (int i = 0; i < associativeCache[indexInDecimal].length; i++){
						if(associativeCache[indexInDecimal][i]!=null){
							if(associativeCache[indexInDecimal][i].getTag().equals(tag)){
								if(fifo_lru.equals("lru")){
									associativeCache[indexInDecimal][i].setTimestamp(new Date());
								}
								hits++;
								cacheHit = "hit";
								replaceFlag = false;
								break;
							}
						} 
				   }
					if (replaceFlag){
						cacheHit ="miss";
						misses++;
						CacheEntry entryToReplace = associativeCache[indexInDecimal][0];
						int indexToReplace = 0;
						for (int i = 0; i < associativeCache[indexInDecimal].length; i++){
							if(associativeCache[indexInDecimal][i]== null){
								associativeCache[indexInDecimal][i] = new CacheEntry(new Date(), tag);
								replaceFlag = false;
								break;
							} else {
								if(entryToReplace.getTimestamp().compareTo
										(associativeCache[indexInDecimal][i].getTimestamp()) > 0){
									entryToReplace = associativeCache[indexInDecimal][i];
									indexToReplace = i;
								}
							}
						}
						if (replaceFlag){
							associativeCache[indexInDecimal][indexToReplace].setTag(tag);
							associativeCache[indexInDecimal][indexToReplace].setTimestamp(new Date());
						}						
					}
				}
				accesses++;
		      if(tracing.equalsIgnoreCase("on")){
		      	printInfo();
		      }
		   }
	   } catch(Exception e){
		   e.printStackTrace();
	   }
      finally{
      	scan.close();
      }
   }
   
   
   
   public static String toBinary(String address){
   	String binary = null;
   	binary = new BigInteger(address, 16).toString(2);
   	return binary;
   }
   
   public static int binaryToInt(String binaryString){
   	return Integer.parseInt(binaryString, 2);
   }
   
   public static String getTag(){
   	String tag = null;	
   	tag = addressInBinary.substring(0, addressInBinary.length()  - (blockOffsetBits + indexBits));
   	return tag;
   }
   
   public static String toHex(String address){
   	String hex = null;
   	hex = Integer.toHexString(Integer.parseInt(address));  	
   	return hex;
   }
   
   public static String binaryStringToHex(String binaryString){
   	if(binaryString == ""){
   		return "";
   	}
   	return Integer.toHexString(Integer.parseInt(binaryString, 2));
   }
   
   public static String to32Bit(String binary){
      String thirtyTwoBit = "";
      int i;		
		String zero = "0";
		for(i=1;i <= 32 - binary.length();i++){
			thirtyTwoBit += zero;
		}
		thirtyTwoBit += binary;
   	return thirtyTwoBit;
   }
   
   public static float getMissRatio(){
   	return (float) ((double) misses/ (double) accesses);
   }
   
   public static void printHeader(){
   	System.out.printf("%-8s %-7s %-7s %-7s %-8s %-7s %-7s %13s   %-7s\n", 
   			"Address", "Tag", "Set#", "Hit/Miss", "#Hits", 
   			"#Miss", "#Access", "Miss Ratio", "Cache");
   }
   
   public static void printFooter(String[] args){
   	System.out.println("Shannon Fluellen");
   	System.out.println("Command Line Parameters: " + args[0] + " " + args[1]
   			+ " " + args[2] + " " + args[3] + " " + args[4] + " " + args[5]);
   	System.out.println("hits: " + hits);
   	System.out.println("misses: " + misses);
   	System.out.format("miss ratio: %.8f", getMissRatio());
   }
   public static void printInfo(){
   	String cacheAddress = "";
   	if (currentCache != null){
   		cacheAddress = currentCache;
   	} 
   	
   	System.out.format("%-8s  %-7s %-7s %-8s %-8d %-7d %-7d %13.8f %-7s",
   			hexNoLeadLowerCase, binaryStringToHex(tag),
   			binaryStringToHex(index), cacheHit, hits, 
   			misses, accesses, getMissRatio(), binaryStringToHex(cacheAddress));
   	
   	System.out.println(curCache);
   	curCache = "";
   }  
}


class CacheEntry {
	String tag;
	Date timestamp;
	
	public CacheEntry(Date timestamp, String tag){
		this.timestamp = timestamp;
		this.tag = tag;
	}
	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	
}