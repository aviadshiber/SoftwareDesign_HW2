package il.ac.technion.cs.softwaredesign.storage.messages

import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

interface IMessageStorage {
    fun getMediaTypeById(messageId: Long, property: String) : CompletableFuture<Long?>
    fun setMediaTypeToId(messageId: Long, property: String, mediaType: Long): CompletableFuture<Unit>

    fun getContentById(messageId: Long, property: String) : CompletableFuture<ByteArray?>
    fun setContentToId(messageId: Long, property: String, content: ByteArray): CompletableFuture<Unit>

    fun getTimeById(messageId: Long, property: String) : CompletableFuture<LocalDateTime?>
    fun setTimeToId(messageId: Long, property: String, time: LocalDateTime): CompletableFuture<Unit>
}