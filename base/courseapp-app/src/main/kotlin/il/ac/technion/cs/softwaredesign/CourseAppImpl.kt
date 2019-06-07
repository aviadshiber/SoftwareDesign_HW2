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
                .thenApply { it!! } ////ImpossibleSituation("getUserIdByToken returned null but token is valid")
                .thenCompose { userId -> isChannelNameExistsFuture(channel, userId) }
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
                    increaseNumberOfActiveMembersInChannelForLoggedInUser(userId, channelId)
                }
    }


    override fun channelPart(token: String, channel: String): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { validateChannelNameExistsFuture(channel) }
                .thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! } //ImpossibleSituation("getUserIdByToken returned null but token is valid")
                .thenCompose { userId -> channelManager.getChannelIdByName(channel).thenApply { Pair(userId, it) } }
                .thenCompose { (userId, channelId) -> validateUserMemberExistsFuture(userId, channelId) }
                .thenCompose { (userId, channelId) ->
                    removeUserFromChannelFuture(channelId, userId)
                }.thenCompose { (userId, channelId) ->
                    decreaseNumberOfActiveMembersInChannelForLoggedInUserFuture(userId, channelId)
                            .thenCompose { removeChannelWhenEmptyFuture(channelId) }
                }
    }


    override fun channelMakeOperator(token: String, channel: String, username: String): CompletableFuture<Unit> {
        return preValidations(token, channel)
                .thenCompose { (initiatorUserId, channelId) ->
                    validateMembershipInChannelFuture(initiatorUserId, channelId)
                }
                .thenCompose { (initiatorUserId, channelId) ->
                    userManager.getUserPrivilege(initiatorUserId)
                            .thenApply { Triple(initiatorUserId, channelId, it) }
                }
                .thenCompose { (initiatorUserId, channelId, operatorPrivilege) ->
                    validateUserIsOperatorOrAdmin(initiatorUserId, channelId, operatorPrivilege)
                }.thenCompose { (initiatorUserId, channelId, operatorPrivilege) ->
                    userManager.getUserId(username)
                            .thenCompose { userId ->
                                validateUserIsOperatorOrChannelAdmin(initiatorUserId, channelId, operatorPrivilege, userId)
                                        .thenApply { userId } }
                            .thenCompose { userId -> validateUserInChannel(userId, channelId) }

                }.thenCompose { (channelId, userId) -> channelManager.addOperatorToChannel(channelId, userId) }

    }

    private fun validateUserInChannel(userId: Long?, channelId: Long): CompletableFuture<Pair<Long, Long>> {
        return if (userId == null)
            throw NoSuchEntityException()
        else
            isUserMember(userId, channelId).thenApply { if (!it) throw NoSuchEntityException() }
                    .thenApply { Pair(channelId, userId) }
    }

    private fun validateUserIsOperatorOrAdmin(initiatorUserId: Long, channelId: Long, operatorPrivilege: IUserManager.PrivilegeLevel): CompletableFuture<Triple<Long, Long, IUserManager.PrivilegeLevel>> {
        return isUserOperator(initiatorUserId, channelId).thenApply {
            if (!it && operatorPrivilege != IUserManager.PrivilegeLevel.ADMIN)
                throw UserNotAuthorizedException()
        }.thenApply { Triple(initiatorUserId, channelId, operatorPrivilege) }
    }

    private fun validateUserIsOperatorOrChannelAdmin(initiatorUserId: Long, channelId: Long, operatorPrivilege: IUserManager.PrivilegeLevel?, userId: Long?): CompletableFuture<Unit> {
        return isUserOperator(initiatorUserId, channelId)
                .thenApply {
                    if (!it && operatorPrivilege == IUserManager.PrivilegeLevel.ADMIN && (userId == null || userId != initiatorUserId))
                        throw UserNotAuthorizedException()
                }
    }


    private fun validateMembershipInChannelFuture(initiatorUserId: Long, channelId: Long) =
            isUserMember(initiatorUserId, channelId).thenApply { if (!it) throw UserNotAuthorizedException() }.thenApply { Pair(initiatorUserId, channelId) }

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
    private fun validateChannelNameExistsFuture(channel: String) =
            channelManager.isChannelNameExists(channel).thenApply { if (!it) throw NoSuchEntityException() }

    private fun removeUserFromChannelFuture(channelId: Long, userId: Long): CompletableFuture<Pair<Long, Long>> {
        val removeMemberFuture = channelManager.removeMemberFromChannel(channelId, userId)
        val removeOperatorFuture = channelManager.removeOperatorFromChannel(channelId, userId)
        return Future.allAsList(listOf(removeMemberFuture, removeOperatorFuture)).thenApply { Pair(userId, channelId) }
    }

    private fun decreaseNumberOfActiveMembersInChannelForLoggedInUserFuture(userId: Long, channelId: Long): CompletableFuture<Unit> {
        return userManager.getUserStatus(userId).thenCompose {
            if (it == IUserManager.LoginStatus.IN)
                channelManager.decreaseNumberOfActiveMembersInChannelBy(channelId)
            else
                ImmediateFuture { Unit }
        }
    }

    private fun removeChannelWhenEmptyFuture(channelId: Long): CompletableFuture<Unit> {
        return channelManager.getNumberOfMembersInChannel(channelId).thenCompose {
            if (it == 0L)
                channelManager.removeChannel(channelId)
            else
                ImmediateFuture { Unit }
        }
    }

    private fun validateUserMemberExistsFuture(userId: Long, channelId: Long) =
            isUserMember(userId, channelId).thenApply { if (!it) throw NoSuchEntityException() else Pair(userId, channelId) }

    private fun isChannelNameExistsFuture(channel: String, userId: Long): CompletableFuture<Pair<Long, Boolean>> =
            channelManager.isChannelNameExists(channel)
                    .thenApply { isChannelNameExists -> Pair(userId, isChannelNameExists) }

    private fun increaseNumberOfActiveMembersInChannelForLoggedInUser(userId: Long, channelId: Long): CompletableFuture<Unit> {
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

    private fun preValidations(token: String, channel: String): CompletableFuture<Pair<Long, Long>> {
        return validateTokenFuture(token)
                .thenCompose { validateChannelNameExistsFuture(channel) }
                .thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! } //ImpossibleSituation("getUserIdByToken returned null but token is valid")
                .thenCompose { initiatorUserId -> getChannelIdByNameFuture(channel, initiatorUserId) }
    }

    private fun getChannelIdByNameFuture(channel: String, initiatorUserId: Long) =
            channelManager.getChannelIdByName(channel).thenApply { Pair(initiatorUserId, it) }

    private fun String.hashString(hashAlgorithm: String): String {
        val positiveNumberSign = 1
        val numberBase = 16
        val hashFunc = MessageDigest.getInstance(hashAlgorithm)
        return BigInteger(positiveNumberSign, hashFunc.digest(this.toByteArray())).toString(numberBase).padStart(32, '0')
    }

    private fun isUserOperator(userId: Long, channelId: Long): CompletableFuture<Boolean> {
        return channelManager.getChannelOperatorsList(channelId).thenApply { it.contains(userId) }
    }

    private fun isUserMember(userId: Long, channelId: Long): CompletableFuture<Boolean> {
        return channelManager.getChannelMembersList(channelId).thenApply { it.contains(userId) }
    }
}