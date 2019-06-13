package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.ALGORITHEMS.HASH_ALGORITHM
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageImpl
import il.ac.technion.cs.softwaredesign.storage.api.IChannelManager
import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import il.ac.technion.cs.softwaredesign.storage.api.ITokenManager
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager
import io.github.vjames19.futures.jdk8.Future
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.github.vjames19.futures.jdk8.recover
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import javax.inject.Inject


class CourseAppImpl
@Inject constructor(private val tokenManager: ITokenManager,
                    private val userManager: IUserManager,
                    private val channelManager: IChannelManager,
                    private val messageManager: IMessageManager
) : CourseApp {

    internal companion object {
        val regex: Regex = Regex("#[#_A-Za-z0-9]*")
    }

    private val userListeners = mutableMapOf<Long, UserListener>()

    override fun login(username: String, password: String): CompletableFuture<String> {
        val hashedPassword = password.hashString(HASH_ALGORITHM)
        return userManager.getUserId(username).thenCompose { userId ->
            if (userId == null)
                registerFuture(username, hashedPassword)
            else
                loginFuture(userId, hashedPassword)
        }
    }

    override fun logout(token: String): CompletableFuture<Unit> {
        return tokenManager.getUserIdByToken(token).thenApply { it ?: throw InvalidTokenException() }
                .thenCompose { userId -> tokenManager.invalidateUserToken(token).thenApply { userId } }
                .recover {
                    if (it is IllegalArgumentException)
                        throw InvalidTokenException()
                    else throw it
                }
                .thenCompose { userId -> updateToLogoutFuture(userId) }.thenApply { Unit }
    }

    override fun isUserLoggedIn(token: String, username: String): CompletableFuture<Boolean?> {
        return validateTokenFuture(token)
                .thenCompose { userManager.getUserId(username) }
                .thenCompose<Boolean?> {
                    if (it != null)
                        userManager.getUserStatus(it).thenApply { status ->
                            status == IUserManager.LoginStatus.IN
                        }
                    else
                        ImmediateFuture { null }
                }
    }

    override fun makeAdministrator(token: String, username: String): CompletableFuture<Unit> {
        return validateTokenFuture(token).thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! }
                .thenCompose { adminId -> validateUserIsAdminFuture(adminId) }
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
    }

    override fun channelPart(token: String, channel: String): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { validateChannelNameExistsFuture(channel) }
                .thenCompose { tokenManager.getUserIdByToken(token) }
                .thenApply { it!! } //ImpossibleSituation("getUserIdByToken returned null but token is valid")
                .thenCompose { userId -> channelManager.getChannelIdByName(channel).thenApply { Pair(userId, it) } }
                .thenCompose { (userId, channelId) -> validateUserMemberExistsFuture(userId, channelId) }
                .thenCompose { (userId, channelId) ->
                    removeUserFromChannelFuture(userId, channelId)
                }.thenCompose { (userId, channelId) ->
                    removeChannelFromUserFuture(userId, channelId)
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
                                        .thenApply { userId }
                            }
                            .thenCompose { userId -> validateUserInChannel(userId, channelId) }

                }.thenCompose { (userId, channelId) -> channelManager.addOperatorToChannel(channelId, userId) }

    }

    override fun channelKick(token: String, channel: String, username: String): CompletableFuture<Unit> {
        return preValidations(token, channel).thenCompose { (initiatorUserId, channelId) -> validateUserIsOperator(initiatorUserId, channelId) }
                .thenCompose { channelId -> userManager.getUserId(username).thenApply { Pair(it, channelId) } }
                .thenCompose { (userId, channelId) -> validateUserInChannel(userId, channelId) }
                .thenCompose { (userId, channelId) -> removeUserFromChannelFuture(userId, channelId) }
                .thenCompose { (userId, channelId) -> removeChannelFromUserFuture(userId, channelId) }
                .thenCompose { (userId, channelId) ->
                    decreaseNumberOfActiveMembersInChannelForLoggedInUserFuture(userId, channelId).thenApply { channelId }
                }
                .thenCompose { channelId -> removeChannelWhenEmptyFuture(channelId) }
    }

    override fun isUserInChannel(token: String, channel: String, username: String): CompletableFuture<Boolean?> {
        return validateUserPrivilegeOnChannelFuture(token, channel)
                .thenCompose { channelId -> userManager.getUserId(username).thenApply { Pair(it, channelId) } }
                .thenCompose<Boolean?> { (userId, channelId) ->
                    if (userId == null)
                        ImmediateFuture { null }
                    else
                        isUserMember(userId, channelId)
                }
    }

    override fun numberOfActiveUsersInChannel(token: String, channel: String): CompletableFuture<Long> {
        return validateUserPrivilegeOnChannelFuture(token, channel)
                .thenCompose { channelId -> channelManager.getNumberOfActiveMembersInChannel(channelId) }
    }

    override fun numberOfTotalUsersInChannel(token: String, channel: String): CompletableFuture<Long> {
        return validateUserPrivilegeOnChannelFuture(token, channel).thenCompose { channelId -> channelManager.getNumberOfMembersInChannel(channelId) }
    }


    override fun addListener(token: String, callback: ListenerCallback): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { tokenManager.getUserIdByToken(token).thenApply { it!! } }
                .thenCompose { userId ->
                    if (!isUserListening(userId)) {
                        listen(userId, callback)
                        val callbacks = userListeners[userId]!!
                        readAllUserMessages(userId, callbacks)
                    } else {
                        ImmediateFuture { listen(userId, callback) }
                    }
                }

    }

    private fun readAllUserMessages(userId: Long, callbacks: UserListener): CompletableFuture<Unit> {
        val broadcastIds = messageManager.getAllBroadcastMessageIds().sorted()
        return readAllBroadcastMessagesFuture(broadcastIds, userId, callbacks)
                .thenCompose { readAllChannelAndPrivateMessagesFuture(callbacks, userId) }
    }

    private fun readAllBroadcastMessagesFuture(broadcastIds: List<Long>, userId: Long, callbacks: UserListener): CompletableFuture<Unit> {
        if (broadcastIds.isEmpty()) return ImmediateFuture { Unit }
        return broadcastIds
                .map { buildMessage(it) }
                .map { srcMsgFuture ->
                    srcMsgFuture.thenCompose { (_, message) ->
                        readBroadcastMessageByListener(userId, message, callbacks)
                    }
                }.reduce { acc, completableFuture ->
                    acc.thenCompose { completableFuture }
                }
    }

    private fun readAllChannelAndPrivateMessagesFuture(callbacks: UserListener, userId: Long): CompletableFuture<Unit> {
        val messagesIds = userManager.getAllChannelAndPrivateUserMessages(userId).get()
        return if (messagesIds.isEmpty()) ImmediateFuture { Unit }
        else {
            messagesIds
                    .map { buildMessage(it) }
                    .map { sourceMessageFuture ->
                        sourceMessageFuture.thenCompose { (source, message) ->
                            callbacks.notifyOnMessageArrive(userId, source, message)
                        }
                    }.reduce { acc,
                               completableFuture ->
                        acc.thenCompose { completableFuture }
                    }
        }


    }


    override fun removeListener(token: String, callback: ListenerCallback): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { tokenManager.getUserIdByToken(token).thenApply { it!! } }
                .thenApply { userId -> if (!isCallbackExists(userId, callback)) throw NoSuchEntityException() else userId }
                .thenApply { userId -> unlisten(userId, callback) }
    }

    override fun channelSend(token: String, channel: String, message: Message): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { validateChannelNameExistsFuture(channel) }
                .thenCompose { tokenManager.getUserIdByToken(token).thenApply { it!! } }
                .thenCompose { userId -> channelManager.getChannelIdByName(channel).thenApply { Pair(userId, it!!) } }
                .thenCompose { (userId, channelId) -> validateMembershipInChannelFuture(userId, channelId).thenApply { Pair(userId, channelId) } }
                .thenCompose { (userId, channelId) -> userManager.getUsernameById(userId).thenApply { Triple(userId, channelId, it) } }
                .thenCompose { (_, channelId, userName) ->
                    val source = "$channel@$userName"
                    messageManager.addMessage(message.id, message.media.ordinal.toLong()
                            , message.contents, message.created, IMessageManager.MessageType.CHANNEL, source
                            , channelId = channelId).thenApply { Pair(channelId, source) }
                }
                .thenCompose { (channelId, source) -> channelManager.addMessageToChannel(channelId, message.id).thenApply { Pair(channelId, source) } }
                .thenCompose { (channelId, source) -> channelManager.getChannelMembersList(channelId).thenApply { Pair(it, source) } }
                .thenCompose { (list, source) ->
                    if (list.isNotEmpty())
                        list.map { memberId -> sendPrivateOrChannelMessageToUser(memberId, source, message) }
                                .reduce { acc, completableFuture -> acc.thenCompose { completableFuture } }
                    else
                        ImmediateFuture { Unit }
                }
    }

    override fun broadcast(token: String, message: Message): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { tokenManager.getUserIdByToken(token).thenApply { it!! } }
                .thenCompose { userId -> validateUserIsAdminFuture(userId) }
                .thenCompose { userManager.getTotalUsers()
                        .thenApply { it }
                }
                .thenCompose { numOfUsers ->
                    messageManager.addMessage(message.id, message.media.ordinal.toLong()
                            , message.contents, message.created, IMessageManager.MessageType.BROADCAST
                            , source = "BROADCAST", startCounter = numOfUsers)
                }
                .thenCompose { sendBroadcastMessageToListeners(message) }

    }

    private fun sendBroadcastMessageToListeners(message: Message): CompletableFuture<Unit> {
        return if (userListeners.isNotEmpty())
            userListeners.map { (userId, listener) ->
                readBroadcastMessageByListener(userId, message, listener)
            }.reduce { acc, completableFuture -> acc.thenCompose { completableFuture } }
        else ImmediateFuture { Unit }
    }

    private fun readBroadcastMessageByListener(userId: Long, message: Message, listener: UserListener): CompletableFuture<Unit> {
        return isMessageNewToUser(userId, message).thenCompose { messageNewToUser ->
            if (messageNewToUser) { //user have not read the message
                listener.notifyOnMessageArrive(userId, "BROADCAST", message)
                        .thenCompose { messageManager.decreaseMessageCounterBy(message.id) }
            } else {
                ImmediateFuture { Unit }
            }
        }

    }


    private fun isMessageNewToUser(userId: Long, message: Message): CompletableFuture<Boolean> {
        return if (isUserBeenCreatedBeforeMessage(userId, message))
            userManager.getUserLastReadMsgId(userId).thenApply { lastMsgId ->
                lastMsgId < message.id
            }
        else ImmediateFuture { false }
    }

    private fun isUserBeenCreatedBeforeMessage(userId: Long, message: Message) = userId < message.id

    override fun privateSend(token: String, user: String, message: Message): CompletableFuture<Unit> {
        return validateTokenFuture(token)
                .thenCompose { userManager.getUserId(user) }
                .thenApply { it ?: throw NoSuchEntityException() }
                .thenCompose { destUserId ->
                    tokenManager.getUserIdByToken(token).thenApply { Pair(it!!, destUserId) }
                }.thenCompose { (srcId, destId) ->
                    userManager.getUsernameById(srcId).thenApply { Pair("@$it", destId) }
                }.thenCompose { (srcName, destId) ->
                    messageManager.addMessage(message.id, message.media.ordinal.toLong()
                            , message.contents, message.created, IMessageManager.MessageType.PRIVATE
                            , source = srcName, destUserId = destId).thenApply { Pair(srcName, destId) }
                }.thenCompose { (srcName, destUserId) ->
                    sendPrivateOrChannelMessageToUser(destUserId, srcName, message)
                }
    }

    private fun sendPrivateOrChannelMessageToUser(destUserId: Long, source: String, message: Message): CompletableFuture<Unit> {
        return if (isUserListening(destUserId)) {
            userListeners[destUserId]!!.notifyOnMessageArrive(destUserId, source, message)
        } else {
            userManager.addMessageToUser(destUserId, message.id)
        }
    }


    override fun fetchMessage(token: String, id: Long): CompletableFuture<Pair<String, Message>> {
        return validateTokenFuture(token)
                .thenCompose { messageManager.isMessageIdExists(id) }
                .thenCompose {
                    if (!it) throw NoSuchEntityException()
                    else {
                        messageManager.getMessageType(id)
                    }
                }.thenCompose {
                    if (it != IMessageManager.MessageType.CHANNEL) throw NoSuchEntityException()
                    else {
                        messageManager.getMessageChannelId(id)
                    }
                }.thenCompose { channelId ->
                    tokenManager.getUserIdByToken(token).thenApply { userId -> Pair<Long, Long>(userId!!, channelId) }
                }.thenCompose { (userId, channelId) ->
                    isUserMember(userId, channelId)
                }.thenCompose {
                    if (!it) throw UserNotAuthorizedException()
                    else {
                        buildMessage(id)
                    }
                }
    }


    /** PRIVATES **/

    private fun isUserListening(userId: Long) = userListeners[userId] != null

    private fun listen(userId: Long, callback: ListenerCallback) {
        val userListener = userListeners[userId]
        if (userListener == null) {
            userListeners[userId] = UserListener(userManager, messageManager).listen(callback)
        } else {
            userListener.listen(callback)
        }
    }

    private fun isCallbackExists(userId: Long, callback: ListenerCallback): Boolean {
        val userListener = userListeners[userId] ?: return false
        return userListener.callbackExist(callback)
    }

    private fun unlisten(userId: Long, callback: ListenerCallback) {
        val userListener = userListeners[userId]
        if (userListener == null) {
            userListeners[userId] = UserListener(userManager, messageManager).unlisten(callback)
        } else {
            userListener.unlisten(callback)
        }
    }

    private fun buildMessage(msgId: Long): CompletableFuture<Pair<String, Message>> {
        return messageManager.getMessageMediaType(msgId)
                .thenApply {
                    val message = MessageImpl()
                    message.id = msgId
                    message.media = MediaType.values()[it.toInt()]
                    message
                }.thenCompose { message ->
                    messageManager.getMessageContent(msgId).thenApply { content -> message.contents = content; message }
                }.thenCompose { message ->
                    messageManager.getMessageCreatedTime(msgId).thenApply { created -> message.created = created; message }
                }.thenCompose { message ->
                    messageManager.getMessageReceivedTime(msgId).thenApply { received -> message.received = received; message }
                }.thenCompose { message ->
                    messageManager.getMessageSource(msgId).thenApply { source -> Pair<String, Message>(source, message) }
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

    private fun updateToLogoutFuture(userId: Long): CompletableFuture<List<Unit>> {
        val updateUserStatusFuture = userManager.updateUserStatus(userId, IUserManager.LoginStatus.OUT)
        val updateChannelStatusFuture = updateUserStatusInChannels(userId, IUserManager.LoginStatus.OUT)
        return Future.allAsList(listOf(updateUserStatusFuture, updateChannelStatusFuture))
    }

    private fun validateTokenFuture(token: String): CompletableFuture<Unit> {
        return tokenManager.isTokenValid(token)
                .thenApply {
                    if (!it)
                        throw InvalidTokenException()
                }
    }


    private fun validateUserInChannel(userId: Long?, channelId: Long): CompletableFuture<Pair<Long, Long>> {
        return if (userId == null)
            throw NoSuchEntityException()
        else
            isUserMember(userId, channelId).thenApply { if (!it) throw NoSuchEntityException() }
                    .thenApply { Pair(userId, channelId) }
    }

    private fun validateUserIsOperatorOrAdmin(initiatorUserId: Long, channelId: Long,
                                              operatorPrivilege: IUserManager.PrivilegeLevel)
            : CompletableFuture<Triple<Long, Long, IUserManager.PrivilegeLevel>> {
        return isUserOperator(initiatorUserId, channelId).thenApply {
            if (!it && operatorPrivilege != IUserManager.PrivilegeLevel.ADMIN)
                throw UserNotAuthorizedException()
        }.thenApply { Triple(initiatorUserId, channelId, operatorPrivilege) }
    }

    private fun validateUserIsOperatorOrChannelAdmin(initiatorUserId: Long, channelId: Long,
                                                     operatorPrivilege: IUserManager.PrivilegeLevel?,
                                                     userId: Long?): CompletableFuture<Unit> {
        return isUserOperator(initiatorUserId, channelId)
                .thenApply {
                    if (!it && operatorPrivilege == IUserManager.PrivilegeLevel.ADMIN && (userId == null || userId != initiatorUserId))
                        throw UserNotAuthorizedException()
                }
    }

    private fun validateMembershipInChannelFuture(initiatorUserId: Long, channelId: Long) =
            isUserMember(initiatorUserId, channelId).thenApply { if (!it) throw UserNotAuthorizedException() }
                    .thenApply { Pair(initiatorUserId, channelId) }

    private fun validateUserPrivilegeOnChannelFuture(token: String, channel: String): CompletableFuture<Long> {
        return preValidations(token, channel)
                .thenCompose { (initiatorUserId, channelId) ->
                    userManager.getUserPrivilege(initiatorUserId)
                            .thenApply { Triple(initiatorUserId, channelId, it) }
                }
                .thenCompose { (initiatorUserId, channelId, userPrivilege) ->
                    validateUserIsAdminOrMemberOfChannel(userPrivilege, initiatorUserId, channelId)
                }
    }

    private fun validateUserIsAdminOrMemberOfChannel(privilege: IUserManager.PrivilegeLevel,
                                                     initiatorUserId: Long, channelId: Long): CompletableFuture<Long> {
        return if (privilege != IUserManager.PrivilegeLevel.ADMIN)
            isUserMember(initiatorUserId, channelId)
                    .thenApply { isUserMember -> if (!isUserMember) throw UserNotAuthorizedException() }
                    .thenApply { channelId }
        else
            ImmediateFuture { channelId }
    }

    private fun removeChannelFromUserFuture(userId: Long, channelId: Long) =
            userManager.removeChannelFromUser(userId, channelId).thenApply { Pair(userId, channelId) }

    private fun validateUserIsOperator(initiatorUserId: Long, channelId: Long) =
            isUserOperator(initiatorUserId, channelId)
                    .thenApply { if (!it) throw UserNotAuthorizedException() }
                    .thenApply { channelId }

    private fun validateChannelNameExistsFuture(channel: String) =
            channelManager.isChannelNameExists(channel).thenApply { if (!it) throw NoSuchEntityException() }

    private fun removeUserFromChannelFuture(userId: Long, channelId: Long): CompletableFuture<Pair<Long, Long>> {
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
                    if (it) {
                        channelManager.increaseNumberOfActiveMembersInChannelBy(channelId)
                    } else ImmediateFuture { Unit }
                }
                .exceptionally { /* if user try to join again, its ok */ }
    }

    private fun addMemberToChannelFuture(channelId: Long, userId: Long): CompletableFuture<Unit> {
        return channelManager.addMemberToChannel(channelId, userId)
                .thenCompose { increaseNumberOfActiveMembersInChannelForLoggedInUser(userId, channelId) }
                .exceptionally { /* if user try to join again, its ok */ }
    }

    private fun addChannelToUserFuture(userId: Long, channelId: Long): CompletableFuture<Pair<Long, Long>> {
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
        return userManager.getChannelListOfUser(userId).thenCompose { list ->
            if (newStatus == IUserManager.LoginStatus.IN && list.isNotEmpty()) {
                list.map { channelId -> channelManager.increaseNumberOfActiveMembersInChannelBy(channelId) }
                        .reduce { acc, completableFuture ->
                            acc.thenCompose { completableFuture }
                        }
            } else if (newStatus == IUserManager.LoginStatus.OUT && list.isNotEmpty()) {
                list.map { channelId -> channelManager.decreaseNumberOfActiveMembersInChannelBy(channelId) }
                        .reduce { acc, completableFuture ->
                            acc.thenCompose { completableFuture }
                        }
            } else {
                ImmediateFuture { Unit }
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
        return BigInteger(positiveNumberSign, hashFunc.digest(this.toByteArray())).toString(numberBase)
                .padStart(32, '0')
    }

    private fun isUserOperator(userId: Long, channelId: Long): CompletableFuture<Boolean> {
        return channelManager.getChannelOperatorsList(channelId).thenApply { it.contains(userId) }
    }

    private fun isUserMember(userId: Long, channelId: Long): CompletableFuture<Boolean> {
        return channelManager.getChannelMembersList(channelId).thenApply { it.contains(userId) }
    }
}