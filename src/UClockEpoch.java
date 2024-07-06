import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetectionengine.uclock_epoch.UClockEpochEngine;

public class UClockEpoch {

	public UClockEpoch() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();
		UClockEpochEngine engine = new UClockEpochEngine(options.parserType, options.path, options.samplingRate,
				options.samplingRNGSeed);
		engine.analyzeTrace(options.multipleRace, options.verbosity);
		System.out.println("Time for analysis = " + engine.analysisTotalDuration + " milliseconds");
		System.out.println("Num releases: " + engine.state.numOriginalReleases);
		System.out.println("Num joins: " + engine.state.numOriginalJoins);
		System.out.println("Num forks: " + engine.state.numOriginalForks);
		System.out.println("Num threads: " + engine.state.numThreads);
		//System.out.println("Num original acquires: " + engine.state.numOriginalAcquires);
		//System.out.println("Num uclock acquires: " + engine.state.numUClockAcquires);
		//System.out.println("Num acquire skipped due to u clock: " + engine.state.uAcquireSkipped);
		//System.out.println("Num original releases: " + engine.state.numOriginalReleases);
		//which is not deep, writing for convience of profiling
		System.out.println("Num increments: " + engine.state.increments);
		System.out.println("Num acquires: " + engine.state.numOriginalAcquires);
		System.out.println("Num acquires skipped due to same threads: " + engine.state.sameThreadAcquireSkipped);
		System.out.println("total acquires skipped: " +(engine.state.uAcquireSkipped+engine.state.sameThreadAcquireSkipped) );	
		System.out.println("Num deep copies: " + engine.state.numUClockReleases);
		System.out.println("Num entry visited in join operations: "+(((long)engine.state.numUClockAcquires+engine.state.numUClockReleases+engine.state.numUClockJoins+engine.state.numUclockForks)*engine.state.numThreads*2));
		System.out.println("Num entry updated: "+((long)engine.state.threadCUpdated+engine.state.threadUUpdated+engine.state.lockCUpdated+engine.state.lockUUpdated));
		System.out.println("addtional o(1) u traversed: "+engine.state.uTraversed);
		//System.out.println("Num uclock joins: " + engine.state.numUClockJoins);
		System.out.println("Num thread C updated: " + engine.state.threadCUpdated);
		System.out.println("Num thread U updated: " + engine.state.threadUUpdated);
		//System.out.println("total work would have done: " + (((long)(engine.state.numOriginalAcquires+engine.state.numOriginalReleases+engine.state.numOriginalJoins+engine.state.numOriginalForks-engine.state.sameThreadAcquireSkipped)*engine.state.numThreads)+engine.state.numOriginalReleases));
		//System.out.println("total work  done: " + engine.state.uTraversed +engine.state.increments+(long)(engine.state.numUClockAcquires+engine.state.numUClockReleases+engine.state.numUClockJoins+engine.state.numOriginalForks)*2*engine.state.numThreads);


	}
}
