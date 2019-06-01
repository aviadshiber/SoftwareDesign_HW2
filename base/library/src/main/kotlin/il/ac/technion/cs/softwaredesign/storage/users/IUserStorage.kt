package il.ac.technion.cs.softwaredesign.storage.users

import java.util.concurrent.CompletableFuture

interface IUserStorage {
    fun getUserIdByUsername(usernameKey : String) : CompletableFuture<Long?>
    fun setUserIdToUsername(usernameKey: String, userIdValue: Long): CompletableFuture<Unit>

    fun getUserIdByToken(tokenKey : String) : Long?
    fun setUserIdToToken(tokenKey: String, userIdValue: Long)

    fun getPropertyStringByUserId(userIdKey : Long, property : String) : String?
    fun setPropertyStringToUserId(userIdKey : Long, property : String, value : String)

    fun getPropertyLongByUserId(userIdKey : Long, property : String) :Long?
    fun setPropertyLongToUserId(userIdKey : Long, property : String, value : Long)

    fun getPropertyListByUserId(userIdKey : Long, property : String) : List<Long>?
    fun setPropertyListToUserId(userIdKey : Long, property : String, listValue : List<Long>)
}