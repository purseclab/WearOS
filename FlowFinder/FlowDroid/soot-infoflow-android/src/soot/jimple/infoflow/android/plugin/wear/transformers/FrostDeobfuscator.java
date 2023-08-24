package soot.jimple.infoflow.android.plugin.wear.transformers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.LookAndFeel;

import java.util.Collections;
import java.lang.reflect.Modifier;

import soot.*;
import soot.util.Chain;

import soot.jimple.Stmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.plugin.wear.analysis.AnalysisUtil;
import soot.jimple.infoflow.plugin.wear.extras.ExtraTypes;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.parser.node.TTableswitch;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.InstanceFieldRef;
import soot.jimple.FieldRef;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.VirtualInvokeExpr;

public class FrostDeobfuscator extends SceneTransformer {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static AnalysisUtil utilInstance = AnalysisUtil.getInstance();

    private SootClass wearableClass = null; 
    private SootClass messageClientClass = null;
    private SootClass putDataRequestClass = null;
    private SootClass oldDataApiClass = null;
    private SootClass dataItemResultClass = null;
    private SootClass dataItemClass = null;
    private SootClass dataMapClass = null;

    private String obfPutDataRequest = "NO";
    private String obfOldDataApi = "NO";
    private String obfPutDataItem = "NO";
    private String obfDataItemResult = "NO";
    private String obfDataItem = "NO";
    private String obfDataMap = "NO";

    @Override
    protected void internalTransform(String phaseName, Map options) {
        logger.info("FrostDeobfuscator");
        // Get com.google.android.gms.wearable.Wearable
        if (identifyWearble()) {
            logger.info("Wearable found: "+wearableClass.getName());
            // Find getMessageClient method
            if(identifyMessageClient()) {
                logger.info("MessageClient found: "+messageClientClass.getName());
                // Transform MessageClient
                identifySendMessage();
                if(Scene.v().containsClass("com.google.android.gms.wearable.MessageClient")){
                            // logger.info("CLASS EXISTS");
                            // logger.info("CLASS METHODS: "+Scene.v().getSootClass("com.google.android.gms.wearable.MessageClient").getMethods().toString());
                        }
                else{
                    RefType type = Scene.v().getRefTypeUnsafe("com.google.android.gms.wearable.MessageClient");
                    logger.info("REFTYPE: "+type.toString());
                }
            }

            // Find oldDataApi
            // Get com.google.android.gms.wearable.PutDataRequest
            if(identifyPutDataRequest()) {
                logger.info("PutDataRequest found: "+putDataRequestClass.getName());
                // Get com.google.android.gms.wearable.DataApi
                if(identifyOldDataApi()) {
                    logger.info("OldDataApi found: "+oldDataApiClass.getName());
                    // Transform DataApi
                    identifyPutDataItem();
                    identifyDataItem();
                    identifyDataMap();
                }
            }

        } else {
            logger.info("Wearable not found");
            System.exit(0);
        }
	}


    private boolean identifyWearble(){
        if (Scene.v().containsClass("com.google.android.gms.wearable.Wearable")){
            wearableClass = Scene.v().getSootClass("com.google.android.gms.wearable.Wearable");
            return true;
        }
        //  TODO: Add check for application class
        for (SootClass sClass : Scene.v().getApplicationClasses()) {
            try {
                SootMethod clinit = sClass.getMethodByName("<clinit>");        
                if (clinit == null) {
                    continue;
                }
                Chain<Unit> units = clinit.retrieveActiveBody().getUnits();
                for (Unit unit : units) {
                    if(unit.toString().contains("Wearable.API")) {    
                        wearableClass = sClass;           
                        return true;
                    }                   
                }  
            } catch (Exception e) {
                continue;
            }
        }
        return false;
    }

