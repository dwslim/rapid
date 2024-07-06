package engine.racedetectionengine.uclock_epoch;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class UClockEpochEvent extends RaceDetectionEvent<UClockEpochState> {

	@Override
	public boolean Handle(UClockEpochState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(UClockEpochState state, int verbosity) {
		if(this.getType().isLockType()){
			if(verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(UClockEpochState state, int verbosity) {
		if(this.getType().isAccessType()){
			if(verbosity == 1 || verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
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
	public void printRaceInfoExtremeType(UClockEpochState state, int verbosity) {
		if(this.getType().isExtremeType()){
			if(verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoTransactionType(UClockEpochState state, int verbosity) {
	}

	@Override
	public boolean HandleSubAcquire(UClockEpochState state, int verbosity) {
		state.numOriginalAcquires++;
		// Acq(t, l):
		// If U_l(LR_l) <= U_t(LR_l):
		// 	Return
		// U_t := U_t join U_l
		// If not (C_l ⊑ C_t):
		// 	C_t := C_t join C_l
		// 	U_t[t]++
		VectorClock U_l = state.getVectorClock(state.lockUVCs, this.getLock());
		VectorClock U_t = state.getVectorClock(state.threadUVCs, this.getThread());
		
		int lock_last_released_thread_index = state.getLockLastReleasedThreadIndex(this.getLock());
		//same thread or fresh lock
		if(lock_last_released_thread_index==state.getThreadIndex(this.getThread())||lock_last_released_thread_index==-1){
			state.sameThreadAcquireSkipped++;
			return false;

		}
		// Skip if the thread knows more information than the lock
		state.uTraversed++;
		if (U_l.getClockIndex(lock_last_released_thread_index) <= U_t.getClockIndex(lock_last_released_thread_index)){
			state.uAcquireSkipped++;
			return false;
		}
			
		state.numUClockAcquires++;

				// Join the augmented VCs
		state.threadUUpdated+=U_t.updateMaxInPlace(U_l);

		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		VectorClock C_l = state.getVectorClock(state.lockVCs, this.getLock());
		int changedEntry = C_t.updateMaxInPlace(C_l);
		if (changedEntry>0){
			state.incThreadUEpoch(this.getThread());
			state.threadCUpdated+=changedEntry;
		} 


		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(UClockEpochState state, int verbosity) {
		state.numOriginalReleases++;
		// Rel(t, l);
		// If U_t(t) != U_l(t):
		// 	 C_l := C_t join C_l // Also equivalent to “C_l := C_t”. When using tree clocks, use the “MonotoneCopy” function; see TC paper
		// 	 U_l := U_t
		// 	 LR_l := t
		// If (smp_t):
		//   U_t(t)++
		// 	 C_t(t)++
		// 	 smp_t := 0
		if (state.didThreadSample(this.getThread())) {
			state.incThreadEpoch(this.getThread());
			state.setThreadSampledStatus(this.getThread(), false);
		}
		VectorClock U_t = state.getVectorClock(state.threadUVCs, this.getThread());
		VectorClock U_l = state.getVectorClock(state.lockUVCs, this.getLock());
		int tIdx = state.getThreadIndex(this.getThread());

		// Join if the lock doesn't know about the thread yet.
		state.uTraversed++;
		if (U_t.getClockIndex(tIdx) != U_l.getClockIndex(tIdx)) {
			state.numUClockReleases++;
			// Join the VCs.
			VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_l = state.getVectorClock(state.lockVCs, this.getLock());
			state.lockCUpdated+=C_l.updateMaxInPlace(C_t);
			state.lockUUpdated+=U_l.updateMaxInPlace(U_t);

			// Record the lock's last released thread.
		}
		state.updateLockLastReleasedThreadIndex(this.getLock(), this.getThread());

		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRead(UClockEpochState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		int tid = state.getThreadIndex(this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t,tid))) {
			raceDetected = true;
			//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		else{
			int c =state.getEpochtThread(this.getThread());
			if(!R_v.isSameEpoch(c, tid)){
				R_v.updateWithMax(C_t, c,state.getThreadIndex(this.getThread()));
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(UClockEpochState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		int tid = state.getThreadIndex(this.getThread());
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t,tid))) {
			raceDetected = true;
		}
		if (!(R_v.isLessThanOrEqual(C_t,tid))) {
			raceDetected = true;
		}
		int c = state.getEpochtThread(this.getThread());
		if(!W_v.isSameEpoch(c, tid)){
			W_v.setEpoch(c, tid);
			if(!R_v.isEpoch()){
				R_v.forceBottomEpoch();
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(UClockEpochState state, int verbosity) {
		// Fork(tp, tc):
		// 	 C_tc := C_tp[tc → 1]
		// 	 U_tc := U_tp[tc → 1]
		// 	 If (smp_tp):
		//     U_tp(tp)++
		// 	   C_tp(tp)++
		// 	   smp_tp := 0
		state.numOriginalForks++;
		if (state.isThreadRelevant(this.getTarget())) {

			if (state.didThreadSample(this.getThread())) {
				state.incThreadEpoch(this.getThread());
				state.setThreadSampledStatus(this.getThread(), false);
			}
			VectorClock C_tp = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.threadVCs, this.getTarget());
			VectorClock U_tp = state.getVectorClock(state.threadUVCs, this.getThread());
			VectorClock U_tc = state.getVectorClock(state.threadUVCs, this.getTarget());
			state.uTraversed++;
			if(U_tp.getClockIndex(state.getThreadIndex(this.getThread()))==0){
				return false; 
			}
			state.numUclockForks++;
			// Copy parent VC to thread
			int entryChanged=C_tc.updateMaxInPlace(C_tp);
			if(entryChanged>0){
				state.threadCUpdated+=entryChanged;
				state.incThreadUEpoch(this.getThread());
			}
			state.threadUUpdated+=U_tc.updateMaxInPlace(U_tp);

			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(UClockEpochState state, int verbosity) {
		state.numOriginalJoins++;
		// Join(tp, tc):
		// If U_tc(tc) <= U_tp(tc):
		//   Return
		// U_tp := U_tp join U_tc
		// If not (C_tc ⊑ C_tp):
		//   C_tp := C_tp join C_tc
		// 	 U_tp[tp]++
		if (state.isThreadRelevant(this.getTarget())) {


			if (state.didThreadSample(this.getTarget())) {
				state.incThreadEpoch(this.getTarget());
				state.setThreadSampledStatus(this.getTarget(), false);
			}
			VectorClock U_tp = state.getVectorClock(state.threadUVCs, this.getThread());
			VectorClock U_tc = state.getVectorClock(state.threadUVCs, this.getTarget());

			// The parent already knows everything about the child.
			int tcIdx = state.getThreadIndex(this.getTarget());
			state.uTraversed++;
			if (U_tc.getClockIndex(tcIdx) <= U_tp.getClockIndex(tcIdx)) return false;

			state.numUClockJoins++;

			// Join the augmented VCs
			state.threadUUpdated+=U_tp.updateMaxInPlace(U_tc);

			VectorClock C_tp = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.threadVCs, this.getTarget());

			// Try to join and see if did actually acquire any new info
			int changedEntry = C_tp.updateMaxInPlace(C_tc);
			if (changedEntry>0) {
				state.threadCUpdated+=changedEntry;
				state.incThreadUEpoch(this.getThread());

			}

			this.printRaceInfo(state, verbosity);
		}
		return false;
	}


	@Override
	public boolean HandleSubBegin(UClockEpochState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(UClockEpochState state, int verbosity) {
		return false;
	}

}
