package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_USERS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.USER_MESSAGE_ID
import java.lang.NullPointerException
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User & Message id's generator
 * @property statisticsStorage IStatisticsStorage
 * @constructor
 */
@Singleton
class UserMessageIdGenerator @Inject constructor(private val statisticsStorage: IStatisticsStorage) : ISequenceGenerator {
    /**
     * generate unique id for user or message
     * @return CompletableFuture<Long>
     */
    override fun next(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(USER_MESSAGE_ID).thenApply { currentValue ->
            if (currentValue == null) throw NullPointerException("Number of channels must be valid key")
            val newValue = currentValue+1L
            statisticsStorage.setLongValue(USER_MESSAGE_ID, newValue)
            newValue
        }
    }
}