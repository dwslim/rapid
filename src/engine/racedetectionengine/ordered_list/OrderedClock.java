package engine.racedetectionengine.ordered_list;
import java.util.Arrays;

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
//for threads, -1 means no parent or no child. At the beginning, all threads have no parent and no child.
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
        //incapsulate all information needed to passed in the structure.
        this.u = 0;
        this.tid = tid;
        this.shared = false;
    }
//for lock, just initate the object. Will perform shallow copy only.
    public OrderedClock(int d){
        this.u = -1;
    }

    public int getU(){
        return this.u;
    }
    public void incU(){
        this.u++;
    }
    public int getT(){
        return this.tid;
    }

    public int getE(){
        return this.de;
    }


    //when increment, only change the scalars. Note the change of u doesn't need to be reflected in the augmented vector clock.

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
    public boolean getShared(){
        return this.shared;
    }
    //shallow copy. The lock would copy the threads information.
    public void shallowCopy(OrderedClock other){

        this.VC = other.VC;
        this.parent = other.parent;
        this.children = other.children;
        this.head = other.head;
        //the thread is the lastReleasedThread.
        this.tid = other.tid;
        //decrement the dirty epoch because it is always off by one.
        this.de = other.de;
        //pass u too.
        this.u = other.u;

    }
    public void forkCopy(OrderedClock other){
        int diff = other.u;
        // System.out.println("pre forkCopy: this (" + this.toString() + ")" + " other (" + other.toString() + ")");
        updateWithMax(other,diff);
        // System.out.println("post forkCopy: this (" + this.toString() + ")" + " other (" + other.toString() + ")");
        
    }

    //deep copy called by the thread on itself.
    private void deepCopy(){
        //copy the vector clock.
        this.VC = new VectorClock(this.VC);
        //deep copy the parent and children.
        int [] tempParent = this.parent;
        int [] tempChildren = this.children;
        this.parent = new int [this.parent.length];
        this.children = new int [this.children.length];
        for(int i = 0; i<parent.length;i++){
            parent[i]=tempParent[i];
            children[i] = tempChildren[i];
        }
        this.shared = false;
        //no need to change other scalars.

    }
    

    // returns value of the VC.
    public int get(int tid){
        return this.VC.getClockIndex(tid);
    }
    //set certain node in the VC
    public void set(int tid, int clock){
        if(shared){
            deepCopy();
        }
        this.VC.setClockIndex(tid,clock);
        move_to_head(tid);
    }
    //function that reorders list.
    public void move_to_head(int tid){
        //if already head
        if(head==tid){
            return;
        }
        //find the parent of the thread.
        int tempParent = parent[tid];
        //parent of the new head is -1;
        parent[tid] = -1;
        //the old parent's child would the child of the current thread.
        if(tempParent!=-1)
        children[tempParent] = children[tid];
        //similarly the parent of the child would be the parent of the thread.
        if(children[tid]!=-1)
        parent[children[tid]] = tempParent;
        // the new child of the thread would be the old-head
        children[tid] = head;
        if (head != -1)
        parent[head] = tid;
        //reset head
        head = tid;
    }
    //join function on two ordered lists.

    public int updateWithMax(OrderedClock other, int diff){

        int otherHead = other.head;
        int count = diff ;
        int updated = 0;
        //while not the end of list and count is not 0
        while(otherHead!=-1 && count!=0){
            //if current thread, go to next node.
            if(otherHead==tid||otherHead==other.tid){
                count = count-1;
                otherHead = other.children[otherHead];
                continue;
            }
            int otherVal = other.get(otherHead);
            //update the value if smaller
            if(this.get(otherHead)<otherVal){
                this.set(otherHead,otherVal);
                u++;
                updated++;
            }
            count = count-1;
            otherHead = other.children[otherHead];
        }
        return updated;


    }
    public String toString(){
        return this.VC.toString()+"parent "+Arrays.toString(parent)+"child" + Arrays.toString(children);
    }




}