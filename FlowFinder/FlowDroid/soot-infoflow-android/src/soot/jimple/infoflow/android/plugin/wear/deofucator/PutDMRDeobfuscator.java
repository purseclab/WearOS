package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class PutDMRDeobfuscator {

	private static PutDMRDeobfuscator instance = null;
	SootClass pdmrClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static PutDMRDeobfuscator getInstance() {
		if (instance == null)
			instance = new PutDMRDeobfuscator();
		return instance;
	}

	private PutDMRDeobfuscator() {
		this.pdmrClass = Scene.v().getSootClass(ExtraTypes.PUT_DATA_MAP_REQUEST);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();
	}

	public boolean deobfuscateClass() {
		boolean obfuscated = false;

		try {
			SootMethod sm = pdmrClass.getMethodByNameUnsafe("create");
			if (sm == null) {

				Type pdr = Scene.v().getTypeUnsafe(ExtraTypes.PUT_DATA_REQUEST);
				List<SootMethod> result = deobUtil.searchMethodsByReturnType(pdmrClass, pdr);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "asPutDataRequest");
					result.get(0).setName("asPutDataRequest");
				}

				result.clear();
				List<Type> params = new ArrayList<>();
				Type e = Scene.v().getTypeUnsafe(ExtraTypes.STRING_TYPE);
				params.add(e);
				Type ret = Scene.v().getTypeUnsafe(ExtraTypes.PUT_DATA_MAP_REQUEST);
				result = deobUtil.searchMethodsFull(pdmrClass, params, ret);
				if (result.size() > 0) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "create");
					result.get(0).setName("create");
					if (result.size() == 2) {
						obfuscated = true;
						obfuscationMap.put(result.get(1).getSubSignature(), "createWithAutoAppendedId");
						result.get(1).setName("createWithAutoAppendedId");
					}
				}

				result.clear();
				ret = Scene.v().getTypeUnsafe(ExtraTypes.DATA_MAP);
				result = deobUtil.searchMethodsByReturnType(pdmrClass, ret);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "getDataMap");
					result.get(0).setName("getDataMap");
				}

				result.clear();
				params.clear();
				e = Scene.v().getTypeUnsafe(ExtraTypes.DATA_MAP_ITEM);
				params.add(e);
				result = deobUtil.searchMethodsByParams(pdmrClass, params);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "createFromDataMapItem");
					result.get(0).setName("createFromDataMapItem");
				}

				result.clear();
				e = Scene.v().getTypeUnsafe(ExtraTypes.URI);
				result = deobUtil.searchMethodByReturnType(pdmrClass, e);
				if (result.size() == 1) {
					obfuscated = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "getUri");
					result.get(0).setName("getUri");
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return obfuscated;
	}
}
