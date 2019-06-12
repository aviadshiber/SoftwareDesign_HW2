package il.ac.technion.cs.softwaredesign.tests

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import il.ac.technion.cs.softwaredesign.storage.api.IChannelManager
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS
import io.github.vjames19.futures.jdk8.ImmediateFuture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ChannelManagerTest {
    private val injector = Guice.createInjector(LibraryTestModule())

    private val channelManager = injector.getInstance<IChannelManager>()

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
    fun `add new channel does not throws`() {
        channelManager.addChannel("ron").get()
    }

    @Test
    fun `cannot add invalid channel name`() {
        assertThrows<IllegalArgumentException> { channelManager.addChannel(MANAGERS_CONSTS.CHANNEL_INVALID_NAME).joinException() }
    }

    @Test
    fun `add new channel return valid id`() {
        val id = channelManager.addChannel("ron").get()
        assertThat(id, !equalTo(MANAGERS_CONSTS.CHANNEL_INVALID_ID), { "addChannel channel returned invalid id" })
    }

    @Test
    fun `add new channel throws if channel name exists`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.addChannel("ron") }
                    .joinException()
        }
    }

    @Test
    fun `remove channel by name and add it again not throws`() {
        channelManager.addChannel("ron")
                .thenCompose { channelManager.removeChannel(it) }
                .thenCompose { channelManager.addChannel("ron") }
                .get()
    }

    @Test
    fun `isChannelNameExists returned true`() {
        assertThat(
                channelManager.addChannel("ron")
                        .thenCompose { channelManager.isChannelNameExists("ron") }
                        .get(),
                isTrue, { "channel name does not exist" }
        )
    }

    @Test
    fun `isChannelNameExists returned false if no channel added`() {
        assertThat(channelManager.isChannelNameExists("ron").get(), isFalse, { "channel name does not exist" })
    }

    @Test
    fun `isChannelNameExists returned false if channel removed`() {
        assertThat(
                channelManager.addChannel("ron")
                        .thenCompose { channelManager.removeChannel(it) }
                        .thenCompose { channelManager.isChannelNameExists("ron") }
                        .get(),
                isFalse, { "channel name does not exist" }
        )
    }

    @Test
    fun `isChannelNameExists returned false for invalid name`() {
        assertThat(channelManager.isChannelNameExists(MANAGERS_CONSTS.CHANNEL_INVALID_NAME).get(), isFalse,
                { "invalid channel name cannot be exists" })
    }

    @Test
    fun `isChannelIdExists returned true`() {
        assertThat(
                channelManager.addChannel("ron")
                        .thenCompose { channelManager.isChannelIdExists(it) }
                        .get(),
                isTrue, { "channel id does not exist" }
        )
    }

    @Test
    fun `isChannelIdExists returned false if no channel added`() {
        assertThat(channelManager.isChannelIdExists(8L).get(), isFalse, { "channel id does not exist" })
    }

    @Test
    fun `isChannelIdExists returned false if channel removed by id`() {
        assertThat(
                channelManager.addChannel("ron")
                        .thenCompose { channelManager.removeChannel(it); ImmediateFuture { it } }
                        .thenCompose { channelManager.isChannelIdExists(it) }
                        .get(),
                isFalse, { "channel id does not exist" }
        )
    }

    @Test
    fun `isChannelIdExists returned false for invalid id`() {
        assertThat(channelManager.isChannelIdExists(MANAGERS_CONSTS.CHANNEL_INVALID_ID).get(), isFalse,
                { "invalid channel id cannot be exists" })
    }

    @Test
    fun `get id throws for invalid channel name`() {
        assertThrows<IllegalArgumentException> { channelManager.getChannelIdByName(MANAGERS_CONSTS.CHANNEL_INVALID_NAME).joinException() }
    }

    @Test
    fun `get id throws for channel name that does not exist`() {
        assertThrows<IllegalArgumentException> { channelManager.getChannelIdByName("ron").joinException() }
    }

    @Test
    fun `get id returned the given id that returned in add`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        assertThat(channelManager.getChannelIdByName("ron").get(), equalTo(id1), { "ids are not identical" })
        assertThat(channelManager.getChannelIdByName("ben").get(), equalTo(id2), { "ids are not identical" })
        assertThat(channelManager.getChannelIdByName("aviad").get(), equalTo(id3), { "ids are not identical" })
    }

    @Test
    fun `get name throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron").thenCompose {
                channelManager.getChannelNameById(MANAGERS_CONSTS.CHANNEL_INVALID_ID)
            }.joinException()
        }
    }

    @Test
    fun `get name throws for channel id that does not exist`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron").thenCompose {
                channelManager.getChannelNameById(2000L)
            }.joinException()
        }
    }

    @Test
    fun `get name returned the right name`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        assertThat(channelManager.getChannelNameById(id1).get(), equalTo("ron"), { "names are not identical" })
        assertThat(channelManager.getChannelNameById(id2).get(), equalTo("ben"), { "names are not identical" })
        assertThat(channelManager.getChannelNameById(id3).get(), equalTo("aviad"), { "names are not identical" })
    }

    @Test
    fun `getNumberOfActiveMembers returned 0 after init`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
    }

    @Test
    fun `getNumberOfActiveMembers throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron").thenCompose {
                channelManager.getNumberOfActiveMembersInChannel(MANAGERS_CONSTS.CHANNEL_INVALID_ID)
            }.joinException()
        }
    }

    @Test
    fun `getNumberOfActiveMembers throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron").thenCompose {
                channelManager.getNumberOfActiveMembersInChannel(it + 1L)
            }.joinException()
        }
    }

    @Test
    fun `getNumberOfActiveMembers throws for removed channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.removeChannel(it); ImmediateFuture { it } }
                    .thenCompose { channelManager.getNumberOfActiveMembersInChannel(it) }
                    .joinException()
        }
    }

    @Test
    fun `getNumberOfMembers returned 0 after init`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfMembersInChannel(id3).get(), equalTo(0L))
    }

    @Test
    fun `getNumberOfMembers throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getNumberOfMembersInChannel(MANAGERS_CONSTS.CHANNEL_INVALID_ID) }
                    .joinException()
        }
    }

    @Test
    fun `getNumberOfMembers throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getNumberOfMembersInChannel(it + 1L) }
                    .joinException()
        }
    }

    @Test
    fun `getNumberOfMembers throws for removed channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.removeChannel(it); ImmediateFuture { it } }
                    .thenCompose { channelManager.getNumberOfMembersInChannel(it) }
                    .joinException()
        }
    }

    @Test
    fun `inc_dec NumberOfActiveMembers throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.increaseNumberOfActiveMembersInChannelBy(MANAGERS_CONSTS.CHANNEL_INVALID_ID, 6L) }
                    .joinException()
        }
        assertThrows<IllegalArgumentException> { channelManager.decreaseNumberOfActiveMembersInChannelBy(MANAGERS_CONSTS.CHANNEL_INVALID_ID, 6L).joinException() }
    }

    @Test
    fun `inc_dec NumberOfActiveMembers throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.increaseNumberOfActiveMembersInChannelBy(it + 1L, 8L) }
                    .joinException()
        }
        assertThrows<IllegalArgumentException> { channelManager.decreaseNumberOfActiveMembersInChannelBy(MANAGERS_CONSTS.CHANNEL_INVALID_ID, 6L).joinException() }
    }

    @Test
    fun `updateNumberOfActiveMembers update value`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        channelManager.increaseNumberOfActiveMembersInChannelBy(id1, 8L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(8L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.increaseNumberOfActiveMembersInChannelBy(id1, 12L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(20L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.increaseNumberOfActiveMembersInChannelBy(id2, 18L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(20L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(18L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
    }

    @Test
    fun `updateNumberOfMembers update value`() {
        val id1 = channelManager.addChannel("ron").get()
        val id2 = channelManager.addChannel("ben").get()
        val id3 = channelManager.addChannel("aviad").get()
        channelManager.increaseNumberOfActiveMembersInChannelBy(id1, 8L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(8L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.increaseNumberOfActiveMembersInChannelBy(id1, 12L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(20L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.increaseNumberOfActiveMembersInChannelBy(id2, 18L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(20L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(18L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))

        channelManager.decreaseNumberOfActiveMembersInChannelBy(id1, 3L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(17L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(18L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id1, 5L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(12L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(18L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id2, 18L).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id1).get(), equalTo(12L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id2).get(), equalTo(0L))
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id3).get(), equalTo(0L))
    }

    @Test
    fun `getMembersList throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getChannelMembersList(MANAGERS_CONSTS.CHANNEL_INVALID_ID) }
                    .joinException()
        }
    }

    @Test
    fun `getMembersList throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getChannelMembersList(it + 1L) }
                    .joinException()
        }
    }

    @Test
    fun `getMembersList throws for removed channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.removeChannel(it); ImmediateFuture { it } }
                    .thenCompose { channelManager.getChannelMembersList(it) }
                    .joinException()
        }
    }

    @Test
    fun `getOperatorsList throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getChannelOperatorsList(MANAGERS_CONSTS.CHANNEL_INVALID_ID) }
                    .joinException()
        }
    }

    @Test
    fun `getOperatorsList throws for invalid channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.getChannelOperatorsList(it + 1L) }
                    .joinException()
        }
    }

    @Test
    fun `getOperatorsList throws for removed channel id`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.removeChannel(it); ImmediateFuture { it } }
                    .thenCompose { channelManager.getChannelOperatorsList(it) }
                    .joinException()
        }
    }

    @Test
    fun `add&remove MemberToChannel throws for CHANNEL_INVALID_ID`() {
        assertThrows<IllegalArgumentException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.addMemberToChannel(MANAGERS_CONSTS.CHANNEL_INVALID_ID, 2L) }
                    .joinException()
        }
        assertThrows<IllegalArgumentException> { channelManager.removeMemberFromChannel(MANAGERS_CONSTS.CHANNEL_INVALID_ID, 2L).joinException() }
    }

    @Test
    fun `add&remove MemberToChannel throws for invalid channel id`() {
        val id = channelManager.addChannel("ron").get()
        assertThrows<IllegalArgumentException> { channelManager.addMemberToChannel(id + 1L, 2L).joinException() }
        assertThrows<IllegalArgumentException> { channelManager.removeMemberFromChannel(id + 1L, 8L).joinException() }
    }

    @Test
    fun `add&remove MemberToChannel throws for removed channel id`() {
        val id = channelManager.addChannel("ron").get()
        channelManager.removeChannel(id).get()
        assertThrows<IllegalArgumentException> { channelManager.addMemberToChannel(id, 20L).joinException() }
        assertThrows<IllegalArgumentException> { channelManager.removeMemberFromChannel(id, 7000L).joinException() }
    }

    @Test
    fun `add&remove MemberToChannel throws for channel id that does not exist`() {
        val id = channelManager.addChannel("ron").get()
        channelManager.removeChannel(id).get()
        assertThrows<IllegalArgumentException> { channelManager.addMemberToChannel(id, 20L).joinException() }
        assertThrows<IllegalArgumentException> { channelManager.removeMemberFromChannel(id, 7000L).joinException() }
    }

    @Test
    fun `addMemberToChannel throws if element exists`() {
        assertThrows<IllegalAccessException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.addMemberToChannel(it, 20L); ImmediateFuture { it } }
                    .thenCompose { channelManager.addOperatorToChannel(it, 7000L); ImmediateFuture { it } }
                    .thenCompose { channelManager.addMemberToChannel(it, 20L) }
                    .joinException()
        }
    }

    @Test
    fun `removeMemberFromChannel throws if element does exists`() {
        assertThrows<IllegalAccessException> {
            channelManager.addChannel("ron")
                    .thenCompose { channelManager.removeMemberFromChannel(it, 20L) }
                    .joinException()
        }
    }

    @Test
    fun `getMembers&operators return empty list after init`() {
        val id = channelManager.addChannel("ron").get()
        assertThat(channelManager.getChannelMembersList(id).get(), equalTo(emptyList()))
        assertThat(channelManager.getChannelOperatorsList(id).get(), equalTo(emptyList()))
    }

    @Test
    fun `getMembers&operators return full list`() {
        val id = channelManager.addChannel("ron").get()

        val members = listOf<Long>(20L, 8000L, 500L, 4747L)
        val operators = listOf<Long>(2066L, 8040L, 5011L, 47337L)

        members.forEach({ channelManager.addMemberToChannel(id, it).get() })
        operators.forEach({ channelManager.addOperatorToChannel(id, it).get() })

        assertThat(channelManager.getChannelMembersList(id).get(), equalTo(members.sorted()))
        assertThat(channelManager.getChannelOperatorsList(id).get(), equalTo(operators.sorted()))
    }

    @Test
    fun `removeMembers&operators removes element`() {
        val id = channelManager.addChannel("ron").get()

        val members = mutableListOf<Long>(20L, 8000L, 500L, 4747L)
        val operators = mutableListOf<Long>(2066L, 8040L, 5011L, 47337L)

        members.forEach({ channelManager.addMemberToChannel(id, it).get() })
        operators.forEach({ channelManager.addOperatorToChannel(id, it).get() })

        channelManager.removeMemberFromChannel(id, members[2]).get()
        channelManager.removeOperatorFromChannel(id, operators[2]).get()

        members.removeAt(2)
        operators.removeAt(2)
        assertThat(channelManager.getChannelMembersList(id).get(), equalTo(members.sorted()))
        assertThat(channelManager.getChannelOperatorsList(id).get(), equalTo(operators.sorted()))
    }

    @Test
    fun `removeMembers&operators makes list empty`() {
        val id = channelManager.addChannel("ron").get()

        val members = mutableListOf<Long>(20L, 8000L, 500L, 4747L)
        val operators = mutableListOf<Long>(2066L, 8040L, 5011L, 47337L)

        members.forEach({ channelManager.addMemberToChannel(id, it).get() })
        operators.forEach({ channelManager.addOperatorToChannel(id, it).get() })

        members.forEach({ channelManager.removeMemberFromChannel(id, it).get() })
        operators.forEach({ channelManager.removeOperatorFromChannel(id, it).get() })

        assertThat(channelManager.getChannelMembersList(id).get(), equalTo(emptyList()))
        assertThat(channelManager.getChannelOperatorsList(id).get(), equalTo(emptyList()))
    }

    @Test
    fun `getNumberOfChannels update after add`() {
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(0L))
        channelManager.addChannel("ron").get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(1L))
        channelManager.addChannel("ron1").get()
        channelManager.addChannel("ron2").get()
        channelManager.addChannel("ron3").get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(4L))
    }

    @Test
    fun `getNumberOfChannels update after remove`() {
        val id0 = channelManager.addChannel("ron").get()
        val id1 = channelManager.addChannel("ron1").get()
        val id2 = channelManager.addChannel("ron2").get()
        var id3 = channelManager.addChannel("ron3").get()
        channelManager.removeChannel(id3).get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(3L))
        channelManager.removeChannel(id0).get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(2L))
        channelManager.removeChannel(id1).get()
        channelManager.removeChannel(id2).get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(0L))
        id3 = channelManager.addChannel("ron3").get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(1L))
        channelManager.removeChannel(id3).get()
        assertThat(channelManager.getNumberOfChannels().get(), equalTo(0L))
    }

    @Test
    fun `increase decrease number of active members`() {
        val id0 = channelManager.addChannel("ron").get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id0).get(), equalTo(0L))
        channelManager.increaseNumberOfActiveMembersInChannelBy(id0).get()
        channelManager.increaseNumberOfActiveMembersInChannelBy(id0).get()
        channelManager.increaseNumberOfActiveMembersInChannelBy(id0).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id0).get(), equalTo(3L))
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id0).get()
        channelManager.increaseNumberOfActiveMembersInChannelBy(id0).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id0).get(), equalTo(3L))
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id0).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id0).get(), equalTo(2L))
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id0).get()
        channelManager.decreaseNumberOfActiveMembersInChannelBy(id0).get()
        assertThat(channelManager.getNumberOfActiveMembersInChannel(id0).get(), equalTo(0L))
    }

    @Test
    fun `after removing channel, list size has changed`() {
        val id1 = channelManager.addChannel("ron").get()
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(0L))
        channelManager.addMemberToChannel(id1, 123L).get()
        channelManager.addMemberToChannel(id1, 128L).get()
        channelManager.addMemberToChannel(id1, 129L).get()
        channelManager.removeMemberFromChannel(id1, 123L).get()
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(2L))
        assertThat(channelManager.getChannelMembersList(id1).get().size.toLong(), equalTo(channelManager.getNumberOfMembersInChannel(id1).get()))
    }

    @Test
    fun `add the same element twice throws and list size is valid`() {
        val id1 = channelManager.addChannel("ron").get()
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(0L))
        channelManager.addMemberToChannel(id1, 123L).get()
        assertThrows<IllegalAccessException> { channelManager.addMemberToChannel(id1, 123L).joinException() }
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(1L))
        assertThat(channelManager.getChannelMembersList(id1).get().size.toLong(), equalTo(channelManager.getNumberOfMembersInChannel(id1).get()))
    }

    @Test
    fun `remove the same element twice throws and list size is valid`() {
        val id1 = channelManager.addChannel("ron").get()
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(0L))
        assertThat(channelManager.getChannelMembersList(id1).get().size.toLong(), equalTo(channelManager.getNumberOfMembersInChannel(id1).get()))
        channelManager.addMemberToChannel(id1, 123L).get()
        channelManager.removeMemberFromChannel(id1, 123L).get()
        assertThrows<IllegalAccessException> { channelManager.removeMemberFromChannel(id1, 123L).joinException() }
        assertThat(channelManager.getNumberOfMembersInChannel(id1).get(), equalTo(0L))
        assertThat(channelManager.getChannelMembersList(id1).get().size.toLong(), equalTo(channelManager.getNumberOfMembersInChannel(id1).get()))
    }

    @Test
    fun `test get top 10`() {
        val ids = (0..40).map { channelManager.addChannel(it.toString()).get() }
        ids.forEach { channelManager.addMemberToChannel(it, it * 100).get() }

        val best = mutableListOf<Long>(ids[14], ids[37], ids[5], ids[7], ids[20], ids[12], ids[18], ids[33], ids[8], ids[0])
        (5000..5028).forEach { channelManager.addMemberToChannel(best[0], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[0]).get(), equalTo(30L))
        (5000..5022).forEach { channelManager.addMemberToChannel(best[1], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[1]).get(), equalTo(24L))
        (5000..5020).forEach { channelManager.addMemberToChannel(best[2], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[2]).get(), equalTo(22L))
        (5000..5019).forEach { channelManager.addMemberToChannel(best[3], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[3]).get(), equalTo(21L))
        (5000..5017).forEach { channelManager.addMemberToChannel(best[4], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[4]).get(), equalTo(19L))
        (5000..5012).forEach { channelManager.addMemberToChannel(best[5], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[5]).get(), equalTo(14L))
        (5000..5009).forEach { channelManager.addMemberToChannel(best[6], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[6]).get(), equalTo(11L))
        (5000..5006).forEach { channelManager.addMemberToChannel(best[7], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[7]).get(), equalTo(8L))
        (5000..5004).forEach { channelManager.addMemberToChannel(best[8], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[8]).get(), equalTo(6L))
        (5000..5001).forEach { channelManager.addMemberToChannel(best[9], it.toLong()).get() }
        assertThat(channelManager.getNumberOfMembersInChannel(best[9]).get(), equalTo(3L))

        val output = channelManager.getTop10ChannelsByUsersCount().get()

        for ((k, username) in output.withIndex()) {
            assertThat(channelManager.getChannelIdByName(username).get(), equalTo(best[k]))
        }
    }

    @Test
    fun `test get top 7`() {
        val ids = (0..6).map { channelManager.addChannel(it.toString()).get() }
        ids.forEach { channelManager.addMemberToChannel(it, it * 100).get() }

        val best = mutableListOf<Long>(ids[2], ids[5], ids[0], ids[4], ids[3], ids[1], ids[6])
        (5000..5028).forEach { channelManager.addMemberToChannel(best[0], it.toLong()).get() }
        (5000..5022).forEach { channelManager.addMemberToChannel(best[1], it.toLong()).get() }
        (5000..5020).forEach { channelManager.addMemberToChannel(best[2], it.toLong()).get() }
        (5000..5019).forEach { channelManager.addMemberToChannel(best[3], it.toLong()).get() }
        (5000..5017).forEach { channelManager.addMemberToChannel(best[4], it.toLong()).get() }

        (5000..5012).forEach { channelManager.addMemberToChannel(best[5], it.toLong()).get() }
        (5000..5012).forEach { channelManager.addMemberToChannel(best[6], it.toLong()).get() }

        val output = channelManager.getTop10ChannelsByUsersCount().get()
        val outputIds = output.map { channelManager.getChannelIdByName(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(best[k]))
        }
    }

    @Test
    fun `check secondary order`() {
        val ids = (0..6).map { channelManager.addChannel(it.toString()).get() }
        ids.forEach { channelManager.addMemberToChannel(it, it * 100).get() }

        val best = mutableListOf<Long>(ids[2], ids[5], ids[0], ids[3], ids[4], ids[1], ids[6])
        (5000..5028).forEach { channelManager.addMemberToChannel(best[0], it.toLong()).get() }
        (5000..5022).forEach { channelManager.addMemberToChannel(best[1], it.toLong()).get() }
        (5000..5020).forEach { channelManager.addMemberToChannel(best[2], it.toLong()).get() }

        (5000..5017).forEach { channelManager.addMemberToChannel(best[3], it.toLong()).get() }
        (5000..5017).forEach { channelManager.addMemberToChannel(best[4], it.toLong()).get() }

        (5000..5012).forEach { channelManager.addMemberToChannel(best[5], it.toLong()).get() }
        (5000..5012).forEach { channelManager.addMemberToChannel(best[6], it.toLong()).get() }

        val output = channelManager.getTop10ChannelsByUsersCount().get()
        val outputIds = output.map { channelManager.getChannelIdByName(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(best[k]))
        }
    }

    @Test
    fun `check secondary order only`() {
        val ids = (0..30).map { channelManager.addChannel(it.toString()).get() }
        ids.forEach { channelManager.addMemberToChannel(it, it * 100).get() }

        val output = channelManager.getTop10ChannelsByUsersCount().get()
        val outputIds = output.map { channelManager.getChannelIdByName(it).get() }
        for ((k, userId) in outputIds.withIndex()) {
            assertThat(userId, equalTo(ids[k]))
        }
    }

    @Test
    fun `add message to channel`() {
        val channelname="#chanelname"
        val msgs = listOf<Long>(123L, 15L, 288L, 45L)

        assertThat(
                channelManager.addChannel(channelname)
                        .thenCompose { channelId -> channelManager.addMessageToChannel(channelId, msgs[0]).thenApply{ channelId } }
                        .thenCompose { channelId -> channelManager.addMessageToChannel(channelId, msgs[1]).thenApply{ channelId } }
                        .thenCompose { channelId -> channelManager.addMessageToChannel(channelId, msgs[2]).thenApply{ channelId } }
                        .thenCompose { channelId -> channelManager.addMessageToChannel(channelId, msgs[3]).thenApply{ channelId } }
                        .thenCompose { channelId -> channelManager.isMessageInChannel(channelId, msgs[2]) }
                        .join(),
                isTrue
        )

        val id = channelManager.addChannel(channelname+channelname).get()
        assertThat(channelManager.getNumberOfMsgsInChannel(id).join(), equalTo(0L))
        assertThat(channelManager.isMessageInChannel(id, msgs[0]).join(), isFalse)
        assertThat(channelManager.isMessageInChannel(id, msgs[1]).join(), isFalse)
        assertThat(channelManager.isMessageInChannel(id, msgs[2]).join(), isFalse)
        assertThat(channelManager.isMessageInChannel(id, msgs[3]).join(), isFalse)
        assertThat(channelManager.getNumberOfMsgsInChannel(id).join(), equalTo(0L))

        channelManager.addMessageToChannel(id, msgs[0])
            .thenCompose { channelManager.addMessageToChannel(id, msgs[1])}
            .thenCompose { channelManager.addMessageToChannel(id, msgs[2])}
            .thenCompose { channelManager.addMessageToChannel(id, msgs[3])}
            .thenCompose { channelManager.isMessageInChannel(id, msgs[0]) }
            .join()

        assertThat(channelManager.getNumberOfMsgsInChannel(id).join(), equalTo(4L))

        assertThat(channelManager.isMessageInChannel(id, msgs[0]).join(), isTrue)
        assertThat(channelManager.isMessageInChannel(id, msgs[1]).join(), isTrue)
        assertThat(channelManager.isMessageInChannel(id, msgs[2]).join(), isTrue)
        assertThat(channelManager.isMessageInChannel(id, msgs[3]).join(), isTrue)


        // channel does not exist
        assertThrows<IllegalArgumentException> { channelManager.isMessageInChannel(id*100L, msgs[0]).joinException() }
        assertThrows<IllegalArgumentException> { channelManager.getNumberOfMsgsInChannel(id*100L).joinException() }
    }

    @Test
    fun `check channels by msg count tree`() {
        val channelname="#chanelname"
        val msgs = listOf<Long>(123L, 15L, 288L, 45L, 483L)
        val expected = listOf<String>(channelname, channelname+channelname)

        val id1 = channelManager.addChannel(expected[0]).join()
        channelManager.addMessageToChannel(id1, msgs[0])
            .thenCompose { channelManager.addMessageToChannel(id1, msgs[1])}
            .thenCompose { channelManager.addMessageToChannel(id1, msgs[2])}
            .thenCompose { channelManager.addMessageToChannel(id1, msgs[3])}
            .thenCompose { channelManager.isMessageInChannel(id1, msgs[2]) }
            .join()

        val id2 = channelManager.addChannel(expected[1]).join()
        channelManager.addMessageToChannel(id2, msgs[0])
                .thenCompose { channelManager.addMessageToChannel(id2, msgs[1])}
                .thenCompose { channelManager.addMessageToChannel(id2, msgs[2])}
                .thenCompose { channelManager.isMessageInChannel(id2, msgs[2]) }
                .join()

        var output = channelManager.getTop10ChannelsByMsgsCount().join()
        assertThat(output[0], equalTo(expected[0]))
        assertThat(output[1], equalTo(expected[1]))

        channelManager.addMessageToChannel(id2, msgs[0])
                .thenCompose { channelManager.addMessageToChannel(id2, msgs[1])}
                .thenCompose { channelManager.addMessageToChannel(id2, msgs[2])}
                .thenCompose { channelManager.isMessageInChannel(id2, msgs[2]) }
                .join()

        output = channelManager.getTop10ChannelsByMsgsCount().join()
        assertThat(output[0], equalTo(expected[0]))
        assertThat(output[1], equalTo(expected[1]))

        channelManager.addMessageToChannel(id2, msgs[3])

        output = channelManager.getTop10ChannelsByMsgsCount().join()
        assertThat(output[0], equalTo(expected[0]))
        assertThat(output[1], equalTo(expected[1]))

        channelManager.addMessageToChannel(id2, msgs[4])

        output = channelManager.getTop10ChannelsByMsgsCount().join()
        assertThat(output[0], equalTo(expected[1]))
        assertThat(output[1], equalTo(expected[0]))

        channelManager.addMessageToChannel(id1, msgs[4])

        output = channelManager.getTop10ChannelsByMsgsCount().join()
        assertThat(output[0], equalTo(expected[0]))
        assertThat(output[1], equalTo(expected[1]))
    }
}