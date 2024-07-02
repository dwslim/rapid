package engine.racedetectionengine.ordered_list;

import java.util.HashSet;
import java.util.Random;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class OrderedListEngine extends RaceDetectionEngine<OrderedListState, OrderedListEvent> {

    private Random rng;
    private double samplingRate;

    public OrderedListEngine(ParserType pType, String trace_folder, double samplingRate, int samplingRNGSeed) {
        super(pType);
        this.threadSet = new HashSet<Thread> ();
        initializeReader(trace_folder);
        this.state = new OrderedListState(this.threadSet);
        this.handlerEvent = new OrderedListEvent();

        this.rng = new Random(samplingRNGSeed);
        this.samplingRate = samplingRate;
    }

    protected boolean skipEvent(OrderedListEvent handlerEvent){
        // Only skip for R/W events
        if (!handlerEvent.getType().isAccessType()) return false;
        // if rng.nextDouble returns a value in [0, 1)
        // so if samplingRate == 1, nothing will be skipped
        return rng.nextDouble() >= samplingRate;
    }

    protected void postHandleEvent(OrderedListEvent handlerEvent){

    }
}
