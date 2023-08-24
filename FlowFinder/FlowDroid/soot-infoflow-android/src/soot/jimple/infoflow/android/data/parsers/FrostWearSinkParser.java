package soot.jimple.infoflow.android.data.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.*;
import soot.util.Chain;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import java.util.ArrayList;
import java.util.List;

public class FrostWearSinkParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    // Constructor
    public FrostWearSinkParser() {
        
    }

    private String identifyDataMap(String wearablePackageName) {
        String dataMapClass = null;
        if (Scene.v().containsClass(ExtraTypes.DATA_MAP)) {
            dataMapClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP).getName();
            logger.info("Non-obfuscated DataMap found");
            return dataMapClass;
        }

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.getPackageName().equals(wearablePackageName) ) {
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isStatic()) {
                        if (sm.isConcrete()) {
                            if (sm.getParameterCount() == 1) {
                                if (sm.getParameterType(0).toString().equals("byte[]")) {
                                    if (sm.getReturnType().toString().equals(sc.getName())) {
                                        for(Unit unit : sm.retrieveActiveBody().getUnits()) {
                                            if(unit.toString().contains("Unable to convert data")) {
                                                dataMapClass = sc.getName();
                                                logger.info("Obfuscated DataMap found: "+dataMapClass);
                                                return dataMapClass;
                                            }
                                        }
                                    }
                                }
                            } else if (sm.getParameterCount() == 5) {
                                if (sm.getParameterType(0).toString().equals("java.lang.String") &&
                                    sm.getParameterType(1).toString().equals("java.lang.Object") &&
                                    sm.getParameterType(2).toString().equals("java.lang.String") &&
                                    sm.getParameterType(3).toString().equals("java.lang.Object") &&
                                    sm.getParameterType(4).toString().equals("java.lang.ClassCastException") ) {
                                    if (sm.getReturnType().toString().equals("void")) {
                                        for(Unit unit : sm.retrieveActiveBody().getUnits()) {
                                            if(unit.toString().contains("DataMap")) {
                                                dataMapClass = sc.getName();
                                                logger.info("Obfuscated DataMap found: "+dataMapClass);
                                                return dataMapClass;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return dataMapClass;
    }

    public List<String> getDataMapSinks(String wearablePackageName) {
        String dataMapClass = identifyDataMap(wearablePackageName);
        List<String> sinks = new ArrayList<String>();

        SootClass sc = Scene.v().getSootClass(dataMapClass);
        for (SootMethod sm : sc.getMethods()) {
            // putAll(DataMap dataMap)
            if (sm.getParameterCount() == 1) {
                if (sm.getParameterType(0).toString().equals(dataMapClass)) {
                    if (sm.getReturnType().toString().equals("void")) {
                        sinks.add(sm.getSignature());
                    }
                }
            } else if (sm.getParameterCount() == 2) {
                if (sm.getReturnType().toString().equals("void")) {
                    sinks.add(sm.getSignature());
                }
            }

        }
        return sinks;
    }

}
