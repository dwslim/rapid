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
	}
}
