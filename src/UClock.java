import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.uclock.UClockEngine;

public class UClock {

	public UClock() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		UClockEngine engine = new UClockEngine(options.parserType, options.path, options.samplingRate);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
	}
}
