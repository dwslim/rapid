package cmd;

import parse.ParserType;

public class CmdOptions {
	
	public ParserType parserType;
	public boolean online;
	public boolean multipleRace;
	public String path;
	public int verbosity;
	public String excludeList;
	public double samplingRate;
	public int samplingRNGSeed;

	public CmdOptions() {
		this.parserType = ParserType.CSV;
		this.online = true;
		this.multipleRace = true;
		this.path = null;
		this.verbosity = 0;
		this.excludeList = null;
		this.samplingRate = 1;
		samplingRNGSeed = 1234;
	}
	
	public String toString(){
		String str = "";
		str += "parserType		" + " = " + this.parserType.toString() 	+ "\n";
		str += "online			" + " = " + this.online					+ "\n";
		str += "multipleRace	" + " = " + this.multipleRace			+ "\n";	
		str += "path			" + " = " + this.path					+ "\n";
		str += "verbosity		" + " = " + this.verbosity				+ "\n";
		str += "excludeList		" + " = " + this.excludeList			+ "\n";
		str += "samplingRate	" + " = " + this.samplingRate			+ "\n";
		str += "samplingRate	" + " = " + this.samplingRNGSeed		+ "\n";
		return str;
	}

}
