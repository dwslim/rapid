package src.engine.racedetectionengine.ordered_list;
import util.vectorclock.VectorClock;

public class OrderedClock {

    private VectorClock VC;

    private int[] parent;
    private int[] children;
    private int de;
    private int head;
    private int tid;
    private int u;
    private boolean shared;
//for threads
    public OrderedClock(int d, int tid){
        this.VC = new VectorClock(d);
        parent = new int[d];
        children = new int[d];
        for(int i = 0; i<d;i++){
            parent[i]=-1;
            children[i]=-1;
        }
        de = 1;
        head = -1;
        this.u = 0;
        this.tid = tid;
        this.shared = false;
    }
//for lock
    public OrderedClock(int d){
        this.u = -1;
    }
    public OrderedClock(OrderedClock other){
        deepCopy(other);

    }

    public int getU(){
        return this.u;
    }
    public int getT(){
        return this.tid;
    }

    public int getE(){
        return this.de;
    }


    public void inc(){

        this.de++;
        this.u++;

    }

    public VectorClock getVC(){
        return this.VC;
    }

    public void setShared(){
        this.shared = true;
    }

    public void shallowCopy(OrderedClock other){

        this.VC = other.VC;
        this.parent = other.parent;
        this.children = other.children;
        this.head = other.head;
        this.tid = other.tid;
        this.de = other.de-1;
        this.u = other.u;

    }

    private void deepCopy(OrderedClock other){

        this.VC = new VectorClock(other.VC);
        this.parent = new int [other.parent.length];
        this.children = new int [other.children.length];
        for(int i = 0; i<parent.length;i++){
            parent[i]=other.parent[i];
            children[i] = other.children[i];
        }
        this.shared = false;

    }
    private void deepCopy(){
        this.VC = new VectorClock(this.VC);
        int [] tempParent = this.parent;
        int [] tempChildren = this.children;
        this.parent = new int [this.parent.length];
        this.children = new int [this.children.length];
        for(int i = 0; i<parent.length;i++){
            parent[i]=tempParent[i];
            children[i] = tempChildren[i];
        }
        this.shared = false;

    }


    public int get(int tid){

        return this.VC.getClockIndex(tid);
    }

    public void set(int tid, int clock){
        if(shared){
            deepCopy();
        }
        this.VC.setClockIndex(tid,clock);
        move_to_head(tid);
    }

    public void move_to_head(int tid){

        int tempParent = parent[tid];
        parent[tid] = -1;
        children[tempParent] = children[tid];
        parent[children[tid]] = tempParent;
        children[tid] = head;
        head = tid;
    }

    public void updateWithMax(OrderedClock other, int diff){

        int otherHead = other.head;
        int count = diff ;
        if(this.get(other.tid)<other.de){
            this.set(other.tid, other.de);
            u++;
        }
        while(otherHead!=-1 && count!=0){
            if(otherHead==tid){
                count = count-1;
                otherHead = other.children[otherHead];
                continue;
            }
            int otherVal = other.get(otherHead);
            if(this.get(otherHead)<otherVal){
                this.set(otherHead,otherVal);
                u++;
            }
            count = count-1;
            otherHead = other.children[otherHead];
        }


    }
    public String toString(){
        return this.VC.toString();
    }




}