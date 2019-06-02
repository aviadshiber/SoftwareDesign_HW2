package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.storage.api.IUserManager.LoginStatus
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager.PrivilegeLevel
import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.api.IStatisticsManager
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager
import il.ac.technion.cs.softwaredesign.storage.datastructures.CountIdKey
import il.ac.technion.cs.softwaredesign.storage.datastructures.SecureAVLTree
import il.ac.technion.cs.softwaredesign.storage.users.IUserStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.INVALID_USER_ID
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.LIST_PROPERTY
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.PASSWORD_PROPERTY
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class UserManager
@Inject constructor(private val userStorage: IUserStorage,
                    private val statisticsManager: IStatisticsManager,
                    @UserIdSeqGenerator private val userIdGenerator: ISequenceGenerator,
                    @UsersByChannelCountStorage private val usersByChannelsCountStorage: SecureStorage
) : IUserManager {

    private val defaultKey: () -> CountIdKey = { CountIdKey() }
    private val usersByChannelsCountTree = SecureAVLTree(usersByChannelsCountStorage, defaultKey)

    override fun addUser(username: String, password: String, status: LoginStatus, privilege: PrivilegeLevel): CompletableFuture<Long> {
        val userId = getUserId(username)
        return userId.thenCompose { id ->
            if (id == INVALID_USER_ID) throw IllegalArgumentException("user id is not valid")
            if (id != null) throw IllegalArgumentException("user already exist")
            userIdGenerator.next()
        }.thenApply { id ->
            // id db
            userStorage.setUserIdToUsername(username, id!!)

            // details db
            userStorage.setPropertyStringToUserId(id, MANAGERS_CONSTS.USERNAME_PROPERTY, username)
            userStorage.setPropertyStringToUserId(id, PASSWORD_PROPERTY, password)
            userStorage.setPropertyStringToUserId(id, MANAGERS_CONSTS.STATUS_PROPERTY, status.ordinal.toString())
            userStorage.setPropertyStringToUserId(id, MANAGERS_CONSTS.PRIVILAGE_PROPERTY, privilege.ordinal.toString())
            initChannelList(id)

            // tree db
            addNewUserToUserTree(userId = id, count = 0L)

            // increase logged in users only, cause number of users was increased by id generator
            if (status == LoginStatus.IN) statisticsManager.increaseLoggedInUsersBy()

            id
        }
    }


    /** GETTERS & SETTERS **/
    override fun getUserId(username: String): CompletableFuture<Long?> {
        return userStorage.getUserIdByUsername(username)
    }

    override fun getUsernameById(userId: Long): CompletableFuture<String> {

        return userStorage.getPropertyStringByUserId(userId, MANAGERS_CONSTS.USERNAME_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }

    }

    override fun getUserPrivilege(userId: Long): CompletableFuture<PrivilegeLevel> {
        return userStorage.getPropertyStringByUserId(userId, MANAGERS_CONSTS.PRIVILAGE_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }
                .thenApply { PrivilegeLevel.values()[it.toInt()] }

    }

    override fun getUserStatus(userId: Long): CompletableFuture<LoginStatus> {
        return userStorage.getPropertyStringByUserId(userId, MANAGERS_CONSTS.STATUS_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }
                .thenApply { LoginStatus.values()[it.toInt()] }
    }

    override fun getUserPassword(userId: Long): CompletableFuture<String> {
        return userStorage.getPropertyStringByUserId(userId, PASSWORD_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }

    }

    override fun updateUserPrivilege(userId: Long, privilege: PrivilegeLevel): CompletableFuture<Unit> {
        return userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.PRIVILAGE_PROPERTY, privilege.ordinal.toString())
    }

    override fun updateUserStatus(userId: Long, status: LoginStatus) :CompletableFuture<Unit> {

            return getUserStatus(userId).thenApply{ if(it == status) Unit }.thenCompose{
                userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.STATUS_PROPERTY, status.ordinal.toString())
                        .thenCompose {
                            if (status == LoginStatus.IN) {
                                statisticsManager.increaseLoggedInUsersBy()
                            } else {
                                statisticsManager.decreaseLoggedInUsersBy()
                            }
                        }



            }.exceptionally {/* user id does not exist, do nothing */  }

    }


    override fun isUsernameExists(username: String): CompletableFuture<Boolean> {
        return userStorage.getUserIdByUsername(username)
                .thenApply { it != null && it != INVALID_USER_ID }
    }

    override fun isUserIdExists(userId: Long): CompletableFuture<Boolean> {
        return userStorage.getPropertyStringByUserId(userId, PASSWORD_PROPERTY).thenApply { it != null }
    }


    /** CHANNELS OF USER **/
    override fun getChannelListOfUser(userId: Long): CompletableFuture<List<Long>> {
        return userStorage.getPropertyListByUserId(userId, LIST_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }

    }

    override fun getUserChannelListSize(userId: Long): CompletableFuture<Long> {
        return userStorage.getPropertyLongByUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY)
                .thenApply { it ?: throw IllegalArgumentException("user id does not exist") }
    }

    override fun addChannelToUser(userId: Long, channelId: Long): CompletableFuture<Unit> {
        return getChannelListOfUser(userId).thenApply { ArrayList<Long>(it) }.thenApply {
            if (it.contains(channelId)) throw IllegalAccessException("channel id already exists in users list")
            it.add(channelId)
            userStorage.setPropertyListToUserId(userId, LIST_PROPERTY, it)
            val currentSize = it.size.toLong()
            userStorage.setPropertyLongToUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY, currentSize)
            // update tree:
            updateUserNode(userId, oldCount = currentSize - 1L, newCount = currentSize)
        }
    }

    override fun removeChannelFromUser(userId: Long, channelId: Long): CompletableFuture<Unit> {
        return getChannelListOfUser(userId)
                .thenApply { ArrayList<Long>(it) }
                .thenApply {
                    if (!it.contains(channelId))
                        throw IllegalAccessException("channel id does not exists in users list")
                    it.remove(channelId)
                    val currentSize = it.size.toLong()
                    userStorage.setPropertyListToUserId(userId, LIST_PROPERTY, it)
                    userStorage.setPropertyLongToUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY, currentSize)
                    // update tree:
                    updateUserNode(userId, oldCount = currentSize + 1L, newCount = currentSize)
                }
    }


    /** USER STATISTICS **/
    override fun getTotalUsers(): CompletableFuture<Long> {
        return statisticsManager.getTotalUsers()
    }

    override fun getLoggedInUsers(): CompletableFuture<Long> {
        return statisticsManager.getLoggedInUsers()
    }


    /** USER COMPLEX STATISTICS **/
    override fun getTop10UsersByChannelsCount(): CompletableFuture<List<CompletableFuture<String>>> {
        return  getTotalUsers()
                .thenApply { buildTop10UsersByChannelCountList(it) }

    }

    private fun buildTop10UsersByChannelCountList(nrOutputUsers: Long ) : MutableList<CompletableFuture<String>> {
        val values = mutableListOf<CompletableFuture<String>>()
        val higherUserIndex=nrOutputUsers-1
        val lowestUserIndex= nrOutputUsers-min(10, nrOutputUsers)
        (higherUserIndex downTo lowestUserIndex).forEach {
            val userId = usersByChannelsCountTree.select(it).getId()
            val userName = getUsernameById(userId)
            values.add(userName)
        }
        return values
    }

    /** PRIVATES **/
    private fun initChannelList(userId: Long) {
        userStorage.setPropertyListToUserId(userId, LIST_PROPERTY, emptyList())
        userStorage.setPropertyLongToUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY, 0L)

    }

    private fun addNewUserToUserTree(userId: Long, count: Long) {
        val key = CountIdKey(count = count, id = userId)
        usersByChannelsCountTree.put(key)
    }

    private fun updateUserNode(userId: Long, oldCount: Long, newCount: Long) {
        val oldKey = CountIdKey(count = oldCount, id = userId)
        usersByChannelsCountTree.delete(oldKey)
        val newKey = CountIdKey(count = newCount, id = userId)
        usersByChannelsCountTree.put(newKey)
    }
}