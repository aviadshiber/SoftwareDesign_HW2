package il.ac.technion.cs.softwaredesign.storage.api

import java.util.concurrent.CompletableFuture

/**
 * This interface used to perform token's operations
 */
interface ITokenManager {
    /**
     * Check if token is valid
     * Token is valid if and only if token exist in the system & was not invalidated
     * @param token String
     * @return Boolean
     */
    fun isTokenValid(token : String) : CompletableFuture<Boolean>
    /**
     * Get the username that been assigned to the token
     * @param token String the token to search for in the system
     * @return  user id or null if token is not valid
     */
    fun getUserIdByToken(token: String) : CompletableFuture<Long?>

    /**
     * Generate a new valid token and assign it to user
     * @param userId user id
     * @throws IllegalArgumentException if userId is not valid
     * @return token has been assigned to user
     */
    fun assignTokenToUserId(userId: Long) : CompletableFuture<String>

    /**
     * Invalidate user token
     * @param token String
     * @throws IllegalArgumentException if token does not belong to any user
     */
    fun invalidateUserToken(token : String): CompletableFuture<Unit>
}