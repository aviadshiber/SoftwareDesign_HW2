package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.managers.MessageManager
import il.ac.technion.cs.softwaredesign.storage.api.IChannelManager
import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class CourseAppStatisticsImpl @Inject constructor(private val userManager: IUserManager,
                                                  private val messageManager: IMessageManager,
                                                  private val channelManager: IChannelManager) : CourseAppStatistics {
    override fun pendingMessages(): CompletableFuture<Long> {
        return messageManager.getNumberOfPendingMessages()
    }

    override fun channelMessages(): CompletableFuture<Long> {
        return channelManager.getNumberOfTotalChannelMessages()
    }

    override fun top10ChannelsByMessages(): CompletableFuture<List<String>> {
        return channelManager.getTop10ChannelsByMsgsCount()
    }

    override fun totalUsers(): CompletableFuture<Long> {
        return userManager.getTotalUsers()
    }

    override fun loggedInUsers(): CompletableFuture<Long> {
        return userManager.getLoggedInUsers()
    }

    override fun top10ChannelsByUsers(): CompletableFuture<List<String>> {
        return channelManager.getTop10ChannelsByUsersCount()
    }

    override fun top10ActiveChannelsByUsers(): CompletableFuture<List<String>> {
        return channelManager.getTop10ChannelsByActiveUsersCount()
    }

    override fun top10UsersByChannels(): CompletableFuture<List<String>> {
        return userManager.getTop10UsersByChannelsCount()
    }
}