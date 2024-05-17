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
		System.out.println("Num original acquires: " + engine.state.numOriginalAcquires);
		System.out.println("Num uclock acquires: " + engine.state.numUClockAcquires);
		System.out.println("Num original releases: " + engine.state.numOriginalReleases);
		System.out.println("Num uclock releases: " + engine.state.numUClockReleases);
		System.out.println("Num original joins: " + engine.state.numOriginalJoins);
		System.out.println("Num uclock joins: " + engine.state.numUClockJoins);
	}
}
