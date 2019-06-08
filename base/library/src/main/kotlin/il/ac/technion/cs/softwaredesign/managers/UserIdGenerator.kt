package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.NUMBER_OF_USERS
import java.lang.NullPointerException
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserIdGenerator @Inject constructor(private val statisticsStorage: IStatisticsStorage) : ISequenceGenerator {
    override fun next(): CompletableFuture<Long> {
        return statisticsStorage.getLongValue(NUMBER_OF_USERS).thenApply { currentValue ->
            if (currentValue == null) throw NullPointerException("Number of channels must be valid key")
            val newValue = currentValue+1L
            statisticsStorage.setLongValue(NUMBER_OF_USERS, newValue)
            newValue
        }
    }
}