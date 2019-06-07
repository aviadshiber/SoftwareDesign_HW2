package il.ac.technion.cs.softwaredesign.storage.proxies

import il.ac.technion.cs.softwaredesign.internals.LRUCache
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import io.github.vjames19.futures.jdk8.ImmediateFuture
import java.util.concurrent.CompletableFuture


class SecureStorageCache(private val secureStorage: SecureStorage) : SecureStorage {
    private val cache: LRUCache<ByteArrayKey, ByteArray?> = LRUCache(capacity = 250_000)

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        val keyWrapper = ByteArrayKey(key)
        val cacheValue = cache[keyWrapper]
        return if (cacheValue == null) {
            secureStorage.read(key).thenApply {
                cache[keyWrapper] = it
                it
            }
        } else {
            ImmediateFuture{cacheValue}
        }
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        val keyWrapper = ByteArrayKey(key)
        cache[keyWrapper] = value
        return secureStorage.write(key, value)
    }

    class ByteArrayKey(private val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean =
                this === other || other is ByteArrayKey && this.bytes contentEquals other.bytes

        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.contentToString()
    }
}