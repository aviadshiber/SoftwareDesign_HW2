package il.ac.technion.cs.softwaredesign.storage.channels

import java.util.concurrent.CompletableFuture

interface IChannelStorage {
    fun getChannelIdByChannelName(channelName : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setChannelIdToChannelName(channelNameKey: CompletableFuture<String?>, channelId: CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getPropertyStringByChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>) : CompletableFuture<String?>
    fun setPropertyStringToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, value: CompletableFuture<String?>): CompletableFuture<Unit>

    fun getPropertyLongByChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>) :CompletableFuture<Long?>
    fun setPropertyLongToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, value: CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getPropertyListByChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>) : CompletableFuture<List<Long>?>
    fun setPropertyListToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, listValue: CompletableFuture<List<Long>>): CompletableFuture<Unit>
}
