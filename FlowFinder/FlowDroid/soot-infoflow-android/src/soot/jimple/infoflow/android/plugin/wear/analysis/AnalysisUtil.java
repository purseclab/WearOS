package soot.jimple.infoflow.android.plugin.wear.analysis;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.infoflow.android.plugin.wear.exception.BlockAnalysisException;
import soot.jimple.infoflow.android.usc.sql.string.StringResult;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.infoflow.plugin.wear.extras.Keys;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;

public class AnalysisUtil {

	private static AnalysisUtil instance = null;
	protected static List<StringResult> stringValues;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private AnalysisUtil() {
		stringValues = StringAnalysis.getStringResults();
	}

	public static AnalysisUtil getInstance() {
		if (instance == null)
			instance = new AnalysisUtil();
		return instance;
	}

	public InvokeExpr getInvokeExpr(Unit sunit) {
		InvokeExpr tmpIexpr = null;
		AssignStmt asigStmt = null;
		if (AssignStmt.class.isAssignableFrom(sunit.getClass())) {
			asigStmt = (AssignStmt) sunit;
			Value rightOp = asigStmt.getRightOp();
			if (InvokeExpr.class.isAssignableFrom(rightOp.getClass()))
				tmpIexpr = (InvokeExpr) rightOp;
		} else if (InvokeStmt.class.isAssignableFrom(sunit.getClass())) {
			tmpIexpr = ((InvokeStmt) sunit).getInvokeExpr();

		}
		return tmpIexpr;
	}

	public SimpleEntry<String, String> parseManifestResult(SootClass sclass, HashMap<String, Set<String>> manifest) {
		if (sclass.getSuperclass().getName().contains(ExtraTypes.WEARABLE_LISTENER_SERVICE)) {
			Set<String> pathConfig = manifest.get(sclass.getShortName());
			if (pathConfig != null) {
				String pathType = (String) pathConfig.toArray()[0];
				String path = (String) pathConfig.toArray()[1];
				AbstractMap.SimpleEntry<String, String> entry = new AbstractMap.SimpleEntry<>(pathType, path);
				return entry;

			}
		}
		return null;
	}

	public boolean isComparison(InvokeExpr iexpr) {
		if (iexpr == null)
			return false;
		String methodName = iexpr.getMethod().getName();

		if (methodName.equals("equals") || methodName.equals("equalsIgnoreCase") || methodName.equals("compareTo")
				|| methodName.equals("compareToIgnoreCase") || methodName.equals("matches")) {
			return true;
		}
		return false;
	}

