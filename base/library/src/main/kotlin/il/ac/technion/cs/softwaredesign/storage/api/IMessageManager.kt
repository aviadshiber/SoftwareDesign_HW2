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
    fun getUniqueMessageId(): CompletableFuture<Long>

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
     * @param received LocalDateTime
     * @param messageType MessageType
     * @throws IllegalArgumentException if id already exists in the system
     * @return CompletableFuture<Unit>
     */
    fun addMessage(id: Long, mediaType: Long, content: ByteArray, created: LocalDateTime,
                   received: LocalDateTime, messageType: MessageType): CompletableFuture<Unit>

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
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message received time
     */
    fun getMessageReceivedTime(msgId: Long): CompletableFuture<LocalDateTime>

    /**
     * gets message type
     * @param msgId message id
     * @throws IllegalArgumentException throws if message id does not exist in the system
     * @return message type
     */
    fun getMessageType(msgId: Long): CompletableFuture<MessageType>

    /**
     * updates a message created time
     * @param msgId message id
     * @param created created time
     * @throws IllegalArgumentException throws if message id does not exist in the system
     */
    fun updateMessageCreatedTime(msgId: Long, created: LocalDateTime): CompletableFuture<Unit>

    /**
     * updates a message received time
     * @param msgId message id
     * @param received received time
     * @throws IllegalArgumentException throws if message id does not exist in the system
     */
    fun updateMessageReceivedTime(msgId: Long, received: LocalDateTime): CompletableFuture<Unit>
}