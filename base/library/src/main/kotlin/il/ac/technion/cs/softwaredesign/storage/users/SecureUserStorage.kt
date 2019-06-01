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

    override fun getUserIdByUsername(usernameKey: String): CompletableFuture<Long?> {
        return userIdStorage.read(usernameKey.toByteArray()).thenApply { userId ->
                if (userId == null ) null // if username does not exist
                else ConversionUtils.bytesToLong(userId)
            }
    }

    override fun setUserIdToUsername(usernameKey: String, userIdValue: Long): CompletableFuture<Unit> {
        return userIdStorage.write(usernameKey.toByteArray(), ConversionUtils.longToBytes(userIdValue))
//        return usernameKey.thenCompose { username ->
//            userIdValue.thenCompose { userId ->
//                if (username != null && userId != null)
//                    userIdStorage.write(username.toByteArray(),ConversionUtils.longToBytes(userId))
//                else CompletableFuture.supplyAsync{ Unit }
//            }
//        }
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
                    tokenStorage.write(token.toByteArray(),ConversionUtils.longToBytes(userId))
                else CompletableFuture.supplyAsync{ Unit }
            }
        }
    }

    override fun getPropertyStringByUserId(userIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>): CompletableFuture<String?> {
        return createPropertyKey(userIdKey, property).thenCompose<ByteArray?> { key ->
            if (key == null) null // userId is null
            else userDetailsStorage.read(key)
        }.thenApply { byteArray ->
            if (byteArray == null) null else String(byteArray) // userId does not exist
        }
    }

    override fun setPropertyStringToUserId(userIdKey: CompletableFuture<Long?>,
                                           property: CompletableFuture<String?>,
                                           value: CompletableFuture<String?>): CompletableFuture<Unit> {
        return createPropertyKey(userIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                value.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else userDetailsStorage.write(propertyKey, valueToWrite.toByteArray())
                }
            }
        }
    }

    override fun getPropertyLongByUserId(userIdKey: CompletableFuture<Long?>,
                                         property: CompletableFuture<String?>): CompletableFuture<Long?> {
        return createPropertyKey(userIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                userDetailsStorage.read(propertyKey).thenApply { byteArray ->
                    if (byteArray == null) null else ConversionUtils.bytesToLong(byteArray) // userId does not exist
                }
            }
        }
    }

    override fun setPropertyLongToUserId(userIdKey: CompletableFuture<Long?>,
                                         property: CompletableFuture<String?>,
                                         value: CompletableFuture<Long?>) : CompletableFuture<Unit> {
        return createPropertyKey(userIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                value.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else userDetailsStorage.write(propertyKey, ConversionUtils.longToBytes(valueToWrite))
                }
            }
        }
    }

    override fun getPropertyListByUserId(userIdKey: CompletableFuture<Long?>,
                                         property: CompletableFuture<String?>): CompletableFuture<List<Long>?> {
        return createPropertyKey(userIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null
            else {
                userDetailsStorage.read(propertyKey).thenApply { list ->
                    if (list == null) null // user id does not exist
                    else {
                        val stringValue = String(list)
                        if (stringValue == "") listOf<Long>()
                        stringValue.split(DELIMITER).map { it.toLong() }.toList()
                    }
                }
            }
        }
    }

    override fun setPropertyListToUserId(userIdKey: CompletableFuture<Long?>,
                                         property: CompletableFuture<String?>,
                                         listValue: CompletableFuture<List<Long?>>): CompletableFuture<Unit> {
        return createPropertyKey(userIdKey, property).thenCompose { propertyKey ->
            if (propertyKey == null) null // should not get here
            else {
                listValue.thenCompose { valueToWrite ->
                    if (valueToWrite == null) null // value should not be null
                    else {
                        val value = valueToWrite.joinToString(DELIMITER)
                        userDetailsStorage.write(propertyKey, value.toByteArray())
                    }
                }
            }
        }
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