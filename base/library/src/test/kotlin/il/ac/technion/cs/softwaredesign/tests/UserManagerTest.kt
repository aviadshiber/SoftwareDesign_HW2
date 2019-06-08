package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import il.ac.technion.cs.softwaredesign.storage.api.IUserManager
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS
import io.github.vjames19.futures.jdk8.ImmediateFuture
import org.junit.jupiter.api.*
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class UserManagerTest {

    private val injector = Guice.createInjector(LibraryTestModule())

    private val userManager = injector.getInstance<IUserManager>()

    private fun initStatistics() {
        val statisticsStorage = injector.getInstance<IStatisticsStorage>()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_USERS, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_LOGGED_IN_USERS, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_CHANNELS, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_CHANNEL_MESSAGES, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_PENDING_MESSAGES, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.MAX_CHANNEL_INDEX, STATISTICS_KEYS.INIT_INDEX_VAL).get()
    }

    @BeforeEach
    private fun init() {
        initStatistics()
    }

    @Test
    fun `gets the user id from the system`() {
        val aviadID = userManager.addUser("aviad", "aviad_password")
                .thenCompose { userManager.addUser("ron", "ron_password"); ImmediateFuture { it } }
                .get()

        assertThat(userManager.getUserId("aviad").get(), equalTo(aviadID))
    }

    @Test
    fun `returns null if user does not exist in the system`() {
        userManager.addUser("aviad", "aviad_password")
                .thenCompose { userManager.addUser("ron", "ron_password") }
                .get()

        Assertions.assertNull(userManager.getUserId("yossi").get())
    }

    @Test
    fun `add user with as logged out`() {
        val aviadID = userManager.addUser("aviad", "aviad_password", IUserManager.LoginStatus.OUT)
                .thenCompose { userManager.addUser("ron", "ron_password"); ImmediateFuture { it } }
                .get()

        assertThat(userManager.getUserStatus(aviadID).get(), equalTo(IUserManager.LoginStatus.OUT))
    }

    @Test
    fun `add user with with admin privilege`() {
        val aviadID = userManager.addUser("aviad", "aviad_password", privilege = IUserManager.PrivilegeLevel.ADMIN)
                .thenCompose { userManager.addUser("ron", "ron_password"); ImmediateFuture { it } }
                .get()

        assertThat(userManager.getUserPrivilege(aviadID).get(), equalTo(IUserManager.PrivilegeLevel.ADMIN))
    }

    @Test
    fun `update privilege from user to admin`() {
        var aviadID: Long = -1L
        assertThat(
                userManager.addUser("aviad", "aviad_password")
                        .thenCompose { aviadID = it; userManager.getUserPrivilege(it) }
                        .get(),
                equalTo(IUserManager.PrivilegeLevel.USER)
        )

        assertThat(
                userManager.addUser("ron", "ron_password")
                        .thenCompose { userManager.updateUserPrivilege(aviadID, IUserManager.PrivilegeLevel.ADMIN) }
                        .thenCompose { userManager.getUserPrivilege(aviadID) }
                        .get(),
                equalTo(IUserManager.PrivilegeLevel.ADMIN)
        )
    }

    @Test
    fun `update user status to logout`() {
        var aviadID: Long = -1L
        assertThat(
                userManager.addUser("aviad", "aviad_password")
                        .thenCompose { aviadID = it; userManager.getUserStatus(aviadID) }
                        .get(),
                equalTo(IUserManager.LoginStatus.IN)
        )

        assertThat(
                userManager.addUser("ron", "ron_password")
                        .thenCompose { userManager.updateUserStatus(aviadID, IUserManager.LoginStatus.OUT) }
                        .thenCompose { userManager.getUserStatus(aviadID) }
                        .get(),
                equalTo(IUserManager.LoginStatus.OUT)
        )
    }

    @Test
    fun `all methods throws IllegalArgumentException if user id does not exist in the system`() {
        userManager.addUser("aviad", "password").get()
        assertThrowsWithTimeout<Long, IllegalArgumentException>({ userManager.addUser("aviad", "password").joinException() })
        assertThrowsWithTimeout<IUserManager.PrivilegeLevel, IllegalArgumentException>({ userManager.getUserPrivilege(1000L).joinException() })
        assertThrowsWithTimeout<IUserManager.LoginStatus, IllegalArgumentException>({ userManager.getUserStatus(1000L).joinException() })
        assertThrowsWithTimeout<String, IllegalArgumentException>({ userManager.getUserPassword(1000L).joinException() })
        assertThrowsWithTimeout<Unit, IllegalArgumentException>({ userManager.removeChannelFromUser(1000L, 0).joinException() })
        assertThrowsWithTimeout<Unit, IllegalArgumentException>({ userManager.addChannelToUser(1000L, 0).joinException() })
        assertThrowsWithTimeout<Unit, IllegalArgumentException>({ userManager.removeChannelFromUser(1000L, 0).joinException() })
        assertThrowsWithTimeout<Unit, IllegalArgumentException>({ userManager.getUserChannelListSize(1000L).joinException() })
    }

    @Test
    fun `is username exist after he was added to the system`() {
        assertThat(
                userManager.addUser("aviad", "aviad_password")
                        .thenCompose { userManager.isUsernameExists("aviad") }
                        .get(),
                isTrue
        )
        assertThat(userManager.isUsernameExists("yossi").get(), isFalse)
    }

    @Test
    fun `is user id exist after he was added to the system`() {
        val aviadID: Long = userManager.addUser("aviad", "aviad_password").get()
        assertThat(userManager.isUserIdExists(aviadID).get(), isTrue)
        assertThat(userManager.isUserIdExists(1000L).get(), isFalse)
    }

    @Test
    fun `getUserPassword after adding new user`() {
        assertThat(
                userManager.addUser("aviad", "aviad_password")
                        .thenCompose { userManager.getUserPassword(it) }
                        .get(),
                equalTo("aviad_password")
        )
    }

    @Test
    fun `adding new channels to user and validating that all channel exists`() {
        val channelList = addUserTo20Channels()
        (1L..20L).forEach { assertThat(channelList, hasElement(it)) }
        assertThat(channelList, !hasElement(30L))
    }

    private fun addUserTo20Channels(): List<Long> {
        return addUserTo20ChannelsAndGetFuture()
                .thenCompose { userManager.getChannelListOfUser(it) }
                .get()
    }

    private fun addUserTo20ChannelsAndGetFuture(): CompletableFuture<Long> {
        return userManager.addUser("aviad", "aviad_password")
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 1L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 2L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 3L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 4L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 5L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 6L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 7L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 8L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 9L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 10L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 11L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 12L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 13L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 14L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 15L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 16L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 17L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 18L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 19L).thenApply { aviadID1 } }
                .thenCompose { aviadID1 -> userManager.addChannelToUser(aviadID1, 20L).thenApply { aviadID1 } }
                .thenApply { it }
    }

    @Test
    fun `adding new channels to user and removing part of them validating that all channel exists`() {
        //var aviadID: Long = -1L
        val channelList = addUserTo20ChannelsAndGetFuture()
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 1L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 5L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 7L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 3L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 9L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 2L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 4L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 8L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.removeChannelFromUser(aviadID, 6L).thenApply { aviadID } }
                .thenCompose { aviadID -> userManager.getChannelListOfUser(aviadID) }
                .get()
        assertThat(channelList.size, equalTo(11))
    }

    @Test
    fun `after inserting users, total users is valid`() {
        assertThat(userManager.getTotalUsers().get(), equalTo(0L))
        assertThat(
                userManager.addUser("ron", "ron_password")
                        .thenCompose { userManager.addUser("aviad", "aviad_password") }
                        .thenCompose { userManager.addUser("gal", "gal_password") }
                        .thenCompose { userManager.getTotalUsers() }
                        .get(),
                equalTo(3L)
        )
    }

    @Test
    fun `after inserting users, loggedInUsers is valid`() {
        assertThat(userManager.getLoggedInUsers().get(), equalTo(0L))
        assertThat(
                userManager.addUser("ron", "ron_password")
                        .thenCompose { userManager.addUser("aviad", "aviad_password") }
                        .thenCompose { userManager.addUser("gal", "gal_password") }
                        .thenCompose { userManager.getLoggedInUsers() }
                        .get(),
                equalTo(3L)
        )
    }

    @Test
    fun `after status update, loggedInUsers is valid`() {
        assertThat(userManager.getLoggedInUsers().get(), equalTo(0L))
        val id1 = userManager.addUser("ron", "ron_password").get()
        val id2 = userManager.addUser("aviad", "aviad_password").get()
        val id3 = userManager.addUser("gal", "gal_password").get()
        assertThat(userManager.getLoggedInUsers().get(), equalTo(3L))

        assertThat(
                userManager.updateUserStatus(id1, IUserManager.LoginStatus.IN)
                        .thenCompose { userManager.updateUserStatus(id1, IUserManager.LoginStatus.OUT) }
                        .thenCompose { userManager.getLoggedInUsers() }
                        .get(),
                equalTo(2L)
        )

        assertThat(
                userManager.updateUserStatus(id1, IUserManager.LoginStatus.IN)
                        .thenCompose { userManager.getLoggedInUsers() }
                        .get(),
                equalTo(3L)
        )

        assertThat(
                userManager.updateUserStatus(id2, IUserManager.LoginStatus.OUT)
                        .thenCompose { userManager.updateUserStatus(id2, IUserManager.LoginStatus.OUT) }
                        .thenCompose { userManager.getLoggedInUsers() }
                        .get(),
                equalTo(2L)
        )

        assertThat(
                userManager.updateUserStatus(id3, IUserManager.LoginStatus.OUT)
                        .thenCompose { userManager.getLoggedInUsers() }
                        .get(),
                equalTo(1L)
        )
    }

    @Test
    fun `after adding channel, list size has changed`() {
        val id1 = userManager.addUser("ron", "ron_password").get()
        assertThat(userManager.getUserChannelListSize(id1).get(), equalTo(0L))

        assertThat(
                userManager.addChannelToUser(id1, 123L)
                        .thenCompose { userManager.addChannelToUser(id1, 128L) }
                        .thenCompose { userManager.addChannelToUser(id1, 129L) }
                        .thenCompose { userManager.getUserChannelListSize(id1) }
                        .get(),
                equalTo(3L)
        )

        assertThat(userManager.getChannelListOfUser(id1).get().size.toLong(), equalTo(userManager.getUserChannelListSize(id1).get()))
    }

    @Test
    fun `after removing channel, list size has changed`() {
        val id1 = userManager.addUser("ron", "ron_password").get()
        assertThat(userManager.getUserChannelListSize(id1).get(), equalTo(0L))

        assertThat(
                userManager.addChannelToUser(id1, 123L)
                        .thenCompose { userManager.addChannelToUser(id1, 128L) }
                        .thenCompose { userManager.addChannelToUser(id1, 129L) }
                        .thenCompose { userManager.removeChannelFromUser(id1, 123L) }
                        .thenCompose { userManager.getUserChannelListSize(id1) }
                        .get(),
                equalTo(2L)
        )
        assertThat(userManager.getChannelListOfUser(id1).get().size.toLong(), equalTo(userManager.getUserChannelListSize(id1).get()))
    }

    @Test
    fun `add the same element twice throws and list size is valid`() {
        val id1 = userManager.addUser("ron", "ron_password").get()
        assertThrows<IllegalAccessException> {
            userManager.addChannelToUser(id1, 123L)
                    .thenCompose { userManager.addChannelToUser(id1, 123L) }
                    .joinException()
        }
        assertThat(userManager.getUserChannelListSize(id1).get(), equalTo(1L))
        assertThat(userManager.getChannelListOfUser(id1).get().size.toLong(), equalTo(userManager.getUserChannelListSize(id1).get()))
    }

    @Test
    fun `remove the same element twice throws and list size is valid`() {
        val id1 = userManager.addUser("ron", "ron_password").get()
        assertThrows<IllegalAccessException> {
            userManager.addChannelToUser(id1, 123L)
                    .thenCompose { userManager.removeChannelFromUser(id1, 123L) }
                    .thenCompose { userManager.removeChannelFromUser(id1, 123L) }
                    .joinException()
        }
        assertThat(userManager.getUserChannelListSize(id1).get(), equalTo(0L))
        assertThat(userManager.getChannelListOfUser(id1).get().size.toLong(), equalTo(userManager.getUserChannelListSize(id1).get()))
    }

    @Test
    fun `test get top 10`() {
        val ids = (1..41).map { it.toLong() }
        val future = add40Users().thenCompose { add40ChannelsToUsers() }

        val best = mutableListOf<Long>(ids[14], ids[37], ids[5], ids[7], ids[20], ids[12], ids[18], ids[33], ids[8], ids[0])

        future.thenCompose { userManager.addChannelToUser(best[0], 5000L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5001L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5002L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5003L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5004L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5005L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5006L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5007L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5008L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5009L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5010L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5011L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5012L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5013L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5014L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5015L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5016L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5017L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5018L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5019L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5020L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5021L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5022L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5023L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5024L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5025L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5026L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5027L) }
                .thenCompose { userManager.addChannelToUser(best[0], 5028L) }.get()

//        (5000..5028).forEach { userManager.addChannelToUser(best[0],it.toLong()).get() }
        (5000..5022).forEach { userManager.addChannelToUser(best[1], it.toLong()).get() }
        (5000..5020).forEach { userManager.addChannelToUser(best[2], it.toLong()).get() }
        (5000..5019).forEach { userManager.addChannelToUser(best[3], it.toLong()).get() }
        (5000..5017).forEach { userManager.addChannelToUser(best[4], it.toLong()).get() }
        (5000..5012).forEach { userManager.addChannelToUser(best[5], it.toLong()).get() }
        (5000..5009).forEach { userManager.addChannelToUser(best[6], it.toLong()).get() }
        (5000..5006).forEach { userManager.addChannelToUser(best[7], it.toLong()).get() }
        (5000..5004).forEach { userManager.addChannelToUser(best[8], it.toLong()).get() }
        (5000..5001).forEach { userManager.addChannelToUser(best[9], it.toLong()).get() }

        val output = userManager.getTop10UsersByChannelsCount().get()
        for ((k, username) in output.withIndex()) {
            assertThat(userManager.getUserId(username).get(), equalTo(best[k]))
        }
    }

    private fun add40ChannelsToUsers(): CompletableFuture<Unit> {
        return userManager.addChannelToUser(1L, 1L * 100L)
                .thenCompose { userManager.addChannelToUser(2L, 2L * 100L) }
                .thenCompose { userManager.addChannelToUser(3L, 3L * 100L) }
                .thenCompose { userManager.addChannelToUser(4L, 4L * 100L) }
                .thenCompose { userManager.addChannelToUser(5L, 5L * 100L) }
                .thenCompose { userManager.addChannelToUser(6L, 6L * 100L) }
                .thenCompose { userManager.addChannelToUser(7L, 7L * 100L) }
                .thenCompose { userManager.addChannelToUser(8L, 8L * 100L) }
                .thenCompose { userManager.addChannelToUser(9L, 9L * 100L) }
                .thenCompose { userManager.addChannelToUser(10L, 10L * 100L) }
                .thenCompose { userManager.addChannelToUser(11L, 11L * 100L) }
                .thenCompose { userManager.addChannelToUser(12L, 12L * 100L) }
                .thenCompose { userManager.addChannelToUser(13L, 13L * 100L) }
                .thenCompose { userManager.addChannelToUser(14L, 14L * 100L) }
                .thenCompose { userManager.addChannelToUser(15L, 15L * 100L) }
                .thenCompose { userManager.addChannelToUser(16L, 16L * 100L) }
                .thenCompose { userManager.addChannelToUser(17L, 17L * 100L) }
                .thenCompose { userManager.addChannelToUser(18L, 18L * 100L) }
                .thenCompose { userManager.addChannelToUser(19L, 19L * 100L) }
                .thenCompose { userManager.addChannelToUser(20L, 20L * 100L) }
                .thenCompose { userManager.addChannelToUser(21L, 21L * 100L) }
                .thenCompose { userManager.addChannelToUser(22L, 22L * 100L) }
                .thenCompose { userManager.addChannelToUser(23L, 23L * 100L) }
                .thenCompose { userManager.addChannelToUser(24L, 24L * 100L) }
                .thenCompose { userManager.addChannelToUser(25L, 25L * 100L) }
                .thenCompose { userManager.addChannelToUser(26L, 26L * 100L) }
                .thenCompose { userManager.addChannelToUser(27L, 27L * 100L) }
                .thenCompose { userManager.addChannelToUser(28L, 28L * 100L) }
                .thenCompose { userManager.addChannelToUser(29L, 29L * 100L) }
                .thenCompose { userManager.addChannelToUser(30L, 30L * 100L) }
                .thenCompose { userManager.addChannelToUser(31L, 31L * 100L) }
                .thenCompose { userManager.addChannelToUser(32L, 32L * 100L) }
                .thenCompose { userManager.addChannelToUser(33L, 33L * 100L) }
                .thenCompose { userManager.addChannelToUser(34L, 34L * 100L) }
                .thenCompose { userManager.addChannelToUser(35L, 35L * 100L) }
                .thenCompose { userManager.addChannelToUser(36L, 36L * 100L) }
                .thenCompose { userManager.addChannelToUser(37L, 37L * 100L) }
                .thenCompose { userManager.addChannelToUser(38L, 38L * 100L) }
                .thenCompose { userManager.addChannelToUser(39L, 39L * 100L) }
                .thenCompose { userManager.addChannelToUser(40L, 40L * 100L) }
                .thenCompose { userManager.addChannelToUser(41L, 41L * 100L) }
    }

    private fun add40Users(): CompletableFuture<Long> {
        return userManager.addUser(0.toString(), 0.toString())
                .thenCompose { userManager.addUser(1.toString(), 1.toString()) }
                .thenCompose { userManager.addUser(2.toString(), 2.toString()) }
                .thenCompose { userManager.addUser(3.toString(), 3.toString()) }
                .thenCompose { userManager.addUser(4.toString(), 4.toString()) }
                .thenCompose { userManager.addUser(5.toString(), 5.toString()) }
                .thenCompose { userManager.addUser(6.toString(), 6.toString()) }
                .thenCompose { userManager.addUser(7.toString(), 7.toString()) }
                .thenCompose { userManager.addUser(8.toString(), 8.toString()) }
                .thenCompose { userManager.addUser(9.toString(), 9.toString()) }
                .thenCompose { userManager.addUser(10.toString(), 10.toString()) }
                .thenCompose { userManager.addUser(11.toString(), 11.toString()) }
                .thenCompose { userManager.addUser(12.toString(), 12.toString()) }
                .thenCompose { userManager.addUser(13.toString(), 13.toString()) }
                .thenCompose { userManager.addUser(14.toString(), 14.toString()) }
                .thenCompose { userManager.addUser(15.toString(), 15.toString()) }
                .thenCompose { userManager.addUser(16.toString(), 16.toString()) }
                .thenCompose { userManager.addUser(17.toString(), 17.toString()) }
                .thenCompose { userManager.addUser(18.toString(), 18.toString()) }
                .thenCompose { userManager.addUser(19.toString(), 19.toString()) }
                .thenCompose { userManager.addUser(20.toString(), 20.toString()) }
                .thenCompose { userManager.addUser(21.toString(), 21.toString()) }
                .thenCompose { userManager.addUser(22.toString(), 22.toString()) }
                .thenCompose { userManager.addUser(23.toString(), 23.toString()) }
                .thenCompose { userManager.addUser(24.toString(), 24.toString()) }
                .thenCompose { userManager.addUser(25.toString(), 25.toString()) }
                .thenCompose { userManager.addUser(26.toString(), 26.toString()) }
                .thenCompose { userManager.addUser(27.toString(), 27.toString()) }
                .thenCompose { userManager.addUser(28.toString(), 28.toString()) }
                .thenCompose { userManager.addUser(29.toString(), 29.toString()) }
                .thenCompose { userManager.addUser(30.toString(), 30.toString()) }
                .thenCompose { userManager.addUser(31.toString(), 31.toString()) }
                .thenCompose { userManager.addUser(32.toString(), 32.toString()) }
                .thenCompose { userManager.addUser(33.toString(), 33.toString()) }
                .thenCompose { userManager.addUser(34.toString(), 34.toString()) }
                .thenCompose { userManager.addUser(35.toString(), 35.toString()) }
                .thenCompose { userManager.addUser(36.toString(), 36.toString()) }
                .thenCompose { userManager.addUser(37.toString(), 37.toString()) }
                .thenCompose { userManager.addUser(38.toString(), 38.toString()) }
                .thenCompose { userManager.addUser(39.toString(), 39.toString()) }
                .thenCompose { userManager.addUser(40.toString(), 40.toString()) }
    }

    @Test
    fun `test get top 7`() {
        val ids = (0..6).map { userManager.addUser(it.toString(), it.toString()).get() }
        ids.forEach { userManager.addChannelToUser(it, it * 100).get() }

        val best = mutableListOf<Long>(ids[2], ids[5], ids[0], ids[4], ids[3], ids[1], ids[6])
        (5000..5028).forEach { userManager.addChannelToUser(best[0], it.toLong()).get() }
        (5000..5022).forEach { userManager.addChannelToUser(best[1], it.toLong()).get() }
        (5000..5020).forEach { userManager.addChannelToUser(best[2], it.toLong()).get() }
        (5000..5019).forEach { userManager.addChannelToUser(best[3], it.toLong()).get() }
        (5000..5017).forEach { userManager.addChannelToUser(best[4], it.toLong()).get() }

        (5000..5012).forEach { userManager.addChannelToUser(best[5], it.toLong()).get() }
        (5000..5012).forEach { userManager.addChannelToUser(best[6], it.toLong()).get() }

        val output = userManager.getTop10UsersByChannelsCount().get()
        val outputIds = output.map { userManager.getUserId(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(best[k]))
        }
    }

    @Test
    fun `check secondary order`() {
        val ids = (0..6).map { userManager.addUser(it.toString(), it.toString()).get() }
        ids.forEach { userManager.addChannelToUser(it, it * 100).get() }

        val best = mutableListOf<Long>(ids[2], ids[5], ids[0], ids[3], ids[4], ids[1], ids[6])
        (5000..5028).forEach { userManager.addChannelToUser(best[0], it.toLong()).get() }
        (5000..5022).forEach { userManager.addChannelToUser(best[1], it.toLong()).get() }
        (5000..5020).forEach { userManager.addChannelToUser(best[2], it.toLong()).get() }

        (5000..5017).forEach { userManager.addChannelToUser(best[3], it.toLong()).get() }
        (5000..5017).forEach { userManager.addChannelToUser(best[4], it.toLong()).get() }

        (5000..5012).forEach { userManager.addChannelToUser(best[5], it.toLong()).get() }
        (5000..5012).forEach { userManager.addChannelToUser(best[6], it.toLong()).get() }

        val output = userManager.getTop10UsersByChannelsCount().get()
        val outputIds = output.map { userManager.getUserId(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(best[k]))
        }
    }

    @Test
    fun `check secondary order only`() {
        val ids = (0..30).map { userManager.addUser(it.toString(), it.toString()).get() }
        ids.forEach { userManager.addChannelToUser(it, it * 100).get() }

        val output = userManager.getTop10UsersByChannelsCount().get()
        val outputIds = output.map { userManager.getUserId(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(ids[k]))
        }
    }

    @Test
    fun `add message to user`() {
        val username="username"
        val pwd = "27dS@@sx1"
        val msgs = listOf<Long>(123L, 15L, 288L, 45L)

        assertThat(
                userManager.addUser(username, pwd)
                        .thenCompose { userManager.addMessageToUser(it, msgs[0]); ImmediateFuture { it } }
                        .thenCompose { userManager.addMessageToUser(it, msgs[1]); ImmediateFuture { it } }
                        .thenCompose { userManager.addMessageToUser(it, msgs[2]); ImmediateFuture { it } }
                        .thenCompose { userManager.addMessageToUser(it, msgs[3]); ImmediateFuture { it } }
                        .thenCompose { userManager.readAllChannelAndPrivateUserMessages(it) }
                        .join(),
                equalTo(msgs.sorted())
        )

        val id = userManager.addUser(username+username, pwd).get()
        assertThat(
            userManager.readAllChannelAndPrivateUserMessages(id).join(),
                equalTo(emptyList())
        )

        assertThat(
                    userManager.addMessageToUser(id, msgs[0])
                        .thenCompose { userManager.addMessageToUser(id, msgs[1]) }
                        .thenCompose { userManager.addMessageToUser(id, msgs[2]) }
                        .thenCompose { userManager.addMessageToUser(id, msgs[3]) }
                        .thenCompose { userManager.readAllChannelAndPrivateUserMessages(id) }
                        .join(),
                equalTo(msgs.sorted())
        )

        assertThat(
                userManager.readAllChannelAndPrivateUserMessages(id).join(),
                equalTo(emptyList())
        )

        // user does not exist
        assertThrows<IllegalArgumentException> { userManager.readAllChannelAndPrivateUserMessages(id*100L).joinException() }
    }

    @Test
    fun `check last read msg id`() {
        val username="username"
        val pwd = "27dS@@sx1"
        val msgIds = listOf<Long>(123L, 15L, 288L, 45L)

        assertThat(
                userManager.addUser(username, pwd)
                        .thenCompose { userManager.getUserLastReadMsgId(it) }
                        .join(),
                equalTo(MANAGERS_CONSTS.MESSAGE_INVALID_ID)
        )

        assertThat(
                userManager.addUser(username+username, pwd)
                        .thenCompose { userId -> userManager.updateUserLastReadMsgId(userId, msgId = msgIds[0]).thenApply { userId }}
                        .thenCompose { userManager.getUserLastReadMsgId(it) }
                        .join(),
                equalTo(msgIds[0])
        )

        assertThat(
                userManager.addUser(username+username+username, pwd)
                        .thenCompose { userId -> userManager.updateUserLastReadMsgId(userId, msgId = msgIds[0]).thenApply { userId }}
                        .thenCompose { userId -> userManager.updateUserLastReadMsgId(userId, msgId = msgIds[3]).thenApply { userId }}
                        .thenCompose { userId -> userManager.updateUserLastReadMsgId(userId, msgId = msgIds[3]).thenApply { userId }}
                        .thenCompose { userId -> userManager.updateUserLastReadMsgId(userId, msgId = msgIds[2]).thenApply { userId }}
                        .thenCompose { userManager.getUserLastReadMsgId(it) }
                        .join(),
                equalTo(msgIds[2])
        )
    }
}