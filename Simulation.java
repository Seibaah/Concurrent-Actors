import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Simulation {
	public static int NumThreads=0;
	public static volatile AtomicInteger OutputSize;
	public static ThreadPoolExecutor ThreadPool;
	public static BlockingQueue<Runnable> TasksQueue;
	public static List<GenericActor> Network = new ArrayList<GenericActor>();
	public static List<GenericActor> Schedulers;
	public static volatile boolean ShutdownOrder=false;
	
	public static void main (String[] args) {
		NumThreads=Integer.parseInt(args[0]);
		OutputSize=new AtomicInteger(0);
		
		//network is build once
		BuildNetwork();
		
		//2 files are processed through the network
		for (int i=0; i<4; i++) {
			TasksQueue = new LinkedBlockingDeque<Runnable>();
			
			ThreadPool = new ThreadPoolExecutor(NumThreads, NumThreads, 0, 
					TimeUnit.NANOSECONDS, TasksQueue);
			
			/*
			 * --------------------------------------
			 * MODIFY HERE IF YOU WISH TO RUN YOUR OWN FILES
			 * STICK TO 1 NUMBER PER LINE FORMAT, NO OTHER CHARACTER ADMITTED
			 * --------------------------------------
			 */
			if (i==0) {
				//small sized input: 6 numbers
				Network.get(0).InPath="data0.txt";
				Network.get(Network.size()-1).OutPath="output0.txt";
			} 
			else if (i==1){
				//medium sized input: ~500 numbers
				Network.get(0).InPath="data1.txt";
				Network.get(Network.size()-1).OutPath="output1.txt";
			}
			else if (i==2){
				//big size input: ~1000 numbers
				Network.get(0).InPath="data2.txt";
				Network.get(Network.size()-1).OutPath="output2.txt";
			}
			else if (i==3){
				//big size input: ~4000 numbers
				Network.get(0).InPath="data3.txt";
				Network.get(Network.size()-1).OutPath="output3.txt";
			}
			
			//start timer
			long t0=System.currentTimeMillis();
			
			//start the network
			Start();
			
			//wait for termination
			try {
				ThreadPool.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			//timer end
			long tf=System.currentTimeMillis();
			
			//print speed
			System.out.println("Time: "+(tf-t0));
		}
	}
	
	/*
	 * Network is build through Factory API calls
	 */
	public static void BuildNetwork() {
				
		GenericActor input=Factory.CreateGenericActor("input");
		
		//comparator token generator
		//////////////////////////////////////////
		GenericActor f1=Factory.CreateGenericActor("fork");
		GenericActor m1=Factory.CreateGenericActor("merge");
		GenericActor m2=Factory.CreateGenericActor("merge");
		GenericActor f2=Factory.CreateGenericActor("fork");		
		GenericActor g1=Factory.CreateGenericActor(">");
		GenericActor sw1=Factory.CreateGenericActor("switch");		
		GenericActor f3=Factory.CreateGenericActor("fork");		
		GenericActor z1=Factory.CreateGenericActor("zero");		
		GenericActor dec1=Factory.CreateGenericActor("dec");		
		GenericActor sw2=Factory.CreateGenericActor("switch");
		GenericActor f4=Factory.CreateGenericActor("fork");
		GenericActor m3=Factory.CreateGenericActor("merge");
		
		//input outbound
		Factory.CreateLinkedTokenChannel(input, f1, 0, 0);
		//fork1 outbound
		Factory.CreateLinkedTokenChannel(f1, m1, 0, 2);
		Factory.CreateLinkedTokenChannel(f1, m2, 1, 2);
		//merge1 outbound
		Factory.CreateLinkedTokenChannel(m1, f2, 0, 0);
		//fork2 outbound
		Factory.CreateLinkedTokenChannel(f2, g1, 0, 0);
		Factory.CreateLinkedTokenChannel(f2, sw1, 1, 1);
		//greater1 outbound
		Factory.CreateLinkedTokenChannel(g1, f3, 0, 0);
		//switch1 outbound
		Factory.CreateLinkedTokenChannel(sw1, z1, 1, 0);
		Factory.CreateLinkedTokenChannel(sw1, dec1, 0, 0);
		//dec1 outbound
		Factory.CreateLinkedTokenChannel(dec1, m1, 0, 1);
		//fork3 outbound
		Factory.CreateLinkedTokenChannel(f3, m1, 0, 0);
		Factory.CreateLinkedTokenChannel(f3, m2, 1, 0);
		Factory.CreateLinkedTokenChannel(f3, sw1, 2, 0);
		Factory.CreateLinkedTokenChannel(f3, sw2, 3, 0);
		Factory.CreateLinkedTokenChannel(f3, m3, 4, 0);
		//zero1 needs no outbound TokenChannel
		//merge 2 outbound
		Factory.CreateLinkedTokenChannel(m2, sw2, 0, 1);
		//switch2 outbound
		Factory.CreateLinkedTokenChannel(sw2, f4, 0, 0);
		Factory.CreateLinkedTokenChannel(sw2, m3, 1, 2);
		//fork4 outbound
		Factory.CreateLinkedTokenChannel(f4, m2, 0, 1);
		Factory.CreateLinkedTokenChannel(f4, m3, 1, 1);
		
		//m1 starting false token
		m1.InSockets[0].Set(false);
		//m2 starting false token
		m2.InSockets[0].Set(false);
		
		//series generator
		//////////////////////////////////////
		GenericActor em1=Factory.CreateGenericActor("emit");
		GenericActor ge1=Factory.CreateGenericActor(">=");
		GenericActor m4=Factory.CreateGenericActor("merge");
		GenericActor f5=Factory.CreateGenericActor("fork");		
		GenericActor f6=Factory.CreateGenericActor("fork");
		GenericActor sw3=Factory.CreateGenericActor("switch");		
		GenericActor f7=Factory.CreateGenericActor("fork");		
		GenericActor z2=Factory.CreateGenericActor("zero");
		GenericActor inc1=Factory.CreateGenericActor("inc");
		
		//emit 1 value
		em1.Constant=1;
		
		//emit 1 inbound
		Factory.CreateLinkedTokenChannel(f1, em1, 2, 0);
		//emit 1 outbound
		Factory.CreateLinkedTokenChannel(em1, m4, 0, 2);
		//merge 4 outbound
		Factory.CreateLinkedTokenChannel(m4, f5, 0, 0);
		//fork 5 outbound
		Factory.CreateLinkedTokenChannel(f5, sw3, 0, 1);
		Factory.CreateLinkedTokenChannel(f5, ge1, 1, 1);
		//switch 3 outbound
		Factory.CreateLinkedTokenChannel(sw3, f7, 0, 0);
		Factory.CreateLinkedTokenChannel(sw3, z2, 1, 0);
		//fork 7 outbound part1
		Factory.CreateLinkedTokenChannel(f7, inc1, 0, 0);
		//inc 1 outbound
		Factory.CreateLinkedTokenChannel(inc1, m4, 0, 1);
		//merge 3 outbound
		Factory.CreateLinkedTokenChannel(m3, ge1, 0, 0);
		//greater equal 1 outbound
		Factory.CreateLinkedTokenChannel(ge1, f6, 0, 0);
		//fork 6 outbound
		Factory.CreateLinkedTokenChannel(f6, m4, 0, 0);
		Factory.CreateLinkedTokenChannel(f6, sw3, 1, 0);

		//m1 starting false token
		m4.InSockets[0].Set(false);
		
		//Partial Sums generator
		//////////////////////////////////////
		GenericActor eq1=Factory.CreateGenericActor("=="); //23
		GenericActor add1=Factory.CreateGenericActor("add");
		GenericActor add2=Factory.CreateGenericActor("add");
		GenericActor f8=Factory.CreateGenericActor("fork");		
		GenericActor f9=Factory.CreateGenericActor("fork");
		GenericActor add3=Factory.CreateGenericActor("add");		
		GenericActor m5=Factory.CreateGenericActor("merge");		
		GenericActor sw4=Factory.CreateGenericActor("switch");
		GenericActor em2=Factory.CreateGenericActor("emit");
		GenericActor output=Factory.CreateGenericActor("output");
		
		//equal comparator value
		eq1.Constant=1;
		
		//fork 7 outbound part 2
		Factory.CreateLinkedTokenChannel(f7, eq1, 1, 0);
		Factory.CreateLinkedTokenChannel(f7, add1, 2, 0);
		Factory.CreateLinkedTokenChannel(f7, add1, 3, 1);
		Factory.CreateLinkedTokenChannel(f7, add2, 4, 0);
		//equal 1 outbound
		Factory.CreateLinkedTokenChannel(eq1, f8, 0, 0);
		//fork 8 outbound
		Factory.CreateLinkedTokenChannel(f8, m5, 0, 0);
		Factory.CreateLinkedTokenChannel(f8, sw4, 1, 0);
		//emit 2 outbound
		Factory.CreateLinkedTokenChannel(em2, m5, 0, 1);
		//Merge 5 outbound
		Factory.CreateLinkedTokenChannel(m5, f9, 0, 0);
		//fork 9 outbound
		Factory.CreateLinkedTokenChannel(f9, add3, 0, 0);
		Factory.CreateLinkedTokenChannel(f9, add2, 1, 1);
		//add 1 outbound
		Factory.CreateLinkedTokenChannel(add1, add3, 0, 1);
		//add 3 outbound
		Factory.CreateLinkedTokenChannel(add3, sw4, 0, 1);
		//switch 4 outbound
		Factory.CreateLinkedTokenChannel(sw4, em2, 0, 0);
		Factory.CreateLinkedTokenChannel(sw4, m5, 1, 2);
		//add 2 outbound
		Factory.CreateLinkedTokenChannel(add2, output, 0, 0);
		
		//switch 4 starting 0 token
		sw4.InSockets[1].Set(0);
		
		//Each thread gets a scheduler node
		Schedulers = new ArrayList<GenericActor>();
		
		for (int i=0; i<NumThreads; i++) {
			Schedulers.add(Factory.CreateGenericActor("scheduler"));	
		}
		
		//Each scheduler node oversees a subset of all the GenericActors
		int GenericActorsPerThread=Network.size()/NumThreads;
		int excessGenericActors=Network.size()%NumThreads;
		
		for (int i=0; i<Schedulers.size(); i++) {
			for (int j=0; j<GenericActorsPerThread; j++) {
				Schedulers.get(i).ThreadSet.add(Network.get(i*GenericActorsPerThread+j));
			}
		}
		
		for (int i=0; i<excessGenericActors; i++) {
			Schedulers.get(Schedulers.size()-1).ThreadSet.add(Network.get(Network.size()-1-i));
		}
		
	}

	/*
	 * Start up the network simulation
	 */
	public static void Start() {		
		//Setup done. Run all threads
		for (int i=0; i<NumThreads; i++) {
			ThreadPool.submit(Schedulers.get(i));	
		}
	}
}

/*
 * Network building API
 */
class Factory {
	public static int i=0;
	
	//creates an Actor
	public static Actor CreateActor(String s) {
		GenericActor a = new GenericActor(s, i++);
		if (s.equals("scheduler")==false) Simulation.Network.add(a);
		return a;
	}
	
	//creates an unconnected Channel
	public static Channel CreateChannel() {
		return new TokenChannel();
	}
	
	//creates an GenericActor. Unless it is a scheduler it add it to the network list
	public static GenericActor CreateGenericActor(String s) {
		GenericActor a = new GenericActor(s, i++);
		if (s.equals("scheduler")==false) Simulation.Network.add(a);
		return a;
	}
	
	//creates an unconnected TokenChannel
	public static TokenChannel CreateTokenChannel() {
		return new TokenChannel();
	}
	
	//creates a TokenChannel between 2 GenericActors and connects in to the specified in and out port ids
	public static TokenChannel CreateLinkedTokenChannel(GenericActor a, GenericActor b, int idA, int idB) {
		TokenChannel c=new TokenChannel();
		a.ConnectOut(c, idA);
		b.ConnectIn(c, idB);
		return c;
	}
}

interface Actor{
	void ConnectIn (TokenChannel c, int i);
	void ConnectOut (TokenChannel c, int i);
}

//GenericActor class. One size fits all design.
class GenericActor implements Runnable, Actor{
	int InCount=0, OutCount=0, Constant=0, CdrCount=0, OutputCapacity=100, id;
	volatile boolean FileParsed=false;
	String Type, InPath, OutPath;
	List<GenericActor> ThreadSet=new ArrayList<GenericActor>();
	List<Integer> Nums;
	TokenChannel[] InSockets =  new TokenChannel[100];
	TokenChannel[] OutSockets =  new TokenChannel[100];
	ArrayList<Token> ConsumedTokens= new ArrayList<Token>();
	ArrayList<FireOrder> ProducedTokens=new ArrayList<FireOrder>();
	FileOutputStream Fos;
    DataOutputStream Dos;
	
	GenericActor() {}
	
	GenericActor(String s){
		Type=s;
	}
	
	GenericActor(String s, int i){
		Type=s; id=i;
	}
	
	//connects a TokenChannel to the specified inbound socket
	public void ConnectIn (TokenChannel c, int i) {
		InSockets[i]=c;
		c.Dest = this;
		InCount++;
	}
	
	//connects a TokenChannel to the specified outbound socket
	public void ConnectOut (TokenChannel c, int i) {
		OutSockets[i]=c;
		OutCount++;
	}
	
	@Override
	public void run() {
		boolean b;
		
		//all the network nodes run a preconditions check to verify an GenericActor can consume and fire
		if (Type.equals("scheduler")==false) {
			b=PreConditionsCheck(true);
			if (b==false) return;
		}
		
		//depending on the type a different method runs
		switch (Type) {
		case "add":
			Add(); break;
		case "fork":
			Fork(); break;
		case "<":
			LessThan(); break;
		case ">":
			GreaterThan(); break;
		case "==":
			Equal(); break;
		case ">=":
			GreaterEqual(); break;
		case "inc":
			Increment(); break;
		case "dec":
			Decrement(); break;	
		case "cdr":
			Cdr(); break;	
		case "merge":
			Merge(); break;
		case "switch":
			Switch(); break;
		case "zero":
			Zero(); break;
		case "emit":
			Emit(); break;
		case "input":
			Input(); break;	
		case "output":
			Output(); break;
		case "scheduler":
			Schedule(); return;
		default:
			break;
		}
		
		FireOutput();
	}
	
	/*
	 * Unpacks each fire order into its token and destination TokenChannel 
	 * Fires the token in the correspondent TokenChannel.
	 */
	public void FireOutput() {
		
		for (FireOrder o : ProducedTokens) {		
			OutSockets[o.TokenChannelId].TokenQueue.offer(o.T);
			
			//Proof the network operates with bounded channels
			if (OutSockets[o.TokenChannelId].TokenQueue.size()>1000) {
				System.out.println("OVERFLOW");
			}
		}
		
		ConsumedTokens.clear();
		ProducedTokens.clear();
	}
	
	/*
	 * Tests if an GenericActor has the necessary tokens to run and if its firing
	 * TokenChannels have space. Consumes and run if successful, returns upon failure.
	 */
	public boolean PreConditionsCheck(boolean flag) {
		
		//special case preconditions check for merge
		if (Type.equals("merge")) {
			if (OutSockets[0].TokenQueue.size()==OutSockets[0].Bandwidth) {
				//System.out.println("merge "+id+"+ outbound TokenChannel full");
				return false;
			}
			if (InSockets[0].TokenQueue.isEmpty()==true) {
				//System.out.println("merge "+id+" control inbound TokenChannel empty");
				return false;
			}
			
			if (InSockets[0].TokenQueue.peek().Bool==true) {
				if (InSockets[1].TokenQueue.isEmpty()==true) {
					//System.out.println("merge "+id+" true inbound TokenChannel empty");
					return false;
				}
				if (flag==true) {
					ConsumedTokens.add(InSockets[0].TokenQueue.remove());
					ConsumedTokens.add(InSockets[1].TokenQueue.remove());
					return true;
				}
			}
			else {
				if (InSockets[2].TokenQueue.isEmpty()==true) {
					//System.out.println("merge "+id+" false inbound TokenChannel empty");
					return false;
				}
				if (flag == true) {
					ConsumedTokens.add(InSockets[0].TokenQueue.remove());
					ConsumedTokens.add(InSockets[2].TokenQueue.remove());
					return true;
				}
			}
		}
		else {
			for (int i=0; i<OutCount; i++) {
				if (OutSockets[i].TokenQueue.size()==OutSockets[i].Bandwidth) {

					//System.out.println(Type +" "+id+" outbound TokenChannel "+i+" full");
					return false;
				}
			}
			
			for (int i=0; i<InCount; i++) {
				if (InSockets[i].TokenQueue.isEmpty()==true) {
					//System.out.println(Type +" "+id+" inbound TokenChannel "+i+" empty");
					return false;
				}
			}
			if (flag==true) {
				for (int i=0; i<InCount; i++) {
					ConsumedTokens.add(InSockets[i].TokenQueue.remove());
				}	
				return true;
			}
		}
		return false;
	}
	
	/*
	 * -----------------------------------
	 * The following methods do what their name states and produce the necessary fire orders
	 * -----------------------------------
	 */
	private void Add(){
		ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(0).Num+ConsumedTokens.get(1).Num, 
				OutSockets[0].Dest), 0));
	}
	
	private void Fork() {
		for (int i=0; i<OutCount; i++) {
			ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(0), OutSockets[i].Dest), i));
		}
	}
	
	private void LessThan() {
		if (ConsumedTokens.size()==1) {
			if (ConsumedTokens.get(0).Num < Constant) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
		else if (ConsumedTokens.size()==2) {
			if (ConsumedTokens.get(0).Num < ConsumedTokens.get(1).Num) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
	}
	
	private void GreaterThan() {
		if (ConsumedTokens.size()==1) {
			if (ConsumedTokens.get(0).Num > Constant) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
		else if (ConsumedTokens.size()==2) {
			if (ConsumedTokens.get(0).Num > ConsumedTokens.get(1).Num) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
	}
	
	private void Equal() {
		if (ConsumedTokens.size()==1) {
			if (ConsumedTokens.get(0).Num == Constant) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
		else if (ConsumedTokens.size()==2) {
			if (ConsumedTokens.get(0).Num == ConsumedTokens.get(1).Num) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
	}
	
	private void GreaterEqual() {
		if (ConsumedTokens.size()==1) {
			if (ConsumedTokens.get(0).Num >= Constant) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
		else if (ConsumedTokens.size()==2) {
			if (ConsumedTokens.get(0).Num >= ConsumedTokens.get(1).Num) {
				ProducedTokens.add(new FireOrder(new Token(true, OutSockets[0].Dest), 0));
			}
			else {
				ProducedTokens.add(new FireOrder(new Token(false, OutSockets[0].Dest), 0));
			}
		}
	}
	
	private void Increment() {
		ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(0).Num+1, OutSockets[0].Dest), 0));
	}
	
	private void Decrement() {
		ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(0).Num-1, OutSockets[0].Dest), 0));
	}
	
	private void Cdr() {
		if (CdrCount++==0) return;
		ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(0), OutSockets[0].Dest), 0));
	}
	
	private void Merge() {
		ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(1), OutSockets[0].Dest), 0));
	}
	
	private void Switch() {
		if (ConsumedTokens.get(0).Bool==true) {
			ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(1), OutSockets[0].Dest), 0));
		}
		else {
			ProducedTokens.add(new FireOrder(new Token(ConsumedTokens.get(1), OutSockets[1].Dest), 1));
		}
	}
	
	private void Zero() {
		return;
	}
	
	private void Emit() {
		ProducedTokens.add(new FireOrder(new Token(Constant, OutSockets[0].Dest), 0));
	}
	
	/*
	 * Processes a file made of numbers and produces its tokens for the network computation
	 */
	private void Input() {
		try {
			//file is parsed in one go at launch and saved in a list
			if (FileParsed==false) {
				Nums= Files.lines(Paths.get(InPath)).map(Integer::parseInt).collect(Collectors.toList());
				FileParsed=true;
				
				/*
				 * for this specific computation we calculate how many tokens the output will process 
				 * to detrmine how long the simulation will run
				 */
				for (Integer i : Nums) {
					Simulation.OutputSize.addAndGet(i.intValue());
				}
			}
			
			//we can only fire a number of tokens that fits in the TokenChannel
			int bound=Math.min(OutSockets[0].Bandwidth-OutSockets[0].TokenQueue.size(), Nums.size());
			for (int i=0; i<bound; i++) {
				ProducedTokens.add(new FireOrder(new Token(Nums.get(i).intValue(), OutSockets[0].Dest), 0));
			}
			
			//we update the list of numbers to be fired for the next run
			for (int i=0; i<bound; i++) {
				Nums.remove(0);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Simulation.ThreadPool.shutdown();
		}
	}
	
	/*
	 * Output GenericActor that writes its received tokens to a file
	 */
	private void Output() {
	
		try {
			//initializes the data streams and creates a file
			if (Fos==null || Dos==null) {
				Fos = new FileOutputStream(new File(OutPath));
			    Dos = new DataOutputStream(new BufferedOutputStream(Fos));
			}
			
			//converts the tokens to strings and writes them to the file
			String s=Integer.toString(ConsumedTokens.get(0).Num)+"\n";
			Dos.writeBytes(s);
			
			/*
			 * if the simulation run is over the streams are closed and reset
			 * and the threadpool is shutdown
			 */
			if (Simulation.OutputSize.decrementAndGet()==0) {
				Dos.close();
				Fos.close();
				
				Fos=null;
				Dos=null;
				
				Simulation.ThreadPool.shutdown();
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			Simulation.ThreadPool.shutdown();
		}
		
	}

	/*
	 * The scheduler GenericActor owns a sector of the network. It constantly tries to run its GenericActors.
	 */
	private void Schedule() {
		while (Simulation.ThreadPool.isShutdown()==false) {
			for (GenericActor a : ThreadSet) {			
				a.run();
			}	
		}
		
		//when the simulation is over the input node resets its state
		for (GenericActor a : ThreadSet) {
			if (a.Type.equals("input")) {
				a.FileParsed=false;
			}
		}
	}
}

interface Channel {
	void Set(int i);
}

/*
 * Created bounded capacity queues to connect GenericActors
 */
class TokenChannel implements Channel{
	public GenericActor Dest;
	public int Bandwidth=1000;
	public ConcurrentLinkedQueue<Token> TokenQueue = new ConcurrentLinkedQueue<>();
	
	//add an int token to the TokenChannel for pre-initialization
	public void Set(int i){
		TokenQueue.add(new Token(i, Dest));
	}
	
	//add a boolean token to the TokenChannel for pre-initialization
	public void Set (boolean b){
		TokenQueue.add(new Token(b, Dest));
	}
}

/*
 * Holds either a boolean or integer
 */
class Token{
	public int Num;
	public boolean Bool;
	public GenericActor Owner;
	
	Token (boolean _data, GenericActor a) {
		Bool=_data;
		Owner=a;
	}
	
	Token (int _data, GenericActor a) {
		Num=_data;
		Owner=a;
	}
	
	Token (Token t, GenericActor newOwner){
		Num=t.Num; Bool=t.Bool; Owner=newOwner;
	}

}

/*
 * A fire order is a package that holds a token and a destination TokenChannel.
 * It helps standardize the firing mechanism of all the GenericActor types. 
 */
class FireOrder {
	public int TokenChannelId=-1;
	public Token T;
	
	FireOrder(Token tok, int id){
		TokenChannelId=id; T=tok;
	}
}