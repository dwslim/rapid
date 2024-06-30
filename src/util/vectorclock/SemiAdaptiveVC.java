package util.vectorclock;
import engine.racedetectionengine.ordered_list.OrderedClock;
public class SemiAdaptiveVC extends AdaptiveVC{
	
	public SemiAdaptiveVC() {
		super();
	}

//	public SemiAdaptiveVC(int dim) {
//		super(dim);
//	}

	@Override
	public boolean isLTEUpdateWithMax(VectorClock vc, int t){
		boolean isLTE = isLessThanOrEqual(vc);
		if(is_epoch){
			if(isLTE){
				this.epoch.setClock(vc.getClockIndex(t));
				this.epoch.setThreadIndex(t);
			}
			else{
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
				this.vc.setClockIndex(t, vc.getClockIndex(t));
			}
		}
		else{
			this.vc.setClockIndex(t, vc.getClockIndex(t));
		}
		return isLTE;
	}
	
	public void updateWithMax(VectorClock vc, int t){
		boolean isLTE = isLessThanOrEqual(vc);
		if(is_epoch){
			if(isLTE){
				this.epoch.setClock(vc.getClockIndex(t));
				this.epoch.setThreadIndex(t);
			}
			else{
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
				this.vc.setClockIndex(t, vc.getClockIndex(t));
			}
		}
		else{
			this.vc.setClockIndex(t, vc.getClockIndex(t));
		}
	}
	// newly added case for the ordered clock comparsion

	public void updateWithMax(OrderedClock o_t, int t){
		VectorClock vc= o_t.getVC();
		boolean isLTE = isLessThanOrEqual(vc);
		if(is_epoch){
			//in addtion check if threads are the same
			if(t==this.epoch.getThreadIndex()||isLTE){
				this.epoch.setClock(o_t.getE());
				this.epoch.setThreadIndex(t);
			}
			else{
				is_epoch = false;
				this.vc = new VectorClock(vc.getDim());
				this.vc.setClockIndex(this.epoch.getThreadIndex(), this.epoch.getClock());
				this.vc.setClockIndex(t, o_t.getE());
			}
		}
		else{
			this.vc.setClockIndex(t, o_t.getE());
		}
	}
}