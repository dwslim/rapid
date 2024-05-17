package engine.racedetectionengine.uclock_epoch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class UClockEpochState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> threadVCs;
	public ArrayList<VectorClock> lockVCs;
	public ArrayList<SemiAdaptiveVC> readVariable;
	public ArrayList<SemiAdaptiveVC> writeVariable;

	// For minjian's sampling algorithm
	public ArrayList<VectorClock> threadAugmentedVCs;
	public ArrayList<VectorClock> lockAugmentedVCs;
	public ArrayList<Integer> lockLastReleasedThreadIndices;
	public ArrayList<Boolean> threadsSampledStatus;

	// stats
	public int numOriginalAcquires;
	public int numUClockAcquires;
	public int numOriginalReleases;
	public int numUClockReleases;
	public int numOriginalJoins;
	public int numUClockJoins;

	public UClockEpochState(HashSet<Thread> tSet) {
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;

		this.numOriginalAcquires = 0;
		this.numUClockAcquires = 0;
		this.numOriginalReleases = 0;
		this.numUClockReleases = 0;
		this.numOriginalJoins = 0;
		this.numUClockJoins = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		// initialize HBPredecessorThread
		this.threadVCs = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.threadVCs, this.numThreads);
		for(int i=0; i < this.numThreads; i++){
			this.threadVCs.get(i).setClockIndex(i, 1);
		}

		// initialize HBThreadAugmented
		this.threadAugmentedVCs = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.threadAugmentedVCs, this.numThreads);
		for(int i=0; i < this.numThreads; i++){
			this.threadAugmentedVCs.get(i).setClockIndex(i, 1);
		}

		this.threadsSampledStatus = new ArrayList<>();
		for (int i = 0; i < numThreads; ++i) this.threadsSampledStatus.add(false);

		// initialize lastReleaseLock
		this.lockVCs = new ArrayList<VectorClock>();
		this.lockAugmentedVCs = new ArrayList<VectorClock>();
		this.lockLastReleasedThreadIndices = new ArrayList<>();

		// initialize readVariable
		this.readVariable = new ArrayList<SemiAdaptiveVC>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<SemiAdaptiveVC>();
	}

	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}


	private int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;

			lockVCs.add(new VectorClock(this.numThreads));
			lockAugmentedVCs.add(new VectorClock(this.numThreads));

			lockLastReleasedThreadIndices.add(0);
		}
		return lockToIndex.get(l);
	}

	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			readVariable	.add(new SemiAdaptiveVC());
			writeVariable	.add(new SemiAdaptiveVC());
		}
		return variableToIndex.get(v);
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return this.threadVCs.get(tIndex).getClockIndex(tIndex);
	}

	public void incThreadEpoch(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = this.threadVCs.get(tIndex).getClockIndex(tIndex);
		this.threadVCs.get(tIndex).setClockIndex(tIndex, (Integer)(origVal + 1));
	}

	public void incThreadAugmentedEpoch(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = this.threadAugmentedVCs.get(tIndex).getClockIndex(tIndex);
		this.threadAugmentedVCs.get(tIndex).setClockIndex(tIndex, (Integer)(origVal + 1));
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getVectorClockFrom1DArray(arr, lIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}

	private <T> T getTFrom1DArray(ArrayList<T> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	public <T> T getAdaptiveVC(ArrayList<T> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getTFrom1DArray(arr, vIndex);
	}

	public int getThreadIndex(Thread t){
		return threadToIndex.get(t);
	}

	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public Integer getLockLastReleasedThreadIndex(Lock l) {
		int lIndex = checkAndAddLock(l);
		return lockLastReleasedThreadIndices.get(lIndex);
	}

	public void updateLockLastReleasedThreadIndex(Lock l, Thread thread) {
		int lIndex = checkAndAddLock(l);
		int tIndex = threadToIndex.get(thread);
		lockLastReleasedThreadIndices.set(lIndex, tIndex);
	}

	public void setThreadSampledStatus(Thread t, boolean status) {
		int tIndex = threadToIndex.get(t);
		this.threadsSampledStatus.set(tIndex, status);
	}

	public boolean didThreadSample(Thread t) {
		int tIndex = threadToIndex.get(t);
		return this.threadsSampledStatus.get(tIndex);
	}

	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = getVectorClock(this.threadVCs, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}

	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}