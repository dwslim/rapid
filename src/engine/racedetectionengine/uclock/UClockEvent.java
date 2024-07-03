package engine.racedetectionengine.uclock;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class UClockEvent extends RaceDetectionEvent<UClockState> {
	public UClockEvent() {
		super();
	}

	public boolean Handle(UClockState state, int verbosity){
		return this.HandleSub(state, verbosity);
	}

	public boolean isReadOrWrite() {
		return getType().isRead() || getType().isWrite();
	}

	/**************Pretty Printing*******************/
	@Override
	public void printRaceInfoLockType(UClockState state, int verbosity){
		if(verbosity >= 2){
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

	@Override
	public void printRaceInfoAccessType(UClockState state, int verbosity){
		if(verbosity >= 2){
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
			System.out.println(str);
		}
	}

	@Override
	public void printRaceInfoExtremeType(UClockState state, int verbosity){
		if(verbosity >= 2){
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
	/************************************************/

	/**************Acquire/Release*******************/
	@Override
	public boolean HandleSubAcquire(UClockState state, int verbosity){
		// Acq(t, l):
		// If U_l(LR_l) <= U_t(LR_l):
		// 	Return
		// U_t := U_t join U_l
		// If not (C_l ⊑ C_t):
		// 	C_t := C_t join C_l
		// 	U_t[t]++

		VectorClock U_l = state.getVectorClock(state.lockAugmentedVCs, this.getLock());
		VectorClock U_t = state.getVectorClock(state.threadAugmentedVCs, this.getThread());
		int lock_last_released_thread_index = state.getLockLastReleasedThreadIndex(this.getLock());

		// Skip if the thread knows more information than the lock
		if (U_l.getClockIndex(lock_last_released_thread_index) <= U_t.getClockIndex(lock_last_released_thread_index))
			return false;

		// Join the augmented VCs
		U_t.updateMaxInPlace(U_l);

		// Join the vanilla VC because we would loop through to check it anyway.
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		VectorClock C_l = state.getVectorClock(state.lockVCs, this.getLock());
		boolean did_acquire = C_t.updateMaxInPlace(C_l);
		if (did_acquire) state.incThreadAugmentedEpoch(this.getThread());

		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(UClockState state, int verbosity) {
		// Rel(t, l);
		// If U_t(t) != U_l(t):
		// 	 C_l := C_t join C_l // Also equivalent to “C_l := C_t”. When using tree clocks, use the “MonotoneCopy” function; see TC paper
		// 	 U_l := U_t
		// 	 LR_l := t
		// If (smp_t):
		//   U_t(t)++
		// 	 C_t(t)++
		// 	 smp_t := 0
		VectorClock U_t = state.getVectorClock(state.threadAugmentedVCs, this.getThread());
		VectorClock U_l = state.getVectorClock(state.lockAugmentedVCs, this.getLock());
		int tIdx = state.getThreadIndex(this.getThread());

		// Join if the lock doesn't know about the thread yet.
		if (U_t.getClockIndex(tIdx) != U_l.getClockIndex(tIdx)) {
			// Join the VCs.
			VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_l = state.getVectorClock(state.lockVCs, this.getLock());
			C_l.updateMaxInPlace(C_t);
			U_l.updateMaxInPlace(U_t);

			// Record the lock's last released thread.
			state.updateLockLastReleasedThreadIndex(this.getLock(), this.getThread());
		}

		if (state.didThreadSample(this.getThread())) {
			state.incThreadEpoch(this.getThread());
			state.incThreadAugmentedEpoch(this.getThread());
			state.setThreadSampledStatus(this.getThread(), false);
		}

		this.printRaceInfo(state, verbosity);
		return false;
	}
	/************************************************/

	/****************Read/Write**********************/
	@Override
	public boolean HandleSubRead(UClockState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariableVCs, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariableVCs, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		R_v.updateWithMax(R_v, C_t);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(UClockState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariableVCs, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariableVCs, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		W_v.updateWithMax(W_v, C_t);

		return raceDetected;
	}
	/************************************************/

	/*****************Fork/Join**********************/
	@Override
	public boolean HandleSubFork(UClockState state,int verbosity) {
		// Fork(tp, tc):
		// 	 C_tc := C_tp[tc → 1]
		// 	 U_tc := U_tp[tc → 1]
		// 	 If (smp_tp):
		//     U_tp(tp)++
		// 	   C_tp(tp)++
		// 	   smp_tp := 0
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_tp = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.threadVCs, this.getTarget());
			VectorClock U_tp = state.getVectorClock(state.threadAugmentedVCs, this.getThread());
			VectorClock U_tc = state.getVectorClock(state.threadAugmentedVCs, this.getTarget());

			// Copy parent VC to thread
			C_tc.copyFrom(C_tp);
			U_tc.copyFrom(U_tp);

			// And set the child's local epoch to 1
			int tcIdx = state.getThreadIndex(this.getTarget());
			C_tc.setClockIndex(tcIdx, 1);
			U_tc.setClockIndex(tcIdx, 1);

			if (state.didThreadSample(this.getThread())) {
				state.incThreadEpoch(this.getThread());
				state.incThreadAugmentedEpoch(this.getThread());
				state.setThreadSampledStatus(this.getThread(), false);
			}

			this.printRaceInfo(state, verbosity);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(UClockState state,int verbosity) {
		// Join(tp, tc):
		// If U_tc(tc) <= U_tp(tc):
		//   Return
		// U_tp := U_tp join U_tc
		// If not (C_tc ⊑ C_tp):
		//   C_tp := C_tp join C_tc
		// 	 U_tp[tp]++
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock U_tp = state.getVectorClock(state.threadAugmentedVCs, this.getThread());
			VectorClock U_tc = state.getVectorClock(state.threadAugmentedVCs, this.getTarget());

			// The parent already knows everything about the child.
			int tcIdx = state.getThreadIndex(this.getTarget());
			if (U_tc.getClockIndex(tcIdx) <= U_tp.getClockIndex(tcIdx)) return false;

			// Join the augmented VCs
			U_tp.updateMaxInPlace(U_tc);

			VectorClock C_tp = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.threadVCs, this.getTarget());

			// Try to join and see if did actually acquire any new info
			boolean did_acquire = C_tp.updateMaxInPlace(C_tc);
			if (did_acquire) state.incThreadAugmentedEpoch(this.getThread());

			this.printRaceInfo(state, verbosity);
		}
		return false;
	}
	/************************************************/

	@Override
	public void printRaceInfoTransactionType(UClockState state, int verbosity) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean HandleSubBegin(UClockState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubEnd(UClockState state, int verbosity) {
		// TODO Auto-generated method stub
		return false;
	}
}