    private boolean identifyMessageClient() {
    
        if (Scene.v().containsClass("com.google.android.gms.wearable.MessageClient")){
            messageClientClass = Scene.v().getSootClass("com.google.android.gms.wearable.MessageClient");
            return true;
        }

        for(SootMethod sm : wearableClass.getMethods()) {
            if(sm.getName().equals("getMessageClient")){
                messageClientClass = Scene.v().getSootClass(sm.getReturnType().getEscapedName());
                return true;
            }
            // Remove if b experimental
            if(sm.isStatic())
                if(sm.getParameterCount() == 1)
                    if(sm.getParameterType(0).toString().equals("android.content.Context")){
                        SootClass curDeobf = Scene.v().getSootClass(sm.getReturnType().getEscapedName());
                        if(curDeobf.isAbstract()) {
                            for(SootMethod curMeth : curDeobf.getMethods()) {
                                if(curMeth.getParameterCount() == 3) {
                                    if (curMeth.getParameterType(0).toString().equals("java.lang.String") &&
                                    curMeth.getParameterType(1).toString().equals("java.lang.String") &&
                                    curMeth.getParameterType(2).toString().equals("byte[]") 
                                    ) {
                                        messageClientClass = curDeobf;
                                        return true;
                                    }
                                }
                            }
                        }
                    }
            
        }
        return false;
    }

    private boolean isMsgClientSendMsg(SootMethod curMeth) {
        if (curMeth.getParameterType(0).toString().equals("java.lang.String") &&
        curMeth.getParameterType(1).toString().equals("java.lang.String") &&
        curMeth.getParameterType(2).toString().equals("byte[]") 
        ) {
            return true;
        }
        return false;
    }

