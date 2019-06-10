package il.ac.technion.cs.softwaredesign.storage.api

import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

/**
 * This interface used to perform message's operations
 */
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
     * @param source source of the message
     * @param startCounter for broadcast messages only, initial number of users
     * @param channelId for channel messages only, the channel id of the message
     * @param destUserId for private messages only, the target user id
     * @throws IllegalArgumentException if id already exists in the system
     * @return CompletableFuture<Unit>
     */
    fun addMessage(id: Long, mediaType: Long, content: ByteArray, created: LocalDateTime,
                   messageType: MessageType, source: String,
                   startCounter: Long? = null, channelId: Long? = null, destUserId: Long? = null): CompletableFuture<Unit>

    /** GETTERS & SETTERS **/
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
     * gets message source
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message source
     */
    fun getMessageSource(msgId: Long): CompletableFuture<String>

    /**
     * gets message counter, i.e. number of users that hasn't been read the message, for BROADCAST messages only
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a broadcast
     * @return message counter
     */
    fun getMessageCounter(msgId: Long): CompletableFuture<Long>

    /**
     * gets message channel id, for CHANNEL messages only
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a channel message
     * @return channel id
     */
    fun getMessageChannelId(msgId: Long): CompletableFuture<Long>

    /**
     * gets message destination - user id, for PRIVATE messages only
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a private message
     * @return destination user id
     */
    fun getMessageDestUserId(msgId: Long): CompletableFuture<Long>

    /**
     * updates a message received time, only if it does not set before!
     * @param msgId message id
     * @param received received time
     * @throws IllegalArgumentException throws if message id does not exist in the system
     */
    fun updateMessageReceivedTime(msgId: Long, received: LocalDateTime): CompletableFuture<Unit>

    /**
     * decrease message counter by count, for BROADCAST messages nly
     * @param msgId message id
     * @param count Long
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @throws IllegalAccessException throws if message is not a broadcast
     */
    fun decreaseMessageCounterBy(msgId: Long, count: Long = 1): CompletableFuture<Unit>


    /** STATISTICS **/
    /**
     * get number of total (broadcast & private) pending messages in the system
     * @return CompletableFuture<Long>
     */
    fun getNumberOfPendingMessages(): CompletableFuture<Long>

    /**
     * get all valid broadcast message ids
     * @return CompletableFuture<List<Long>>
     */
    fun getAllBroadcastMessageIds(): List<Long>
}