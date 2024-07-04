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
		System.out.println("Num releases: " + engine.state.releases);
		System.out.println("Num joins: " + engine.state.joins);
		System.out.println("Num forks: " + engine.state.forks);
		System.out.println("Num threads: " + engine.state.numThreads);

		System.out.println("Num C traversed: " + engine.state.cTraversed);
		System.out.println("Num C updated: " + engine.state.cUpdated);
		System.out.println("Num U traversed: " + engine.state.uTraversed);
		System.out.println("Num U updated: " + engine.state.uUpdated);
		System.out.println("Num increments: " + engine.state.increments);
		System.out.println("Num acquires: " + engine.state.acquires);
		System.out.println("Num acquires skipped due to same threads: " + engine.state.sameThreadAcquireSkipped);
		System.out.println("Num acquire skipped due to u clock: " +(engine.state.acquires-engine.state.uAcquireSkipped-engine.state.sameThreadAcquireSkipped) );

		System.out.println("processed acquires " + engine.state.sameThreadAcquireSkipped);
		
		System.out.println("total work would have done: " + (((engine.state.acquires+engine.state.releases+engine.state.joins+engine.state.forks-engine.state.sameThreadAcquireSkipped)*engine.state.numThreads)+engine.state.releases));
		System.out.println("Num deep copies: " + engine.state.deepcopies);
		System.out.println("total work done: "+(engine.state.cTraversed+engine.state.uTraversed+(engine.state.deepcopies*engine.state.numThreads)) );
		System.out.println("saving from ordered list " + engine.state.saveOl);

	}
}
