import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.uclock_epoch.UClockEpochEngine;

public class UClockEpoch {

	public UClockEpoch() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		UClockEpochEngine engine = new UClockEpochEngine(options.parserType, options.path, options.samplingRate);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
	}
}
