package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import il.ac.technion.cs.softwaredesign.*
import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
import il.ac.technion.cs.softwaredesign.exceptions.NoSuchEntityException
import il.ac.technion.cs.softwaredesign.exceptions.UserNotAuthorizedException
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import org.junit.jupiter.api.*

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.streams.toList

class CourseAppStaffTest {
    private var injector = Guice.createInjector(CourseAppModule(), SecureStorageModule())

    init {
        injector.getInstance<CourseAppInitializer>().setup().join()
    }

    private var courseApp = injector.getInstance<CourseApp>()
    private var courseAppStatistics = injector.getInstance<CourseAppStatistics>()
    private var messageFactory = injector.getInstance<MessageFactory>()

    private val MESSAGE_IDX_IN_FILE = 2
    private fun getDefaultMessage() : Message = messageFactory.create(MediaType.TEXT, stringToByteArray("staff message")).get()

    @BeforeEach
    fun doSetup()
    {
        injector = Guice.createInjector(CourseAppModule(), SecureStorageModule())
        injector.getInstance<CourseAppInitializer>().setup().join()
        courseApp = injector.getInstance<CourseApp>()
        courseAppStatistics = injector.getInstance<CourseAppStatistics>()
        messageFactory = injector.getInstance<MessageFactory>()

    }

