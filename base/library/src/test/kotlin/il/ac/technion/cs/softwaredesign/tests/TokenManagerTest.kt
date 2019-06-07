package il.ac.technion.cs.softwaredesign.tests

import com.natpryce.hamkrest.equalTo
import il.ac.technion.cs.softwaredesign.managers.TokenManager
import il.ac.technion.cs.softwaredesign.storage.users.IUserStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.INVALID_USER_ID
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class TokenManagerTest{

    private val userStorage = mockk<IUserStorage>()
    private val tokenManager=TokenManager(userStorage)


    /**
     * isTokenValid
     */
    @Test
    fun `token that has'nt been written to the system is not exist`() {
        every{userStorage.getUserIdByToken(any()) } returns ImmediateFuture{null}
        assertWithTimeout({ tokenManager.isTokenValid("InvalidToken").get()}, isFalse)
    }

    @Test
    fun `token that has been written to the system is exist`() {
        every { userStorage.getUserIdByToken("validToken") } returns ImmediateFuture{5L}
        assertWithTimeout({ tokenManager.isTokenValid("validToken").get() }, isTrue)
    }


    /**
     * getUserIdByToken
     */
    @Test
    fun `returned user compatible to written token-user mapping`() {
        every{ userStorage.getUserIdByToken("aviad")} returns ImmediateFuture{10L}
        assertWithTimeout({ tokenManager.getUserIdByToken("aviad").get() }, equalTo(10L))
    }

    /**
     * getUserIdByToken
     */
    /*@Test TODO: fix and uncomment
    fun `getUserIdByToken returns null if token is not valid`() {
        Assertions.assertNull( tokenManager.getUserIdByToken("aviad"))
    }

    @Test
    fun `token been assigned to user`() {
        every { userStorage.setUserIdToToken(any(),any()) } answers {}
        val token=tokenManager.assignTokenToUserId(1L)
        every{ userStorage.getUserIdByToken(token)} returns 1L
        assertWithTimeout({tokenManager.isTokenValid(token)}, isTrue)
    }*/

    @Test
    fun `token been invalidated`() {
        every { userStorage.setUserIdToToken(any(),any()) } answers { ImmediateFuture{Unit} }
        every{ userStorage.getUserIdByToken(any())} returns ImmediateFuture{null}
        val token = tokenManager.assignTokenToUserId(1L).join()
        every{ userStorage.getUserIdByToken(any())} returns ImmediateFuture{1L}
        assertWithTimeout({tokenManager.isTokenValid(token).get()}, isTrue)
        tokenManager.invalidateUserToken(token).join()
        every{ userStorage.getUserIdByToken(token)} returns ImmediateFuture{INVALID_USER_ID}
        assertWithTimeout({tokenManager.isTokenValid(token).get()}, isFalse)

    }

    @Test
    fun `throws IllegalArgumentException if token does not belong to any user`() {
        every {userStorage.getUserIdByToken(any())} returns ImmediateFuture{1L}
        every{ userStorage.getUserIdByToken("invalidToken")} returns ImmediateFuture{null}
        assertThrowsWithTimeout<Unit, IllegalArgumentException>({ tokenManager.invalidateUserToken("invalidToken").joinException() })
    }
}