package engine.racedetectionengine.minjian_epoch;

import java.util.HashSet;
import java.util.Random;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class MinjianEpochEngine extends RaceDetectionEngine<MinjianEpochState, MinjianEpochEvent>{

	private Random rng;
	private double samplingRate;

	public MinjianEpochEngine(ParserType pType, String trace_folder, double samplingRate) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new MinjianEpochState(this.threadSet);
		handlerEvent = new MinjianEpochEvent();

		this.rng = new Random();
		this.samplingRate = samplingRate;
	}

	@Override
	protected boolean skipEvent(MinjianEpochEvent handlerEvent) {
		// Only skip for R/W events
		if (!handlerEvent.getType().isAccessType()) return false;
		// if rng.nextDouble returns a value in [0, 1)
		// so if samplingRate == 1, nothing will be skipped
		return rng.nextDouble() >= samplingRate;
	}

	@Override
	protected void postHandleEvent(MinjianEpochEvent handlerEvent) {
	}

}
