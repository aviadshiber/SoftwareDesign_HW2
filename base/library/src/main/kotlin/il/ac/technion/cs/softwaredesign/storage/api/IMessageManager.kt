package il.ac.technion.cs.softwaredesign.storage.api

import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

interface IMessageManager {
    enum class MessageType{
        PRIVATE,
        CHANNEL,
        BROADCAST
    }

    /**
     * get a unique message id
     */
    fun generateUniqueMessageId(): CompletableFuture<Long>

    /**
     * return true if message id exists and false otherwise
     * @param msgId Long
     * @return CompletableFuture<Boolean>
     */
    fun isMessageIdExists(msgId: Long): CompletableFuture<Boolean>

    /**
     * Add a new message to the system.
     * @param id Long
     * @param mediaType Long (according enum mediaType)
     * @param content ByteArray, msg content
     * @param created LocalDateTime
     * @param messageType MessageType
     * @throws IllegalArgumentException if id already exists in the system
     * @return CompletableFuture<Unit>
     */
    fun addMessage(id: Long, mediaType: Long, content: ByteArray, created: LocalDateTime,
                   messageType: MessageType, startCounter: Long? = null): CompletableFuture<Unit>

    /**
     * gets message mediaType
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message mediaType
     */
    fun getMessageMediaType(msgId: Long): CompletableFuture<Long>

    /**
     * gets message content
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message content
     */
    fun getMessageContent(msgId: Long): CompletableFuture<ByteArray>

    /**
     * gets message created time
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message created time
     */
    fun getMessageCreatedTime(msgId: Long): CompletableFuture<LocalDateTime>

    /**
     * gets message received time
     * @param msgId message id
     * @return message received time or null if it has'nt been set
     */
    fun getMessageReceivedTime(msgId: Long): CompletableFuture<LocalDateTime?>

    /**
     * gets message type
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message type
     */
    fun getMessageType(msgId: Long): CompletableFuture<MessageType>

    /**
     * gets message counter, i.e. number of users that hasn't been read the message
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a broadcast
     * @return message counter
     */
    fun getMessageCounter(msgId: Long): CompletableFuture<Long>

    /**
     * updates a message received time
     * @param msgId message id
     * @param received received time
     * @throws IllegalArgumentException throws if message id does not exist in the system
     */
    fun updateMessageReceivedTime(msgId: Long, received: LocalDateTime): CompletableFuture<Unit>

    /**
     * decrease message counter by count
     * @param msgId message id
     * @param count Long
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a broadcast
     */
    fun decreaseMessageCounterBy(msgId: Long, count: Long = 1): CompletableFuture<Unit>

    /**
     *
     * @return CompletableFuture<Long> number of total (broadcast & private) pending messages in the system
     */
    fun getNumberOfPendingMessages(): CompletableFuture<Long>

    /**
     * get all valid broadcast message ids
     * @return CompletableFuture<List<Long>>
     */
    fun getAllBroadcastMessageIds(): List<Long>
}