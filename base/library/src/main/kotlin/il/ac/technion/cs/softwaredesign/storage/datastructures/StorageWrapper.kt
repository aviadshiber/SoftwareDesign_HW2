package il.ac.technion.cs.softwaredesign.storage.datastructures

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils

/**
 * Wrapper for primitive storage with read/write operations that returned after the operation terminated
 * @property secureStorage SecureStorage
 * @property keyPrefixByteArray ByteArray - this prefix is going to be added to the accepted keys in read write
 * @constructor
 */
class StorageWrapper(private val secureStorage: SecureStorage, keyPrefix: Long?) {
    private val keyPrefixByteArray =
            if (keyPrefix==null) byteArrayOf()
            else ConversionUtils.longToBytes(keyPrefix)

    // <keyPrefix><key>
    fun read(key: ByteArray): ByteArray? {
        val completeKey = keyPrefixByteArray + key
        return secureStorage.read(completeKey).join()
    }

    fun write(key: ByteArray, value: ByteArray): Unit {
        val completeKey = keyPrefixByteArray + key
        return secureStorage.write(completeKey, value).join()
    }
}