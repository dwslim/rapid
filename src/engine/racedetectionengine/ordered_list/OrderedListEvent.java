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
        boolean sharedbefore = O_t.getShared();
        boolean epochUpdate=false; 
 
         // Skip if the thread knows more information than the lock. init u of O_l is -1, and init u of U_t is 0.
        state.uTraversed++;
        int diff = O_l.getU()-U_t.getClockIndex(O_l.getT());
        if (diff<=0){
            state.uAcquireSkipped++;
            if(sharedbefore&&!O_t.getShared()){
                state.deepcopies++;
            }
            if(epochUpdate){
                state.uUpdated++;
                state.uTraversed++;
            }
            return false;

        }
        state.cTraversed++;
        if(O_l.getE()-1>O_t.get(O_l.getT())){
            state.cUpdated++;
            O_t.set(O_l.getT(), O_l.getE()-1);
            O_t.incU();
            epochUpdate = true;
        }

        state.cTraversed+=Math.min(diff,state.numThreads);
        if(diff<state.numThreads){
            state.saveOl +=state.numThreads-diff;
        }
        // update the U vector clock.
        state.uTraversed++;
        state.uUpdated++;
        U_t.setClockIndex(O_l.getT(),O_l.getU());
        //learn from the lock.
        int changedEntry = O_t.updateWithMax(O_l,diff);
        state.cUpdated+=changedEntry;

        if(changedEntry>0||epochUpdate){
            state.uTraversed++;
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
  
            
		    int changedEntry=O_tc.forkCopy(O_tp);
            state.cTraversed += Math.min(O_tp.getU(),state.numThreads);
            if(changedEntry>0||epochUpdate){
                state.cUpdated+=changedEntry;
                state.uUpdated++;
                state.uTraversed++;
            }
           



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
               int changedEntry=O_tp.updateWithMax(O_tc, diff);
               state.cTraversed+=Math.min(diff,state.numThreads);
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