	public boolean isAndroidLibrary(SootClass sc) {
		String name = sc.getPackageName();

		String[] android = { "android.net.wifi.hotspot2", "android.animation", "androidx.annotation",
				"java.security.acl", "org.apache.http.params", "android.net.sip", "java.time.zone",
				"android.hardware.display", "java.nio.channels", "android.net.wifi", "android.speech.tts",
				"android.content.pm", "android.net.ssl", "android.text.format", "android", "org.xml.sax.helpers",
				"android.media.audiofx", "android.widget", "android.service.quicksettings", "android.app",
				"android.view.textclassifier", "javax.xml", "org", "android.service.voice", "android.hardware.input",
				"java.security.cert", "java.time.format", "android.telephony.data", "org.w3c.dom", "java.util.logging",
				"android.service.media", "android.app.role", "android.provider", "java.util.zip",
				"android.view.animation", "android.telephony.cdma", "android.service.vr", "android.app.backup",
				"javax.security.auth", "javax.xml.transform.stream", "android.telecom", "dalvik.bytecode",
				"android.app.assist", "android.hardware.camera2.params", "javax.sql", "android.annotation",
				"android.hardware", "android.app.usage", "java.net", "android.os.health", "android.text.util",
				"assets.webkit", "java.lang.invoke", "java.lang.annotation", "android.app.job", "java.util.concurrent",
				"android.gesture", "android.graphics", "android.media.projection", "android.renderscript",
				"android.view.textservice", "android.icu.util", "android.view.accessibility",
				"android.security.keystore", "java.nio", "java.security.interfaces", "javax.crypto",
				"android.graphics.fonts", "java.util.concurrent.locks", "java.util.jar", "android.service.wallpaper",
				"javax.net", "android.net.wifi.p2p.nsd", "android.icu.math", "android.service.textservice",
				"android.view.contentcapture", "javax.microedition.khronos.egl", "android.os.storage", "org.w3c",
				"javax.xml.xpath", "java.time.temporal", "android.opengl", "androidx", "android.webkit",
				"android.telephony.emergency", "org.apache.http", "java.time", "java.awt", "android.net.wifi.p2p",
				"android.bluetooth", "javax.xml.transform.dom", "android.system", "android.telephony.gsm",
				"android.se.omapi", "java.lang", "javax.security.auth.login", "javax.net.ssl", "java.awt.font",
				"android.icu", "android.service", "android.accessibilityservice", "android.print",
				"android.service.autofill", "android.mtp", "android.telephony", "android.graphics.pdf", "dalvik",
				"org.xml", "javax.xml.datatype", "android.service.chooser", "assets", "org.apache.http.conn.ssl",
				"java.util.function", "java.security.spec", "java.nio.charset.spi", "android.net.nsd", "java.util",
				"javax.xml.parsers", "javax.xml.transform", "android.os.strictmode", "android.graphics.text",
				"android.net.rtp", "java.nio.channels.spi", "android.content.res", "javax.crypto.spec",
				"org.xmlpull.v1", "android.nfc.tech", "java.beans", "android.print.pdf", "android.hardware.fingerprint",
				"java.nio.file.spi", "javax.xml.namespace", "java.util.concurrent.atomic", "android.speech",
				"org.apache.http.conn.scheme", "android.view.inspector", "android.icu.text", "javax.security",
				"javax.xml.validation", "org.apache.http.conn", "android.accounts", "android.net",
				"android.service.restrictions", "org.xmlpull", "dalvik.annotation", "android.media",
				"javax.microedition.khronos", "android.sax", "android.security", "android.text", "java.util.prefs",
				"javax.microedition.khronos.opengles", "org.xml.sax.ext", "android.location", "java.lang.ref",
				"java.lang.reflect", "java.util.regex", "android.hardware.camera2", "javax.security.auth.callback",
				"android.text.method", "java.security", "android.preference", "java.time.chrono",
				"android.net.wifi.rtt", "dalvik.system", "android.hardware.biometrics", "org.json", "android.net.http",
				"java.text", "android.nfc", "android.service.notification", "android.companion", "android.media.tv",
				"android.icu.lang", "android.graphics.drawable", "java", "android.media.midi", "android.hardware.usb",
				"android.media.session", "java.sql", "javax.crypto.interfaces", "android.telephony.mbms",
				"android.text.style", "java.nio.file.attribute", "android.app.admin", "javax.security.auth.x500",
				"android.app.slice", "org.apache", "android.transition", "android.media.browse", "java.nio.file",
				"android.printservice", "android.drm", "android.util", "org.w3c.dom.ls", "java.nio.charset",
				"android.database.sqlite", "android.se", "android.net.wifi.hotspot2.pps", "android.view.autofill",
				"javax.xml.transform.sax", "java.util.stream", "javax.security.cert", "android.service.dreams",
				"org.xmlpull.v1.sax2", "android.service.carrier", "android.inputmethodservice", "assets.images",
				"java.io", "android.nfc.cardemulation", "android.database", "android.net.wifi.aware",
				"android.net.wifi.hotspot2.omadm", "android.view", "android.appwidget", "android.telephony.euicc",
				"javax.microedition", "android.os", "javax", "android.graphics.drawable.shapes", "android.bluetooth.le",
				"android.content", "java.math", "org.xml.sax", "android.media.effect", "android.view.inputmethod" };

		List<String> packages = new ArrayList<String>(Arrays.asList(android));
		for (String pname : packages) {
			if (pname.equals(name))
				return true;
		}

		return false;
	}

	public List<Unit> filterSlice(List<Unit> slice, Unit unit, SootMethod sm) throws BlockAnalysisException {
		ExceptionalBlockGraph bcfg = new ExceptionalBlockGraph(sm.getActiveBody());
		int blockIndex = getBlockFromUnitFull(unit, bcfg);
		if (blockIndex == -1)
			throw new BlockAnalysisException("unit not found in slice");
		List<Integer> predecesors = getPredecesorBlocks(blockIndex, bcfg);
		List<Unit> filteredSlice = new ArrayList<Unit>();
		for (Unit u : slice) {
			Integer uindex = getBlockFromUnitFull(u, bcfg);
			if (predecesors.contains(uindex))
				filteredSlice.add(u);
		}
		return filteredSlice;
	}

