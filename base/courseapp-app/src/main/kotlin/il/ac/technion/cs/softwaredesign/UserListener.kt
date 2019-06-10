package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.managers.MessageManager
import il.ac.technion.cs.softwaredesign.managers.UserManager
import il.ac.technion.cs.softwaredesign.messages.Message
import io.github.vjames19.futures.jdk8.ImmediateFuture
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class UserListener @Inject constructor(private val userManager: UserManager,private val messageManager: MessageManager) {
    private var callbacks = mutableListOf<ListenerCallback>()

    /**
     * add a new callback
     * @param callback ListenerCallback
     * @return Boolean, true if callback added successfully and false if the callback is already contained in the set
     */
    fun listen(callback: ListenerCallback): UserListener {
        callbacks.add(callback)
        return this
    }

    /**
     * remove callback
     * @param callback ListenerCallback
     * @return Boolean, true if callback removed successfully and false if the callback is not contained in the set
     */
    fun unlisten(callback: ListenerCallback): UserListener {
        callbacks.remove(callback)
        return this
    }

    fun notifyOnMessageArrive(destUserId: Long, source: String, message: Message): CompletableFuture<Unit> {
        return userManager.updateUserLastReadMsgId(destUserId, message.id)
                .thenApply { message.received = LocalDateTime.now() }
                .thenCompose { messageManager.updateMessageReceivedTime(message.id, message.received!!) }
                .thenCompose { applyCallbacks(source, message) }
    }

    private fun applyCallbacks(source: String, message: Message): CompletableFuture<Unit> {
        return if (callbacks.isNotEmpty())
            callbacks.map { callback -> callback(source, message) }
                    .reduce { acc, completableFuture ->
                        acc.thenCompose { completableFuture }
                    }
        else
            ImmediateFuture { Unit }
    }

    fun callbackExist(callback: ListenerCallback) = callbacks.find { it == callback } != null


    /**
     * check if there are no callbacks to this user
     * @return Boolean, true if there are no callbacks, false if there is at least one callback
     */
    fun isEmpty(): Boolean = callbacks.isEmpty()

}