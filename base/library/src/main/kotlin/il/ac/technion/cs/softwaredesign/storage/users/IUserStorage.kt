package il.ac.technion.cs.softwaredesign.storage.users

import java.util.concurrent.CompletableFuture

interface IUserStorage {
    fun getUserIdByUsername(usernameKey : String) : CompletableFuture<Long?>
    fun setUserIdToUsername(usernameKey: String, userIdValue: Long): CompletableFuture<Unit>

    fun getUserIdByToken(tokenKey: String) : CompletableFuture<Long?>
    fun setUserIdToToken(tokenKey: String, userIdValue: Long): CompletableFuture<Unit>

    fun getPropertyStringByUserId(userIdKey : Long, property : String) : CompletableFuture<String?>
    fun setPropertyStringToUserId(userIdKey : Long, property : String, value : String): CompletableFuture<Unit>

    fun getPropertyLongByUserId(userIdKey: Long, property: String) : CompletableFuture<Long?>
    fun setPropertyLongToUserId(userIdKey: Long, property: String, value: Long): CompletableFuture<Unit>

    fun getPropertyListByUserId(userIdKey: Long, property: String) : CompletableFuture<List<Long>?>
    fun setPropertyListToUserId(userIdKey: Long, property: String, listValue: List<Long?>): CompletableFuture<Unit>
}