package il.ac.technion.cs.softwaredesign.storage.users

import il.ac.technion.cs.softwaredesign.managers.AuthenticationStorage
import il.ac.technion.cs.softwaredesign.managers.MemberDetailsStorage
import il.ac.technion.cs.softwaredesign.managers.MemberIdStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.DELIMITER
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureUserStorage @Inject constructor(
        @MemberIdStorage private val userIdStorage: SecureStorage,
        @MemberDetailsStorage private val userDetailsStorage: SecureStorage,
        @AuthenticationStorage private val tokenStorage: SecureStorage) : IUserStorage {
    override fun getUserIdByUsername(usernameKey: String): CompletableFuture<Long?> {
        return userIdStorage.read(usernameKey.toByteArray()).thenApply { userId ->
            if (userId == null) null // if username does not exist
            else ConversionUtils.bytesToLong(userId)
        }
    }

    override fun setUserIdToUsername(usernameKey: String, userIdValue: Long): CompletableFuture<Unit> {
        return userIdStorage.write(usernameKey.toByteArray(), ConversionUtils.longToBytes(userIdValue))
    }

    override fun getUserIdByToken(tokenKey: String): CompletableFuture<Long?> {
        return tokenStorage.read(tokenKey.toByteArray()).
                thenApply { if (it == null) null
                            else ConversionUtils.bytesToLong(it) }
    }

    override fun setUserIdToToken(tokenKey: String, userIdValue: Long): CompletableFuture<Unit> {
        return tokenStorage.write(tokenKey.toByteArray(), ConversionUtils.longToBytes(userIdValue))
    }

    override fun getPropertyStringByUserId(userIdKey: Long, property: String): CompletableFuture<String?> {
        val key = createPropertyKey(userIdKey, property)
        val value = userDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else String(it) }
    }

    override fun setPropertyStringToUserId(userIdKey: Long, property: String, value: String): CompletableFuture<Unit> {
        val key = createPropertyKey(userIdKey, property)
        return userDetailsStorage.write(key, value.toByteArray())
    }

    override fun getPropertyLongByUserId(userIdKey: Long, property: String): CompletableFuture<Long?> {
        val key = createPropertyKey(userIdKey, property)
        val value = userDetailsStorage.read(key)
        return value.thenApply { if (it == null) null else ConversionUtils.bytesToLong(it) }
    }

    override fun setPropertyLongToUserId(userIdKey: Long, property: String, value: Long): CompletableFuture<Unit> {
        val key = createPropertyKey(userIdKey, property)
        return userDetailsStorage.write(key, ConversionUtils.longToBytes(value))
    }

    override fun getPropertyListByUserId(userIdKey: Long, property: String): CompletableFuture<List<Long>?> {
        val key = createPropertyKey(userIdKey, property)
        val value = userDetailsStorage.read(key)
        return value.thenApply {
            if (it == null) null else delimitedByteArrayToList(it)
        }
    }

    private fun delimitedByteArrayToList(byteArray: ByteArray): List<Long> {
        val stringValue = String(byteArray)
        return if (stringValue == "") emptyList<Long>().toMutableList()
        else stringValue.split(DELIMITER).map { it.toLong() }.toMutableList()
    }

    override fun setPropertyListToUserId(userIdKey: Long, property: String, listValue: List<Long?>):
            CompletableFuture<Unit> {
        val key = createPropertyKey(userIdKey, property)
        val value = listValue.joinToString(DELIMITER)
        return userDetailsStorage.write(key, value.toByteArray())
    }

    private fun createPropertyKey(userId: Long, property: String): ByteArray {
        val userIdByteArray = ConversionUtils.longToBytes(userId)
        val keySuffixByteArray = "$DELIMITER$property".toByteArray()
        return userIdByteArray + keySuffixByteArray
    }
}