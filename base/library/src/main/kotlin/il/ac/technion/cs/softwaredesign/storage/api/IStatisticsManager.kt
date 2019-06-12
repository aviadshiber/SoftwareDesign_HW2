package il.ac.technion.cs.softwaredesign.storage.api

import java.util.concurrent.CompletableFuture

/**
 * This interface used to perform operations on global data statistics
 */
interface IStatisticsManager {
    /** PRIMITIVE STATISTICS **/
    /**
     * get number of total users in the system
     * @return Long
     */
    fun getTotalUsers() : CompletableFuture<Long>

    /**
     * get number of total logged in users in the system
     * @return Long
     */
    fun getLoggedInUsers() : CompletableFuture<Long>

    /**
     * get number of channels in the system
     * @return Long
     */
    fun getNumberOfChannels() : CompletableFuture<Long>

    /**
     * get number of pending messages in the system, without channel messages
     * @return Long
     */
    fun getNumberOfPendingMessages() : CompletableFuture<Long>

    /**
     * get number of total channel messages in the system
     * @return Long
     */
    fun getNumberOfChannelMessages() : CompletableFuture<Long>

    /**
     * Increase number of users in the system by [count]
     * @param count Int, increase value by count
     * @return Long, the updated value
     */
    fun increaseNumberOfUsersBy(count: Int = 1): CompletableFuture<Unit>

    /**
     * Increase number of logged in users in the system by [count]
     * @param count Int, increase value by count
     * @return Long, the updated value
     */
    fun increaseLoggedInUsersBy(count : Int = 1) : CompletableFuture<Unit>

    /**
     * Decrease number of logged in users in the system by [count]
     * @param count Int, decrease value by count
     * @return Long, the updated value
     */
    fun decreaseLoggedInUsersBy(count : Int = 1) :CompletableFuture<Unit>

    /**
     * Increase number of channels in the system by [count]
     * @param count Int, increase value by count
     * @return Long, the updated value
     */
    fun increaseNumberOfChannelsBy(count : Int = 1) :CompletableFuture<Unit>

    /**
     * Decrease number of channels in the system by [count]
     * @param count Int, decrease value by count
     * @return Long, the updated value
     */
    fun decreaseNumberOfChannelsBy(count : Int = 1) :CompletableFuture<Unit>

    /**
     * Increase number of pending messages in the system by [count]
     * @param count Int, increase value by count
     * @return Long, the updated value
     */
    fun increaseNumberOfPendingMsgsBy(count: Int = 1): CompletableFuture<Unit>

    /**
     * Decrease number of pending messages in the system by [count]
     * @param count Int, decrease value by count
     * @return Long, the updated value
     */
    fun decreaseNumberOfPendingMsgsBy(count: Int = 1): CompletableFuture<Unit>

    /**
     * Increase number of channel messages in the system by [count]
     * @param count Int, increase value by count
     * @return Long, the updated value
     */
    fun increaseNumberOfChannelMsgsBy(count: Int = 1): CompletableFuture<Unit>

    /**
     * Decrease number of channel messages in the system by [count]
     * @param count Int, decrease value by count
     * @return Long, the updated value
     */
    fun decreaseNumberOfChannelMsgsBy(count: Int = 1): CompletableFuture<Unit>
}