    private void identifySendMessage() {

        String obfClassMap = "NONEEDTODEFROSTCLASS";
        String obfMethMap = "NONEEDTODEFROSTMETHOD";

        if (messageClientClass.getName() != "com.google.android.gms.wearable.MessageClient") {
            ExtraTypes.addObfInstClass(messageClientClass.getName());
            obfClassMap = messageClientClass.getName();
            messageClientClass.setName("com.google.android.gms.wearable.MessageClient");
            //messageClientClass.setInScene(false);
            Scene.v().removeClass(messageClientClass);
            Scene.v().addClass(messageClientClass);
            if(Scene.v().containsClass("com.google.android.gms.wearable.MessageClient")){
                logger.info("CLASS EXISTS");
                logger.info("CLASS METHODS: "+Scene.v().getSootClass("com.google.android.gms.wearable.MessageClient").getMethods().toString());
            }
        }

        for(SootMethod curMeth : messageClientClass.getMethods()) {
            if(curMeth.getParameterCount() == 3) {
                if (curMeth.getParameterType(0).toString().equals("java.lang.String") &&
                curMeth.getParameterType(1).toString().equals("java.lang.String") &&
                curMeth.getParameterType(2).toString().equals("byte[]") 
                ) {
                    obfMethMap = curMeth.getName();
                    SootClass tmpClass = new SootClass("com.google.android.gms.tasks.Task",Modifier.PUBLIC);
                    tmpClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
                    Scene.v().addClass(tmpClass);
                    curMeth.setReturnType(Scene.v().getType("com.google.android.gms.tasks.Task"));
                    curMeth.setName("sendMessage");
                    // curMeth.setReturnType(RefType.v("java.lang.String"));
                    curMeth.setDeclaringClass(messageClientClass);
                    break;
                }
            }
        }

        if (obfClassMap.equals("NONEEDTODEFROSTCLASS") 
        || obfMethMap.equals("NONEEDTODEFROSTMETHOD") ) {
            return;
        }

        for (SootClass sClass : Scene.v().getApplicationClasses()) {

            // Debug
            // if (!sClass.getName().contains("ServiceLoc")) {
            //     continue;
            // }

            // TODO: FIELDS

            // Deobf methods
            for (SootMethod sm : sClass.getMethods()) {

                // Debug
                // if (!sm.getName().equals("z3")) {
                //     continue;
                // }

                // TODO: LOCALS

                if (sm.isConcrete()) {
                    Body b = sm.retrieveActiveBody();
                    // Get the list of statements
                    Chain<Unit> units = b.getUnits();
                    // Iterate through the list of statements
                    for (Unit unit : units) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr iexp = (InvokeExpr) stmt.getInvokeExpr();
                            if ((iexp.getMethod().getDeclaringClass().getName().equals(ExtraTypes.MESSAGE_CLIENT) || 
                            iexp.getMethod().getDeclaringClass().getName().equals(obfClassMap)
                            ) &&
                            iexp.getMethod().getName().equals(obfMethMap) && 
                            iexp.getMethod().getParameterCount() == 3
                            )  
                            {
                                SootMethod ixMethod = iexp.getMethod();
                                if(isMsgClientSendMsg(ixMethod)) {
                                    SootMethod frostSendMsg = Scene.v().getSootClass(obfClassMap).getMethodByName("sendMessage");
                                    iexp.setMethodRef(frostSendMsg.makeRef());
                                }

                            }
                        }
                    }
                    b.validate();
                }
            }
        }    
    }

    private boolean identifyPutDataRequest() {
        int heat = 0;

        if (Scene.v().containsClass(ExtraTypes.PUT_DATA_REQUEST)){
            putDataRequestClass = Scene.v().getSootClass(ExtraTypes.PUT_DATA_REQUEST);
            return true;
        }

        for (SootClass sClass : Scene.v().getApplicationClasses()) {
            for(SootMethod sm : sClass.getMethods()){
                if(sm.getParameterCount() == 1){
                    if(sm.getParameterType(0).toString().equals("boolean") && sm.getReturnType().toString().equals("java.lang.String")) {
                        for(Unit unit : sm.retrieveActiveBody().getUnits()) {
                             if(unit.toString().contains("PutDataRequest[")) {
                                putDataRequestClass = sClass;
                                return true;
                             }
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private boolean identifyOldDataApi() {

        if (Scene.v().containsClass(ExtraTypes.DATA_API)) {
            oldDataApiClass = Scene.v().getSootClass(ExtraTypes.DATA_API);
            return true;
        }

        for(SootField sf : wearableClass.getFields()) {
            SootClass curFieldClass = Scene.v().getSootClass(sf.getSubSignature().split(" ")[0]);
            for(SootMethod sm : curFieldClass.getMethods()) {
                if(sm.getParameterCount() == 2){
                    if(sm.getParameterType(1).toString().equals(putDataRequestClass.getType().toString())){
                        oldDataApiClass = curFieldClass;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // TODO: Defrost GoogleApiClient and PendingResult
    
    private void identifyPutDataItem() {

        if (putDataRequestClass.getName() != ExtraTypes.PUT_DATA_REQUEST) {
            ExtraTypes.addObfInstClass(putDataRequestClass.getName());
            obfPutDataRequest = putDataRequestClass.getName();
            putDataRequestClass.setName(ExtraTypes.PUT_DATA_REQUEST);
            RefType rr =  RefType.v("com.google.android.gms.wearable.PutDataRequest");
            rr.setClassName("com.google.android.gms.wearable.PutDataRequest");
            Scene.v().getSootClass(obfPutDataRequest).setRefType(rr);
            Scene.v().removeClass(putDataRequestClass);
            Scene.v().addClass(putDataRequestClass);
        }
        
        if (oldDataApiClass.getName() != ExtraTypes.DATA_API) {
            ExtraTypes.addObfInstClass(oldDataApiClass.getName());
            obfOldDataApi = oldDataApiClass.getName();
            oldDataApiClass.setName(ExtraTypes.DATA_API);
            Scene.v().removeClass(oldDataApiClass);
            Scene.v().addClass(oldDataApiClass);
        }
        
        for(SootMethod sm : oldDataApiClass.getMethods()) {
            if(sm.getParameterCount() == 2) {
                if (sm.getParameterType(1).toString().equals(obfPutDataRequest) ||
                    sm.getParameterType(1).toString().equals(ExtraTypes.PUT_DATA_REQUEST)
                ) {
                    // Set name
                    obfPutDataItem = sm.getName();
                    sm.setName("putDataItem");
                    // Set return type
                    if (Scene.v().containsClass("com.google.android.gms.common.api.PendingResult") ) {
                        sm.setReturnType(Scene.v().getType("com.google.android.gms.common.api.PendingResult"));
                    } else {
                        SootClass PendingResultClass = new SootClass("com.google.android.gms.common.api.PendingResult", Modifier.PUBLIC);
                        PendingResultClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
                        Scene.v().addClass(PendingResultClass);
                        sm.setReturnType(Scene.v().getType("com.google.android.gms.common.api.PendingResult"));
                    }
                    // Set parameter type
                    List<Type> paramTypes = new ArrayList<Type>();
                    if (Scene.v().containsClass("com.google.android.gms.common.api.GoogleApiClient")) {
                       paramTypes.add(Scene.v().getType("com.google.android.gms.common.api.GoogleApiClient"));
                    } else {
                        SootClass GoogleApiClientClass = new SootClass("com.google.android.gms.common.api.GoogleApiClient", Modifier.PUBLIC);
                        GoogleApiClientClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
                        Scene.v().addClass(GoogleApiClientClass);
                        paramTypes.add(Scene.v().getType("com.google.android.gms.common.api.GoogleApiClient"));
                    }
                    if (Scene.v().containsClass(ExtraTypes.PUT_DATA_REQUEST)) {
                        paramTypes.add(Scene.v().getType(ExtraTypes.PUT_DATA_REQUEST));
                     } else {
                        RefType rr =  RefType.v("com.google.android.gms.wearable.PutDataRequest");
                        rr.setClassName("com.google.android.gms.wearable.PutDataRequest");
                        Scene.v().getSootClass(obfPutDataRequest).setRefType(rr);
                        paramTypes.add(Scene.v().getType(ExtraTypes.PUT_DATA_REQUEST));
                     }
                    sm.setParameterTypes(paramTypes); 
                    break;
                }
            }
        }

        if (obfOldDataApi == "NO") {
            logger.info("datapi No Obf");
            return;
        }

        // Manual transform
        for (SootClass sClass : Scene.v().getApplicationClasses()) {

            // Debug
            // if (!sClass.getName().equals("com.spiraledge.swimapp.wear2.service.l.f")) {
            //     continue;
            // }

            // TODO: Fields

            // Deobf Methods
            for(SootMethod sm : sClass.getMethods()) {

                // Debug 
                // if (!sm.getName().equals("b")) {
                //     continue;
                // }

                // TODO: Locals

                if (sm.isConcrete()) {
                    Body b = sm.retrieveActiveBody();
                    // Get the list of statements
                    Chain<Unit> units = b.getUnits();
                    // Iterate through the list of statements
                    for (Unit unit : units) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr iexp = (InvokeExpr) stmt.getInvokeExpr();
                            if ((iexp.getMethod().getDeclaringClass().getName().equals(ExtraTypes.DATA_API) || 
                            iexp.getMethod().getDeclaringClass().getName().equals(obfOldDataApi)
                            ) &&
                            iexp.getMethod().getName().equals(obfPutDataItem) && 
                            iexp.getMethod().getParameterCount() == 2
                            )  
                            {
                                SootMethod ixMethod = iexp.getMethod();
                                if(isDataApiPutDataItem(ixMethod)) {
                                    SootMethod frostPutDataItem = Scene.v().getSootClass(obfOldDataApi).getMethodByName("putDataItem");
                                    iexp.setMethodRef(frostPutDataItem.makeRef());
                                }

                            }
                        }
                    }
                    b.validate();
                }
            }
        }
    }

    private boolean isDataApiPutDataItem(SootMethod curMethod) {  
        if (curMethod.getParameterType(1).toString().equals(ExtraTypes.PUT_DATA_REQUEST) || 
        curMethod.getParameterType(1).toString().equals(obfPutDataRequest)) {
            return true;
        }
        return false;
    }

    private void identifyDataItem() {

        // Find DataItemResult
        if (Scene.v().containsClass("com.google.android.gms.wearable.DataApi$DataItemResult") ) {
            dataItemResultClass = Scene.v().getSootClass("com.google.android.gms.wearable.DataApi$DataItemResult");
            logger.info("DataItemResult found");
        }
        
        if (dataItemResultClass == null) {
            for(SootClass sc : Scene.v().getApplicationClasses()) {
                if (sc.getName().split("[$]")[0].equals(obfOldDataApi)){
                    if (sc.getInterfaceCount() == 1) {
                        // Result
                        for (SootClass resultInter : sc.getInterfaces()) {
                            if(resultInter.getMethodCount() == 1) {
                                for(SootMethod resultMeth : resultInter.getMethods()) {
                                    if (resultMeth.getReturnType().toString().equals("com.google.android.gms.common.api.Status")){
                                        dataItemResultClass = sc;
                                        obfDataItemResult = sc.getName();
                                        logger.info("DataItemResult found");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (dataItemResultClass == null) {
            logger.error("DataItemResult not found");
            return;
        }

        if (Scene.v().containsClass(ExtraTypes.DATA_ITEM)) {
            dataItemClass = Scene.v().getSootClass(ExtraTypes.DATA_ITEM);
            logger.info("DataItem found");
            return;
        }

        // Find DataItem
        for(SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.implementsInterface(dataItemResultClass.getName())) {
                // zzcq
                if(sc.getFieldCount() == 2) {
                    boolean zzStatus = false; 
                    for(SootField zzField : sc.getFields()) {
                        if (zzField.getType().toString().equals("com.google.android.gms.common.api.Status")) {
                            zzStatus = true;
                        }
                    }
                    if (zzStatus) {
                        for(SootField zzField : sc.getFields()) {
                            if (!zzField.getType().toString().equals("com.google.android.gms.common.api.Status")) {
                                dataItemClass = Scene.v().getSootClass(zzField.getType().toString());
                                obfDataItem = dataItemClass.getName();
                                dataItemClass.setName(ExtraTypes.DATA_ITEM);
                                logger.info("DataItem Found");
                            }
                        }
                    } 
                }
            }        
        }

        // Defrost DataItem
        // TODO: Manual transform
    }


    // Cheap Code
    private void identifyDataMap() {
        
        if (Scene.v().containsClass(ExtraTypes.DATA_MAP)) {
            dataMapClass = Scene.v().getSootClass(ExtraTypes.DATA_MAP);
            logger.info("DataMap found");
            return;
        }

        for (SootClass sc : Scene.v().getApplicationClasses()) {
            if (sc.getPackageName().equals("com.google.android.gms.wearable") ) {
                for (SootMethod sm : sc.getMethods()) {
                    if (sm.isStatic()) {
                        if (sm.isConcrete()) {
                            if (sm.getParameterCount() == 1) {
                                if (sm.getParameterType(0).toString().equals("byte[]")) {
                                    if(sm.getReturnType().toString().equals(sc.getName())) {
                                        for(Unit unit : sm.retrieveActiveBody().getUnits()) {
                                            if(unit.toString().contains("Unable to convert data")) {
                                                dataMapClass = sc;
                                                obfDataMap = dataMapClass.getName();
                                                dataMapClass.setName(ExtraTypes.DATA_MAP);
                                                logger.info("DataMap found");
                                                fixDataMapMethods();
                                                return;
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

   
    }

    private void fixDataMapMethods() {
            // Fix DataMap Methods
        for(SootMethod sm : dataMapClass.getMethods()) {
            if(!sm.isStatic()) {
                if(sm.getParameterCount() == 2 && sm.getReturnType().toString().equals("void")) {
                    // putString
                    if(sm.getParameterType(0).toString().equals("java.lang.String") 
                    &&  sm.getParameterType(1).toString().equals("java.lang.String")) {
                        sm.setName("putString");
                        logger.info("DataMap putString found");
                        return;
                    }
                }
            }
        }
    }


}