	/**
	 * Gets the block number which correspond to an unit Throws and Exception if
	 * there is a Unit with the same hasCode. This shouldn't be the case in
	 * principle This method is for testing. Change to getBlockFromUnit in final
	 * version
	 * 
	 * @param target
	 * @param bcfg
	 * @return
	 * @throws Exception
	 */
	public int getBlockFromUnitFull(Unit target, ExceptionalBlockGraph bcfg) throws BlockAnalysisException {
		int index = -1, count = 0;
		for (Block b : bcfg.getBlocks()) {
			for (Iterator<Unit> it = b.iterator(); it.hasNext();) {
				Unit u = it.next();
				if (u.equals(target)) {
					count++;
					index = b.getIndexInMethod();
				}
			}
		}
		if (count > 1)
			throw new BlockAnalysisException("repeated stmt in function");
		return index;
	}

	public int getBlockFromUnit(Unit target, ExceptionalBlockGraph bcfg) {
		int index = -1;
		for (Block b : bcfg.getBlocks()) {
			for (Iterator<Unit> it = b.iterator(); it.hasNext();) {
				Unit u = it.next();
				if (u.equals(target)) {
					index = b.getIndexInMethod();
				}
			}
		}

		return index;
	}

	/**
	 * Get predecessors blocks from the block specified by index This method assumes
	 * there is a unique path from block[index] to block[0], e.g. there is only one
	 * predecessor per block.
	 * 
	 * @param index
	 * @param bcfg
	 * @throws Exception
	 */
	public List<Integer> getPredecesorBlocks(int index, ExceptionalBlockGraph bcfg) throws BlockAnalysisException {

		Block tmp = bcfg.getBlocks().get(index);
		List<Integer> predecesors = new ArrayList<Integer>();
		predecesors.add(tmp.getIndexInMethod());
		while (!tmp.getPreds().isEmpty()) {
			List<Block> preds = tmp.getPreds();
			Block item = preds.get(0);
			predecesors.add(item.getIndexInMethod());
			tmp = item;
			if (preds.size() > 1)
				throw new BlockAnalysisException("Block with more than 1 predecesor");

		}
		return predecesors;
	}

	public int getUnitLine(Unit unit, SootMethod sMethod) {
		int index = 1;
		SootClass sclass = sMethod.getDeclaringClass();
		for (SootMethod sm : sclass.getMethods()) {
			for (Unit u : sm.getActiveBody().getUnits()) {
				if (u.equals(unit))
					break;
				else
					index++;
			}
		}
		return index;
	}

	public String getStringValue(StringResult search) {
		for (StringResult value : stringValues) {
			// logger.debug(value.toString());
			if (value.equals(search)) {
				if (value.getValues().size() == 1) {
					return value.getValues().get(0);
				} else {
					logger.debug("Multiple valaues for" + search.getJimple() + "\n using default value");
				}
			}
		}
		return null;
	}

	public String stackToString(List<Unit> stack) {
		StringBuffer tmp = new StringBuffer();
		for (Unit unit : stack) {
			tmp = tmp.append(unit.toString() + "\n");
		}
		return tmp.toString();

	}

	public String getStringValue(SootMethod sm, Unit unit, int line, int offset, int nthparam) {
		String value = null;
		StringResult tmp = new StringResult(sm, line, offset, nthparam, unit.toString());
		value = getStringValue(tmp);
		return value;
	}

	public int getLineNumber(Unit unit) {
		int line = unit.getJavaSourceStartLineNumber();
		if (line == -1) {
			logger.debug("Line not found, check offset");
		}
		return line;
	}

	public int getOffset(Unit unit) {
		int bytecodeOffset = -1;

		Stmt stmt = (Stmt) unit;
		if (stmt != null) {
			if (stmt.containsInvokeExpr()) {
				for (Tag t : stmt.getTags()) {
					if (t instanceof BytecodeOffsetTag)
						bytecodeOffset = ((BytecodeOffsetTag) t).getBytecodeOffset();
				}
			}
		}

		return bytecodeOffset;

	}

