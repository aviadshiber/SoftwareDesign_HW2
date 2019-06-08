package il.ac.technion.cs.softwaredesign.storage.messages

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

interface IMessageStorage {
    fun getLongById(messageId: Long, property: String) : CompletableFuture<Long?>
    fun setLongToId(messageId: Long, property: String, mediaType: Long): CompletableFuture<Unit>

    fun getByteArrayById(messageId: Long, property: String) : CompletableFuture<ByteArray?>
    fun setByteArrayToId(messageId: Long, property: String, content: ByteArray): CompletableFuture<Unit>

    fun getTimeById(messageId: Long, property: String) : CompletableFuture<LocalDateTime?>
    fun setTimeToId(messageId: Long, property: String, time: LocalDateTime): CompletableFuture<Unit>

    fun getStringById(messageId: Long, property: String) : CompletableFuture<String?>
    fun setStringToId(messageId: Long, property: String, value: String): CompletableFuture<Unit>
}