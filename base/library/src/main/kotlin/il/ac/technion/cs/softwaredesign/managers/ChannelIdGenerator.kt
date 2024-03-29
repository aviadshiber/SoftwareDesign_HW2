package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.MAX_CHANNEL_INDEX
import java.lang.NullPointerException
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Channel id's generator
 * @property statisticsStorage IStatisticsStorage
 * @constructor
 */
@Singleton
class ChannelIdGenerator @Inject constructor(private val statisticsStorage: IStatisticsStorage) : ISequenceGenerator {
    /**
     * generate unique channel id
     * @return CompletableFuture<Long>
     */
    override fun next(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(MAX_CHANNEL_INDEX).thenApply { currentValue ->
            if (currentValue == null) throw NullPointerException("Number of channels must be valid key")
            val newValue = currentValue+1L
            statisticsStorage.setLongValue(MAX_CHANNEL_INDEX, newValue)
            newValue
        }
    }
}