package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import il.ac.technion.cs.softwaredesign.storage.api.IStatisticsManager
import il.ac.technion.cs.softwaredesign.storage.datastructures.IdKey
import il.ac.technion.cs.softwaredesign.storage.datastructures.SecureAVLTree
import il.ac.technion.cs.softwaredesign.storage.messages.IMessageStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import io.github.vjames19.futures.jdk8.Future
import io.github.vjames19.futures.jdk8.ImmediateFuture

class MessageManager @Inject constructor(
        private val statisticsManager: IStatisticsManager,
        @UserMessageIdSeqGenerator private val messageIdSeqGenerator: ISequenceGenerator, // user id seq generator and msg seq generator should be the same one
        @MessageDetailsStorage private val messageDetailsStorage: SecureStorage,
        private val messageStorage: IMessageStorage
) : IMessageManager {
    private val defaultIdKey: () -> IdKey = { IdKey() }
    private val broadcastMessagesTree = SecureAVLTree(messageDetailsStorage, defaultIdKey, -2L)

    override fun generateUniqueMessageId(): CompletableFuture<Long> {
        return messageIdSeqGenerator.next()
    }

    override fun isMessageIdExists(msgId: Long): CompletableFuture<Boolean> {
        return messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_MEDIA_TYPE).thenApply { it != null }
    }

    override fun addMessage(id: Long, mediaType: Long, content: ByteArray, created: LocalDateTime,
                            messageType: IMessageManager.MessageType, source: String,
                            startCounter: Long?, channelId: Long?, destUserId: Long?): CompletableFuture<Unit> {
        return isMessageIdExists(id).thenCompose {
            if (it) throw IllegalArgumentException("message id already exists")
            val mediaTypeSetter = messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_MEDIA_TYPE, mediaType = mediaType)
            val contentSetter = messageStorage.setByteArrayToId(id, MANAGERS_CONSTS.MESSAGE_CONTENTS, content)
            val createsSetter = messageStorage.setTimeToId(id, MANAGERS_CONSTS.MESSAGE_CREATED_TIME, created)
            // no need to set received time because it is null
            val messageTypeSetter = messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_TYPE, messageType.ordinal.toLong())
            val sourceSetter = messageStorage.setStringToId(id, MANAGERS_CONSTS.MESSAGE_SOURCE, source)
            val ls = mutableListOf(mediaTypeSetter, contentSetter, createsSetter, messageTypeSetter, sourceSetter)
            if (messageType == IMessageManager.MessageType.BROADCAST && startCounter != null) {
                val counterSetter = messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_COUNTER, startCounter)
                ls.add(counterSetter)
            }
            if (messageType == IMessageManager.MessageType.CHANNEL && channelId != null) {
                val channelIdSetter = messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_CHANNEL_ID, channelId)
                ls.add(channelIdSetter)
            }
            if (messageType == IMessageManager.MessageType.PRIVATE && destUserId != null) {
                val userIdSetter = messageStorage.setLongToId(id, MANAGERS_CONSTS.MESSAGE_DEST_USER_ID, destUserId)
                ls.add(userIdSetter)
            }
            Future.allAsList(ls).thenCompose {
                if (messageType == IMessageManager.MessageType.BROADCAST)
                    broadcastMessagesTree.put(IdKey(id))
                if (messageType != IMessageManager.MessageType.CHANNEL) statisticsManager.increaseNumberOfPendingMsgsBy()
                else statisticsManager.increaseNumberOfChannelMsgsBy()
            }
        }
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

    override fun getMessageReceivedTime(msgId: Long): CompletableFuture<LocalDateTime?> {
        return messageStorage.getTimeById(msgId, MANAGERS_CONSTS.MESSAGE_RECEIVED_TIME)
    }

    override fun getMessageType(msgId: Long): CompletableFuture<IMessageManager.MessageType> {
        return messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_TYPE)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
                .thenApply { IMessageManager.MessageType.values()[it.toInt()] }
    }

    override fun getMessageSource(msgId: Long): CompletableFuture<String> {
        return messageStorage.getStringById(msgId, MANAGERS_CONSTS.MESSAGE_SOURCE)
                .thenApply { it ?: throw IllegalArgumentException("message id does not exist") }
    }

    override fun getMessageCounter(msgId: Long): CompletableFuture<Long> {
        return getMessageType(msgId).thenCompose {
            if (it == null) throw IllegalArgumentException("message id does not exist")
            if (it != IMessageManager.MessageType.BROADCAST) throw IllegalAccessException("message is not broadcast")
            else messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_COUNTER).thenApply { counter->counter!! }
        }
    }

    override fun getMessageChannelId(msgId: Long): CompletableFuture<Long> {
        return getMessageType(msgId).thenCompose {
            if (it == null) throw IllegalArgumentException("message id does not exist")
            if (it != IMessageManager.MessageType.CHANNEL) throw IllegalAccessException("message is not channel")
            else messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_CHANNEL_ID).thenApply { counter->counter!! }
        }
    }

    override fun getMessageDestUserId(msgId: Long): CompletableFuture<Long> {
        return getMessageType(msgId).thenCompose {
            if (it == null) throw IllegalArgumentException("message id does not exist")
            if (it != IMessageManager.MessageType.PRIVATE) throw IllegalAccessException("message is not channel")
            else messageStorage.getLongById(msgId, MANAGERS_CONSTS.MESSAGE_DEST_USER_ID).thenApply { counter->counter!! }
        }
    }

    override fun updateMessageReceivedTime(msgId: Long, received: LocalDateTime): CompletableFuture<Unit> {
        return messageStorage.setTimeToId(msgId, MANAGERS_CONSTS.MESSAGE_RECEIVED_TIME, received)
                .thenCompose { getMessageType(msgId) }
                .thenCompose {
                    if (it == IMessageManager.MessageType.PRIVATE)
                        statisticsManager.decreaseNumberOfPendingMsgsBy()
                    else ImmediateFuture { Unit }
                }
    }

    override fun decreaseMessageCounterBy(msgId: Long, count: Long): CompletableFuture<Unit> {
        return getMessageCounter(msgId).thenCompose {
            messageStorage.setLongToId(msgId, MANAGERS_CONSTS.MESSAGE_COUNTER, it - count)
        }.thenCompose {
            getMessageCounter(msgId)
        }.thenCompose {
            if (it == 0L) {
                broadcastMessagesTree.delete(IdKey(msgId))
                statisticsManager.decreaseNumberOfPendingMsgsBy()
            }
            else ImmediateFuture { Unit }
        }
    }

    override fun getNumberOfPendingMessages(): CompletableFuture<Long> {
        return statisticsManager.getNumberOfPendingMessages()
    }

    override fun getAllBroadcastMessageIds(): List<Long> {
        return broadcastMessagesTree.keys().map { it.getId() }
    }
}