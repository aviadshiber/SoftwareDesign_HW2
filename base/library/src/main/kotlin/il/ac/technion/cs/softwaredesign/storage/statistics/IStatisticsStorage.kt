package il.ac.technion.cs.softwaredesign.storage.statistics

import java.util.concurrent.CompletableFuture

interface IStatisticsStorage {
    fun getLongValue(key: String): CompletableFuture<Long?>
    fun setLongValue(key : String, value : Long) : CompletableFuture<Unit>
}