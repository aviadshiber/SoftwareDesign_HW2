package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.storage.api.IStatisticsManager
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_CHANNELS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_CHANNEL_MESSAGES
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_LOGGED_IN_USERS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_PENDING_MESSAGES
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_USERS
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsManager @Inject constructor(private val statisticsStorage: IStatisticsStorage) : IStatisticsManager {
    override fun getTotalUsers(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(STATISTICS_KEYS.NUMBER_OF_USERS)
                .thenApply { it ?: throw IllegalAccessException("should not get here, NUMBER_OF_USERS is a valid key")}
    }

    override fun getLoggedInUsers(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(NUMBER_OF_LOGGED_IN_USERS)
                .thenApply { it ?:throw IllegalAccessException("should not get here, NUMBER_OF_LOGGED_IN_USERS is a valid key") }
    }

    override fun getNumberOfChannels(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(NUMBER_OF_CHANNELS)
                .thenApply {it ?: throw IllegalAccessException("should not get here, NUMBER_OF_CHANNELS is a valid key") }
    }

    override fun getNumberOfPendingMessages(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(NUMBER_OF_PENDING_MESSAGES)
                .thenApply {it ?: throw IllegalAccessException("should not get here, NUMBER_OF_CHANNELS is a valid key") }
    }

    override fun getNumberOfChannelMessages(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(STATISTICS_KEYS.NUMBER_OF_CHANNEL_MESSAGES)
                .thenApply { it ?: throw IllegalAccessException("should not get here, NUMBER_OF_CHANNEL_MESSAGES is a valid key")}
    }

    override fun increaseNumberOfUsersBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_USERS, count)
    }

    override fun increaseLoggedInUsersBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_LOGGED_IN_USERS, count)
    }

    override fun decreaseLoggedInUsersBy(count: Int): CompletableFuture<Unit> {
       return updateKeyBy(NUMBER_OF_LOGGED_IN_USERS, -count)
    }

    override fun increaseNumberOfChannelsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_CHANNELS, count)
    }

    override fun decreaseNumberOfChannelsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_CHANNELS, -count)
    }

    override fun increaseNumberOfChannelMsgsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_CHANNEL_MESSAGES, count)
    }

    override fun decreaseNumberOfChannelMsgsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_CHANNEL_MESSAGES, -count)
    }

    override fun increaseNumberOfPendingMsgsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_PENDING_MESSAGES, count)
    }

    override fun decreaseNumberOfPendingMsgsBy(count: Int): CompletableFuture<Unit> {
        return updateKeyBy(NUMBER_OF_PENDING_MESSAGES, -count)
    }

    private fun updateKeyBy(key: String, count: Int) :CompletableFuture<Unit> {
        val oldValue =
                when (key) {
                    NUMBER_OF_LOGGED_IN_USERS -> getLoggedInUsers()
                    NUMBER_OF_USERS -> getTotalUsers()
                    NUMBER_OF_CHANNELS -> getNumberOfChannels()
                    NUMBER_OF_CHANNEL_MESSAGES -> getNumberOfChannelMessages()
                    NUMBER_OF_PENDING_MESSAGES -> getNumberOfPendingMessages()
                    else -> throw IllegalAccessException("Cannot increase this value, should not get here")
                }
        return oldValue.thenCompose {statisticsStorage.setLongValue(key, it+count)  }
    }
}