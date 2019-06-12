package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CourseAppTest{
    private val injector = Guice.createInjector(CourseAppTestModule())
    private val courseApp = injector.getInstance<CourseApp>()
    private val courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private val courseAppInitializer = injector.getInstance<CourseAppInitializer>()
    private val messageFactory = injector.getInstance<MessageFactory>()
    init {
        courseAppInitializer.setup()
    }
    @Test
    fun `after login, a user is logged in`() {
        val token = courseApp.login("gal", "hunter2")
                .thenCompose { courseApp.login("imaman", "31337") }
                .thenCompose { courseApp.login("matan", "s3kr1t") }
                .join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(token, "gal").join() },
                present(isTrue))
    }

    @Test
    fun `throws NoSuchEntityException after login with wrong password`(){
        val username="gal"
        val password="gal_password"
                courseApp.login(username, password)
                        .thenCompose { token -> courseApp.login("aviad", "shiber!$75").thenApply { token } }
                        .thenCompose { courseApp.logout(it) }
                        .join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.login(username, "wrong_password").joinException() }
        }
    }

    @Test
    fun `throws UserAlreadyLoggedInException after re-login`(){
        val username="gal"
        val password="gal_password"
        courseApp.login(username, password)
                .thenCompose { token -> courseApp.login("aviad", "shiber!$75").thenApply { token } }
                .join()

        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.login(username, password).joinException() }
        }
    }

    @Test
    fun `throws InvalidTokenException after logout with invalid token`(){
        val username="aviad"
        val password="aviad_password"
        val ronToken = courseApp.login(username, password)
                .thenCompose { courseApp.login("ron", password)}
                .thenCompose { t -> courseApp.logout(t).thenApply { t } }
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout("").joinException() }
        }
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout("bad_token").joinException() }
        }
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.logout(ronToken).joinException() }
        }
    }

    /**
     * the test checks that after registering to the system we can login again after logout
     * also the test CHECKS with exhaustive search that no assumptions are made
     * regarding the password & username charSet.
     */
    @Test
    fun `login after register`(){
        val printableAsciiRange = ' '..'~'
        for(char in printableAsciiRange){
            val username= "Aviad$char"
            val password=username+"Password"
            val ronToken=courseApp.login(username,password).join()
            courseApp.logout(ronToken).join()
            courseApp.login(username,password).join()
        }
    }

    @Test
    fun `throws InvalidTokenException after checking user login status with invalid token`(){
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn("","notExistingUser").joinException()}
        }
        val username="ron"
        val password="ron_password"

        val ronToken = courseApp.login("aviad","aviad_password")
                .thenCompose { courseApp.login(username,password)}
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn("bad_token","notExistingUser").joinException()}
        }

        courseApp.logout(ronToken)
                .thenCompose { courseApp.login(username,password) }
                .join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {courseApp.isUserLoggedIn(ronToken,username).joinException()}
        }
    }

    @Test
    fun `user login and then logout`(){
        val username="aviad"
        val password="aviad_password"
        val aviadToken= courseApp.login(username, password).join()
        val adminToken=courseApp.login("admin","123456").join()
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserLoggedIn(aviadToken, username).join() },
                present(equalTo(true)))
        courseApp.logout(aviadToken).join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserLoggedIn(adminToken, username).join() },
                present(equalTo(false)))
    }

    @Test
    fun `returns null if user does not exist`(){
        Assertions.assertNull(
                courseApp.login("aviad","aviad_password")
                        .thenCompose { courseApp.isUserLoggedIn(it,"notExsitingUser")}
                        .join()
        )
    }

    @Test
    fun `test regex`() {
        val channelMatch = "#dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val channelNoMatch = "dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val channelNoMatch2 = "#@dksnsjfs287342347s7s7s_sdk__#_fdad__#"
        val empty = ""
        assertThat(CourseAppImpl.regex matches channelMatch, isTrue)
        assertThat(CourseAppImpl.regex matches channelNoMatch, isFalse)
        assertThat(CourseAppImpl.regex matches channelNoMatch2, isFalse)
        assertThat(CourseAppImpl.regex matches empty, isFalse)
    }

    @Test
    fun `login exceptions`() {
        courseApp.login("admin", "admin").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.login("admin", "wrong_password").joinException() }
        }

        assertThrows<UserAlreadyLoggedInException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.login("admin", "admin").joinException() }
        }
    }

    @Test
    fun `logout exceptions`() {
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.login("admin", "admin")
                        .thenCompose { courseApp.logout(it + "b") }
                        .joinException()
            }
        }
    }

    @Test
    fun `logout no exceptions`() {
        runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.login("admin", "admin")
                    .thenCompose { courseApp.logout(it) }
                    .join()
        }
    }

    @Test
    fun `isUserLoggedIn exceptions`() {
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn(adminToken+adminToken, userToken).joinException() }
        }
    }

    @Test
    fun `test number of valid users`() {
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.totalUsers().join()
        },
                equalTo(0L))
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.makeAdministrator(adminToken, "user")
                    .thenCompose { courseApp.channelJoin(userToken, "#channel") }
                    .thenCompose { courseApp.channelJoin(adminToken, "#channel") }
                    .thenCompose { courseAppStatistics.totalUsers() }
                    .join()
        },
                equalTo(2L))

        val userToken2 = courseApp.login("user2", "user2_pas").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.totalUsers().join()
        },
                equalTo(3L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, "#channel")
                    .thenCompose { courseApp.logout(userToken2) }
                    .thenCompose { courseAppStatistics.totalUsers() }
                    .join()
        },
                equalTo(3L))
    }

    @Test
    fun `test number of valid active users`() {
        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.loggedInUsers().join()
        },
                equalTo(0L))
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.makeAdministrator(adminToken, "user")
                    .thenCompose { courseApp.channelJoin(userToken, "#channel") }
                    .thenCompose { courseApp.channelJoin(adminToken, "#channel") }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        val userToken2 = courseApp.login("user2", "user2_pas").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseAppStatistics.loggedInUsers().join()
        },
                equalTo(3L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, "#channel")
                    .thenCompose { courseApp.logout(userToken2) }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, "#channel")
                    .thenCompose { courseApp.channelPart(userToken, "#channel") }
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseAppStatistics.loggedInUsers() }
                    .join()
        },
                equalTo(0L))
    }

    @Test
    fun `test numberOfActiveUsersInChannel exceptions`() {
        val channel = "#channel"
        val invalidToken = "invalidToken"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(invalidToken, channel).joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(adminToken, channel).joinException() }
        }
        courseApp.channelJoin(adminToken, channel)
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(userToken, channel).joinException() }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        val userToken2 = courseApp.login("second","pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(userToken2, channel) }
                    .join()
        },
                equalTo(1L))
    }

    @Test
    fun `test numberOfTotalUsersInChannel exceptions`() {
        val channel = "#channel"
        val invalidToken = "invalidToken"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(invalidToken, channel).joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(adminToken, channel).joinException() }
        }
        courseApp.channelJoin(adminToken, channel).join()
        val userToken = courseApp.login("user", "user_pass").join()
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfTotalUsersInChannel(userToken, channel).joinException() }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(adminToken, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(adminToken)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(1L))

        val userToken2 = courseApp.login("second","pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken, channel) }
                    .join()
        },
                equalTo(2L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.logout(userToken)
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(userToken2, channel) }
                    .join()
        },
                equalTo(2L))
    }

    @Test
    fun `isUserInChannel test`(){
        val channel = "#channel"
        val adminToken = courseApp.login("admin", "admin").join()
        val userToken = courseApp.login("user", "user_pass").join()
        val userToken2 = courseApp.login("user222", "user_pass222").join()


        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken, channel)
                    .thenCompose { courseApp.channelJoin(userToken, channel) }
                    .thenCompose { courseApp.isUserInChannel(userToken, channel, "admin") }
                    .join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, channel, "user").join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.isUserInChannel(adminToken, channel, "user222").join()
        },
                isFalse)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(userToken, channel)
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isFalse)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.isUserInChannel(userToken2, channel, "user") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `channelKick exceptions`(){
        val channel = "#channel"
        val invalidToken = "invalidToken"

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(invalidToken, channel, "bl").joinException() }
        }

        val adminToken = courseApp.login("admin", "admin").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(adminToken, channel, "bl").joinException() }
        }
        val userToken = courseApp.login("user", "user_pass").join()

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(adminToken, channel)
                        .thenCompose { courseApp.channelJoin(userToken, channel) }
                        .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                        .joinException()
            }
        }


        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.login("bla", "bla")
                        .thenCompose { courseApp.channelKick(adminToken, channel, "bb") }
                        .joinException()
            }
        }
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelKick(adminToken, channel, "bla").joinException() }
        }
    }

    @Test
    fun channelKickTest() {
        val channel = "#channel"
        val adminToken = courseApp.login("admin", "admin").join()
        courseApp.channelJoin(adminToken, channel).join()
        val userToken = courseApp.login("user", "user_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken, channel)
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelKick(adminToken, channel, "user")
                    .thenCompose { courseApp.isUserInChannel(adminToken, channel, "user") }
                    .join()
        },
                isFalse)

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(userToken, channel)
                        .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                        .joinException()
            }
        }

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelMakeOperator(adminToken, channel, "user")
                    .thenCompose { courseApp.channelKick(userToken, channel, "admin") }
                    .thenCompose { courseApp.isUserInChannel(userToken, channel, "admin") }
                    .join()
        },
                isFalse)

        val userToken2 = courseApp.login("user222", "user2_pass").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(userToken2, channel)
                    .thenCompose { courseApp.channelKick(userToken, channel, "user") }
                    .thenCompose { courseApp.isUserInChannel(userToken2, channel, "user") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `channelMakeOperator exceptions`() {
        val channel = "#channel"
        val invalidToken = "token"
        val invalidChannel = "channel"
        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(invalidToken, channel, "user").joinException() }
        }
        val adminToken = courseApp.login("admin", "admin").join()

        assertThrows<InvalidTokenException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(adminToken, channel)
                        .thenCompose { courseApp.channelMakeOperator(invalidToken, channel, "user") }
                        .joinException()
            }
        }
        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(adminToken, invalidChannel, "admin").joinException() }
        }

        val userToken = courseApp.login("user", "user_pass").join()
        val userToken2 = courseApp.login("user222", "user222_pass").join()

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelJoin(userToken, channel)
                        .thenCompose { courseApp.channelJoin(userToken2, channel) }
                        .thenCompose { courseApp.channelMakeOperator(adminToken, invalidChannel, "user") }
                        .joinException()
            }
        }

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken, channel, "user222").joinException() }
        }

        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.makeAdministrator(adminToken, "user")
                        .thenCompose { courseApp.channelMakeOperator(userToken, channel, "user222") }
                        .joinException()
            }
        }
        assertThrows<UserNotAuthorizedException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken, channel, "b").joinException() }
        }

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.channelMakeOperator(userToken, channel, "user")
                        .thenCompose { courseApp.channelMakeOperator(userToken, channel, "user222") }
                        .thenCompose { courseApp.login("user22ddd2", "usedddr222_pass") }
                        .thenCompose { courseApp.channelMakeOperator(userToken2, channel, "b") }
                        .joinException()
            }
        }

        assertThrows<NoSuchEntityException> {
            runWithTimeout(Duration.ofSeconds(10)) { courseApp.channelMakeOperator(userToken2, channel, "user22ddd2").joinException() }
        }
    }

    @Test
    fun `user can join a channel and then leave`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)
        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(aviadToken,"#1")
                    .thenCompose { courseApp.isUserInChannel(adminToken,"#1","aviad") }
                    .join()
        },
                isFalse)
    }

    @Test
    fun `users join a channel, and the channel is destroyed when empty`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)

        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(aviadToken,"#1")
                    .thenCompose { courseApp.isUserInChannel(adminToken,"#1","aviad") }
                    .join()
        },
                isFalse)
        //channel should have destroyed by now, let's try to re-use his name without getting exception
        courseApp.channelPart(adminToken,"#1")
                .thenCompose { courseApp.channelJoin(adminToken,"#1") }
                .join()
    }

    @Test
    fun `channelPart throws InvalidTokenException If the auth token is invalid`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(adminToken,"#1")
                    .thenCompose { courseApp.channelJoin(adminToken,"#2") }
                    .thenCompose { courseApp.channelJoin(aviadToken,"#1") }
                    .thenCompose { courseApp.channelJoin(ronToken,"#2") }
                    .thenCompose { courseApp.logout(ronToken) }
                    .thenCompose { courseApp.isUserInChannel(aviadToken,"#1","aviad") }
                    .join()
        },
                isTrue)

        //verify that the users joined
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)

        assertThrowsWithTimeout<Unit, InvalidTokenException>({ courseApp.channelPart("invalidToken","#1").joinException()})
    }

    @Test
    fun `channelPart throws NoSuchEntityException If token identifies a user who is not a member of channel, or channel does exist`() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()
        courseApp.channelJoin(adminToken,"#1").join()
        courseApp.channelJoin(adminToken,"#2").join()
        courseApp.channelJoin(aviadToken,"#1").join()
        courseApp.channelJoin(ronToken,"#2").join()
        courseApp.logout(ronToken).join()
        //verify that the users joined
        assertThat(courseApp.isUserInChannel(aviadToken,"#1","aviad").join(), isTrue)
        assertThat(courseApp.isUserInChannel(adminToken,"#2","ron").join(), isTrue)
        courseApp.channelPart(aviadToken,"#1").join()
        assertThat(courseApp.isUserInChannel(adminToken,"#1","aviad").join(), isFalse)
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.channelPart(aviadToken,"#1").joinException()})
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.channelPart(aviadToken,"#nonExistingChannel").joinException()})
    }

    @Test
    fun channelJoinTest() {
        val adminToken=courseApp.login("admin","password").join()
        val aviadToken=courseApp.login("aviad","aviad123").join()
        val ronToken=courseApp.login("ron","r4123").join()
        courseApp.channelJoin(adminToken,"#1").join()
        courseApp.channelJoin(aviadToken,"#1").join()
        courseApp.channelJoin(adminToken,"#2").join()
        courseApp.channelJoin(ronToken,"#2").join()
        courseApp.channelJoin(aviadToken,"#2").join()
        courseApp.logout(ronToken).join()
        assertThrowsWithTimeout<Unit, InvalidTokenException>({ courseApp.channelJoin(ronToken,"#nonExistingChannel").joinException()})
        assertThrowsWithTimeout<Unit, NameFormatException>({ courseApp.channelJoin(adminToken,"123#nonExistingChannel").joinException()})
        assertThrowsWithTimeout<Unit, NameFormatException>({ courseApp.channelJoin(adminToken,"badNaming").joinException()})
        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({ courseApp.channelJoin(aviadToken,"#notExistingChannel").joinException()})
    }

    @Test
    fun makeAdminTest() {
        val admin= courseApp.login("admin","admin").join()
        val aviad=courseApp.login("aviad","aviad123").join()
        val ron=courseApp.login("ron","ron123").join()

        assertThrowsWithTimeout<Unit, InvalidTokenException>({
            courseApp.makeAdministrator(admin,"aviad") //only admin can create a channel so let's call channel Join with the new admin
                    .thenCompose { courseApp.channelJoin(aviad,"#1") }
                    .thenCompose { courseApp.makeAdministrator("INVALIDToken","#1") }
                    .joinException()
        })

        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({ courseApp.makeAdministrator(ron,"ron").joinException()})
        assertThrowsWithTimeout<Unit, NoSuchEntityException>({ courseApp.makeAdministrator(aviad,"NotExistingUser").joinException()})
        //we can even make logout users admins
        courseApp.logout(ron)
                .thenCompose { courseApp.makeAdministrator(admin,"ron") }
                .join()
    }

    @Test
    fun `numberOfTotalUsersInChannel get updated after join and part`(){
        val admin= courseApp.login("admin","admin").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(admin,"#1")
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(1L))

        (1..511).forEach{
            courseApp.login("$it","password")
                    .thenCompose { courseApp.channelJoin(it,"#1") }
                    .join()
        }
        assertThat(courseApp.numberOfTotalUsersInChannel(admin,"#1").join(), equalTo(512L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(admin,"#1")
                    .thenCompose { courseApp.numberOfTotalUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(511L))
    }

    @Test
    fun `numberOfActiveUsersInChannel get updated after join and part`(){
        val admin= courseApp.login("admin","admin").join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelJoin(admin,"#1")
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(admin,"#1") }
                    .join()
        },
                equalTo(1L))

        lateinit var token:String
        (1..511).forEach{
            token=courseApp.login("$it","password").join()
            courseApp.channelJoin(token,"#1").join()
        }
        assertThat(runWithTimeout(Duration.ofSeconds(10)) { courseApp.numberOfActiveUsersInChannel(admin, "#1").join() }, equalTo(512L))

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            courseApp.channelPart(admin,"#1")
                    .thenCompose { courseApp.logout(token) }
                    .thenCompose { courseApp.numberOfActiveUsersInChannel(admin, "#1") }
                    .join()
        },
                equalTo(510L))
    }

    @Test
    fun `making sure cant join operator more than once`() {
        val firstUser = "aviad"
        val aviad = courseApp.login(firstUser, "shiber").join()
        val secondUser = "ron"
        val ron = courseApp.login(secondUser, "ron").join()
        val channel = "#SoftwareDesign"

        assertThrowsWithTimeout<Unit, UserNotAuthorizedException>({
            courseApp.channelJoin(aviad, channel) //only admin can create a channel so let's call channel Join with the new admin
                    .thenCompose { courseApp.channelJoin(ron, channel) }
                    .thenCompose { courseApp.channelMakeOperator(aviad, channel, secondUser) }
                    .thenCompose { courseApp.channelMakeOperator(aviad, channel, secondUser) }
                    .thenCompose { courseApp.channelMakeOperator(ron, channel, secondUser) }
                    .thenCompose { courseApp.channelPart(ron, channel) }
                    .thenCompose { courseApp.channelJoin(ron, channel) }
                    //an Exception should be thrown if because
                    //ron should  not an operator anymore(maybe the test were able to add it twice)
                    .thenCompose { courseApp.channelMakeOperator(ron, channel, firstUser) }
                    .joinException()
        })
    }

