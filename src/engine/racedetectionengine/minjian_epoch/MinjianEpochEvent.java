package engine.racedetectionengine.minjian_epoch;

import engine.racedetectionengine.RaceDetectionEvent;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class MinjianEpochEvent extends RaceDetectionEvent<MinjianEpochState> {

	@Override
	public boolean Handle(MinjianEpochState state, int verbosity) {
		return this.HandleSub(state, verbosity);
	}

	@Override
	public void printRaceInfoLockType(MinjianEpochState state, int verbosity) {
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
	public void printRaceInfoAccessType(MinjianEpochState state, int verbosity) {
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
	public void printRaceInfoExtremeType(MinjianEpochState state, int verbosity) {
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
	public void printRaceInfoTransactionType(MinjianEpochState state, int verbosity) {
	}

	@Override
	public boolean HandleSubAcquire(MinjianEpochState state, int verbosity) {
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
		U_t.updateMax2(U_l);

		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		VectorClock C_l = state.getVectorClock(state.lockVCs, this.getLock());
		boolean did_acquire = C_t.updateMax2(C_l);
		if (did_acquire) state.incThreadAugmentedEpoch(this.getThread());

		this.printRaceInfo(state, verbosity);
		return false;
	}

	@Override
	public boolean HandleSubRelease(MinjianEpochState state, int verbosity) {
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
			C_l.updateMax2(C_t);
			U_l.updateMax2(U_t);

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

	@Override
	public boolean HandleSubRead(MinjianEpochState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		else{
			int tIndex = state.getThreadIndex(this.getThread());
			int c = C_t.getClockIndex(tIndex);
			if(!R_v.isSameEpoch(c, tIndex)){
				R_v.updateWithMax(C_t, state.getThreadIndex(this.getThread()));
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(MinjianEpochState state, int verbosity) {
		state.setThreadSampledStatus(this.getThread(), true);

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.threadVCs, this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state, verbosity);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		int tIndex = state.getThreadIndex(this.getThread());
		int c = C_t.getClockIndex(tIndex);
		if(!W_v.isSameEpoch(c, tIndex)){
			W_v.setEpoch(c, tIndex);
			if(!R_v.isEpoch()){
				R_v.forceBottomEpoch();
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(MinjianEpochState state, int verbosity) {
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
	public boolean HandleSubJoin(MinjianEpochState state, int verbosity) {
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
			U_tp.updateMax2(U_tc);

			VectorClock C_tp = state.getVectorClock(state.threadVCs, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.threadVCs, this.getTarget());

			// Try to join and see if did actually acquire any new info
			boolean did_acquire = C_tp.updateMax2(C_tc);
			if (did_acquire) state.incThreadAugmentedEpoch(this.getThread());

			this.printRaceInfo(state, verbosity);
		}
		return false;
	}


	@Override
	public boolean HandleSubBegin(MinjianEpochState state, int verbosity) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(MinjianEpochState state, int verbosity) {
		return false;
	}

}
