package il.ac.technion.cs.softwaredesign.storage.users

import java.util.concurrent.CompletableFuture

interface IUserStorage {
    fun getUserIdByUsername(usernameKey : String) : CompletableFuture<Long?>
    fun setUserIdToUsername(usernameKey: String, userIdValue: Long): CompletableFuture<Unit>

    fun getUserIdByToken(tokenKey : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setUserIdToToken(tokenKey: CompletableFuture<String?>, userIdValue: CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getPropertyStringByUserId(userIdKey : Long, property : String) : CompletableFuture<String?>
    fun setPropertyStringToUserId(userIdKey : Long, property : String, value : String): CompletableFuture<Unit>

    fun getPropertyLongByUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setPropertyLongToUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>, value : CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getPropertyListByUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>) : CompletableFuture<List<Long>?>
    fun setPropertyListToUserId(userIdKey: CompletableFuture<Long?>, property: CompletableFuture<String?>, listValue: CompletableFuture<List<Long?>>): CompletableFuture<Unit>
}