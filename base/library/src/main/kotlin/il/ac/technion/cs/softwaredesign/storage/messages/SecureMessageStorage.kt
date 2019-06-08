package il.ac.technion.cs.softwaredesign.storage.messages

import il.ac.technion.cs.softwaredesign.managers.MessageDetailsStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils.createPropertyKey
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class SecureMessageStorage @Inject constructor(@MessageDetailsStorage private val messageDetailsStorage: SecureStorage) : IMessageStorage {
    override fun getLongById(messageId: Long, property: String): CompletableFuture<Long?> {
        val key = createPropertyKey(messageId, property)
        val value = messageDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else ConversionUtils.bytesToLong(it) }
    }

    override fun setLongToId(messageId: Long, property: String, mediaType: Long): CompletableFuture<Unit> {
        val key = createPropertyKey(messageId, property)
        return messageDetailsStorage.write(key, ConversionUtils.longToBytes(mediaType))
    }

    override fun getByteArrayById(messageId: Long, property: String): CompletableFuture<ByteArray?> {
        val key = createPropertyKey(messageId, property)
        return messageDetailsStorage.read(key)
    }

    override fun setByteArrayToId(messageId: Long, property: String, content: ByteArray): CompletableFuture<Unit> {
        val key = createPropertyKey(messageId, property)
        return messageDetailsStorage.write(key, content)
    }

    override fun getTimeById(messageId: Long, property: String): CompletableFuture<LocalDateTime?> {
        val key = createPropertyKey(messageId, property)
        val value = messageDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else ConversionUtils.byteArrayToLocalDateTime(it) }
    }

    override fun setTimeToId(messageId: Long, property: String, time: LocalDateTime): CompletableFuture<Unit> {
        val key = createPropertyKey(messageId, property)
        return messageDetailsStorage.write(key, ConversionUtils.localDateTimeToBytes(time))
    }
}