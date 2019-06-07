package il.ac.technion.cs.softwaredesign.storage.datastructures

import il.ac.technion.cs.softwaredesign.storage.SecureStorage

class StorageWrapper(private val secureStorage: SecureStorage) {
    fun read(key: ByteArray): ByteArray? = secureStorage.read(key).join()

    fun write(key: ByteArray, value: ByteArray): Unit = secureStorage.write(key, value).join()
}