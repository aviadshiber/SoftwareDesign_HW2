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

    override fun getChannelIdByChannelName(channelName: String): CompletableFuture<Long?> {
        val channelIdByteArray = channelIdsStorage.read(channelName.toByteArray())
        return channelIdByteArray.thenApply { if (it == null) null else ConversionUtils.bytesToLong(it) }
    }

    override fun setChannelIdToChannelName(channelNameKey: String, channelId: Long): CompletableFuture<Unit> {
        return channelIdsStorage.write(channelNameKey.toByteArray(), ConversionUtils.longToBytes(channelId))
    }

    override fun getPropertyStringByChannelId(channelIdKey: Long,
                                              property: String): CompletableFuture<String?> {
        val key = createPropertyKey(channelIdKey, property)
        val value = channelDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else String(it) }
    }

    override fun setPropertyStringToChannelId(channelIdKey: Long, property: String, value: String): CompletableFuture<Unit> {
        val key = createPropertyKey(channelIdKey, property)
        return channelDetailsStorage.write(key, value.toByteArray())
    }

    override fun getPropertyLongByChannelId(channelIdKey: Long, property: String): CompletableFuture<Long?> {
        val key = createPropertyKey(channelIdKey, property)
        val value = channelDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else ConversionUtils.bytesToLong(it) }
    }

    override fun setPropertyLongToChannelId(channelIdKey: Long, property: String, value: Long): CompletableFuture<Unit> {
        val key = createPropertyKey(channelIdKey, property)
        return channelDetailsStorage.write(key, ConversionUtils.longToBytes(value))
    }

    override fun getPropertyListByChannelId(channelIdKey: Long, property: String): CompletableFuture<List<Long>?> {
        val key = createPropertyKey(channelIdKey, property)
        val value = channelDetailsStorage.read(key)
        return value.thenApply {
            if (it == null) null else delimitedByteArrayToList(it)
        }
    }

    private fun delimitedByteArrayToList(byteArray: ByteArray): MutableList<Long> {
        val stringValue = String(byteArray)
        return if (stringValue == "") emptyList<Long>().toMutableList()
        else stringValue.split(DELIMITER).map { it.toLong() }.toMutableList()
    }


    override fun setPropertyListToChannelId(channelIdKey: Long, property: String, listValue: List<Long>): CompletableFuture<Unit> {
        val key = createPropertyKey(channelIdKey, property)
        val value = listValue.joinToString(DELIMITER)
        return channelDetailsStorage.write(key, value.toByteArray())
    }

    private fun createPropertyKey(channelId: Long, property: String): ByteArray {
        val channelIdByteArray = ConversionUtils.longToBytes(channelId)
        val keySuffixByteArray = "$DELIMITER$property".toByteArray()
        return channelIdByteArray + keySuffixByteArray
    }
}