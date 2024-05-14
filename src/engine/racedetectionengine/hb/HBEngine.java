package engine.racedetectionengine.hb;

import java.util.HashSet;
import java.util.Random;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class HBEngine extends RaceDetectionEngine<HBState, HBEvent> {

	private Random rng;
	private double samplingRate;

	public HBEngine(ParserType pType, String trace_folder, double samplingRate) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new HBState(this.threadSet);
		this.handlerEvent = new HBEvent();

		this.rng = new Random();
		this.samplingRate = samplingRate;
	}

	protected boolean skipEvent(HBEvent handlerEvent){
		// Only skip for R/W events
		if (!handlerEvent.getType().isAccessType()) return false;
		// if rng.nextDouble returns a value in [0, 1)
		// so if samplingRate == 1, nothing will be skipped
		return rng.nextDouble() >= samplingRate;
	}

	protected void postHandleEvent(HBEvent handlerEvent){}
}
