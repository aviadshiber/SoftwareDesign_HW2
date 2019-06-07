package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.ALGORITHEMS.HASH_ALGORITHM
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.storage.api.IChannelManager
import il.ac.technion.cs.softwaredesign.storage.api.ITokenManager
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager
import io.github.vjames19.futures.jdk8.Future
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.github.vjames19.futures.jdk8.recoverWith
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


class CourseAppImpl
@Inject constructor(private val tokenManager: ITokenManager,
                    private val userManager: IUserManager,
                    private val channelManager: IChannelManager) : CourseApp {

    internal companion object {
        val regex: Regex = Regex("#[#_A-Za-z0-9]*")
    }

    override fun login(username: String, password: String): CompletableFuture<String> {
        val hashedPassword = password.hashString(HASH_ALGORITHM)
        return userManager.getUserId(username).thenCompose { userId ->
            if (userId == null)
                registerFuture(username, hashedPassword)
            else
                loginFuture(userId, hashedPassword)
        }
    }

    private fun loginFuture(userId: Long, hashedPassword: String): CompletableFuture<String> {
        return userManager.getUserPassword(userId).thenApply { validatedPassword(it, hashedPassword) }
                .thenCompose { userManager.getUserStatus(userId) }
                .thenApply { if (it == IUserManager.LoginStatus.IN) throw UserAlreadyLoggedInException() }
                .thenCompose { updateToLoginFuture(userId) }
                .thenCompose { tokenManager.assignTokenToUserId(userId) }
    }

    private fun updateToLoginFuture(userId: Long): CompletableFuture<List<Unit>> {
        val updateUserStatusFuture = userManager.updateUserStatus(userId, IUserManager.LoginStatus.IN)
        val updateChannelStatusFuture = updateUserStatusInChannels(userId, IUserManager.LoginStatus.IN)
        return Future.allAsList(listOf(updateUserStatusFuture, updateChannelStatusFuture))
    }


    private fun validatedPassword(it: String?, hashedPassword: String) =
            if (it != hashedPassword) throw NoSuchEntityException() else it

    private fun registerFuture(username: String, hashedPassword: String): CompletableFuture<String> {
        return userManager.addUser(username, hashedPassword)
                .thenCompose { userId -> upgradeFirstUserToAdminFuture(userId) }
                .thenCompose { userId -> tokenManager.assignTokenToUserId(userId) }
    }

    private fun upgradeFirstUserToAdminFuture(userId: Long): CompletableFuture<Long> {
        return if (userId == 1L)
            userManager.updateUserPrivilege(userId, IUserManager.PrivilegeLevel.ADMIN).thenApply { userId }
        else
            ImmediateFuture { userId }
    }

    override fun logout(token: String): CompletableFuture<Unit> {
        return tokenManager.getUserIdByToken(token).thenApply { it ?: throw InvalidTokenException() }
                .thenCompose { userId -> tokenManager.invalidateUserToken(token).thenApply { userId } }
                .recoverWith { if (it is IllegalArgumentException) throw InvalidTokenException() else throw it }
                .thenCompose { userId -> updateToLogoutFuture(userId) }.thenApply { Unit }
    }

    private fun updateToLogoutFuture(userId: Long): CompletableFuture<List<Unit>> {
        val updateUserStatusFuture = userManager.updateUserStatus(userId, IUserManager.LoginStatus.OUT)
        val updateChannelStatusFuture = updateUserStatusInChannels(userId, IUserManager.LoginStatus.OUT)
        return Future.allAsList(listOf(updateUserStatusFuture, updateChannelStatusFuture))
    }

    override fun isUserLoggedIn(token: String, username: String): CompletableFuture<Boolean?> {
        validateTokenFuture(token)
                .thenCompose { userManager.getUserId(username) }
                .thenCompose {
                    if (it != null)
                        userManager.getUserStatus(it).thenApply { status ->
                            status == IUserManager.LoginStatus.IN
                        }
                    else
                        null
                }
    }

    private fun validateTokenFuture(token: String) =
            tokenManager.isTokenValid(token).thenApply { if (!it) throw InvalidTokenException() }

    override fun makeAdministrator(token: String, username: String): CompletableFuture<Unit> {
        return validateTokenFuture(token).thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! }
                .thenCompose { adminId ->
                    userManager.getUserPrivilege(adminId)
                            .thenApply {
                                if (it != IUserManager.PrivilegeLevel.ADMIN) throw UserNotAuthorizedException()
                            }
                }
                .thenCompose { userManager.getUserId(username).thenApply { it ?: throw NoSuchEntityException() } }
                .thenCompose { userManager.updateUserPrivilege(it, IUserManager.PrivilegeLevel.ADMIN) }
    }

    override fun channelJoin(token: String, channel: String): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenApply { regex matches channel }.thenApply { if (!it) throw NameFormatException() }
                .thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! }
                .thenCompose { userId ->
                    channelManager.isChannelNameExists(channel)
                            .thenApply { isChannelNameExists -> Pair(userId, isChannelNameExists) }
                }
                .thenCompose { (userId, isChannelNameExists) ->
                    if (!isChannelNameExists) // channel does not exist
                        validateUserIsAdminFuture(userId)
                                .thenCompose { createChannelAndMakeUserOperatorFuture(channel, userId) }
                    else
                        channelManager.getChannelIdByName(channel).thenApply { channelId -> Pair(channelId, userId) }
                }
                .thenCompose { (channelId, userId) -> addChannelToUserFuture(userId, channelId) }
                .thenCompose { (channelId, userId) -> addMemberToChannelFuture(channelId, userId) }
                .thenCompose { (channelId, userId) ->
                    increaseNumberOfActiveMembersInChannelForUser(userId, channelId)
                }
    }

    private fun increaseNumberOfActiveMembersInChannelForUser(userId: Long, channelId: Long): CompletableFuture<Unit> {
        return userManager.getUserStatus(userId)
                .thenApply { it == IUserManager.LoginStatus.IN }
                .thenCompose {
                    if (it) channelManager.increaseNumberOfActiveMembersInChannelBy(channelId)
                    else ImmediateFuture { Unit }
                }
                .exceptionally { /* if user try to join again, its ok */ }
    }

    private fun addMemberToChannelFuture(channelId: Long, userId: Long) =
            channelManager.addMemberToChannel(channelId, userId).thenApply { Pair(channelId, userId) }

    private fun addChannelToUserFuture(userId: Long, channelId: Long): CompletableFuture<Pair<Long, Long>>? {
        return userManager.addChannelToUser(userId, channelId)
                .exceptionally { /* if user try to join again, its ok */ }
                .thenApply { Pair(channelId, userId) }
    }

    private fun validateUserIsAdminFuture(userId: Long): CompletableFuture<Unit> {
        return userManager.getUserPrivilege(userId)
                .thenApply {
                    if (it != IUserManager.PrivilegeLevel.ADMIN)
                        throw UserNotAuthorizedException()
                }
    }

    private fun createChannelAndMakeUserOperatorFuture(channel: String, userId: Long): CompletableFuture<Pair<Long, Long>> {
        return channelManager.addChannel(channel)
                .thenCompose { cid ->
                    channelManager.addOperatorToChannel(cid, userId)
                            .thenApply { cid }
                }.thenApply { channelId -> Pair(channelId, userId) }
    }

    override fun channelPart(token: String, channel: String) {
        if (!tokenManager.isTokenValid(token)) throw InvalidTokenException()
        if (!channelManager.isChannelNameExists(channel)) throw NoSuchEntityException()
        val userId = tokenManager.getUserIdByToken(token)
                ?: throw ImpossibleSituation("getUserIdByToken returned null but token is valid")
        val channelId = channelManager.getChannelIdByName(channel)
        if (!isUserMember(userId, channelId)) throw NoSuchEntityException()
        channelManager.removeMemberFromChannel(channelId, userId)
        channelManager.removeOperatorFromChannel(channelId, userId)
        userManager.removeChannelFromUser(userId, channelId) // should not throw!!
        if (userManager.getUserStatus(userId) == IUserManager.LoginStatus.IN) {
            channelManager.decreaseNumberOfActiveMembersInChannelBy(channelId)
        }
        if (channelManager.getNumberOfMembersInChannel(channelId) == 0L) {
            channelManager.removeChannel(channelId)
        }
    }

    override fun channelMakeOperator(token: String, channel: String, username: String) {
        val (initiatorUserId, channelId) = preValidations(token, channel)

        if (!isUserMember(initiatorUserId, channelId)) throw UserNotAuthorizedException()

        val operatorPrivilege = userManager.getUserPrivilege(initiatorUserId)
        if (!isUserOperator(initiatorUserId, channelId) && operatorPrivilege != IUserManager.PrivilegeLevel.ADMIN)
            throw UserNotAuthorizedException()

        val userId = userManager.getUserId(username)
        if (!isUserOperator(initiatorUserId, channelId) && operatorPrivilege == IUserManager.PrivilegeLevel.ADMIN
                && (userId == null || userId != initiatorUserId)) {
            throw UserNotAuthorizedException()
        }

        if (userId == null || !isUserMember(userId, channelId)) throw NoSuchEntityException()
        channelManager.addOperatorToChannel(channelId, userId)
    }

    override fun channelKick(token: String, channel: String, username: String) {
        val (initiatorUserId, channelId) = preValidations(token, channel)
        if (!isUserOperator(initiatorUserId, channelId)) throw UserNotAuthorizedException()
        val userId = userManager.getUserId(username)
        if (userId == null || !isUserMember(userId, channelId)) throw NoSuchEntityException()

        channelManager.removeMemberFromChannel(channelId, userId)
        channelManager.removeOperatorFromChannel(channelId, userId)
        userManager.removeChannelFromUser(userId, channelId) // should not throw!!
        if (userManager.getUserStatus(userId) == IUserManager.LoginStatus.IN) {
            channelManager.decreaseNumberOfActiveMembersInChannelBy(channelId)
        }
        if (channelManager.getNumberOfMembersInChannel(channelId) == 0L) {
            channelManager.removeChannel(channelId)
        }
    }

    override fun isUserInChannel(token: String, channel: String, username: String): Boolean? {
        val (initiatorUserId, channelId) = preValidations(token, channel)
        val isUserAdmin = userManager.getUserPrivilege(initiatorUserId) == IUserManager.PrivilegeLevel.ADMIN
        if (!isUserAdmin && !isUserMember(initiatorUserId, channelId)) throw UserNotAuthorizedException()
        val userId = userManager.getUserId(username) ?: return null
        return isUserMember(userId, channelId)
    }

    override fun numberOfActiveUsersInChannel(token: String, channel: String): Long {
        val (initiatorUserId, channelId) = preValidations(token, channel)
        val isUserAdmin = userManager.getUserPrivilege(initiatorUserId) == IUserManager.PrivilegeLevel.ADMIN
        if (!isUserAdmin && !isUserMember(initiatorUserId, channelId)) throw UserNotAuthorizedException()
        return channelManager.getNumberOfActiveMembersInChannel(channelId)
    }

    override fun numberOfTotalUsersInChannel(token: String, channel: String): Long {
        val (initiatorUserId, channelId) = preValidations(token, channel)
        val isUserAdmin = userManager.getUserPrivilege(initiatorUserId) == IUserManager.PrivilegeLevel.ADMIN
        if (!isUserAdmin && !isUserMember(initiatorUserId, channelId)) throw UserNotAuthorizedException()
        return channelManager.getNumberOfMembersInChannel(channelId)
    }

    /** TODO: REMOVE/REPLACE/UNUSE THIS CLASS IN BEFORE SUBMISSION **/
    class ImpossibleSituation(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

    /** PRIVATES **/
    private fun updateUserStatusInChannels(userId: Long, newStatus: IUserManager.LoginStatus): CompletableFuture<Unit> {
        val channelsList = userManager.getChannelListOfUser(userId)
        for (channelId in channelsList) {
            if (newStatus == IUserManager.LoginStatus.IN)
                channelManager.increaseNumberOfActiveMembersInChannelBy(channelId)
            else {
                channelManager.decreaseNumberOfActiveMembersInChannelBy(channelId)
                channelManager.removeOperatorFromChannel(channelId, userId)
            }
        }
    }

    private fun preValidations(token: String, channel: String): Pair<Long, Long> {
        if (!tokenManager.isTokenValid(token)) throw InvalidTokenException()
        if (!channelManager.isChannelNameExists(channel)) throw NoSuchEntityException()
        val initiatorUserId = tokenManager.getUserIdByToken(token)
                ?: throw ImpossibleSituation("getUserIdByToken returned null but token is valid")
        val channelId = channelManager.getChannelIdByName(channel)
        return Pair(initiatorUserId, channelId)
    }

    private fun String.hashString(hashAlgorithm: String): String {
        val positiveNumberSign = 1
        val numberBase = 16
        val hashFunc = MessageDigest.getInstance(hashAlgorithm)
        return BigInteger(positiveNumberSign, hashFunc.digest(this.toByteArray())).toString(numberBase).padStart(32, '0')
    }

    private fun isUserOperator(userId: Long, channelId: Long): Boolean {
        return channelManager.getChannelOperatorsList(channelId).contains(userId)
    }

    private fun isUserMember(userId: Long, channelId: Long): Boolean {
        return channelManager.getChannelMembersList(channelId).contains(userId)
    }
}