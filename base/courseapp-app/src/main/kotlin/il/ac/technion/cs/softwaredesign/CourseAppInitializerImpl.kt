package il.ac.technion.cs.softwaredesign

import com.google.common.primitives.Longs
import il.ac.technion.cs.softwaredesign.TREE_KEYS.ROOT_INIT_INDEX
import il.ac.technion.cs.softwaredesign.TREE_KEYS.ROOT_KEY
import il.ac.technion.cs.softwaredesign.managers.ChannelByActiveUserCountStorage
import il.ac.technion.cs.softwaredesign.managers.ChannelByUserCountStorage
import il.ac.technion.cs.softwaredesign.managers.StatisticsStorage
import il.ac.technion.cs.softwaredesign.managers.UsersByChannelCountStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import javax.inject.Inject

class CourseAppInitializerImpl
@Inject constructor(@StatisticsStorage private val statisticsStorage: SecureStorage,
                    @UsersByChannelCountStorage private val usersByChannelStorage: SecureStorage,
                    @ChannelByUserCountStorage private val channelByUserCoundStorage: SecureStorage,
                    @ChannelByActiveUserCountStorage private val channelByActiveUserCountStorage: SecureStorage
) : CourseAppInitializer {

    override fun setup() {
        initStatistics()
        initTrees()
    }

    private fun initStatistics() {
        statisticsStorage.write(STATISTICS_KEYS.NUMBER_OF_USERS.toByteArray(), Longs.toByteArray(STATISTICS_KEYS.INIT_INDEX_VAL))
        statisticsStorage.write(STATISTICS_KEYS.NUMBER_OF_LOGGED_IN_USERS.toByteArray(), Longs.toByteArray(STATISTICS_KEYS.INIT_INDEX_VAL))
        statisticsStorage.write(STATISTICS_KEYS.NUMBER_OF_CHANNELS.toByteArray(), Longs.toByteArray(STATISTICS_KEYS.INIT_INDEX_VAL))
        statisticsStorage.write(STATISTICS_KEYS.MAX_CHANNEL_INDEX.toByteArray(), Longs.toByteArray(STATISTICS_KEYS.INIT_INDEX_VAL))

    }

    private fun initTrees() {
        usersByChannelStorage.write(ROOT_KEY.toByteArray(), Longs.toByteArray(ROOT_INIT_INDEX))
        channelByUserCoundStorage.write(ROOT_KEY.toByteArray(), Longs.toByteArray(ROOT_INIT_INDEX))
        channelByActiveUserCountStorage.write(ROOT_KEY.toByteArray(), Longs.toByteArray(ROOT_INIT_INDEX))
    }


}