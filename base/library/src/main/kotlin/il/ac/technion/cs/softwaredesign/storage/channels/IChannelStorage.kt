package il.ac.technion.cs.softwaredesign.storage.channels

import java.util.concurrent.CompletableFuture

interface IChannelStorage {
    fun getChannelIdByChannelName(channelName: String) : CompletableFuture<Long?>
    fun setChannelIdToChannelName(channelNameKey: String, channelId: Long): CompletableFuture<Unit>

    fun getPropertyStringByChannelId(channelIdKey: Long, property: String) : CompletableFuture<String?>
    fun setPropertyStringToChannelId(channelIdKey: Long, property: String, value: String): CompletableFuture<Unit>

    fun getPropertyLongByChannelId(channelIdKey: Long, property: String) :CompletableFuture<Long?>
    fun setPropertyLongToChannelId(channelIdKey: Long, property: String, value: Long): CompletableFuture<Unit>

    fun getPropertyListByChannelId(channelIdKey: Long, property: String) : CompletableFuture<List<Long>?>
    fun setPropertyListToChannelId(channelIdKey: Long, property: String, listValue: List<Long>): CompletableFuture<Unit>
}