//
//    @Test
//    fun `top 10 channel by users`() {
//        //TODO: impl
//
//    }
//
    @Test
    fun `get10TopUsersTest primary order only`() {
        val tokens = (0..50).map {Pair(courseApp.login(it.toString(), it.toString()).join(), it.toString())}
        (0..50).forEach {courseApp.makeAdministrator(tokens[0].first, it.toString()).join()}

        val best = tokens.shuffled().take(20)
        (0..0).forEach{ courseApp.channelJoin(best[15].first, "#$it").join() }
        (0..40).forEach{ courseApp.channelJoin(best[0].first, "#$it").join() }
        (0..30).forEach{ courseApp.channelJoin(best[3].first, "#$it").join() }
        (0..40).forEach{ courseApp.channelJoin(best[0].first, "#$it").join() }
        (0..33).forEach{ courseApp.channelJoin(best[1].first, "#$it").join() }
        (0..12).forEach{ courseApp.channelJoin(best[13].first, "#$it").join() }
        (0..31).forEach{ courseApp.channelJoin(best[2].first, "#$it").join() }
        (0..21).forEach{ courseApp.channelJoin(best[7].first, "#$it").join() }
        (0..30).forEach{ courseApp.channelJoin(best[3].first, "#$it").join() }
        (0..25).forEach{ courseApp.channelJoin(best[4].first, "#$it").join() }
        (0..22).forEach{ courseApp.channelJoin(best[6].first, "#$it").join() }
        (0..15).forEach{ courseApp.channelJoin(best[11].first, "#$it").join() }
        (0..21).forEach{ courseApp.channelJoin(best[7].first, "#$it").join() }
        (0..20).forEach{ courseApp.channelJoin(best[8].first, "#$it").join() }
        (0..18).forEach{ courseApp.channelJoin(best[9].first, "#$it").join() }
        (0..16).forEach{ courseApp.channelJoin(best[10].first, "#$it").join() }
        (0..23).forEach{ courseApp.channelJoin(best[5].first, "#$it").join() }
        (0..13).forEach{ courseApp.channelJoin(best[12].first, "#$it").join() }
        (0..8).forEach{ courseApp.channelJoin(best[14].first, "#$it").join() }
        tokens.forEach {courseApp.logout(it.first).join()}
        (100..150).forEach {courseApp.login(it.toString(), it.toString()).join()}

        val output = courseAppStatistics.top10UsersByChannels().join()

        assertThat(runWithTimeout(Duration.ofSeconds(10)) {
            output
        },
                equalTo(best.take(10).map { it.second }))
    }


    //AVI


    @Nested
    inner class Login {
        @Test
        fun `after login, a user is logged in`() {
            val token = courseApp.login("gal", "hunter2")
                    .thenCompose { courseApp.login("imaman", "31337") }
                    .thenCompose { courseApp.login("matan", "s3kr1t") }
                    .join()

            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token, "gal")
                        .join()
            },
                    present(isTrue))
        }

        @Test
        fun `an authentication token is invalidated after logout`() {
            val token = courseApp
                    .login("matan", "s3kr1t")
                    .thenCompose { token -> courseApp.logout(token).thenApply { token } }
                    .join()

            assertThrows<InvalidTokenException> {
                runWithTimeout(Duration.ofSeconds(10)) {
                    courseApp.isUserLoggedIn(token, "matan").joinException()
                }
            }
        }

        @Test
        fun `throw on invalid tokens`() {
            assertThrows<InvalidTokenException> {
                runWithTimeout(Duration.ofSeconds(10)) { courseApp.isUserLoggedIn("a", "any").joinException() }
            }

            assertThrows<InvalidTokenException> {
                runWithTimeout(Duration.ofSeconds(10)) { courseApp.logout("a").joinException() }
            }
        }

        @Test
        fun `login after logout`() {
            val token = courseApp.login("name", "pass")
                    .thenCompose { courseApp.logout(it) }
                    .thenCompose { courseApp.login("name", "pass") }
                    .join()

            // new token works
            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token, "name").join()
            }, present(isTrue))
        }

        @Test
        fun `throws when already logged in`() {
            courseApp.login("someone", "123").join()

            assertThrows<UserAlreadyLoggedInException> {
                runWithTimeout(Duration.ofSeconds(10)) {
                    courseApp.login("someone", "123").joinException()
                }
            }
        }

        @Test
        fun `bad password throws nosuchEntity`() {
            courseApp.login("name", "pass")
                    .thenCompose { courseApp.logout(it) }
                    .join()

            assertThrows<NoSuchEntityException> {
                runWithTimeout(Duration.ofSeconds(10)) {
                    courseApp.login("name", "badpass").joinException()
                }
            }
        }

        @Test
        fun `One user checking another`() {
            val token1 = courseApp.login("name1", "pass").join()
            val token2 = courseApp.login("name2", "pass").join()

            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token1, "name2").join()
            }, present(isTrue))
            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token2, "name1").join()
            }, present(isTrue))
        }

        @Test
        fun `User is logged out after log out`() {
            val token1 = courseApp.login("name1", "pass").join()
            val token2 = courseApp.login("name2", "pass").join()

            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token2, "name1").join()
            }, present(isTrue))

            courseApp.logout(token1).join()

            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token2, "name1").join()
            }, present(isFalse))
        }

        @Test
        fun `User not existing returns null when asked if logged in`() {
            val token1 = courseApp.login("name1", "pass").join()

            assertThat(runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.isUserLoggedIn(token1, "name2").join()
            }, absent())
        }
    }

    @Test
    fun `First user is admin and making others admin causes no exceptions`() {
        val tokenAdmin = courseApp.login("name1", "pass")
                .thenCompose { admin ->
                    courseApp.login("name2", "pass")
                            .thenApply { admin }
                }.join()

        Assertions.assertDoesNotThrow { courseApp.makeAdministrator(tokenAdmin, "name2").join() }

    }

    @Test
    fun `Second user is not an admin`() {
        val second = courseApp.login("name1", "pass")
                .thenCompose { courseApp.login("name2", "pass") }
                .join()

        assertThrows<UserNotAuthorizedException> {
            courseApp.makeAdministrator(second, "name1").joinException()
        }
    }

    @Nested
    inner class Channels {
        @Test
        fun `Test Channel name`() {
            val tokenAdmin = courseApp.login("name1", "pass").join()

            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "hello").joinException() }
            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "1234").joinException() }
            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "a1").joinException() }
            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "עברית").joinException() }
            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "#עברית").joinException() }
            assertThrows<NameFormatException> { courseApp.channelJoin(tokenAdmin, "#hello[").joinException() }
            Assertions.assertDoesNotThrow { courseApp.channelJoin(tokenAdmin, "#hello").joinException() }
            Assertions.assertDoesNotThrow { courseApp.channelJoin(tokenAdmin, "#").joinException() }
        }

        @Test
        fun `Only admin can make channels`() {
            val tokenAdmin = courseApp.login("name1", "pass").join()
            val tokenSecond = courseApp.login("name2", "pass").join()

            courseApp.channelJoin(tokenAdmin, "#ch1").join()
            assertThrows<UserNotAuthorizedException> {
                courseApp.channelJoin(tokenSecond, "#ch2")
                        .joinException()
            }
        }

        @Test
        fun `Non admin can't join deleted channel`() {
            val tokenAdmin = courseApp.login("name1", "pass").join()
            val tokenSecond = courseApp.login("name2", "pass").join()

            courseApp.channelJoin(tokenAdmin, "#ch1")
                    .thenCompose { courseApp.channelPart(tokenAdmin, "#ch1") }
                    .join()
            assertThrows<UserNotAuthorizedException> {
                courseApp.channelJoin(tokenSecond, "#ch1")
                        .joinException()
            }
        }


        @Test
        fun `Admins cant kick from channels they're not in`() {
            val admin = courseApp.login("name1", "pass").join()
            val notAdmin = courseApp.login("name2", "pass").join()

            courseApp.makeAdministrator(admin, "name2")
                    .thenCompose { courseApp.channelJoin(notAdmin, "#ch1") }
                    .join()

            assertThrows<UserNotAuthorizedException> {
                courseApp.channelKick(admin, "#ch1", "name2").joinException()
            }
        }

        @Test
        fun `Operator can make operators and can kick admins`() {
            val tokenAdmin = courseApp.login("name1", "pass").join()
            val tokenSecond = courseApp.login("name2", "pass").join()

            courseApp.channelJoin(tokenAdmin, "#ch1")
                    .thenCompose { courseApp.channelJoin(tokenSecond, "#ch1") }.join()

            assertThrows<UserNotAuthorizedException> {
                courseApp.channelKick(tokenSecond, "#ch1", "name1").joinException()
            }

            Assertions.assertEquals(2, courseApp.numberOfTotalUsersInChannel(tokenSecond, "#ch1").join().toInt())
            courseApp.channelMakeOperator(tokenAdmin, "#ch1", "name2")
                    .thenCompose { courseApp.channelKick(tokenSecond, "#ch1", "name1") }
                    .join()
            Assertions.assertEquals(1, courseApp.numberOfTotalUsersInChannel(tokenSecond, "#ch1").join().toInt())
        }

        @Test
        fun `Nothing happens when joining channel twice`() {
            val tokenAdmin = courseApp.login("name1", "pass")
                    .thenCompose { admin ->
                        courseApp.channelJoin(admin, "#ch1")
                                .thenCompose { courseApp.channelJoin(admin, "#ch1") }
                                .thenApply { admin }
                    }.join()

            Assertions.assertEquals(1, courseApp.numberOfTotalUsersInChannel(tokenAdmin, "#ch1").join().toInt())
        }

        @Test
        fun `Throws when leaving channel twice`() {
            val tokenAdmin = courseApp.login("name1", "pass")
                    .thenCompose { admin ->
                        courseApp.channelJoin(admin, "#ch1")
                                .thenCompose { courseApp.channelPart(admin, "#ch1") }
                                .thenApply { admin }
                    }.join()

            assertThrows<InvalidTokenException> {
                courseApp.channelPart("kishkush", "#ch1")
                        .joinException()
            }
            assertThrows<NoSuchEntityException> {
                courseApp.channelPart(tokenAdmin, "#ch1")
                        .joinException()
            }
            assertThrows<NoSuchEntityException> {
                courseApp.numberOfTotalUsersInChannel(tokenAdmin, "#ch1").joinException()
            }
        }

        @Test
        fun `Throws when user leaves channel that is not in it`() {
            val tokenAdmin = courseApp.login("name1", "pass")
                    .thenCompose { admin ->
                        courseApp.channelJoin(admin, "#ch1")
                                .thenCompose { courseApp.login("someone", "asdfasdfa") }
                                .thenCompose { courseApp.channelJoin(it, "#ch1") }
                                .thenCompose { courseApp.channelPart(admin, "#ch1") }
                                .thenApply { admin }
                    }.join()

            assertThrows<NoSuchEntityException> {
                courseApp.channelPart(tokenAdmin, "#ch1").joinException()
            }
        }

        @Test
        fun `IsUserInChannel return desired results`() {
            val tokenAdmin = courseApp.login("name1", "pass")
                    .thenCompose { admin ->
                        courseApp.login("name2", "pass")
                                .thenCompose { courseApp.channelJoin(admin, "#ch1") }
                                .thenApply { admin }
                    }.join()

            Assertions.assertNull(courseApp.isUserInChannel(tokenAdmin, "#ch1", "name3").join())
            Assertions.assertFalse(courseApp.isUserInChannel(tokenAdmin, "#ch1", "name2").join()!!)
            Assertions.assertTrue(courseApp.isUserInChannel(tokenAdmin, "#ch1", "name1").join()!!)
        }

        @Test
        fun `IsUserInChannel throws on bad input`() {
            assertThrows<InvalidTokenException> {
                courseApp.isUserInChannel("aaa", "#ch1", "name1").joinException()
            }
            val tokenAdmin = courseApp.login("name1", "pass").join()
            val tokenOther = courseApp.login("name2", "pass").join()

            courseApp.channelJoin(tokenAdmin, "#ch1").join()

            assertThrows<NoSuchEntityException> {
                courseApp.isUserInChannel(tokenAdmin,
                        "#ch2",
                        "name1").joinException()
            }
            assertThrows<UserNotAuthorizedException> {
                courseApp.isUserInChannel(tokenOther,
                        "#ch1",
                        "name1").joinException()
            }
        }

        @Test
        fun `Test channel active and nonactive user count`() {
            val tokenAdmin = courseApp.login("name1", "pass").join()
            courseApp.channelJoin(tokenAdmin, "#ch1").join()

            val tokens = ArrayList<String>()
            for (i in 101..130) {
                courseApp.login("name$i", "pass")
                        .thenAccept {
                            courseApp.channelJoin(it, "#ch1").join()
                            tokens.add(it)
                        }.join()
            }
            for (i in 2..30) {
                courseApp.login("name$i", "pass")
                        .thenCompose { courseApp.channelJoin(it, "#ch1") }
                        .join()
            }
            for (i in 201..230) {
                courseApp.login("name$i", "pass").join()
            }

            Assertions.assertEquals(60, courseApp.numberOfTotalUsersInChannel(tokenAdmin, "#ch1").join().toInt())
            Assertions.assertEquals(60, courseApp.numberOfActiveUsersInChannel(tokenAdmin, "#ch1").join().toInt())


            tokens.forEach { courseApp.logout(it).join() }

            Assertions.assertEquals(60, courseApp.numberOfTotalUsersInChannel(tokenAdmin, "#ch1").join().toInt())
            Assertions.assertEquals(30, courseApp.numberOfActiveUsersInChannel(tokenAdmin, "#ch1").join().toInt())
        }
    }

    @Nested
    inner class Messages {

        @Test
        fun `addListener throws on bad input`() {
            courseApp.login("who", "ha").join()

            assertThrows<InvalidTokenException> {
                courseApp.addListener("invalid token", mockk()).joinException()
            }
        }

        @Test
        fun `removeListener throws on bad input`() {
            val token = courseApp.login("who", "ha").join()

            assertThrows<InvalidTokenException> {
                courseApp.removeListener("invalid token", mockk()).joinException()
            }

            courseApp.addListener(token, mockk(name = "A cute listener")).join()

            assertThrows<NoSuchEntityException> {
                courseApp.removeListener(token, mockk(name = "who's that listener?!")).joinException()
            }
        }

        @Test
        fun `removeListener removes old listener`() {
            val token = courseApp.login("who", "ha").join()

            val callback = mockk<ListenerCallback>(name = "A cute listener")
            courseApp.addListener(token, callback).join()

            Assertions.assertDoesNotThrow { courseApp.removeListener(token, callback).joinException() }
        }

        @Test
        fun `channelSend throws on bad input`() {
            val token = courseApp.login("who", "ha").join()

            assertThrows<InvalidTokenException> {
                courseApp.channelSend("invalid token", "#what", mockk()).joinException()
            }

            assertThrows<NoSuchEntityException> {
                courseApp.channelSend(token, "#what", mockk()).joinException()
            }

            courseApp.login("bla", "bla")
                    .thenCompose { bla -> courseApp.makeAdministrator(token, "bla").thenApply { bla } }
                    .thenCompose { courseApp.channelJoin(it, "#what") }
                    .join()

            assertThrows<UserNotAuthorizedException> {
                courseApp.channelSend(token, "#what", mockk()).joinException()
            }
        }

        @Test
        fun `private message received by all listeners`() {
            val listeners = Array(5) { mockk<ListenerCallback>() }

            listeners.forEach { listener ->
                every { listener(any(), any()) } returns CompletableFuture.completedFuture(Unit)
            }

            val (token, message) =
                    courseApp.login("who", "ha")
                            .thenAccept { who -> listeners.forEach { courseApp.addListener(who, it).join() } }
                            .thenCompose { courseApp.login("user2", "user2") }
                            .thenCompose { user2 ->
                                messageFactory.create(MediaType.TEXT, "@who".toByteArray())
                                        .thenApply { Pair(user2, it) }
                            }.join()

            courseApp.privateSend(token, "who", message).join()

            listeners.forEach { listener ->
                verify(exactly = 1) {
                    listener(match { it == "@user2" },
                            match { it.contents contentEquals "@who".toByteArray() })
                }
            }
            confirmVerified()
        }

        @Test
        fun `channelSend message received by all users`() {
            val listener = mockk<ListenerCallback>()

            every { listener(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            val (tokens, message) =
                    courseApp.login("who", "ha")
                            .thenCompose { admin ->
                                courseApp.login("user2", "user2")
                                        .thenApply { Pair(admin, it) }
                            }.thenCompose { tokens ->
                                courseApp.addListener(tokens.first, listener)
                                        .thenCompose { courseApp.addListener(tokens.second, listener) }
                                        .thenCompose { courseApp.channelJoin(tokens.first, "#channel") }
                                        .thenCompose { courseApp.channelJoin(tokens.second, "#channel") }
                                        .thenCompose {
                                            messageFactory.create(MediaType.TEXT,
                                                    "1 2".toByteArray())
                                        }.thenApply { Pair(tokens, it) }
                            }.join()

            courseApp.channelSend(tokens.first, "#channel", message).join()

            verify(exactly = 2) {
                listener(match { it == "#channel@who" },
                        match { it.contents contentEquals "1 2".toByteArray() })
            }
            confirmVerified()

            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 0)
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 1)
        }

        @Test
        fun `pending messages is updated when a user starts listening`() {
            val admin = courseApp.login("who", "ha").join()
            val other = courseApp.login("user2", "user2").join()

            val m = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()
            courseApp.broadcast(admin, m).join()

            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 1) // error
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 0)

            courseApp.addListener(admin) { _, _ -> CompletableFuture.completedFuture(Unit) }.join()

            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 1)
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 0)

            courseApp.addListener(other) { _, _ -> CompletableFuture.completedFuture(Unit) }.join()

            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 0)
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 0)

            val m2 = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()
            courseApp.broadcast(admin, m2).join()

            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 0)
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 0)
        }

        @Test
        fun `message read time is set to first reader - private`() {
            val admin = courseApp.login("who", "ha").join()
            val other = courseApp.login("user2", "user2").join()

            val m = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()

            var receivedm: Message? = null


            courseApp.addListener(other) { _, received ->
                receivedm = received
                CompletableFuture.completedFuture(Unit)
            }.join()


            val before = LocalDateTime.now()
            Thread.sleep(1000)
            courseApp.privateSend(admin, "user2", m).join()
            Thread.sleep(1000)
            val after = LocalDateTime.now()


            assert(receivedm!!.received!! > before)
            assert(receivedm!!.received!! < after)
        }

        @Test
        fun `message read time is set to first reader - channel`() {
            val admin = courseApp.login("who", "ha").join()
            val other = courseApp.login("user2", "user2").join()
            val other2 = courseApp.login("user3", "user3").join()

            courseApp.channelJoin(admin, "#ch").join()
            courseApp.channelJoin(other, "#ch").join()
            courseApp.channelJoin(other2, "#ch").join()

            val m = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()

            var receivedm: Message? = null

            courseApp.addListener(other) { _, _ ->
                CompletableFuture.completedFuture(Unit)
            }.join()


            val before = LocalDateTime.now()
            Thread.sleep(1000)
            courseApp.channelSend(admin, "#ch", m).join()
            Thread.sleep(1000)
            val after = LocalDateTime.now()
            courseApp.addListener(other2) { _, received ->
                receivedm = received
                CompletableFuture.completedFuture(Unit)
            }.join()

            val res = courseApp.fetchMessage(admin, m.id).join().second
            assert(res!!.received!! > before)
            assert(res!!.received!! < after)

//            assert(receivedm!!.received!! > before)
//            assert(receivedm!!.received!! < after)
        }


        @Test
        fun `message read time is set to first reader - broadcast`() {
            val admin = courseApp.login("who", "ha").join()
            val other = courseApp.login("user2", "user2").join()
            val other2 = courseApp.login("user3", "user3").join()


            val m = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()

            var receivedm: Message? = null

            courseApp.addListener(other) { _, _ ->
                CompletableFuture.completedFuture(Unit)
            }


            val before = LocalDateTime.now()
            Thread.sleep(1000)
            courseApp.broadcast(admin, m).join()
            Thread.sleep(1000)
            val after = LocalDateTime.now()
            courseApp.addListener(other2) { _, received ->
                receivedm = received
                CompletableFuture.completedFuture(Unit)
            }.join()

            // this check is not valid
//            assert(receivedm!!.received!! > before)
//            assert(receivedm!!.received!! < after)
        }


        @Test
        fun `broadcast message received by all users`() {
            val listener = mockk<ListenerCallback>()

            every { listener(any(), any()) } returns CompletableFuture.completedFuture(Unit)

            val admin = courseApp.login("who", "ha").join()
            val other = courseApp.login("user2", "user2").join()

            courseApp.addListener(admin, listener).join()
            courseApp.addListener(other, listener).join()

            val m = messageFactory.create(MediaType.TEXT, "1 2".toByteArray()).join()
            courseApp.broadcast(admin, m).join()


            verify(exactly = 2) {
                listener(match { it == "BROADCAST" },
                        match { it.contents contentEquals "1 2".toByteArray() })
            }
            confirmVerified()


            Assertions.assertEquals(courseAppStatistics.pendingMessages().join(), 0)
            Assertions.assertEquals(courseAppStatistics.channelMessages().join(), 0)
        }


        @Test
        fun `broadcast throws on bad input`() {
            val notAdmin = courseApp.login("who", "ha")
                    .thenCompose { courseApp.login("someone else", "some password") }
                    .join()

            assertThrows<InvalidTokenException> {
                courseApp.broadcast("invalid token", mockk()).joinException()
            }

            assertThrows<UserNotAuthorizedException> {
                courseApp.broadcast(notAdmin, mockk()).joinException()
            }
        }

        @Test
        fun `privateSend throws on bad input`() {
            val admin = courseApp.login("who", "ha").join()

            assertThrows<InvalidTokenException> {
                courseApp.privateSend("invalid token", "some one", mockk()).joinException()
            }

            assertThrows<NoSuchEntityException> {
                courseApp.privateSend(admin, "some one", mockk()).joinException()
            }
        }

        @Test
        fun `add listener returns with pending private messages`() {
            val token = courseApp.login("admin", "admin")
                    .thenCompose { adminToken ->
                        courseApp.login("gal", "hunter2").thenApply { Pair(adminToken, it) }
                    }.thenCompose { (adminToken, nonAdminToken) ->
                        messageFactory.create(MediaType.TEXT, "hello, world\n".toByteArray())
                                .thenCompose { courseApp.privateSend(adminToken, "gal", it) }
                                .thenApply { nonAdminToken }
                    }.join()

            val listener = mockk<ListenerCallback>()
            every { listener(any(), any()) }.returns(CompletableFuture.completedFuture(Unit))

            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.addListener(token, listener).join()
            }

            verify {
                listener(match { it == "@admin" },
                        match { it.contents contentEquals "hello, world\n".toByteArray() })
            }
            confirmVerified(listener)
        }

        @Test
        fun `add listener returns with pending broadcast messages`() {
            val tokens = courseApp.login("admin", "admin")
                    .thenCompose { adminToken ->
                        courseApp.login("gal", "hunter2")
                                .thenCompose { token1 ->
                                    courseApp.login("tal", "hunter5")
                                            .thenApply { listOf(adminToken, token1, it) }
                                }
                    }.thenCompose { tokens ->
                        messageFactory.create(MediaType.TEXT, "hello".toByteArray())
                                .thenCompose { courseApp.broadcast(tokens[0], it) }
                                .thenCompose { messageFactory.create(MediaType.TEXT, "world".toByteArray()) }
                                .thenCompose { courseApp.broadcast(tokens[0], it) }
                                .thenApply { tokens }
                    }.join()

            val listener = mockk<ListenerCallback>()
            every { listener(any(), any()) }.returns(CompletableFuture.completedFuture(Unit))

            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.addListener(tokens[1], listener).join()
            }
            runWithTimeout(Duration.ofSeconds(10)) {
                courseApp.addListener(tokens[2], listener).join()
            }

            verify(exactly = 2) {
                listener(match { it == "BROADCAST" },
                        match { it.contents contentEquals "hello".toByteArray() })
                listener(match { it == "BROADCAST" },
                        match { it.contents contentEquals "world".toByteArray() })
            }
            confirmVerified(listener)
        }

        @Test
        fun `fetchMessage throws on bad input`() {
            val admin = courseApp.login("who", "ha").join()

            assertThrows<InvalidTokenException> {
                courseApp.fetchMessage("invalid token", 4).joinException()
            }

            assertThrows<NoSuchEntityException> {
                courseApp.fetchMessage(admin, 4).joinException()
            }

            val id = courseApp.login("someone", "1234")
                    .thenCompose { token ->
                        courseApp.makeAdministrator(admin, "someone")
                                .thenCompose { courseApp.channelJoin(token, "#wawa") }
                                .thenCompose {
                                    messageFactory.create(MediaType.TEXT,
                                            "important message".toByteArray())
                                }
                                .thenCompose { msg ->
                                    courseApp.channelSend(token, "#wawa", msg)
                                            .thenApply { msg.id }
                                }
                    }.join()

            assertThrows<UserNotAuthorizedException> {
                courseApp.fetchMessage(admin, id).joinException()
            }
        }

        @Test
        fun `fetchMessage throws NoSuchEntityException on non channel message`() {
            val (token, message) = courseApp.login("who", "ha")
                    .thenCompose { admin ->
                        courseApp.login("someone", "1234")
                                .thenApply { Pair(admin, it) }
                    }.thenCompose { (admin, notAdmin) ->
                        messageFactory.create(MediaType.TEXT, "broad !".toByteArray())
                                .thenCompose { msg ->
                                    courseApp.broadcast(admin, msg)
                                            .thenApply { Pair(notAdmin, msg) }
                                }
                    }.join()

            assertThrows<NoSuchEntityException> {
                courseApp.fetchMessage(token, message.id).joinException()
            }
        }
    }

    @Nested
    inner class Statistics {
        @Test
        fun `top 10 channels`() {
            val tokens = ArrayList<String>()

            courseApp.login("admin", "pass")
                    .thenApply {
                        for (i in 1..20) courseApp.channelJoin(it, "#ch$i").join()
                    }.thenApply {
                        for (i in 1..20) {
                            courseApp.login("name$i", "pass")
                                    .thenAccept {
                                        for (j in 1..i) courseApp.channelJoin(it, "#ch$j").join()
                                        tokens.add(it)
                                    }.join()
                        }
                    }.join()

            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ChannelsByUsers().join(),
                        containsElementsInOrder(
                                "#ch1", "#ch2", "#ch3", "#ch4", "#ch5",
                                "#ch6", "#ch7", "#ch8", "#ch9", "#ch10"))
            }


            // Test order by creation time (index)
            courseApp.channelPart(tokens[0], "#ch1").join()
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ChannelsByUsers().join(),
                        containsElementsInOrder(
                                "#ch1", "#ch2", "#ch3", "#ch4", "#ch5",
                                "#ch6", "#ch7", "#ch8", "#ch9", "#ch10"))
            }

            // Test order by count
            courseApp.channelPart(tokens[1], "#ch1").join()
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ChannelsByUsers().join(),
                        containsElementsInOrder(
                                "#ch2", "#ch1", "#ch3", "#ch4", "#ch5",
                                "#ch6", "#ch7", "#ch8", "#ch9", "#ch10"))
            }
        }

        @Test
        fun `top 10 Active`() {
            courseApp.login("admin", "pass")
                    .thenAccept {
                        for (j in 1..2) courseApp.channelJoin(it, "#ch$j").join()
                    }.join()

            val tokens = ArrayList<String>()
            for (i in 1..2) {
                courseApp.login("name$i", "pass")
                        .thenCompose {
                            tokens.add(it)
                            courseApp.channelJoin(it, "#ch$i")
                        }.join()
            }

            val token3 = courseApp.login("name3", "pass")
                    .thenCompose { token ->
                        courseApp.channelJoin(token, "#ch1").thenApply { token }
                    }.join()

            // (3,2) in channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ActiveChannelsByUsers().join(),
                        containsElementsInOrder("#ch1", "#ch2"))
            }

            courseApp.logout(token3).join()
            // (2,2) in channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ActiveChannelsByUsers().join(),
                        containsElementsInOrder("#ch1", "#ch2"))
            }


            courseApp.logout(tokens[0]).join()
            // (1,2) in channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ActiveChannelsByUsers().join(),
                        containsElementsInOrder("#ch2", "#ch1"))
            }


            courseApp.login("name1", "pass").join()
            // (2,2) in channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ActiveChannelsByUsers().join(),
                        containsElementsInOrder("#ch1", "#ch2"))
            }
        }

        @Test
        fun `top 10 Users`() {

            courseApp.login("admin", "pass")
                    .thenAccept { for (j in 1..20) courseApp.channelJoin(it, "#ch$j").join() }
                    .join()

            val tokens = ArrayList<String>()
            for (i in 1..20) {
                courseApp.login("name$i", "pass")
                        .thenAccept {
                            for (j in 1..i) courseApp.channelJoin(it, "#ch$j").join()
                            tokens.add(it)
                        }.join()
            }

            // admin in all channels and created first. for rest later users have more channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10UsersByChannels().join(),
                        containsElementsInOrder(
                                "admin", "name20", "name19", "name18", "name17",
                                "name16", "name15", "name14", "name13", "name12"))
            }
        }

        @Test
        fun `top 10 Users with less than 10`() {
            courseApp.login("admin", "pass")
                    .thenAccept { for (j in 1..6) courseApp.channelJoin(it, "#ch$j").join() }
                    .join()

            val tokens = ArrayList<String>()
            for (i in 1..6) {
                courseApp.login("name$i", "pass")
                        .thenAccept {
                            for (j in 1..i) courseApp.channelJoin(it, "#ch$j").join()
                            tokens.add(it)
                        }.join()
            }

            // admin in all channels and created first. for rest later users have more channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10UsersByChannels().join(),
                        containsElementsInOrder("admin",
                                "name6",
                                "name5",
                                "name4",
                                "name3",
                                "name2",
                                "name1"))
            }
        }

        @Test
        fun top10ChannelsByMessages() {
            // create 20 channels
            val token = courseApp.login("admin", "pass")
                    .thenApply { token ->
                        repeat(20) { courseApp.channelJoin(token, "#c$it").join() }
                        token
                    }.join()


            // 11111 11111 11111
            // -----|-----|-----|-----|
            for (i in 0 until 15) {
                messageFactory.create(MediaType.TEXT, "@#$".toByteArray())
                        .thenCompose { courseApp.channelSend(token, "#c$i", it) }.join()
            }

            // 11111 11111 22222
            // -----|-----|-----|-----|
            for (i in 10 until 15) {
                messageFactory.create(MediaType.TEXT, "@#$".toByteArray())
                        .thenCompose { courseApp.channelSend(token, "#c$i", it) }.join()
            }

            // 33333 11111 22222
            // -----|-----|-----|-----|
            for (i in 0 until 5) {
                repeat(2) {
                    messageFactory.create(MediaType.TEXT, "@#$".toByteArray())
                            .thenCompose { courseApp.channelSend(token, "#c$i", it) }.join()
                }
            }

            // 33333 11111 22222   5
            // -----|-----|-----|-----|
            repeat(5) {
                messageFactory.create(MediaType.TEXT, "@#$".toByteArray())
                        .thenCompose { courseApp.channelSend(token, "#c18", it) }.join()

            }

            // admin in all channels and created first. for rest later users have more channels
            runWithTimeout(Duration.ofSeconds(10)) {
                assertThat(courseAppStatistics.top10ChannelsByMessages().join(),
                        containsElementsInOrder("#c18",
                                "#c0",
                                "#c1",
                                "#c2",
                                "#c3",
                                "#c4",
                                "#c10",
                                "#c11",
                                "#c12",
                                "#c13"))
            }
        }

        @Test
        fun `user count statistics`() {
            val tokens = ArrayList<String>()
            for (i in 1..20) {
                courseApp.login("name$i", "pass")
                        .thenAccept { tokens.add(it) }
                        .join()
            }

            Assertions.assertEquals(20, courseAppStatistics.totalUsers().join().toInt())
            Assertions.assertEquals(20, courseAppStatistics.loggedInUsers().join().toInt())
            courseApp.logout(tokens[0])
                    .thenCompose { courseApp.logout(tokens[5]) }
                    .thenCompose { courseApp.logout(tokens[10]) }
                    .join()
            Assertions.assertEquals(20, courseAppStatistics.totalUsers().join().toInt())
            Assertions.assertEquals(17, courseAppStatistics.loggedInUsers().join().toInt())
        }
    }


}