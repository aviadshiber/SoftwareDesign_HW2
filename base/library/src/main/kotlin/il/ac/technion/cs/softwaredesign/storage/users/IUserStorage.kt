package il.ac.technion.cs.softwaredesign.storage.users

import java.util.concurrent.CompletableFuture

interface IUserStorage {
    fun getUserIdByUsername(usernameKey : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setUserIdToUsername(usernameKey: CompletableFuture<String?>, userIdValue: CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getUserIdByToken(tokenKey : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setUserIdToToken(tokenKey: CompletableFuture<String?>, userIdValue: CompletableFuture<Long?>): CompletableFuture<Unit>

    fun getPropertyStringByUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>) : CompletableFuture<String?>
    fun setPropertyStringToUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>, value : CompletableFuture<String?>)

    fun getPropertyLongByUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>) : CompletableFuture<Long?>
    fun setPropertyLongToUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>, value : CompletableFuture<Long?>)

    fun getPropertyListByUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>) : CompletableFuture<List<Long>?>
    fun setPropertyListToUserId(userIdKey : CompletableFuture<Long?>, property : CompletableFuture<String?>, listValue : List<Long?>)
}