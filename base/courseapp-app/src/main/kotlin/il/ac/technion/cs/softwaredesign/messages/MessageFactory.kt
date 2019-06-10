package il.ac.technion.cs.softwaredesign.messages

import java.util.concurrent.CompletableFuture

/**
 * This interface used as a message factory to create new messages
 */
interface MessageFactory {
    /**
     * Create a [Message] with a unique ID using the provided [media] and [contents], which is marked as being sent now,
     * and not being received (null).
     * @param media MediaType
     * @param contents ByteArray
     * @return CompletableFuture<Message>
     */
    fun create(media: MediaType, contents: ByteArray): CompletableFuture<Message>
}