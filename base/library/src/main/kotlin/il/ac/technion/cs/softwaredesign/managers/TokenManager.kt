package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.storage.api.ITokenManager
import il.ac.technion.cs.softwaredesign.storage.users.IUserStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.INVALID_USER_ID
import java.security.SecureRandom
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(private val userStorage: IUserStorage) : ITokenManager {

    override fun isTokenValid(token: String): CompletableFuture<Boolean> {
        return userStorage.getUserIdByToken(token).thenApply { if(it==null) null else it != INVALID_USER_ID }
    }

    override fun getUserIdByToken(token: String): CompletableFuture<Long?> {
        return userStorage.getUserIdByToken(token)
                .thenApply {  if (it == null || it == INVALID_USER_ID)  null else it }
    }

    override fun assignTokenToUserId(userId: Long): CompletableFuture<String> {
        if (userId == INVALID_USER_ID) throw IllegalArgumentException("User id is not valid")
        val token = generateValidUserToken()
        return userStorage.setUserIdToToken(token, userId).thenApply { token }

    }


    override fun invalidateUserToken(token: String):CompletableFuture<Unit> {
        return getUserIdByToken(token).thenApply { it ?: throw java.lang.IllegalArgumentException("token does not exist") }
                .thenCompose { userStorage.setUserIdToToken(token, INVALID_USER_ID)  }
    }

    private fun isTokenUnique(token: String): CompletableFuture<Boolean> {
        return getUserIdByToken(token).thenApply { it==null }
    }

    /**
     * Generate 8 byte token for user and return it
     * @return String
     */
    private fun generateUserToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(8)
        random.nextBytes(bytes)
        return bytes.toString()
    }

    /**
     * Generate token that does not exist in the persistent memory
     * @return String token
     */
    private fun generateValidUserToken(): String {
        var token: String
        var it = 0
        do {
            token = generateUserToken()
            if (it > 30) {
                break
            }
            it += 1
        } while (!isTokenUnique(token)) //TODO: fix
        return token
    }
}