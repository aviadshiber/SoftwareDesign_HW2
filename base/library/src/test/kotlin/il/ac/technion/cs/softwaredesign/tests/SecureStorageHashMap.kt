package il.ac.technion.cs.softwaredesign.tests

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import java.util.concurrent.CompletableFuture

class SecureStorageHashMap : SecureStorage{
    class ByteArrayKey(private val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean =
                this === other || other is ByteArrayKey && this.bytes contentEquals other.bytes
        override fun hashCode(): Int = bytes.contentHashCode()
        override fun toString(): String = bytes.contentToString()
    }
    private val storage = mutableMapOf<ByteArrayKey,ByteArray>()

    override fun read(key: ByteArray): CompletableFuture<ByteArray?> {
        return CompletableFuture.supplyAsync{storage[ByteArrayKey(key)]}
    }

    override fun write(key: ByteArray, value: ByteArray): CompletableFuture<Unit> {
        storage[ByteArrayKey(key)] = value
        return CompletableFuture.supplyAsync{Unit}
    }
}