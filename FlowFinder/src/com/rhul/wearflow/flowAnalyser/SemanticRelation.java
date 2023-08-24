package com.rhul.wearflow.flowAnalyser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Define the semantic relation to join flows between a mobile and wearable app
 * or vice versa. This relation connects flows from sources to sinks from the
 * Wearable Data Layer. Note that these are the instrumented function call and
 * not the originals from google play services. Interpret the relation as
 * follow: key (sink) -> value (source)
 * 
 * @author Marcos Tileria
 *
 */
public class SemanticRelation {
	private static final Map<String, String> sinkSourceMap = initMap();
	private static final Map<String, String> sourceSinkMap = initInverted();

	private static Map<String, String> initMap() {
		Map<String, String> map = new HashMap<>();
		map.put("putStringSink", "getStringSource");
		map.put("putIntSink", "getIntSource");
		map.put("putLongSink", "getLongSource");
		map.put("putDoubleSink", "getDoubleSource");
		map.put("putFloatSink", "getFloatSource");
		map.put("putBooleanSink", "getBooleanSource");
		map.put("putByteSink", "getByteSource");
		map.put("putByteArraySink", "getByteArraySource");
		map.put("putFloatArraySink", "getFloatArraySource");
		map.put("putAssetSink", "getAssetSource");
		map.put("putDataMapSink", "getDataMapSource");
		map.put("putStringArraySink", "getStringArraySource");
		map.put("putStringArrayListSink", "getStringArrayListSource");
		map.put("putDataMapArrayListSink", "getDataMapArrayListSource");

		// relation for message api
		map.put("sendMessage", "getDataSource");

		// relation for channel api
		// map.put("getOutputStreamSink", "getInputStreamtSource");
		map.put("getOutputStreamSink", "taintUri");
		// map.put("sendFileSink", "receiveFileSource");
		map.put("sendFileSink", "taintUri");
		// map.put("sendFile2Sink", "receiveFileSource");
		map.put("sendFile2Sink", "taintUri");

		return Collections.unmodifiableMap(map);
	}

	private static Map<String, String> initInverted() {

		Map<String, String> invertedMap = new HashMap<>();
		invertedMap.put("getStringSource", "putStringSink");
		invertedMap.put("getIntSource", "putIntSink");
		invertedMap.put("getLongSource", "putLongSink");
		invertedMap.put("getDoubleSource", "putDoubleSink");
		invertedMap.put("getFloatSource", "putFloatSink");
		invertedMap.put("getBooleanSource", "putBooleanSink");
		invertedMap.put("getByteSource", "putByteSink");
		invertedMap.put("getByteArraySource", "putByteArraySink");
		invertedMap.put("getFloatArraySource", "putFloatArraySink");
		invertedMap.put("getAssetSource", "putAssetSink");
		invertedMap.put("getDataMapSource", "putDataMapSink");
		invertedMap.put("getStringArrayListSource", "putStringArrayListSink");
		invertedMap.put("getDataMapArrayListSource", "putDataMapArrayListSink");

		// relation message api
		invertedMap.put("getDataSource", "sendMessage");

		// relation for channel api
		invertedMap.put("getInputStreamtSource", "getOutputStreamSink");
		invertedMap.put("taintUri", "sendFileSink");

		return Collections.unmodifiableMap(invertedMap);
	}

	public static Map<String, String> getSinkSourceMap() {
		return sinkSourceMap;
	}

	public static Map<String, String> getSourceSinkMap() {
		return sourceSinkMap;
	}

}
