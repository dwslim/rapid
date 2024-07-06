package engine.racedetectionengine.ordered_list;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import util.vectorclock.SemiAdaptiveVC;

import engine.racedetectionengine.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class OrderedListState extends State{

    // Internal data
    private HashMap<Thread, Integer> threadToIndex;
    private HashMap<Lock, Integer> lockToIndex;
    private HashMap<Variable, Integer> variableToIndex;
    public int numThreads;
    private int numLocks;
    private int numVariables;
    public long acquires;
    public long joins;
    public long releases;
    public long forks;
    public long cTraversed;
    public long cUpdated;
    public long uTraversed;
    public long uUpdated;
    public long increments;
    public long deepcopies;
    public long uAcquireSkipped;
    public long sameThreadAcquireSkipped;
    public long saveOl;
    // Data used for algorithm
    public ArrayList<OrderedClock> threadVCs;
    public ArrayList<OrderedClock> lockVCs;
    public ArrayList<SemiAdaptiveVC> readVariable;
    public ArrayList<SemiAdaptiveVC> writeVariable;
    // For minjian's sampling algorithm
    public ArrayList<VectorClock> threadAugmentedVCs;
    public ArrayList<Boolean> threadsSampledStatus;

    public OrderedListState(HashSet<Thread> tSet) {
        initInternalData(tSet);
        initData(tSet);
    }

    private void initInternalData(HashSet<Thread> tSet) {
        this.threadToIndex = new HashMap<Thread, Integer>();
        this.numThreads = 0;
        Iterator<Thread> tIter = tSet.iterator();
        while (tIter.hasNext()) {
            Thread thread = tIter.next();
            this.threadToIndex.put(thread, (Integer)this.numThreads);
            this.numThreads ++;
        }

        this.lockToIndex = new HashMap<Lock, Integer>();
        this.numLocks = 0;
        this.variableToIndex = new HashMap<Variable, Integer>();
        this.numVariables = 0;
        this.cTraversed=0;
        this.cUpdated=0; 
        this.deepcopies=0;
        this.increments=0;
        this.uTraversed=0;
        this.uUpdated=0;
        this.uAcquireSkipped = 0; 
        this.sameThreadAcquireSkipped=0;
        this.saveOl = 0;
    }

    public void initData(HashSet<Thread> tSet) {
        // initialize OrderedClock
        this.threadVCs = new ArrayList<OrderedClock>();
        initialize1DArrayOfOrderedClocksWithBottom(this.threadVCs, this.numThreads, this.numThreads);
        // initialize HBThreadAugmented
        this.threadAugmentedVCs = new ArrayList<VectorClock>();
        initialize1DArrayOfVectorClocksWithBottom(this.threadAugmentedVCs, this.numThreads, this.numThreads);

        this.threadsSampledStatus = new ArrayList<>();
        for (int i = 0; i < numThreads; ++i) this.threadsSampledStatus.add(false);

        this.lockVCs = new ArrayList<OrderedClock>();

        // initialize readVariable
        this.readVariable = new ArrayList<SemiAdaptiveVC>();

        // initialize writeVariable
        this.writeVariable = new ArrayList<SemiAdaptiveVC>();
    }

    //Access Methods
    void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len, int dim) {
        for (int i = 0; i < len; i++) {
            arr.add(new VectorClock(dim));
        }
    }

    void initialize1DArrayOfOrderedClocksWithBottom(ArrayList<OrderedClock> arr, int len, int dim){
        for (int i = 0; i < len; i++) {
            //let i be the thread id
            arr.add(new OrderedClock(dim,i));
        }
    }

    private int checkAndAddLock(Lock l){
        if(!lockToIndex.containsKey(l)){
            lockToIndex.put(l, this.numLocks);
            this.numLocks ++;
            lockVCs.add(new OrderedClock(this.numThreads));

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

    public int getThreadIndex(Thread t) {
        return threadToIndex.get(t);
    }

    public int getThreadClock(Thread t) {
        int tIndex = threadToIndex.get(t);
        return this.threadVCs.get(tIndex).get(tIndex);
    }

    public VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
        if (index < 0 || index >= arr.size()) {
            throw new IllegalArgumentException("Illegal Out of Bound access");
        }
        return arr.get(index);
    }

    public OrderedClock getOrderedClockFrom1DArray(ArrayList<OrderedClock> arr, int index) {
        if (index < 0 || index >= arr.size()) {
            throw new IllegalArgumentException("Illegal Out of Bound access");
        }
        return arr.get(index);
    }
    // inc epoch, just call the inc function that modifies two functions.
    public void incThreadEpoch(Thread t) {
        int tIndex = threadToIndex.get(t);
        this.threadVCs.get(tIndex).inc();
        this.increments++;
        this.cUpdated++;
        this.uUpdated++;
    }


    public OrderedClock getOrderedClock(ArrayList<OrderedClock> arr, Thread t) {
        int tIndex = threadToIndex.get(t);
        return getOrderedClockFrom1DArray(arr, tIndex);
    }

    public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
        int tIndex = threadToIndex.get(t);
        return getVectorClockFrom1DArray(arr, tIndex);
    }

    public OrderedClock getOrderedClock(ArrayList<OrderedClock> arr, Lock l) {
        int lIndex = checkAndAddLock(l);
        return getOrderedClockFrom1DArray(arr, lIndex);
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
            VectorClock C_t = getOrderedClock(this.threadVCs, thread).getVC();
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
        System.err.println("Number of threads = " + Long.toString(this.numThreads));
        System.err.println("Number of locks = " + Long.toString(this.numLocks));
        System.err.println("Number of variables = " + Long.toString(this.numVariables));
    }
}