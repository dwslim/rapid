import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.ordered_list.OrderedListEngine;

public class OrderedList {

	public OrderedList() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		OrderedListEngine engine = new OrderedListEngine(options.parserType, options.path, options.samplingRate, options.samplingRNGSeed);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
		System.out.println("Num acquires: " + engine.state.acquires);
		System.out.println("Num releases: " + engine.state.releases);
		System.out.println("Num joins: " + engine.state.joins);
		System.out.println("Num forks: " + engine.state.forks);
		System.out.println("Num acq traversed: " + engine.state.acqTraversed);
		System.out.println("Num acq updated: " + engine.state.acqUpdated);
		System.out.println("Num deep copies: " + engine.state.deepcopies);
	}
}