	/**
	 * Check if a method needs instrumentation. Specifically checks if there is any
	 * local of type [DataClient, DataMap, ...]
	 * 
	 * @param sMethod
	 * @return
	 */
	public boolean instrumentationNeeded(SootMethod sMethod) {
		List<String> relevant = new ArrayList<String>(Arrays.asList(ExtraTypes.classesForInstrumentation));

		for(String extraClass : ExtraTypes.getObfInstClasses()) {
			relevant.add(extraClass);
		}
		
		for (Local local : sMethod.retrieveActiveBody().getLocals()) {
			if (relevant.contains(local.getType().toString()))
				return true;
		}
		return false;
	}

	public String formatFlow(JSONObject jsonObj) {

		String sourceMethod = (String) jsonObj.get(Keys.SOURCE_METHOD);
		String sinkMethod = (String) jsonObj.get(Keys.SINK_METHOD);

		String sinkPath = (String) jsonObj.get(Keys.SINK_PATH);
		String sourcePath = (String) jsonObj.get(Keys.SOURCE_PATH);

		String sourceKey = (String) jsonObj.get(Keys.SOURCE_KEY);
		String sinkKey = (String) jsonObj.get(Keys.SINK_KEY);

		if (sourceKey == null)
			sourceKey = "null";

		if (sinkKey == null)
			sinkKey = "null";
		String formated = "\nSource-> Method:" + sourceMethod + " path: " + sourcePath + " key:" + sourceKey + "\n"
				+ "Sink-> Met:" + sinkMethod + " path: " + sinkPath + " key:" + sinkKey;

		return formated;
	}

	/**
	 * Add a Field to the class. Type is infer from register
	 * 
	 * @param register
	 * @param sClass
	 * @return
	 */
	public SootField addGlobalVar(ValueBox register, SootClass sClass) {
		Value value = register.getValue();
		Type type = value.getType();
		Integer number = sClass.getFields().size();
		SootField newField = new SootField("var" + number, type, Modifier.STATIC);
		sClass.addField(newField);
		return newField;
	}

	public Local addLocal(ValueBox register, SootMethod sMethod) {
		Type type = register.getValue().getType();
		Body body = sMethod.retrieveActiveBody();
		Integer count = body.getLocalCount() + 1;
		Local local = Jimple.v().newLocal("tmp" + count, type);
		body.getLocals().add(local);
		return local;
	}

	protected AssignStmt generateDummyInstrumentation(Body body) {
		Local arg = Jimple.v().newLocal("k10", RefType.v("java.lang.String"));
		body.getLocals().add(arg);
		AssignStmt astm = Jimple.v().newAssignStmt(arg, StringConstant.v("HELLO"));
		return astm;
	}

	public List<Unit> getRelevantUnits(SootMethod sMethod, Unit startUnit, Unit endUnit) {
		UnitPatchingChain units = sMethod.retrieveActiveBody().getUnits();
		boolean addUnits = false;
		List<Unit> relevantUnits = new ArrayList<Unit>();

		for (Iterator<Unit> i = units.snapshotIterator(); i.hasNext();) {
			Unit unit = i.next();
			if (unit.equals(startUnit))
				addUnits = true;
			if (addUnits) {
				relevantUnits.add(unit);
			}
			if (unit.equals(endUnit))
				break;
		}

		return relevantUnits;
	}

	/**
	 * Returns all the units between defUnit and callUnit in order.
	 * 
	 * @param defUnit
	 * @param callUnit
	 * @param body
	 * @return
	 */
	public List<Unit> getUnitsBetween(Unit defUnit, Unit callUnit, Body body) {
		List<Unit> slice = new ArrayList<Unit>();
		boolean addUnits = false;
		for (Unit unit : body.getUnits()) {
			if (!addUnits && unit.equals(defUnit))
				addUnits = true;

			if (addUnits)
				slice.add(unit);

			if (unit.equals(callUnit))
				break;
		}
		return slice;
	}

	public Local addLocal(String dataClientType, SootMethod sMethod) {
		Body body = sMethod.retrieveActiveBody();
		Integer count = body.getLocalCount() + 1;
		Local arg = Jimple.v().newLocal("k" + count, RefType.v(dataClientType));
		body.getLocals().add(arg);
		return arg;
	}

}