    @Nested
    inner class BasicTest {

        @Test
        fun `add listener returns as expected`() {

            assertWithTimeout {
                var testToken: String? = "NONE"
                assertDoesNotThrow {
                    courseApp.login("user2", "pass").thenCompose { token ->
                        testToken = token
                        courseApp.addListener(token
                                , getDefaultCallback())
                    }.joinException()
                }
                assertThrows<InvalidTokenException> {
                    courseApp.addListener("932901ds" + testToken + "hdshg", getDefaultCallback()).joinException()
                }
            }
        }

        @Test
        fun `remove listener returns as expected`() {
            assertWithTimeout {
                var testToken: String = "NONE"
                assertDoesNotThrow {
                    courseApp.login("user2", "pass").thenCompose { token ->
                        testToken = token
                        courseApp.addListener(token
                                , getDefaultCallback())
                    }.thenCompose { courseApp.removeListener(testToken, getDefaultCallback()) }.joinException()
                }

                assertThrows<InvalidTokenException> {
                    courseApp.removeListener("932901ds" + testToken + "hdshg", getDefaultCallback()).joinException()
                }

                assertThrows<NoSuchEntityException> {
                    courseApp.removeListener(testToken, getDefaultCallback()).joinException()
                }
            }

        }

        @Disabled
        @Test
        fun `channel send returns as expected`() {
            val adminToken = courseApp.login("admin", "pass").get()
            val token = courseApp.login("ig32","236606").get()
            val message = getDefaultMessage()

            assertWithTimeout {
                assertDoesNotThrow {
                    courseApp.channelJoin(adminToken, "#sd_channel").thenCompose {
                        courseApp.channelSend(adminToken, "#sd_channel", message)
                    }.joinException()
                }

                assertThrows<InvalidTokenException> {courseApp.channelSend("dsadads" + adminToken + "45hdshg",
                        "#sd_channel", message).joinException()  }
                assertThrows<NoSuchEntityException> {courseApp.channelSend(adminToken,
                        "#hagana_channel", message).joinException()  }
                assertThrows<UserNotAuthorizedException> {courseApp.channelSend(token,
                        "#sd_channel", message).joinException()  }
                assertDoesNotThrow {
                    courseApp.channelJoin(token, "#sd_channel").thenCompose {
                        courseApp.channelSend(token, "#sd_channel", message)
                    }.joinException()
                }
            }
        }

        @Test
        fun `broadcast returns as expected`()
        {
            val userMap = loadDataForTestWithoutMessages(courseApp,"small_test")
            val message = getDefaultMessage()
            assertWithTimeout{
                assertThrows<InvalidTokenException>{courseApp.broadcast(
                        userMap["User0"]!!.token+"something_that_should_make_token_illegal*&^",message).joinException()}
                assertThrows<UserNotAuthorizedException>{courseApp.broadcast(
                        userMap["User1"]!!.token,message).joinException()}
                assertDoesNotThrow{
                    courseApp.broadcast(userMap["MainAdmin"]!!.token, message).joinException()
                }

            }

        }

        @Test
        fun `fetch message returns as expected`()
        {
            val adminToken = courseApp.login("admin", "pass").get()
            val token = courseApp.login("ig32","236606").get()
            val token2 = courseApp.login("user","pass").get()
            val message = getDefaultMessage()
            val message2 = getDefaultMessage()
            courseApp.channelJoin(adminToken, "#sd_channel").thenCompose{
                courseApp.channelJoin(token,"#sd_channel")}
                    .thenCompose { courseApp.channelSend(adminToken,"#sd_channel", message) }
                    .thenCompose {courseApp.privateSend(adminToken, "ig32", message2)}.join()
            assertWithTimeout{
                val messageReceived = courseApp.fetchMessage(token, message.id).join()
                assertThat(messageReceived.second.created == message.created &&
                        messageReceived.second.media == message.media &&
                        messageReceived.second.contents.contentEquals(message.contents), isTrue)
                assertThrows<InvalidTokenException>{courseApp.fetchMessage(token+"blablablasdsad", message.id).joinException()}
                assertThrows<NoSuchEntityException>{courseApp.fetchMessage(token, message.id+message2.id+459201).joinException()}
                assertThrows<NoSuchEntityException>{courseApp.fetchMessage(token, message2.id).joinException()}
                assertThrows<UserNotAuthorizedException>{courseApp.fetchMessage(token2, message.id).joinException()}
            }


        }

        @Test
        fun `broadcast delivers the message correctly`()
        {
            val userMap = loadDataForTestWithoutMessages(courseApp,"small_test")
            val message = getDefaultMessage()
            courseApp.broadcast(userMap["MainAdmin"]!!.token, message).join()
            assertWithTimeout {
                val user0BroadcastMsgContents = userMap["User0"]!!.messages[0].second.contents
                assertThat(userMap["User0"]!!.messages[0].second.contents.contentEquals(user0BroadcastMsgContents), isTrue)
                assertThat(userMap["User6"]!!.messages[0].second.contents.contentEquals(user0BroadcastMsgContents), isTrue)
                assertThat(userMap["User28"]!!.messages[0].second.contents.contentEquals(user0BroadcastMsgContents), isTrue)
            }
        }

        @Test
        fun `messages delivered before a listener is added are still forwarded to the callback`()
        {
            val adminToken = courseApp.login("admin", "pass").get()
            val token = courseApp.login("ig32","236606").get()
            val message = getDefaultMessage()
            val message2 = getDefaultMessage()
            val message3 = getDefaultMessage()
            var currentVal = 0
            var currentVal2 = 0
            var currentVal3 = 0
            courseApp.channelJoin(adminToken, "#sd_channel").thenCompose{
                courseApp.channelJoin(token,"#sd_channel")}.thenCompose { courseApp.channelSend(adminToken,"#sd_channel", message) }
                    .thenCompose{courseApp.privateSend(adminToken,"ig32",message2)}
                    .thenCompose{courseApp.broadcast(adminToken, message3)}
                    .thenCompose { courseApp.addListener(token,
                            {
                              source,msg->
                                if(source[0] == '@')
                                    currentVal = 1
                                else if(source.equals("BROADCAST"))
                                    currentVal2 = 7
                                else
                                    currentVal3 = 2
                                CompletableFuture.completedFuture(Unit)
                            }
                            ) }.join()
            currentVal += currentVal2 + currentVal3
            assertThat(currentVal, equalTo(10))
        }

        @Test
        fun `messages delivered before a 2nd listener is added aren't forwarded to it`()
        {
            val adminToken = courseApp.login("admin", "pass").get()
            val token = courseApp.login("ig32","236606").get()
            val message = getDefaultMessage()
            val message2 = getDefaultMessage()
            val message3 = getDefaultMessage()
            var currentVal = 0
            var currentVal2 = 0
            var currentVal3 = 0
            courseApp.channelJoin(adminToken, "#sd_channel").thenCompose { courseApp.addListener(token,
                    {
                        source,_->
                        if(source[0] == '@')
                            currentVal =1
                        else if(source == "BROADCAST")
                            currentVal2 = 5
                        else
                            currentVal3 = 2
                        CompletableFuture.completedFuture(Unit)
                    }
            ) }.thenCompose{
                courseApp.channelJoin(token,"#sd_channel")}.thenCompose { courseApp.channelSend(adminToken,"#sd_channel", message) }
                    .thenCompose{courseApp.privateSend(adminToken,"ig32",message2)}
                    .thenCompose{courseApp.broadcast(adminToken, message3)}
                    .thenCompose{courseApp.addListener(token,
                            {
                                _,_->
                                currentVal += 500
                                CompletableFuture.completedFuture(Unit)
                            })}.
                            join()
            currentVal += currentVal2 + currentVal3
            assertThat(currentVal, equalTo(8))
        }
		
		@Test
    fun `received date is the same for both listeners for a user's message`()
    {
		var date1 : LocalDateTime? = null
		var date2 : LocalDateTime? = null
        val adminToken = courseApp.login("admin", "pass").get()
        val token = courseApp.login("ig32","236606").get()
        val message = getDefaultMessage()
        courseApp.addListener(token,
                {
                    _,msg->
                    date1=msg.received
                    CompletableFuture.completedFuture(Unit)
                }).thenCompose {
            courseApp.addListener(token,
                    {
                        _,msg->
                        date2=msg.received
                        CompletableFuture.completedFuture(Unit)
                    })
        }.thenCompose{courseApp.privateSend(adminToken,"ig32",message)}.join()

        assertWithTimeout{
            
            assertThat(date1, present(equalTo(date2)))
        }
    }
    }

    

