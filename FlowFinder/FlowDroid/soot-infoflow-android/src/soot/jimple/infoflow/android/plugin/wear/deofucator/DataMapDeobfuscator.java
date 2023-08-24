package soot.jimple.infoflow.android.plugin.wear.deofucator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.infoflow.plugin.wear.extras.DataMapGetFunctions;
import soot.jimple.infoflow.plugin.wear.extras.DataMapPutFunctions;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;

public class DataMapDeobfuscator {

	private static DataMapDeobfuscator instance = null;
	SootClass dataMapClass;
	HashMap<String, String> obfuscationMap;
	DeofuscatorUtil deobUtil;

	public HashMap<String, String> getObfuscationMap() {
		return obfuscationMap;
	}

	public static DataMapDeobfuscator getInstance() {
		if (instance == null)
			instance = new DataMapDeobfuscator();
		return instance;
	}

	private DataMapDeobfuscator() {
		this.dataMapClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
		obfuscationMap = new HashMap<String, String>();
		deobUtil = DeofuscatorUtil.getInstance();

	}

	public boolean deobfuscateDataMap() {
		boolean obf = false;

		try {
			deobfuscateGets();
			deobfuscatePuts();

			SootMethod res = dataMapClass.getMethodByNameUnsafe("putAll");
			if (res == null) {
				List<Type> params = new ArrayList<>();
				params.add(RefType.v(ExtraTypes.DATA_MAP));
				List<SootMethod> result = deobUtil.searchMethodsFull(dataMapClass, params, VoidType.v());
				if (result.size() == 1) {
					obf = true;
					obfuscationMap.put(result.get(0).getSubSignature(), "putAll");
					result.get(0).setName("putAll");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return obf;
	}

	private boolean existMethodName(String name) {
		for (SootMethod sm : dataMapClass.getMethods()) {
			if (sm.getName().equals(name))
				return true;
		}
		return false;
	}

	private void deobfuscateGets() {
		for (DataMapGetFunctions func : DataMapGetFunctions.values()) {
			Type stringType = RefType.v(ExtraTypes.STRING_TYPE);
			Type returnType = null;
			List<Type> params = new ArrayList<>();
			params.add(stringType);

			if (existMethodName(func.name()))
				continue;

			List<SootMethod> result = null;
			switch (func.name()) {
			case "getString":
				returnType = stringType;
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getString");
					result.get(0).setName("getString");
				}
				break;
			case "getInt":
				returnType = IntType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getInt");
					result.get(0).setName("getInt");
				}
				break;
			case "getLong":
				returnType = LongType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getLong");
					result.get(0).setName("getLong");
				}
				break;

			case "getDouble":
				returnType = DoubleType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getDouble");
					result.get(0).setName("getDouble");
				}
				break;
			case "getFloat":
				returnType = FloatType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getFloat");
					result.get(0).setName("getFloat");
				}
				break;
			case "getBoolean":
				returnType = BooleanType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getBoolean");
					result.get(0).setName("getBoolean");
				}
				break;
			case "getByte":
				returnType = ByteType.v();
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getByte");
					result.get(0).setName("getByte");
				}
				break;
			case "getByteArray":
				returnType = ArrayType.v(ByteType.v(), 1);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getByteArray");
					result.get(0).setName("getByteArray");
				}
				break;
			case "getStringArray":
				returnType = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getStringArray");
					result.get(0).setName("getStringArray");
				}
				break;
			case "getLongArray":
				returnType = ArrayType.v(LongType.v(), 1);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getLongArray");
					result.get(0).setName("getLongArray");
				}
				break;
			case "getFloatArray":
				returnType = ArrayType.v(FloatType.v(), 1);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getFloatArray");
					result.get(0).setName("getFloatArray");
				}
				break;
			case "getAsset":
				returnType = RefType.v(ExtraTypes.ASSETS);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getAsset");
					result.get(0).setName("getAsset");
				}
				break;
			case "getDataMap":
				returnType = RefType.v(ExtraTypes.DATA_MAP);
				result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
				if (result.size() == 1) {
					obfuscationMap.put(result.get(0).getSubSignature(), "getDataMap");
					result.get(0).setName("getDataMap");
				}
				break;
			// case "getStringArrayList":
			// returnType = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
			// result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
			// if (result.size() == 1) {
			// obfuscationMap.put(result.get(0).getSubSignature(), "getStringArrayList");
			// result.get(0).setName("getStringArrayList");
			// }
			// break;
			// case "getDataMapArrayList":
			// returnType = ArrayType.v(RefType.v(ExtraTypes.DATA_MAP), 1);
			// result = deobUtil.searchMethodsFull(dataMapClass, params, returnType);
			// if (result.size() == 1) {
			// obfuscationMap.put(result.get(0).getSubSignature(), "getDataMapArrayList");
			// result.get(0).setName("getDataMapArrayList");
			// }
			// break;
			}
		}
	}

	private void deobfuscatePuts() {
		for (DataMapPutFunctions func : DataMapPutFunctions.values()) {
			SootMethod sm = dataMapClass.getMethodByNameUnsafe(func.name());
			Type voidType = VoidType.v();
			Type type1 = null;
			Type stringType = RefType.v(ExtraTypes.STRING_TYPE);
			List<Type> params = new ArrayList<>();
			params.add(stringType);

			List<SootMethod> result = null;

			if (sm == null) {
				switch (func.name()) {
				case "putString":
					params.add(stringType);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putString");
						result.get(0).setName("putString");
					}
					break;
				case "putInt":
					type1 = IntType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putInt");
						result.get(0).setName("putInt");
					}
					break;
				case "putLong":
					type1 = LongType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putLong");
						result.get(0).setName("putLong");
					}
					break;

				case "putDouble":
					type1 = DoubleType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putDouble");
						result.get(0).setName("putDouble");
					}
					break;
				case "putFloat":
					type1 = FloatType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putFloat");
						result.get(0).setName("putFloat");
					}
					break;
				case "putBoolean":
					type1 = BooleanType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putBoolean");
						result.get(0).setName("putBoolean");
					}
					break;
				case "putByte":
					type1 = ByteType.v();
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putByte");
						result.get(0).setName("putByte");
					}
					break;
				case "putByteArray":
					type1 = ArrayType.v(ByteType.v(), 1);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putByteArray");
						result.get(0).setName("putByteArray");
					}
					break;
				case "putStringArray":
					type1 = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putStringArray");
						result.get(0).setName("putStringArray");
					}
					break;
				case "putLongArray":
					type1 = ArrayType.v(LongType.v(), 1);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putLongArray");
						result.get(0).setName("putLongArray");
					}
					break;
				case "putFloatArray":
					type1 = ArrayType.v(FloatType.v(), 1);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putFloatArray");
						result.get(0).setName("putFloatArray");
					}
					break;
				case "putAsset":
					type1 = RefType.v(ExtraTypes.ASSETS);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putAsset");
						result.get(0).setName("putAsset");
					}
					break;
				case "putDataMap":
					type1 = RefType.v(ExtraTypes.DATA_MAP);
					params.add(type1);
					result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
					if (result.size() == 1) {
						obfuscationMap.put(result.get(0).getSubSignature(), "putDataMap");
						result.get(0).setName("putDataMap");
					}
					break;
				// case "putStringArrayList":
				// type1 = ArrayType.v(RefType.v(ExtraTypes.STRING_TYPE), 1);
				// params.add(type1);
				// result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
				// if (result.size() == 1) {
				// obfuscationMap.put(result.get(0).getSubSignature(), "putStringArrayList");
				// result.get(0).setName("putStringArrayList");
				// }
				// break;
				// case "putDataMapArrayList":
				// type1 = ArrayType.v(RefType.v(ExtraTypes.DATA_MAP), 1);
				// params.add(type1);
				// result = deobUtil.searchMethodsFull(dataMapClass, params, voidType);
				// if (result.size() == 1) {
				// obfuscationMap.put(result.get(0).getSubSignature(), "putDataMapArrayList");
				// result.get(0).setName("putDataMapArrayList");
				// }
				// break;

				}

			}

		}
	}

}
