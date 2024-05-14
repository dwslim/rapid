import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.hb.HBEngine;

public class HB {

	public HB() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		HBEngine engine = new HBEngine(options.parserType, options.path, options.samplingRate);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
	}
}
