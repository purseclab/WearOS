package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Pack;
import soot.PackManager;
import soot.SceneTransformer;
import soot.Transform;
import soot.jimple.infoflow.android.usc.sql.string.JavaAndroid;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;

public class StringAnalysis {

	public static Map<String, Set<String>> result;
	public static List<StringResult> stringResults = new ArrayList<StringResult>();
	// private static final Logger logger =
	// LoggerFactory.getLogger(StringAnalysis.class);

	public static void runStringAnalysis(String path, String packageName, String pathPlatform, String pathApk) {
		try {

			int index = pathApk.lastIndexOf("/");
			String apkFolder = pathApk.substring(0, index);
			String apkName = pathApk.substring(index + 1);
			String androidJar = pathPlatform;
			String content = new String(Files.readAllBytes(Paths.get(path)));
			String[] configs = content.split("\n");
			// String androidJar = configs[0].split("=")[1];
			// String apkFolder = configs[1].split("=")[1];
			// String apkName = "/" + configs[2].split("=")[1];
			int loopItr = Integer.parseInt(configs[0].split("=")[1]);

			Map<String, List<Integer>> target = new HashMap<>();
			for (int i = 2; i < configs.length; i++) {
				if (configs[i].trim().equals("END"))
					break;
				if (!configs[i].startsWith("#")) {
					String[] targets = configs[i].split("@");
					String hotspot = targets[0];
					List<Integer> paraSet = new ArrayList<>();
					for (int j = 1; j < targets.length; j++)
						paraSet.add(Integer.parseInt(targets[j]));
					target.put(hotspot, paraSet);
				}
			}
			String apkPath = apkFolder + apkName;
			setupAndInvokeSoot(apkPath, androidJar, target, loopItr, apkFolder, packageName);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static void setupAndInvokeSoot(String apkPath, String androidJarPath,
			final Map<String, List<Integer>> targetSignature, final int loopItr, final String outputPath,
			final String packageName) {

		String packName = "wjtp";
		String phaseName = "wjtp.string";

		Pack pack = PackManager.v().getPack(packName);
		pack.add(new Transform(phaseName, new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map<String, String> options) {

				JavaAndroid ja = new JavaAndroid(targetSignature, loopItr, outputPath, packageName);
				result = ja.getInterpretedValues();
				stringResults.addAll(ja.getStringResults());

			}
		}));
		PackManager.v().getPack("wjtp").get(phaseName).apply();
		PackManager.v().getPack("wjtp").remove(phaseName);

	}

	public static List<StringResult> getStringResults() {
		return stringResults;
	}
}
