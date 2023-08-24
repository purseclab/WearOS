**com.google.android.gms.wearable.ChannelClient**\
abstract Task<Void>	sendFile(ChannelClient.Channel channel, Uri uri)\
abstract Task<Void>	sendFile(ChannelClient.Channel channel, Uri uri, long startOffset, long length)\
public abstract Task<OutputStream> getOutputStream(ChannelClient.Channel channel)

*Deprecated ChannelApi*\
**com.google.android.gms.wearable.Channel**\
abstract PendingResult<Status> sendFile(GoogleApiClient client, Uri uri)\
abstract PendingResult<Status> sendFile(GoogleApiClient client, Uri uri, long startOffset, long length)\
abstract PendingResult<Channel.GetOutputStreamResult> getOutputStream(GoogleApiClient client)

**com.google.android.gms.wearable.MessageClient**\
abstract Task<Integer> sendMessage(String nodeId, String path, byte[] data)\
abstract Task<Integer> sendMessage(String nodeId, String path, byte[] data, MessageOptions options)\
abstract Task<byte[]> sendRequest(String nodeId, String path, byte[] data)

*Deprecated MessageApi*\
**com.google.android.gms.wearable.MessageApi**\
abstract PendingResult<MessageApi.SendMessageResult> sendMessage(GoogleApiClient client, String nodeId, String path, byte[] data)

**com.google.android.gms.wearable.DataMap**\
void putAll(DataMap dataMap)\
void putAsset(String key, Asset value)\
void putBoolean(String key, boolean value)\
void putByte(String key, byte value)\
void putByteArray(String key, byte[] value)\
void putDataMap(String key, DataMap value)\
void putDataMapArrayList(String key, ArrayList<DataMap> value)\
void putDouble(String key, double value)\
void putFloat(String key, float value)\
void putFloatArray(String key, float[] value)\
void putInt(String key, int value)\
void putIntegerArrayList(String key, ArrayList<Integer> value)\
void putLong(String key, long value)\
void putLongArray(String key, long[] value)\
void putString(String key, String value)\
void putStringArray(String key, String[] value)\
void putStringArrayList(String key, ArrayList<String> value)