    @Nested
    inner class MainSmallTest
    {
        @Test
        fun `check private messages received`()
        {

            val userMap = loadDataForTest(courseApp, messageFactory, "small_test")
            assertWithTimeout{
                val userMessagesPairs = userMap["User29"]!!.messages.stream().filter{it.first[0] == '@'}.toList()
                val messages = userMessagesPairs.stream().map { it.second }
                        .toList()
                assertThat(messages.stream().map{it.media}.toList(), containsElementsInOrder(MediaType.FILE,
                        MediaType.TEXT, MediaType.PICTURE,MediaType.AUDIO, MediaType.REFERENCE, MediaType.TEXT))
                assertThat(messages.stream().map{byteArrayToString(it.contents)!!.split('_')[MESSAGE_IDX_IN_FILE].toInt() }.toList(),
                        containsElementsInOrder(0,3,1,1,0,2))
                val userSourcesNumbers = userMessagesPairs.stream().map{it.first.split('r')[1].toInt()}.toList()
                assertThat(userSourcesNumbers,containsElementsInOrder(4,46,68,69,86,96))

            }
        }

        @Test
        fun `check listener was called on channel messages`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "small_test")
            assertWithTimeout{
                val user7MessagesFromChnOne = userMap["User7"]!!.messages.
                        filter{it.first.split('@')[0] == "#channel_1"}
                assertThat(user7MessagesFromChnOne.stream().filter{it.first.split('@')[1] == "User72"}
                        .map{it.second.media}.toList(),containsElementsInOrder(MediaType.PICTURE, MediaType.PICTURE, MediaType.FILE
                ,MediaType.STICKER))

                assertThat(user7MessagesFromChnOne.stream().filter{it.second.media == MediaType.TEXT}
                        .map{it.first.split('r')[1].toInt()}.toList(),
                        containsElementsInOrder(30, 37, 38, 42, 42, 43, 53, 53, 55, 63, 73, 79, 85, 86, 90))

            }
        }

        @Test
        fun `pending messages statistic test`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "medium_test")
            assertWithTimeout{
                assertThat(courseAppStatistics.pendingMessages().join(), equalTo(3.toLong()))
            }
        }

        @Test
        fun `pending messages statistic with broadcast`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "medium_test")
            val message = getDefaultMessage()
            courseApp.broadcast(userMap["MainAdmin"]!!.token, message).join()
            assertWithTimeout{
                        val pendingMessages = courseAppStatistics.pendingMessages().join()
                         assertThat(pendingMessages == 4.toLong(),
                                 isTrue) {"Amount of pending messages is $pendingMessages but expected either 4"}

            }
        }

        @Test
        fun `channel messages statistic test`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "medium_test")
            assertWithTimeout{
                assertThat(courseAppStatistics.channelMessages().join(), equalTo(392.toLong()))
            }
        }

        @Test
        fun `top 10 channels by messages with less then 10 channels`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "medium_test")
            assertWithTimeout({
                assertThat(courseAppStatistics.top10ChannelsByMessages().join(),
                        containsElementsInOrder("#channel_2", "#channel_4", "#channel_3", "#channel_1", "#channel_0"))
            }, Duration.ofSeconds(15))

        }

        @Test
        fun `course app after reconstruction doesn't save listeners`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "small_test")
            var value : Int = 0
            courseApp.login("MyUser","pass").thenCompose { courseApp.addListener(it,
                    {
                        src,msg->
                        value += 10
                        CompletableFuture.completedFuture(Unit)
                    }
                    ) }.join()
            courseApp = injector.getInstance<CourseApp>()
            courseApp.privateSend(userMap["User1"]!!.token,"MyUser",getDefaultMessage()).join()
            assertWithTimeout{
               assertThat(value,equalTo(0))
            }
        }
    }

    @Nested
    inner class MainLargeTest
    {

        @Test
        fun `top 10 channels by messages returns correctly`()
        {
            val userMap = loadDataForTest(courseApp, messageFactory, "large_test")
            assertWithTimeout({
                assertThat(courseAppStatistics.top10ChannelsByMessages().join(),
                        containsElementsInOrder("#channel_4", "#channel_2", "#channel_11", "#channel_16", "#channel_17", "#channel_27",
                                "#channel_28", "#channel_8",  "#channel_20", "#channel_22"))
            }, Duration.ofSeconds(28))

        }

    }
}
