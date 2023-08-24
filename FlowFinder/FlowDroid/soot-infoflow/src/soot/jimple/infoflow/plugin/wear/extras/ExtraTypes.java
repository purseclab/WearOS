package soot.jimple.infoflow.plugin.wear.extras;

import java.util.ArrayList;
import java.util.List;

public class ExtraTypes {

	private String type;
	public static final String SENSOR_EVENT = "android.hardware.SensorEvent";
	public static final String SENSOR_EVENT_LISTENER = "android.hardware.SensorEventListener";
	public static final String SENSOR_EVENT_CALLBACK = "android.hardware.SensorEventCallback";
	public static final String STRING_TYPE = "java.lang.String";
	public static final String OBJECT_TYPE = "java.lang.Object";
	public static final String VOID_TYPE = "void";
	public static final String CHAR_TYPE = "java.lang.Character";
	public static final String INTEGER_TYPE = "java.lang.Integer";
	public static final String BOOLEAN_TYPE = "java.lang.Boolean";
	public static final String BYTE_TYPE = "java.lang.Byte";
	public static final String DOUBLE_TYPE = "java.lang.Double";
	public static final String FLOAT_TYPE = "java.lang.Float";
	public static final String LONG_TYPE = "java.lang.Long";
	public static final String SHORT_TYPE = "java.lang.Short";
	public static final Object INTENT_TYPE = "android.content.Intent";
	public static final Object STRING_BUILDER_TYPE = "java.lang.StringBuilder";
	public static final String DATA_CLIENT = "com.google.android.gms.wearable.DataClient";
	public static final String DATA_API = "com.google.android.gms.wearable.DataApi";
	public static final String MESSAGE_CLIENT = "com.google.android.gms.wearable.MessageClient";
	public static final String MESSAGE_API = "com.google.android.gms.wearable.MessageApi";
	public static final String DATA_ITEM = "com.google.android.gms.wearable.DataItem";
	public static final String DATA_MAP_ITEM = "com.google.android.gms.wearable.DataMapItem";
	public static final String PUT_DATA_MAP_REQUEST = "com.google.android.gms.wearable.PutDataMapRequest";
	public static final String PUT_DATA_REQUEST = "com.google.android.gms.wearable.PutDataRequest";
	public static final String DATA_MAP = "com.google.android.gms.wearable.DataMap";
	public static final String ASSETS = "com.google.android.gms.wearable.Asset";
	public static final String DATA_EVENT = "com.google.android.gms.wearable.DataEvent";
	public static final String MESSAGE_EVENT = "com.google.android.gms.wearable.MessageEvent";
	public static final String URI = "android.net.Uri";
	public static final String WEARABLE_LISTENER_SERVICE = "com.google.android.gms.wearable.WearableListenerService";
	public static final String CHANNEL_CLIENT = "com.google.android.gms.wearable.ChannelClient";
	public static final String CHANNEL = "com.google.android.gms.wearable.ChannelClient$Channel";
	public static final String TASK = "com.google.android.gms.tasks.Task";

	public static final String DATA_EVENT_BUFFER = "com.google.android.gms.wearable.DataEventBuffer";

	public static final String ON_DATA_CHANGED_LISTENER = "com.google.android.gms.wearable.DataClient$OnDataChangedListener";
	public static final String DATA_LISTENER = "com.google.android.gms.wearable.DataApi$DataListener";

	public static final String MESSAGE_LISTENER = "com.google.android.gms.wearable.MessageApi$MessageListener";
	public static final String ON_MESSAGE_RECEIVED_LISTENER = "com.google.android.gms.wearable.MessageClient$OnMessageReceivedListener";

	public static final String ON_DATA_CHANGED = "onDataChanged";
	public static final String ON_MESSAGE_RECEIVED = "onMessageReceived";
	public static final String ON_CHANNEL_OPENED = "onChannelOpened";

	public static final String PENDING_RESULT = "com.google.android.gms.common.api.PendingResult";
	public static final String GOOGLE_API_CLIENT = "com.google.android.gms.common.api.GoogleApiClient";

	public static final String CAPABILITY_API_LISTENER = "com.google.android.gms.wearable.CapabilityApi$CapabilityListener";
	public static final String CAPABILITY_API_INFO = "com.google.android.gms.wearable.CapabilityInfo";
	public static final String CAPABILITY_CLIENT_ON_CHANGED_LISTENER = "com.google.android.gms.wearable.CapabilityClient$OnCapabilityChangedListener";

	public static final String BUNDLE = "android.os.Bundle";

	public static final String GOOGLE_API_CLIENT_CALLBACKS = "com.google.android.gms.common.api.GoogleApiClient$ConnectionCallbacks";

	public static final String[] WEAR_CALLBACKS = { ON_DATA_CHANGED, ON_MESSAGE_RECEIVED, ON_CHANNEL_OPENED };

	public static final String[] fullAnalysisTypes = { DATA_MAP, PUT_DATA_REQUEST, PUT_DATA_MAP_REQUEST };
	public static final String[] classesForInstrumentation = { DATA_CLIENT, MESSAGE_CLIENT, DATA_EVENT, MESSAGE_EVENT,
			CHANNEL, CHANNEL_CLIENT, DATA_API, MESSAGE_API, DATA_MAP, PUT_DATA_REQUEST, PUT_DATA_MAP_REQUEST,
			DATA_EVENT_BUFFER };
	public static final String[] wearActionsListener = { "com.google.android.gms.wearable.MESSAGE_RECEIVED",
			"com.google.android.gms.wearable.DATA_CHANGED", "com.google.android.gms.wearable.BIND_LISTENER",
			"com.google.android.gms.wearable.onChannelOpened" };

	public static final String[] oldApis = { DATA_API, MESSAGE_API };

	private static List<String> obfuscatedInstrumentationClasses = new ArrayList<>();

	public static void addObfInstClass(String className) {
		obfuscatedInstrumentationClasses.add(className);
	}

	public static List<String> getObfInstClasses() {
		return obfuscatedInstrumentationClasses;
	}

	public ExtraTypes(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

}
