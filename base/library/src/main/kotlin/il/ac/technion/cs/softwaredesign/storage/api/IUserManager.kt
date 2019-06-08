package il.ac.technion.cs.softwaredesign.storage.api

import java.lang.IllegalArgumentException
import java.util.concurrent.CompletableFuture


interface IUserManager {
    enum class LoginStatus{
        OUT,
        IN
    }
    enum class PrivilegeLevel{
        USER,
        ADMIN
    }

    /**
     * Add a new user to the system.
     * @param username String the user name of the user
     * @param password String the password of the user
     * @param status IStorageLayer.LoginStatus is logged in as default
     * @throws IllegalArgumentException if username is already exist
     * @return user id that was added to the system
     */
    fun addUser(username: String, password: String, status: LoginStatus = LoginStatus.IN, privilege: PrivilegeLevel = PrivilegeLevel.USER):CompletableFuture<Long>


    /** GETTERS & SETTERS **/
    /**
     * gets the user id from the system
     * @param username user name
     * @return user id or null if user does not exist in the system
     */
    fun getUserId(username:String):CompletableFuture<Long?>

    /**
     * gets the user name
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @return the user name
     */
    fun getUsernameById(userId:Long): CompletableFuture<String>

    /**
     * gets the user privilege
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @return the user privilege
     */
    fun getUserPrivilege(userId:Long): CompletableFuture<PrivilegeLevel>

    /**
     * gets the user status
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @return login status of user
     */
    fun getUserStatus(userId:Long): CompletableFuture<LoginStatus>

    /**
     * Get the password of the given user id
     * @param userId String
     * @throws IllegalArgumentException if user id does not exist in the system
     * @return String - the password if username exists
     */
    fun getUserPassword(userId: Long) : CompletableFuture<String>

    /**
     * updates a user privilege
     * @param userId String the user name of the user
     * @param privilege privilege of the user
     */
    fun updateUserPrivilege(userId: Long, privilege: PrivilegeLevel): CompletableFuture<Unit>

    /**
     * updates a user privilege
     * @param userId String the user name of the user
     * @param status status of the user
     */
    fun updateUserStatus(userId:Long, status: LoginStatus): CompletableFuture<Unit>


    /**
     * Check if username already exist in the system
     * @param username String
     * @return Boolean - true if exist, false if not
     */
    fun isUsernameExists(username : String) : CompletableFuture<Boolean>

    /**
     * Check if user id already exist in the system
     * @param userId String
     * @return Boolean - true if exist, false if not
     */
    fun isUserIdExists(userId : Long) : CompletableFuture<Boolean>


    /** CHANNELS OF USER **/
    /**
     * gets the channel list of a specific user
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @return ids of the channels
     */
    fun getChannelListOfUser(userId: Long): CompletableFuture<List<Long>>

    /**
     * gets the channel list size of the user
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @return size of the channel list
     */
    fun getUserChannelListSize(userId: Long): CompletableFuture<Long>

    /**
     * addChannel a channel to a specific user
     * Important: this function assumes that channel id is a valid channel
     * @param userId user id
     * @param channelId channel id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @throws IllegalAccessException throws if channelId already exists in users list
     */
    fun addChannelToUser(userId:Long, channelId:Long): CompletableFuture<Unit>

    /**
     * removes a channel to a specific user
     * Important: this function assumes that channel id is a valid channel
     * @param userId user id
     * @param channelId channel id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     * @throws IllegalAccessException throws if channelId does not exist in users list
     */
    fun removeChannelFromUser(userId: Long,channelId: Long): CompletableFuture<Unit>

    /**
     * add *pending* msg to user
     * @param userId user id
     * @param msgId message id
     */
    fun addMessageToUser(userId: Long, msgId: Long): CompletableFuture<Unit>

    /**
     * get all user pending messages and clear the collection
     * @param userId user id
     * @throws IllegalArgumentException throws if user id does not exist in the system
     */
    fun readAllUsersMessages(userId: Long): CompletableFuture<List<Long>>


    /** USER STATISTICS **/
    /**
     * get number of total users in the system
     * @return Long
     */
    fun getTotalUsers() : CompletableFuture<Long>

    /**
     * get number of total logged in users in the system
     * @return Long
     */
    fun getLoggedInUsers() : CompletableFuture<Long>


    /** USER COMPLEX STATISTICS **/
    /**
     * Get a list contains 10 best users by channels count (or less than 10 if nr of total users < 10)
     * @return List<String> of usernames
     */
    fun getTop10UsersByChannelsCount() : CompletableFuture<List<String>>
}