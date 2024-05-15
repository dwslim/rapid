package engine.racedetectionengine.uclock_epoch;

import java.util.HashSet;
import java.util.Random;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class UClockEpochEngine extends RaceDetectionEngine<UClockEpochState, UClockEpochEvent>{

	private Random rng;
	private double samplingRate;

	public UClockEpochEngine(ParserType pType, String trace_folder, double samplingRate) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new UClockEpochState(this.threadSet);
		handlerEvent = new UClockEpochEvent();

		this.rng = new Random();
		this.samplingRate = samplingRate;
	}

	@Override
	protected boolean skipEvent(UClockEpochEvent handlerEvent) {
		// Only skip for R/W events
		if (!handlerEvent.getType().isAccessType()) return false;
		// if rng.nextDouble returns a value in [0, 1)
		// so if samplingRate == 1, nothing will be skipped
		return rng.nextDouble() >= samplingRate;
	}

	@Override
	protected void postHandleEvent(UClockEpochEvent handlerEvent) {
	}

}
