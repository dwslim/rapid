import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.minjian_epoch.MinjianEpochEngine;

public class MinjianEpoch {

	public MinjianEpoch() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		MinjianEpochEngine engine = new MinjianEpochEngine(options.parserType, options.path, options.samplingRate);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
	}
}
