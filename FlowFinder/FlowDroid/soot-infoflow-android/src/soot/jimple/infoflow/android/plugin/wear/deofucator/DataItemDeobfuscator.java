package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataItemDeobfuscator {

	private static DataItemDeobfuscator instance = null;
	SootClass diClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static DataItemDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataItemDeobfuscator();
		return instance;
	}

	private DataItemDeobfuscator() {
		this.diClass = Scene.v().getSootClass(ExtraTypes.DATA_ITEM);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {

		boolean obf = false;
		try {

			SootMethod sm = diClass.getMethodByNameUnsafe("getUri");
			if (sm == null) {
				Type returnType = Scene.v().getTypeUnsafe(ExtraTypes.URI);
				List<SootMethod> methods = deobUtil.searchMethodByReturnType(diClass, returnType);
				if (methods.size() == 1) {
					obf = true;
					obfuscationMap.put(methods.get(0).getSubSignature(), "getUri");
					methods.get(0).setName("getUri");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

}
