package il.ac.technion.cs.softwaredesign.storage.channels

import il.ac.technion.cs.softwaredesign.managers.ChannelDetailsStorage
import il.ac.technion.cs.softwaredesign.managers.ChannelIdStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.DELIMITER
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureChannelStorage
@Inject constructor(@ChannelIdStorage private val channelIdsStorage: SecureStorage,
                    @ChannelDetailsStorage private val channelDetailsStorage: SecureStorage) : IChannelStorage {

    override fun getChannelIdByChannelName(channelName: CompletableFuture<String?>): CompletableFuture<Long?> {
        return channelName.thenCompose<Long?> { channel ->
            if (channel == null) CompletableFuture.supplyAsync{ null } // if usernameKey is null
            else channelIdsStorage.read(channel.toByteArray()).thenApply { userId ->
                if (userId == null ) null // if username does not exist
                else ConversionUtils.bytesToLong(userId)
            }
        }
    }

    override fun setChannelIdToChannelName(channelNameKey: CompletableFuture<String?>,
                                           channelId: CompletableFuture<Long?>): CompletableFuture<Unit> {
        return channelNameKey.thenCompose { channelName ->
            channelId.thenCompose { channelIdVal ->
                if (channelName != null && channelIdVal != null)
                    channelIdsStorage.write(channelName.toByteArray(),ConversionUtils.longToBytes(channelIdVal))
                else CompletableFuture.supplyAsync{ Unit }
            }
        }
    }

    override fun getPropertyStringByChannelId(channelIdKey: CompletableFuture<Long?>,
                                              property: CompletableFuture<String?>): CompletableFuture<String?> {
        return createPropertyKey(channelIdKey, property).thenCompose<ByteArray?> { key ->
            if (key == null) null // userId is null
            else channelDetailsStorage.read(key)
        }.thenApply { byteArray ->
            if (byteArray == null) null else String(byteArray) // userId does not exist
        }
    }

    override fun setPropertyStringToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, value: CompletableFuture<String?>): CompletableFuture<Unit> {
        return createPropertyKey(channelIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                value.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else channelDetailsStorage.write(propertyKey, valueToWrite.toByteArray())
                }
            }
        }
    }

    override fun getPropertyLongByChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>): CompletableFuture<Long?> {
        return createPropertyKey(channelIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                channelDetailsStorage.read(propertyKey).thenApply { byteArray ->
                    if (byteArray == null) null else ConversionUtils.bytesToLong(byteArray) // userId does not exist
                }
            }
        }
    }

    override fun setPropertyLongToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, value: CompletableFuture<Long?>): CompletableFuture<Unit> {
        return createPropertyKey(channelIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                value.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else channelDetailsStorage.write(propertyKey, ConversionUtils.longToBytes(valueToWrite))
                }
            }
        }
    }

    override fun getPropertyListByChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>): CompletableFuture<List<Long>?> {
        return createPropertyKey(channelIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null
            else {
                channelDetailsStorage.read(propertyKey).thenApply { list ->
                    if (list == null) null // user id does not exist
                    else {
                        val stringValue = String(list)
                        if (stringValue == "") listOf<Long>()
                        stringValue.split(DELIMITER).map { it.toLong() }.toList()
                    }
                }
            }
        }
    }

    override fun setPropertyListToChannelId(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, listValue: CompletableFuture<List<Long>>): CompletableFuture<Unit> {
        return createPropertyKey(channelIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                listValue.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else {
                        val value = valueToWrite.joinToString(DELIMITER)
                        channelDetailsStorage.write(propertyKey, value.toByteArray())
                    }
                }
            }
        }
    }

    private fun createPropertyKey(channelIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>) : CompletableFuture<ByteArray?>{
        return channelIdKey.thenCombine<String?, ByteArray?>(property
        ) { channelId, propertyVal ->
            if (channelId != null && propertyVal != null) {
                val channelIdByteArray = ConversionUtils.longToBytes(channelId)
                val keySuffixByteArray = "$DELIMITER$propertyVal".toByteArray()
                channelIdByteArray + keySuffixByteArray
            } else {
                null
            }
        }
    }
}