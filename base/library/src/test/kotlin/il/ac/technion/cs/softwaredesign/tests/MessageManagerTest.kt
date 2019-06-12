package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import il.ac.technion.cs.softwaredesign.storage.api.IMessageManager
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS
import org.junit.jupiter.api.*
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class MessageManagerTest {

    private val injector = Guice.createInjector(LibraryTestModule())

    private val messageManager = injector.getInstance<IMessageManager>()

    private fun initStatistics() {
        val statisticsStorage = injector.getInstance<IStatisticsStorage>()
        statisticsStorage.setLongValue(STATISTICS_KEYS.NUMBER_OF_USERS, STATISTICS_KEYS.INIT_INDEX_VAL).get()
        statisticsStorage.setLongValue(STATISTICS_KEYS.USER_MESSAGE_ID, STATISTICS_KEYS.INIT_INDEX_VAL).get()
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
    fun `id generator generate unique id`() {
        val id1 = messageManager.generateUniqueMessageId().join()
        val id2 = messageManager.generateUniqueMessageId().join()
        val id3 = messageManager.generateUniqueMessageId().join()
        val id4 = messageManager.generateUniqueMessageId().join()
        Assertions.assertNotEquals(id1, id2)
        Assertions.assertNotEquals(id1, id3)
        Assertions.assertNotEquals(id1, id4)
        Assertions.assertNotEquals(id2, id3)
        Assertions.assertNotEquals(id2, id4)
        Assertions.assertNotEquals(id3, id4)
    }

    @Test
    fun `add message succeeded`() {
        val id1 = messageManager.generateUniqueMessageId().join()
        val mediaType = 0L
        val content = "hello".toByteArray()
        val createdTime = LocalDateTime.now()
        val messageType = IMessageManager.MessageType.BROADCAST
        messageManager.addMessage(id1, mediaType, content, createdTime, messageType, source = "source").join()

        Assertions.assertEquals(messageManager.getMessageMediaType(id1).join(), mediaType)
        Assertions.assertEquals(messageManager.getMessageContent(id1).join(), content)
        Assertions.assertEquals(messageManager.getMessageCreatedTime(id1).join(), createdTime)
        Assertions.assertEquals(messageManager.getMessageType(id1).join(), messageType)
        Assertions.assertEquals(messageManager.getMessageReceivedTime(id1).join(), null)

        val receivedTime = LocalDateTime.now()
        messageManager.updateMessageReceivedTime(id1, receivedTime).join()

        Assertions.assertEquals(messageManager.getMessageMediaType(id1).join(), mediaType)
        Assertions.assertEquals(messageManager.getMessageContent(id1).join(), content)
        Assertions.assertEquals(messageManager.getMessageCreatedTime(id1).join(), createdTime)
        Assertions.assertEquals(messageManager.getMessageType(id1).join(), messageType)
        Assertions.assertEquals(messageManager.getMessageReceivedTime(id1).join(), receivedTime)
    }

    @Test
    fun `add broadcast message succeeded`() {
        val id1 = messageManager.generateUniqueMessageId().join()
        val mediaType = 0L
        val content = "hello".toByteArray()
        val createdTime = LocalDateTime.now()
        val messageType = IMessageManager.MessageType.BROADCAST
        messageManager.addMessage(id1, mediaType, content, createdTime, messageType, startCounter = 7, source = "source").join()

        Assertions.assertEquals(messageManager.getMessageMediaType(id1).join(), mediaType)
        Assertions.assertEquals(messageManager.getMessageContent(id1).join(), content)
        Assertions.assertEquals(messageManager.getMessageCreatedTime(id1).join(), createdTime)
        Assertions.assertEquals(messageManager.getMessageType(id1).join(), messageType)
        Assertions.assertEquals(messageManager.getMessageReceivedTime(id1).join(), null)
        Assertions.assertEquals(messageManager.getMessageCounter(id1).join(), 7)

        val receivedTime = LocalDateTime.now()
        messageManager.updateMessageReceivedTime(id1, receivedTime).join()

        Assertions.assertEquals(messageManager.getMessageMediaType(id1).join(), mediaType)
        Assertions.assertEquals(messageManager.getMessageContent(id1).join(), content)
        Assertions.assertEquals(messageManager.getMessageCreatedTime(id1).join(), createdTime)
        Assertions.assertEquals(messageManager.getMessageType(id1).join(), messageType)
        Assertions.assertEquals(messageManager.getMessageReceivedTime(id1).join(), receivedTime)
        Assertions.assertEquals(messageManager.getMessageCounter(id1).join(), 7)

        assertThrows<IllegalArgumentException> {messageManager.getMessageCounter(id1+id1).joinException()  }
        val id2 = messageManager.generateUniqueMessageId().join()
        messageManager.addMessage(id2, mediaType, content, createdTime, IMessageManager.MessageType.CHANNEL, startCounter = 7, source = "source").join()
        assertThrows<IllegalAccessException> {
            messageManager.getMessageCounter(id2).joinException()
        }
        assertThrows<IllegalAccessException> {
            messageManager.decreaseMessageCounterBy(id2, 5).joinException()
        }
        messageManager.decreaseMessageCounterBy(id1, 5).join()
        Assertions.assertEquals(messageManager.getMessageCounter(id1).join(), 2)
    }

    @Test
    fun `get number of pending messages`(){
        val id1 = messageManager.generateUniqueMessageId().join()
        val id2 = messageManager.generateUniqueMessageId().join()
        val id3 = messageManager.generateUniqueMessageId().join()
        val mediaType = 0L
        val content = "hello".toByteArray()
        val createdTime = LocalDateTime.now()
        val messageType = IMessageManager.MessageType.BROADCAST
        Assertions.assertEquals(messageManager.getNumberOfPendingMessages().join(), 0)
        messageManager.addMessage(id1, mediaType, content, createdTime, messageType, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getNumberOfPendingMessages().join(), 1)
        messageManager.addMessage(id2, mediaType, content, createdTime, IMessageManager.MessageType.PRIVATE, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getNumberOfPendingMessages().join(), 2)
        messageManager.addMessage(id3, mediaType, content, createdTime, IMessageManager.MessageType.CHANNEL, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getNumberOfPendingMessages().join(), 2)
        assertThrows<IllegalArgumentException> { messageManager.addMessage(id3, mediaType, content, createdTime, IMessageManager.MessageType.CHANNEL, startCounter = 7, source = "source").joinException() }
    }

    @Test
    fun `test broadcast msgs tree`(){
        val id1 = messageManager.generateUniqueMessageId().join()
        val id2 = messageManager.generateUniqueMessageId().join()
        val id3 = messageManager.generateUniqueMessageId().join()
        val id4 = messageManager.generateUniqueMessageId().join()
        val mediaType = 0L
        val content = "hello".toByteArray()
        val createdTime = LocalDateTime.now()
        val messageType = IMessageManager.MessageType.BROADCAST
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds().size, 0)
        messageManager.addMessage(id1, mediaType, content, createdTime, messageType, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id1))
        messageManager.addMessage(id2, mediaType, content, createdTime, messageType, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id1, id2).sorted())
        messageManager.addMessage(id3, mediaType, content, createdTime, IMessageManager.MessageType.CHANNEL, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id1, id2).sorted())
        messageManager.addMessage(id4, mediaType, content, createdTime, IMessageManager.MessageType.PRIVATE, startCounter = 7, source = "source").join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id1, id2).sorted())
        messageManager.decreaseMessageCounterBy(id1, 7).join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id2))
        messageManager.decreaseMessageCounterBy(id2, 5).join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), listOf(id2))
        messageManager.decreaseMessageCounterBy(id2, 2).join()
        Assertions.assertEquals(messageManager.getAllBroadcastMessageIds(), emptyList<Long>())
    }
}