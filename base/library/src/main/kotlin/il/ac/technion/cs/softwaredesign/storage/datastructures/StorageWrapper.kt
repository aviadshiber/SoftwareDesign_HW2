package il.ac.technion.cs.softwaredesign.storage.datastructures

import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils

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