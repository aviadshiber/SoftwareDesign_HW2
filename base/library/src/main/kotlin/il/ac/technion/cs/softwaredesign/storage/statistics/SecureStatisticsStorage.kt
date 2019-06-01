package il.ac.technion.cs.softwaredesign.storage.statistics

import il.ac.technion.cs.softwaredesign.managers.StatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStatisticsStorage @Inject constructor(@StatisticsStorage private val statisticsStorage: SecureStorage) : IStatisticsStorage {

    override fun getLongValue(key: String): CompletableFuture<Long?> {
        return statisticsStorage.read(key.toByteArray()).thenApply { k ->
            if (k == null) null
            else ConversionUtils.bytesToLong(k)
        }
    }

    override fun setLongValue(key: String, value: Long): CompletableFuture<Unit> {
        return statisticsStorage.write(key.toByteArray(), ConversionUtils.longToBytes(value))
    }
}