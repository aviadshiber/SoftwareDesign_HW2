package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.KotlinModule
import com.google.inject.Provides
import com.google.inject.Singleton
import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.managers.*
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import il.ac.technion.cs.softwaredesign.storage.api.*
import il.ac.technion.cs.softwaredesign.storage.channels.IChannelStorage
import il.ac.technion.cs.softwaredesign.storage.channels.SecureChannelStorage
import il.ac.technion.cs.softwaredesign.storage.messages.IMessageStorage
import il.ac.technion.cs.softwaredesign.storage.messages.SecureMessageStorage
import il.ac.technion.cs.softwaredesign.storage.proxies.SecureStorageCache
import il.ac.technion.cs.softwaredesign.storage.statistics.IStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.statistics.SecureStatisticsStorage
import il.ac.technion.cs.softwaredesign.storage.users.IUserStorage
import il.ac.technion.cs.softwaredesign.storage.users.SecureUserStorage
import il.ac.technion.cs.softwaredesign.storage.utils.DB_NAMES
import javax.inject.Inject


class LibraryModule : KotlinModule() {
    override fun configure() {
        bind<IStatisticsStorage>().to<SecureStatisticsStorage>()
        bind<IStatisticsManager>().to<StatisticsManager>()
        bind<IChannelStorage>().to<SecureChannelStorage>()

        //bindInterceptor(createSecureStorageFactoryMatcher(),Matchers.any(), SecureStorageFactoryCacheConcern)
        bind<ISequenceGenerator>().annotatedWith<UserMessageIdSeqGenerator>().to<UserMessageIdGenerator>()
        bind<ISequenceGenerator>().annotatedWith<ChannelIdSeqGenerator>().to<ChannelIdGenerator>()
        bind<IUserStorage>().to<SecureUserStorage>()
        bind<IMessageStorage>().to<SecureMessageStorage>()
        bind<ITokenManager>().to<TokenManager>()
        bind<IUserManager>().to<UserManager>()
        bind<IMessageManager>().to<MessageManager>()
        bind<IChannelManager>().to<ChannelManager>()
    }

    private fun SecureStorage.addCache() :SecureStorage{
        return SecureStorageCache(this)
    }
    @Provides @Singleton @Inject
    @AuthenticationStorage
    fun provideTokenStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TOKEN.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @MemberDetailsStorage
    fun provideUserDetailsStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.USER_DETAILS.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @MemberIdStorage
    fun provideUserIdStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.USER_ID.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @StatisticsStorage
    fun provideStatisticsStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.STATISTICS.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelIdStorage
    fun provideChannelIdStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.CHANNEL_ID.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelDetailsStorage
    fun provideChannelDetailsStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.CHANNEL_DETAILS.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @MessageDetailsStorage
    fun provideMessageDetailsStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.MESSAGE_DETAILS.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelByUserCountStorage
    fun provideChannelByUserCountStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TREE_CHANNELS_BY_USERS_COUNT.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelByActiveUserCountStorage
    fun provideChannelByActiveUserCountStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TREE_CHANNELS_BY_ACTIVE_USERS_COUNT.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @UsersByChannelCountStorage
    fun provideUsersByChannelCountStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TREE_USERS_BY_CHANNELS_COUNT.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @UsersMessagesTreesStorage
    fun provideUsersMessagesTreesStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.USERS_MSGS_TREES .toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelMessagesTreesStorage
    fun provideChannelMessagesTreesStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.CHANNELS_MSGS_TREES.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelMembersStorage
    fun provideChannelMembersStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TREE_CHANNEL_MEMBERS.toByteArray()).get().addCache()
    }

    @Provides @Singleton @Inject
    @ChannelOperatorsStorage
    fun provideChannelOperatorsStorage(factory: SecureStorageFactory): SecureStorage {
        return factory.open(DB_NAMES.TREE_CHANNEL_OPERATORS.toByteArray()).get().addCache()
    }
}