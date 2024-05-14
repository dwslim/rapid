import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.minjian.MinjianEngine;

public class Minjian {

	public Minjian() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		MinjianEngine engine = new MinjianEngine(options.parserType, options.path, options.samplingRate);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
	}
}
