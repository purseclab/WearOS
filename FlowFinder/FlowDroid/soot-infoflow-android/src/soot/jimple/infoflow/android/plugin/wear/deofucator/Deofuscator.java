package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class Deofuscator {

	HashMap<String, String> mApiMap = MessageApiDeofuscator.getInstance().getObfuscationMap();
	HashMap<String, String> mApiCBMap = MessageApiDeofuscator.getInstance().getObfuscationCallbackMap();
	HashMap<String, String> mEventMap = MessageEventDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> mClientMap = MessageClientDeofuscator.getInstance().getObfuscationMap();
	HashMap<String, String> mClientCBMap = MessageClientDeofuscator.getInstance().getObfCallbackMap();

	HashMap<String, String> dClientMap = DataClientDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> dClientCBMap = DataClientDeobfuscator.getInstance().getObfuscationCallbackMap();

	HashMap<String, String> dApiMap = DataApiDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> dApiCBMap = DataApiDeobfuscator.getInstance().getObfuscationCallbackMap();

	HashMap<String, String> dMapMap = DataMapDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> dataItemMap = DataItemDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> dMapItemMap = DataMapItemDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> dEventMap = DataEventDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> pdmrMap = PutDMRDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> pdrMap = PutDRDeobfuscator.getInstance().getObfuscationMap();

	HashMap<String, String> callbackMap = CallbacksDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> servicesMap = WearableServiceObfuscator.getInstance().getObfuscationMap();

	HashMap<String, String> capabilityClientMap = CapabilityClientDeobfuscator.getInstance().getObfuscationMap();
	HashMap<String, String> capabilityApiMap = CapabilityApiDeobfuscator.getInstance().getObfuscationMap();

	DeofuscatorUtil util = DeofuscatorUtil.getInstance();

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Deobfuscates non-library method defined in a Component
	 * 
	 * @param sm
	 */
	public void deofucateMethod(SootMethod sm) {

		Body body = sm.retrieveActiveBody();
		for (Iterator<Unit> i = body.getUnits().snapshotIterator(); i.hasNext();) {
			try {
				Unit unit = i.next();
				Stmt stmt = (Stmt) unit;
				if (stmt.containsInvokeExpr()) {
					InvokeExpr iexp = (InvokeExpr) stmt.getInvokeExpr();
					String declaringClass = iexp.getMethod().getDeclaringClass().toString();
					String name = null;
					switch (declaringClass) {

					case ExtraTypes.MESSAGE_API:

						name = mApiMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;

					case ExtraTypes.MESSAGE_CLIENT:
						name = mApiMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.MESSAGE_EVENT:
						name = mEventMap.get(iexp.getMethod().getSubSignature());

						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;

					case ExtraTypes.DATA_CLIENT:
						name = dClientMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;

					case ExtraTypes.DATA_API:
						name = dApiMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;

					case ExtraTypes.DATA_EVENT:
						name = dEventMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.DATA_ITEM:
						name = dataItemMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.PUT_DATA_MAP_REQUEST:
						name = pdmrMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.PUT_DATA_REQUEST:
						name = pdrMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.DATA_MAP_ITEM:
						name = dMapItemMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							SootMethod smr = iexp.getMethod().getDeclaringClass().getMethodByName(name);
							SootMethodRef ref = smr.makeRef();
							iexp.setMethodRef(ref);
						}
						break;
					case ExtraTypes.DATA_MAP:
						name = dMapMap.get(iexp.getMethod().getSubSignature());
						if (name != null) {
							List<Type> params = iexp.getMethod().getParameterTypes();
							Type rtype = iexp.getMethod().getReturnType();
							List<SootMethod> methods = util.searchMethodsFull(iexp.getMethod().getDeclaringClass(),
									params, rtype);
							if (methods.size() > 0) {
								SootMethod smr = getMethodByName(methods, name);
								if (smr == null)
									throw new Exception("this is wrong! Two method with same name and signature");
								SootMethodRef ref = smr.makeRef();
								iexp.setMethodRef(ref);
							}
						}
						break;

					}
				}
			} catch (Exception e) {
				String msg = "Method: " + sm.toString() + " - Stmt: " + i.toString();
				logger.error(msg);
				e.printStackTrace();

			}
		}
	}

	private SootMethod getMethodByName(List<SootMethod> methods, String name) {
		List<SootMethod> res = new ArrayList<>();
		for (SootMethod sm : methods) {
			if (sm.getName().equals(name)) {
				res.add(sm);
			}
		}
		if (res.size() == 0)
			logger.error("Error looking " + name + " in " + methods.toString());
		if (res.size() != 1)
			return null;
		return res.get(0);
	}

	public void deobfuscateCallbacks(SootClass sc) {
		String value = null;
		HashMap<SootMethod, String> map = new HashMap<>();
		for (int i = 0; i < sc.getMethods().size(); i++) {
			SootMethod m = sc.getMethods().get(i);
			String signature = m.getSubSignature();

			value = callbackMap.get(signature);
			if (value != null) {
				map.put(m, value);
				// m.setName(value);
				continue;
			}
			value = servicesMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = dClientCBMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = mClientCBMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = mApiCBMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = dApiCBMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = capabilityClientMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
			value = capabilityApiMap.get(signature);
			if (value != null) {
				map.put(m, value);
				continue;
			}
		}
		if (map.size() > 0) {
			for (SootMethod sm : map.keySet())
				sm.setName(map.get(sm));
		}
	}
}
