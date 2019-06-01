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
        @MemberIdStorage private val userIdStorage:SecureStorage,
        @MemberDetailsStorage private val userDetailsStorage:SecureStorage,
        @AuthenticationStorage private val tokenStorage:SecureStorage) : IUserStorage {

    override fun getUserIdByUsername(usernameKey: CompletableFuture<String?>): CompletableFuture<Long?> {
        return usernameKey.thenCompose<Long?> { username ->
            if (username == null) CompletableFuture.supplyAsync{ null } // if usernameKey is null
            else userIdStorage.read(username.toByteArray()).thenApply { userId ->
                if (userId == null ) null // if username does not exist
                else ConversionUtils.bytesToLong(userId)
            }
        }
    }

    override fun setUserIdToUsername(usernameKey: CompletableFuture<String?>, userIdValue: CompletableFuture<Long?>): CompletableFuture<Unit> {
        return usernameKey.thenCompose { username ->
            userIdValue.thenCompose { userId ->
                if (username != null && userId != null)
                    userIdStorage.write(username.toByteArray(),ConversionUtils.longToBytes(userId))
                else CompletableFuture.supplyAsync{ Unit }
            }
        }
    }

    override fun getUserIdByToken(tokenKey: CompletableFuture<String?>): CompletableFuture<Long?> {
        return tokenKey.thenCompose<Long?> { token ->
            if (token == null) {
                CompletableFuture.supplyAsync{ null }
            } else {
                tokenStorage.read(token.toByteArray()).thenApply { userId ->
                    if (userId == null ) null // if token does not exist
                    else ConversionUtils.bytesToLong(userId)
                }
            }
        }
    }

    override fun setUserIdToToken(tokenKey: CompletableFuture<String?>, userIdValue: CompletableFuture<Long?>): CompletableFuture<Unit> {
        return tokenKey.thenCompose { token ->
            userIdValue.thenCompose { userId ->
                if (token != null && userId != null)
                    userIdStorage.write(token.toByteArray(),ConversionUtils.longToBytes(userId))
                else CompletableFuture.supplyAsync{ Unit }
            }
        }
    }

    override fun getPropertyStringByUserId(userIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>): CompletableFuture<String?> {
        val key = createPropertyKey(userIdKey, property)
        val value= userDetailsStorage.read(key) ?: return null
        return String(value)
    }

    override fun setPropertyStringToUserId(userIdKey: Long, property: String, value: String) {
        val key = createPropertyKey(userIdKey, property)
        userDetailsStorage.write(key, value.toByteArray())
    }

    override fun getPropertyLongByUserId(userIdKey: Long, property: String): Long? {
        val key = createPropertyKey(userIdKey, property)
        val value= userDetailsStorage.read(key) ?: return null
        return ConversionUtils.bytesToLong(value)
    }

    override fun setPropertyLongToUserId(userIdKey: Long, property: String, value: Long) {
        val key = createPropertyKey(userIdKey, property)
        userDetailsStorage.write(key, ConversionUtils.longToBytes(value))
    }

    override fun getPropertyListByUserId(userIdKey: Long, property: String): List<Long>? {
        val key = createPropertyKey(userIdKey, property)
        val value= userDetailsStorage.read(key) ?: return null
        val stringValue = String(value)
        if (stringValue == "") return emptyList()
        return stringValue.split(DELIMITER).map { it.toLong() }.toMutableList()
    }

    override fun setPropertyListToUserId(userIdKey: Long, property: String, listValue: List<Long>) {
        val key = createPropertyKey(userIdKey, property)
        val value = listValue.joinToString(DELIMITER)
        userDetailsStorage.write(key, value.toByteArray())
    }

    private fun createPropertyKey(userId: Long, property: String) : ByteArray{
        val userIdByteArray = ConversionUtils.longToBytes(userId)
        val keySuffixByteArray = "$DELIMITER$property".toByteArray()
        return userIdByteArray + keySuffixByteArray
    }

    private fun createPropertyKey(userIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>) : CompletableFuture<ByteArray?>{
        return userIdKey.thenCombine<String?, ByteArray?>(property
        ) { userId, propertyVal ->
            if (userId != null && propertyVal != null) {
                val userIdByteArray = ConversionUtils.longToBytes(userId)
                val keySuffixByteArray = "$DELIMITER$propertyVal".toByteArray()
                userIdByteArray + keySuffixByteArray
            } else {
                null
            }
        }
    }
}