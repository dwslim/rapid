import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb_epoch.HBEpochEngine;

public class HBEpoch {

	public HBEpoch() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		HBEpochEngine engine = new HBEpochEngine(options.parserType, options.path, options.samplingRate, options.samplingRNGSeed);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
		System.out.println("Num entry updated: "+(engine.state.updated));
		System.out.println("Num entry visited in join operations: "+((long)engine.state.numMax*engine.state.numThreads));


	}
}
