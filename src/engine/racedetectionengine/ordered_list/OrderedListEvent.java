package engine.racedetectionengine.ordered_list;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class OrderedListEvent extends RaceDetectionEvent<OrderedListState> {

    @Override
    public boolean Handle(OrderedListState state, int verbosity) {
        return this.HandleSub(state, verbosity);
    }

    @Override
    public void printRaceInfoLockType(OrderedListState state, int verbosity) {
        if (this.getType().isLockType()) {
            if (verbosity == 2) {
                String str = "#";
                str += Integer.toString(getLocId());
                str += "|";
                str += this.getType().toString();
                str += "|";
                str += this.getLock().toString();
                str += "|";
                OrderedClock C_t = state.getOrderedClock(state.threadVCs, this.getThread());
                str += C_t.toString();
                str += "|";
                str += this.getThread().getName();
                System.out.println(str);
            }
        }
    }

    @Override
    public void printRaceInfoAccessType(OrderedListState state, int verbosity) {
        if (this.getType().isAccessType()) {
            if (verbosity == 1 || verbosity == 2) {
                String str = "#";
                str += Integer.toString(getLocId());
                str += "|";
                str += this.getType().toString();
                str += "|";
                str += this.getVariable().getName();
                str += "|";
                OrderedClock C_t = state.getOrderedClock(state.threadVCs, this.getThread());
                str += C_t.toString();
                str += "|";
                str += this.getThread().getName();
                str += "|";
                str += this.getAuxId();
                System.out.println(str);
            }
        }
    }

    @Override
    public void printRaceInfoExtremeType(OrderedListState state, int verbosity) {
        if (this.getType().isExtremeType()) {
            if (verbosity == 2) {
                String str = "#";
                str += Integer.toString(getLocId());
                str += "|";
                str += this.getType().toString();
                str += "|";
                str += this.getTarget().toString();
                str += "|";
                OrderedClock C_t = state.getOrderedClock(state.threadVCs, this.getThread());
                str += C_t.toString();
                str += "|";
                str += this.getThread().getName();
                System.out.println(str);
            }
        }
    }

    @Override
    public void printRaceInfoTransactionType(OrderedListState state, int verbosity) {
    }

    @Override
    public boolean HandleSubAcquire(OrderedListState state, int verbosity) {
        state.acquires++;

        OrderedClock O_t = state.getOrderedClock(state.threadVCs, this.getThread());
        OrderedClock O_l = state.getOrderedClock(state.lockVCs, this.getLock());

        VectorClock U_t = state.getVectorClock(state.threadAugmentedVCs, this.getThread());

        //if released by this thread or fresh lock, do nothing
        if (O_l.getT()==O_t.getT()||O_l.getT()==-1){
            state.sameThreadAcquireSkipped++;
            return false;
        }
        //check dirty epoch
        // state.acqTraversed++;
        boolean epochUpdate=false; 
 
         // Skip if the thread knows more information than the lock. init u of O_l is -1, and init u of U_t is 0.
        state.uTraversed++;
        int diff = O_l.getU()-U_t.getClockIndex(O_l.getT());
        if (diff<=0){
            state.uAcquireSkipped++;
            return false;      
        }
        boolean sharedbefore = O_t.getShared();
        //increment nodes traversed due to checking dirty epoch
        state.cTraversed++;
        state.acqTraversed++;
        if(O_l.getE()-1>O_t.get(O_l.getT())){
            //updated dirty epoch
            state.cUpdated++;
            O_t.set(O_l.getT(), O_l.getE()-1);
            O_t.incU();
            epochUpdate = true;
        }

        // update the U vector clock.
        state.uUpdated++;
        U_t.setClockIndex(O_l.getT(),O_l.getU());
        //learn from the lock.
        int[] updateRes = O_t.updateWithMax(O_l,diff);
        int changedEntry =updateRes[0];
        //updating num updated nodes
        state.cUpdated+=changedEntry;
        //updating nodes traversed
        int traversedEntry =updateRes[1];
        state.cTraversed+=traversedEntry;
        state.acqTraversed+=traversedEntry;
        if(traversedEntry<state.numThreads){
            state.saveOl +=state.numThreads-traversedEntry;
        }
        if(changedEntry>0||epochUpdate){
            state.uUpdated++;
        }

        if(sharedbefore&&!O_t.getShared()){
            state.deepcopies++;
        }
        this.printRaceInfo(state, verbosity);
        return false;
    }

    @Override
    public boolean HandleSubRelease(OrderedListState state, int verbosity) {
        state.releases++;

        //if sampled, increment the epoch.
        if (state.didThreadSample(this.getThread())) {
            //the inc function supposedly incremnt u and e.
            state.incThreadEpoch(this.getThread());
            state.setThreadSampledStatus(this.getThread(), false);
        }
        OrderedClock O_l = state.getOrderedClock(state.lockVCs, this.getLock());
        OrderedClock O_t = state.getOrderedClock(state.threadVCs, this.getThread());
        //shallow copy.
        O_l.shallowCopy(O_t);
        //mark that O_t is shared.
        O_t.setShared();
        this.printRaceInfo(state, verbosity);
        return false;
    }

    @Override
    public boolean HandleSubRead(OrderedListState state, int verbosity) {
        state.setThreadSampledStatus(this.getThread(), true);

        boolean raceDetected = false;
        OrderedClock O_t = state.getOrderedClock(state.threadVCs, this.getThread());
        int tid = O_t.getT();
        VectorClock C_t = O_t.getVC();
        SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
        SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

        this.printRaceInfo(state, verbosity);

        //redefined the lessthan function so that it handles the case when threads are the same. Note that C_t[t] is a meaningless value.
        if (!(W_v.isLessThanOrEqual(C_t,tid))) {
            raceDetected = true;
            //		System.out.println("HB race detected on variable " + this.getVariable().getName());
        }
        else{
            int tIndex = state.getThreadIndex(this.getThread());
            int c = O_t.getE();
            if(!R_v.isSameEpoch(c, tIndex)){
                //also refined the update function so that it learns from the dirty epoch.
                R_v.updateWithMax(O_t, state.getThreadIndex(this.getThread()));
            }
        }
        return raceDetected;
    }

    @Override
    public boolean HandleSubWrite(OrderedListState state, int verbosity) {
        state.setThreadSampledStatus(this.getThread(), true);
        boolean raceDetected = false;
        OrderedClock O_t = state.getOrderedClock(state.threadVCs, this.getThread());
        int tid = O_t.getT();
        VectorClock C_t = O_t.getVC();
        SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
        SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

        this.printRaceInfo(state, verbosity);

        if (!(W_v.isLessThanOrEqual(C_t,tid))) {
            raceDetected = true;
        }
        if (!(R_v.isLessThanOrEqual(C_t,tid))) {
            raceDetected = true;
        }
        int tIndex = state.getThreadIndex(this.getThread());
        int c = O_t.getE();
        if (!W_v.isSameEpoch(c, tIndex)) {
            W_v.setEpoch(c, tIndex);
            if (!R_v.isEpoch()) {
                R_v.forceBottomEpoch();
            }
        }
        return raceDetected;
    }

    @Override
    public boolean HandleSubFork(OrderedListState state, int verbosity) {
        state.forks++;

         // Fork(tp, tc):
        // 	 C_tc := C_tp[tc → 1]
        // 	 U_tc := U_tp[tc → 1]
        // 	 If (smp_tp):
        //     U_tp(tp)++
        // 	   C_tp(tp)++
        // 	   smp_tp := 0
        if (state.isThreadRelevant(this.getTarget())) {

			if (state.didThreadSample(this.getThread())) {
				state.incThreadEpoch(this.getThread());
				state.setThreadSampledStatus(this.getThread(), false);
			}
			OrderedClock O_tp = state.getOrderedClock(state.threadVCs, this.getThread());
			OrderedClock O_tc = state.getOrderedClock(state.threadVCs, this.getTarget());
            state.cTraversed++;
            boolean epochUpdate = false;
            if(O_tp.getE()-1!=0){
                O_tc.set(O_tp.getT(), O_tp.getE()-1);
                O_tc.incU();
                epochUpdate=true;
                state.cUpdated++;

            }
            //u traversed for checking if parent has been updated
            state.uTraversed++;
            if(O_tp.getU()==0){
                return false;
            }
            int [] updateRes = O_tc.forkCopy(O_tp);
		    int changedEntry=updateRes[0];
            state.cTraversed += updateRes[1];
            if(changedEntry>0||epochUpdate){
                state.cUpdated+=changedEntry;
                state.uUpdated++;
            }
            VectorClock U_tc = state.getVectorClock(state.threadAugmentedVCs, this.getTarget());
            U_tc.setClockIndex(O_tp.getT(), O_tp.getU());
            state.uUpdated++;
			this.printRaceInfo(state, verbosity);
		}
		return false;
       
    }

    @Override
    public boolean HandleSubJoin(OrderedListState state, int verbosity) {
        state.joins++;

        if (state.isThreadRelevant(this.getTarget())) {

			if (state.didThreadSample(this.getTarget())) {
				state.incThreadEpoch(this.getTarget());
				state.setThreadSampledStatus(this.getTarget(), false);
			}
			OrderedClock O_tp = state.getOrderedClock(state.threadVCs, this.getThread());
			OrderedClock O_tc = state.getOrderedClock(state.threadVCs, this.getTarget());
            VectorClock U_t = state.getVectorClock(state.threadAugmentedVCs, this.getThread());
            boolean sharedbefore = O_tp.getShared();
            state.uTraversed++;
            int diff = O_tc.getU()-U_t.getClockIndex(O_tc.getT());
            boolean updateU = false;
           
            if(diff>0){
                state.cTraversed++;
                if(O_tc.getE()-1>O_tp.get(O_tc.getT())){
                    state.cUpdated++;
                    O_tp.set(O_tc.getT(), O_tc.getE()-1);
                    O_tp.incU();
                    updateU = true;
                }
               int[] updateRes = O_tp.updateWithMax(O_tc, diff);
               int changedEntry=updateRes[0];
               state.cTraversed+=updateRes[1];
               if(changedEntry>0){
                state.cUpdated+=changedEntry;
                updateU = true;
               }

            }
            if(sharedbefore&&!O_tp.getShared()){
                state.deepcopies++;
            }
            if(updateU){
                state.uUpdated++;
                state.uTraversed++;

            }
			this.printRaceInfo(state, verbosity);
		}
        return false;
    }


    @Override
    public boolean HandleSubBegin(OrderedListState state, int verbosity) {
        return false;
    }

    @Override
    public boolean HandleSubEnd(OrderedListState state, int verbosity) {
        return false;
    }
}