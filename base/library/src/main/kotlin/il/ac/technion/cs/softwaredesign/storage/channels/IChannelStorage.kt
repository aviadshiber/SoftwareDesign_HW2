package il.ac.technion.cs.softwaredesign.storage.channels

import java.util.concurrent.CompletableFuture

/**
 * Wrapper storage functions for channel usage
 */
interface IChannelStorage {
    /**
     * get channel id by channel name
     * @param channelName String
     * @return CompletableFuture<Long?>, internal object is null if channel name key does not exist
     */
    fun getChannelIdByChannelName(channelName: String) : CompletableFuture<Long?>

    /**
     * set channel id to channel name
     * @param channelNameKey String
     * @param channelId Long
     * @return CompletableFuture<Unit>
     */
    fun setChannelIdToChannelName(channelNameKey: String, channelId: Long): CompletableFuture<Unit>

    /**
     * get channel id [property] of type String
     * @param channelIdKey Long
     * @param property String
     * @return CompletableFuture<String?>, internal object is null if key does not exist
     */
    fun getPropertyStringByChannelId(channelIdKey: Long, property: String) : CompletableFuture<String?>

    /**
     * set channel id [property] of [channelIdKey] to [value] of type String
     * @param channelIdKey Long
     * @param property String
     * @param value String
     * @return CompletableFuture<Unit>
     */
    fun setPropertyStringToChannelId(channelIdKey: Long, property: String, value: String): CompletableFuture<Unit>

    /**
     * get channel id [property] of type Long
     * @param channelIdKey Long
     * @param property String
     * @return CompletableFuture<Long?>, internal object is null if key does not exist
     */
    fun getPropertyLongByChannelId(channelIdKey: Long, property: String) :CompletableFuture<Long?>

    /**
     * set channel id [property] of [channelIdKey] to [value] of type Long
     * @param channelIdKey Long
     * @param property String
     * @param value Long
     * @return CompletableFuture<Unit>
     */
    fun setPropertyLongToChannelId(channelIdKey: Long, property: String, value: Long): CompletableFuture<Unit>

    /**
     * get channel id [property] of type List
     * @param channelIdKey Long
     * @param property String
     * @return CompletableFuture<List<Long>?>, internal object is null if key does not exist
     */
    fun getPropertyListByChannelId(channelIdKey: Long, property: String) : CompletableFuture<List<Long>?>

    /**
     * set channel id [property] of [channelIdKey] to [listValue] of type List<Long>
     * @param channelIdKey Long
     * @param property String
     * @param listValue List<Long>
     * @return CompletableFuture<Unit>, internal object is null if key does not exist
     */
    fun setPropertyListToChannelId(channelIdKey: Long, property: String, listValue: List<Long>): CompletableFuture<Unit>
}
