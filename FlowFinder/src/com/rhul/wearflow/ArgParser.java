package com.rhul.wearflow;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgParser {
	
	public static final String APK = "a";
	public static final String JARS = "p";
	public static final String WRAPPER = "luw";
	public static final String TAINT_WRAPPER_FILE = "tw";
	public static final String SOURCE_SINKS_FILE = "s";
	public static final String STRING_ANALYSIS_CONFIG = "wsc";
	public static final String MATCHING_ANALYSIS = "wfa";
	public static final String MATCHING_INPUT = "imatch";
	
	private String[] args;
	private Options options;

	public ArgParser(String[] args) {
		options = new Options();
		this.args = args;
		addBoleanOptions();
		addArgumentOptions();
	}
	
	public CommandLine getCli() throws ParseException {
		CommandLineParser parser = new DefaultParser();

		// parse the command line arguments
		CommandLine line;
		try {
			line = parser.parse(options, args);
			return line;
		} catch (MissingOptionException mExc) {
			throw new ParseException("You must provide the required arguments");
		}

	}

	private void addArgumentOptions() {
		Option apk = Option.builder("a").argName("a").hasArg().desc("path to the apk to analize").build();
		Option jars = Option.builder("p").argName("p").hasArg().desc("path to android jars folder").build();		
		Option taintWrapperFile = Option.builder("tw").argName("tw")
				.desc("Specify taint wrapper file location").hasArg()
				.desc("Specify taint wrapper file location").build();
		
		Option sourceAndSinksFile = Option.builder("s").argName("s")
				.desc("Specify sources and sinks file location").hasArg()
				.desc("Specify sources and sinks file location").build();
		
		Option stringAnalysisFile = Option.builder("wsc").argName("wsc")
				.desc("Specify string analysis config file location").hasArg()
				.desc("Specify string analysis config file location").build();
		
		Option inputMatching = Option.builder("imatch").argName("imatch")
				.desc("Input of matching analysis").hasArg()
				.desc("Input of matching analysis").build();
		
		options.addOption(apk);
		options.addOption(jars);
		options.addOption(taintWrapperFile);
		options.addOption(sourceAndSinksFile);
		options.addOption(stringAnalysisFile);
		options.addOption(inputMatching);

	}

	private void addBoleanOptions() {
		Option match = new Option("wfa", "run the matching analysis");
		options.addOption(match);
		Option stringAnalysis = new Option("wsa", "run the string analysis");
		options.addOption(stringAnalysis);
	}
	
	public void printHelp() {
		HelpFormatter hFormatter = new HelpFormatter();
		hFormatter.printHelp("WearFlow", options, true);
	}

}
