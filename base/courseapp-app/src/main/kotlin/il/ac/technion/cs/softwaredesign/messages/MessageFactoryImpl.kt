package il.ac.technion.cs.softwaredesign.messages

import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class MessageFactoryImpl @Inject constructor(private val messageManager: IMessageManager) : MessageFactory {
    override fun create(media: MediaType, contents: ByteArray): CompletableFuture<Message> {
        return messageManager.generateUniqueMessageId().thenApply {
            MessageImpl(it, media = media, contents = contents, created = LocalDateTime.now(), received = null)
        }
    }
}