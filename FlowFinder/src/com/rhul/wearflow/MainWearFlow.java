package com.rhul.wearflow;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.rhul.wearflow.flowAnalyser.FlowMatch;
import com.rhul.wearflow.flowAnalyser.MatchingFlowAnalysis;

import soot.jimple.infoflow.cmd.MainClass;

public class MainWearFlow {
	private static final Logger logger = LoggerFactory.getLogger(MainWearFlow.class);
	// private final Logger logger = LoggerFactory.getLogger(getClass());
	private static Config config = Config.getInstance();

	public static void main(String[] args) {

		System.setProperty("java.util.logging.SimpleFormatter.format",
				"%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
		ArgParser aParser = new ArgParser(args);

		try {

			final CommandLine cli = aParser.getCli();

			if (cli.hasOption("help")) {
				aParser.printHelp();
				System.exit(0);
			}

			if (cli.hasOption(ArgParser.MATCHING_ANALYSIS)) {

				if (cli.hasOption(ArgParser.MATCHING_INPUT)) {

					String path = cli.getOptionValue(ArgParser.MATCHING_INPUT);

					String[] files = path.split(";");
					if (files.length == 2) {

						String mobileOutput = files[0];
						String wearOutput = files[1];
						MatchingFlowAnalysis flowAnalysis = new MatchingFlowAnalysis();
						flowAnalysis.matchFlows(mobileOutput, wearOutput);
						logger.info("List of matched flow:");
						for (FlowMatch f : flowAnalysis.getFlowsMatched())
							logger.info(f.toString());
					}
				} else
					logger.error("No files for matching given. Please provide the files with the option -imatch");

			} else if (cli.hasOption(ArgParser.APK) && cli.hasOption(ArgParser.JARS)) {
				final String apkPath = cli.getOptionValue(ArgParser.APK);
				final String androidJars = cli.getOptionValue(ArgParser.JARS);
				config.setMobileApkPath(apkPath);
				config.setAndroidJars(cli.getOptionValue(ArgParser.JARS));
				config.setTaintWrapperFilePath(cli.getOptionValue(ArgParser.TAINT_WRAPPER_FILE));
				config.setSourceSinksFilePath(cli.getOptionValue(ArgParser.SOURCE_SINKS_FILE));
				config.setStringFilePath(cli.getOptionValue(ArgParser.STRING_ANALYSIS_CONFIG));

				String[] wargs = getParams(apkPath, androidJars);
				MainClass.main(wargs);
			} else
				logger.error("Please provide a valid input");

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private static String[] getParams(String apkPath, String androidJars) throws Exception {
		String taintWrapperFilePath = "";
		if (config.getTaintWrapperFilePath() == null || "".equals(config.getTaintWrapperFilePath())) {
			Path currentRelativePath = Paths.get("EasyTaintWrapperSource.txt");
			taintWrapperFilePath = currentRelativePath.toAbsolutePath().toString();
		} else {
			taintWrapperFilePath = config.getTaintWrapperFilePath();
		}

		String sourcesSinksFilePath = "";
		if (config.getSourceSinksFilePath() == null || "".equals(config.getSourceSinksFilePath())) {
			Path currentRelativePath = Paths.get("SourcesAndSinks.txt");
			sourcesSinksFilePath = currentRelativePath.toAbsolutePath().toString();
		} else {
			sourcesSinksFilePath = config.getSourceSinksFilePath();
		}

		String stringAnalysisFilePath = "";
		if (config.getStringFilePath() == null || "".equals(config.getStringFilePath())) {
			Path currentRelativePath = Paths.get("config2.txt");
			stringAnalysisFilePath = currentRelativePath.toAbsolutePath().toString();

		} else
			stringAnalysisFilePath = config.getStringFilePath();

		String[] args = new String[] { "-a", apkPath, "-p", androidJars, "-t", taintWrapperFilePath, "-s",
				sourcesSinksFilePath, "-d", "-ct", "180", "-dt", "300", "-wsa", "-wsc", stringAnalysisFilePath };

		return args;

	}

}
