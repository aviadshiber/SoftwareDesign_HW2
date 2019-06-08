package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import il.ac.technion.cs.softwaredesign.storage.messages.IMessageStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import io.github.vjames19.futures.jdk8.Future

class MessageManager @Inject constructor(
        @UserIdSeqGenerator private val messageIdSeqGenerator: ISequenceGenerator, // user id seq generator and msg seq generator should be the same one
        private val messageStorage: IMessageStorage
) : IMessageManager {
    override fun getUniqueMessageId(): CompletableFuture<Long> {
        return messageIdSeqGenerator.next()
    }

    override fun isMessageIdExists(msgId: Long): CompletableFuture<Boolean> {
        return messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_MEDIA_TYPE).thenApply { it != null }
    }

    override fun addMessage(id: Long, mediaType: Long, content: ByteArray, created: LocalDateTime,
                            received: LocalDateTime, messageType: IMessageManager.MessageType): CompletableFuture<Unit> {
        val mediaTypeSetter=messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_MEDIA_TYPE, mediaType = mediaType)
        val contentSetter=messageStorage.setByteArrayToId(id, MANAGERS_CONSTS.MESSAGE_CONTENTS, content)
        val createsSetter=messageStorage.setTimeToId(id, MANAGERS_CONSTS.MESSAGE_CREATED_TIME, created)
        val receivedSetter=messageStorage.setTimeToId(id, MANAGERS_CONSTS.MESSAGE_RECEIVED_TIME, received)
        val messageTypeSetter=messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_TYPE, messageType.ordinal.toLong())
        val ls= listOf(mediaTypeSetter,contentSetter,createsSetter,receivedSetter,messageTypeSetter)
        return Future.allAsList(ls).thenApply { Unit }
    }

    override fun getMessageMediaType(msgId: Long): CompletableFuture<Long> {
        return messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_MEDIA_TYPE)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
    }

    override fun getMessageContent(msgId: Long): CompletableFuture<ByteArray> {
        return messageStorage.getByteArrayById(msgId, MANAGERS_CONSTS.MESSAGE_CONTENTS)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
    }

    override fun getMessageCreatedTime(msgId: Long): CompletableFuture<LocalDateTime> {
        return messageStorage.getTimeById(msgId, MANAGERS_CONSTS.MESSAGE_CREATED_TIME)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
    }

    override fun getMessageReceivedTime(msgId: Long): CompletableFuture<LocalDateTime> {
        return messageStorage.getTimeById(msgId, MANAGERS_CONSTS.MESSAGE_RECEIVED_TIME)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
    }

    override fun getMessageType(msgId: Long): CompletableFuture<IMessageManager.MessageType> {
        return messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_TYPE)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
                .thenApply { IMessageManager.MessageType.values()[it.toInt()] }
    }

    override fun updateMessageCreatedTime(msgId: Long, created: LocalDateTime): CompletableFuture<Unit> {
        return messageStorage.setTimeToId(msgId, MANAGERS_CONSTS.MESSAGE_CREATED_TIME, created)
    }

    override fun updateMessageReceivedTime(msgId: Long, received: LocalDateTime): CompletableFuture<Unit> {
        return messageStorage.setTimeToId(msgId, MANAGERS_CONSTS.MESSAGE_RECEIVED_TIME, received)
    }
}