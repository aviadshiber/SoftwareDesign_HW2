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
import io.github.vjames19.futures.jdk8.Future
import io.github.vjames19.futures.jdk8.map
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
        //probably can be optimized even more with combine instead of compose but this is okay for now
        return getUserId(username)
               .thenCompose { userId->generateNextUserId(userId) }
               .thenCompose { userId->propertiesSettersFuture(userId,username,password,status,privilege) }
               .thenCompose { userId->initChannelListFuture(userId) }
               .thenCompose { userId->addNewUserToUserTreeFuture(userId = userId) }
               .thenCompose { userId->increaseUserLoginFuture(userId,status) }
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
        return userStorage.getPropertyStringByUserId(userId, MANAGERS_CONSTS.PRIVILEGE_PROPERTY)
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
        return userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.PRIVILEGE_PROPERTY, privilege.ordinal.toString())
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
            it
        }.thenCompose { updateListFuture(userId, it) }
                .thenCompose {
            // update tree:
            val currentSize = it.size.toLong()
            updateUserNodeFuture(userId, oldCount = currentSize - 1L, newCount = currentSize)
        }
    }

    private fun updateListFuture(userId: Long, list: ArrayList<Long>): CompletableFuture<ArrayList<Long>> {
        val listPropertySetterFuture = userStorage.setPropertyListToUserId(userId, LIST_PROPERTY, list)
        val listSizeSetterFuture = userStorage.setPropertyLongToUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY, list.size.toLong())

        return Future.allAsList(listOf(listPropertySetterFuture, listSizeSetterFuture)).map { list}
    }

    override fun removeChannelFromUser(userId: Long, channelId: Long): CompletableFuture<Unit> {
        return getChannelListOfUser(userId)
                .thenApply { ArrayList<Long>(it) }
                .thenApply {
                    if (!it.contains(channelId))
                        throw IllegalAccessException("channel id does not exists in users list")
                    it.remove(channelId)
                    it
                }.thenCompose { updateListFuture(userId, it) }.thenCompose {
                    // update tree:
                    val currentSize = it.size.toLong()
                    updateUserNodeFuture(userId, oldCount = currentSize + 1L, newCount = currentSize)
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
    override fun getTop10UsersByChannelsCount(): CompletableFuture<List<String>> {

        return  getTotalUsers()
                .thenCompose { buildTop10UsersByChannelCountList(it) }

    }

    private fun buildTop10UsersByChannelCountList(nrOutputUsers: Long ) : CompletableFuture<List<String>> {

        val higherUserIndex=nrOutputUsers-1
        val lowestUserIndex= nrOutputUsers-min(10, nrOutputUsers)
        val topUsers=buildTopUsersFromHigherToLower(higherUserIndex,lowestUserIndex)
        return topUsers.thenApply { it }
    }

    private fun buildTopUsersFromHigherToLower(higherUserIndex:Long,lowerUserIndex:Long) :CompletableFuture<MutableList<String>>{
        return if(higherUserIndex<lowerUserIndex){
            Future{ mutableListOf<String>()}
        }else{
            //TODO: fix after tree refactoring (remove Future init)
            val selectedIdFuture= Future{usersByChannelsCountTree.select(higherUserIndex).getId()}
            val userNameFuture= selectedIdFuture.thenCompose { getUsernameById(it) }
            buildTopUsersFromHigherToLower(higherUserIndex-1,lowerUserIndex)
                    .thenCompose { list-> userNameFuture.thenApply { name-> list.add(name); list } }
        }

    }

    /** PRIVATES **/
    private fun initChannelListFuture(userId: Long): CompletableFuture<Long> {
        val listPropertySetterFuture=userStorage.setPropertyListToUserId(userId, LIST_PROPERTY, emptyList())
        val sizePropertySetterFuture=userStorage.setPropertyLongToUserId(userId, MANAGERS_CONSTS.SIZE_PROPERTY, 0L)
        return Future.allAsList(listOf(listPropertySetterFuture,sizePropertySetterFuture)).map { userId }
    }

    private fun addNewUserToUserTreeFuture(userId: Long) :CompletableFuture<Long> {
        return Future { //TODO: remove when done refactoring of the tree & apply with userId
            val key = CountIdKey(count = 0L, id = userId)
            usersByChannelsCountTree.put(key)
            userId
        }
    }

    private fun updateUserNodeFuture(userId: Long, oldCount: Long, newCount: Long): CompletableFuture<Unit> {
        //TODO: remove Future block after tree refactoring
        return Future {
            val oldKey = CountIdKey(count = oldCount, id = userId)
            usersByChannelsCountTree.delete(oldKey)
        }.thenCompose {
            Future {
                val newKey = CountIdKey(count = newCount, id = userId)
                usersByChannelsCountTree.put(newKey)
            }
        }

    }

    private fun increaseUserLoginFuture(userId: Long, status: LoginStatus): CompletableFuture<Long> {
        // increase logged in users only, cause number of users was increased by id generator
        return if (status == LoginStatus.IN) statisticsManager.increaseLoggedInUsersBy().thenApply { userId }
        else Future{ userId}
    }


    private fun generateNextUserId(currentUserId: Long?): CompletableFuture<Long> {
        if (currentUserId == INVALID_USER_ID) throw IllegalArgumentException("user id is not valid")
        if (currentUserId != null) throw IllegalArgumentException("user already exist")
        return userIdGenerator.next()
    }

    private fun propertiesSettersFuture(userId: Long, username: String, password: String, status:LoginStatus, privilege: PrivilegeLevel): CompletableFuture<Long> {
        val usernamePropertySetterFuture=userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.USERNAME_PROPERTY, username)
        val passwordPropertySetterFuture=userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.PASSWORD_PROPERTY, password)
        val statusPropertySetterFuture=userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.STATUS_PROPERTY, status.ordinal.toString())
        val privilegePropertySetterFuture= userStorage.setPropertyStringToUserId(userId, MANAGERS_CONSTS.PRIVILEGE_PROPERTY, privilege.ordinal.toString())
        val ls= listOf(usernamePropertySetterFuture,passwordPropertySetterFuture,statusPropertySetterFuture,privilegePropertySetterFuture)
        return Future.allAsList(ls).map { userId }
    }
}