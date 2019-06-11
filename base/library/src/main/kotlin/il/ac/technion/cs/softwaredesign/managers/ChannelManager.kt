package il.ac.technion.cs.softwaredesign.managers

import il.ac.technion.cs.softwaredesign.internals.CountIdKey
import il.ac.technion.cs.softwaredesign.internals.ISequenceGenerator
import il.ac.technion.cs.softwaredesign.internals.IdKey
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.api.IChannelManager
import il.ac.technion.cs.softwaredesign.storage.api.IStatisticsManager
import il.ac.technion.cs.softwaredesign.storage.channels.IChannelStorage
import il.ac.technion.cs.softwaredesign.storage.datastructures.SecureAVLTree
import il.ac.technion.cs.softwaredesign.storage.utils.DB_NAMES.TREE_CHANNELS_BY_ACTIVE_USERS_COUNT
import il.ac.technion.cs.softwaredesign.storage.utils.DB_NAMES.TREE_CHANNELS_BY_USERS_COUNT
import il.ac.technion.cs.softwaredesign.storage.utils.DB_NAMES.TREE_CHANNELS_MSG_COUNT
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.CHANNEL_INVALID_ID
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.CHANNEL_INVALID_NAME
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.CHANNEL_NAME_PROPERTY
import io.github.vjames19.futures.jdk8.Future
import io.github.vjames19.futures.jdk8.ImmediateFuture
import io.github.vjames19.futures.jdk8.map
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ChannelManager
@Inject constructor(private val channelStorage: IChannelStorage,
                    private val statisticsManager: IStatisticsManager,
                    @ChannelIdSeqGenerator private val channelIdGenerator: ISequenceGenerator,
                    @ChannelByUserCountStorage private val channelsByUsersCountStorage: SecureStorage,
                    @ChannelByActiveUserCountStorage private val channelsByActiveUsersCountStorage: SecureStorage,
                    @ChannelMembersStorage private val channelMembersStorage: SecureStorage,
                    @ChannelOperatorsStorage private val channelOperatorsStorage: SecureStorage,
                    @ChannelMessagesTreesStorage private val channelMessagesTreesStorage: SecureStorage
) : IChannelManager {
    private val defaultCountIdKey: () -> CountIdKey = { CountIdKey() }
    private val defaultIdKey: () -> IdKey = { IdKey() }
    private val channelsByUsersCountTree = SecureAVLTree(channelsByUsersCountStorage, defaultCountIdKey)
    private val channelsByActiveUsersCountTree = SecureAVLTree(channelsByActiveUsersCountStorage, defaultCountIdKey)
    private val channelsMsgsCountTree = SecureAVLTree(channelMessagesTreesStorage, defaultCountIdKey)

    override fun addChannel(channelName: String): CompletableFuture<Long> {
        return getNextChannelIdFuture(channelName).thenCompose { channelId-> propertiesSetterFuture(channelId,channelName) }
                .thenCompose {
                    addNewChannelToChannelTrees(channelId = it)
                    increaseNumberOfChannelsFuture(it)
                }
    }

    private fun propertiesSetterFuture(channelId: Long, channelName: String): CompletableFuture<Long> {
             // id db
            val channelNameSetterFuture=  channelStorage.setChannelIdToChannelName(channelName, channelId)
            // details db
            val channelIdSetterFuture= channelStorage.setPropertyStringToChannelId(channelId, CHANNEL_NAME_PROPERTY, channelName)
            val channelNumberSetterFuture=channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS, 0L)
            val channelActiveNumberSetterFuture= channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_ACTIVE_MEMBERS, 0L)
            val channelNumberOfMsgsSetterFuture= channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MESSAGES, 0L)
            val channelMemberListFuture= channelStorage.setPropertyListToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_MEMBERS_LIST, emptyList())
            val channelOperatorListFuture= channelStorage.setPropertyListToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_OPERATORS_LIST, emptyList())
            val futures= listOf(channelNameSetterFuture,channelIdSetterFuture,channelNumberSetterFuture,channelActiveNumberSetterFuture,channelNumberOfMsgsSetterFuture,channelMemberListFuture,channelOperatorListFuture)
            return Future.allAsList(futures).thenApply { channelId }
    }


    private fun getNextChannelIdFuture(channelName: String): CompletableFuture<Long> {
        if (channelName == CHANNEL_INVALID_NAME) throw IllegalArgumentException("channel name cannot be empty")
        return isChannelNameExists(channelName).thenCompose {
            if(it) throw IllegalArgumentException("channel name already exist")
            else channelIdGenerator.next()
        }
    }

    override fun removeChannel(channelId: Long): CompletableFuture<Unit> {
        removeChannelFromChannelTrees(channelId)
        invalidateChannelFuture(channelId)
        return statisticsManager.decreaseNumberOfChannelsBy()
    }

    override fun isChannelNameExists(channelName: String): CompletableFuture<Boolean> {
        return isChannelValid(channelName)
    }

    override fun isChannelIdExists(channelId: Long): CompletableFuture<Boolean> {
        return isChannelValid(channelId)
    }

    override fun getChannelIdByName(channelName: String): CompletableFuture<Long> {
        return isChannelValid(channelName = channelName)
                .thenApply { if(!it) throw IllegalArgumentException("channel name is not valid") }
                .thenCompose { channelStorage.getChannelIdByChannelName(channelName) }
                .thenApply { it!! }
    }

    override fun getChannelNameById(channelId: Long): CompletableFuture<String> {
        return validateChannelIdFuture(channelId).thenCompose {
                    channelStorage.getPropertyStringByChannelId(channelId, CHANNEL_NAME_PROPERTY)
                }.thenApply { it!! }
    }

    override fun getNumberOfChannels(): CompletableFuture<Long> {
        return statisticsManager.getNumberOfChannels()
    }


    /** CHANNEL MESSAGES **/
    override fun addMessageToChannel(channelId: Long, msgId: Long): CompletableFuture<Unit> {
        return isChannelIdExists(channelId).thenCompose {
            if (!it) ImmediateFuture{Unit}
            else {
                val channelMessagesTree = SecureAVLTree(channelMessagesTreesStorage, defaultIdKey, channelId)
                val key = IdKey(msgId)
                if (channelMessagesTree[key]!=null) ImmediateFuture{Unit}
                else {
                    //increase number of messages
                    channelMessagesTree.put(key)
                    increaseNumberOfMsgsInChannelByOne(channelId)
                }
            }
        }
    }

    override fun isMessageInChannel(channelId: Long, msgId: Long): CompletableFuture<Boolean> {
        return isChannelIdExists(channelId).thenApply {
            if (!it) throw IllegalArgumentException("channel id does not exist")
            else {
                val channelMessagesTree = SecureAVLTree(channelMessagesTreesStorage, defaultIdKey, channelId)
                channelMessagesTree[IdKey(msgId)] != null
            }
        }
    }

    private fun increaseNumberOfMsgsInChannelByOne(channelId: Long): CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId).thenCompose { getNumberOfMsgsInChannel(channelId) }
                .thenCompose {currentValue->
                    val newValue=1L+currentValue
                    val nextValue= Pair(currentValue, newValue)
                    channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS, newValue).thenApply { nextValue }
                }.thenApply {
                    (currentValue, newValue)->
                    updateKeyInTree(TREE_CHANNELS_MSG_COUNT, channelId, currentValue, newValue)
                }
    }

    override fun getNumberOfMsgsInChannel(channelId: Long): CompletableFuture<Long> {
        return validateChannelIdFuture(channelId).thenCompose {
            channelStorage.getPropertyLongByChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS) }
                .thenApply { it ?: throw IllegalArgumentException("channel id is valid but returned null") }
    }


    /** NUMBER OF ACTIVE MEMBERS **/
    /** this property should be updated regardless members list updates **/
    override fun getNumberOfActiveMembersInChannel(channelId: Long): CompletableFuture<Long> {
        return validateChannelIdFuture(channelId).thenCompose {
                    channelStorage.getPropertyLongByChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_ACTIVE_MEMBERS) }
                .thenApply { it ?: throw IllegalArgumentException("channel id is valid but returned null") }
    }

    override fun increaseNumberOfActiveMembersInChannelBy(channelId: Long, count: Long): CompletableFuture<Unit> {
        return changeNumberOfActiveMembersInChannelBy(channelId, count)
    }

    override fun decreaseNumberOfActiveMembersInChannelBy(channelId: Long, count: Long): CompletableFuture<Unit> {
        return changeNumberOfActiveMembersInChannelBy(channelId, -count)
    }


    /** MEMBERS LIST **/
    override fun getNumberOfMembersInChannel(channelId: Long): CompletableFuture<Long> {
        return validateChannelIdFuture(channelId).thenCompose {
            channelStorage.getPropertyLongByChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS) }
                .thenApply { it ?:  throw IllegalArgumentException("channel id is valid but returned null")}
    }

    override fun getChannelMembersList(channelId: Long): CompletableFuture<List<Long>> {
        return validateChannelIdFuture(channelId).thenApply {
            SecureAVLTree(channelMembersStorage, defaultIdKey, channelId)
                    .keys().map { it.getId() }.toList()
        }
    }

    override fun addMemberToChannel(channelId: Long, memberId: Long) :CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId)
                .thenCompose {
                    val membersTree = SecureAVLTree(channelMembersStorage, defaultIdKey, channelId)
                    if (membersTree.contains(IdKey(memberId))) throw IllegalAccessException("member id already exists in channels list")
                    membersTree.put(IdKey(memberId))
                    channelStorage.getPropertyLongByChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS)
                            .thenCompose { currentSize ->
                                val newSize = currentSize!! + 1L
                                updateKeyInTree(TREE_CHANNELS_BY_USERS_COUNT, channelId, currentSize, newSize)
                                channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS, newSize)
                            }
                }
    }

    override fun removeMemberFromChannel(channelId: Long, memberId: Long) :CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId)
                .thenCompose {
                    val membersTree = SecureAVLTree(channelMembersStorage, defaultIdKey, channelId)
                    if (!membersTree.contains(IdKey(memberId))) throw IllegalAccessException("member id does not exists in channels list")
                    membersTree.delete(IdKey(memberId))
                    channelStorage.getPropertyLongByChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS)
                            .thenCompose { currentSize ->
                                val newSize = currentSize!! - 1L
                                updateKeyInTree(TREE_CHANNELS_BY_USERS_COUNT, channelId, currentSize, newSize)
                                channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS, newSize)
                            }
                }
    }


    /** OPERATORS LIST **/
    override fun getChannelOperatorsList(channelId: Long): CompletableFuture<List<Long>> {
        return validateChannelIdFuture(channelId).thenApply {
            SecureAVLTree(channelOperatorsStorage, defaultIdKey, channelId)
                    .keys().map { it.getId() }.toList()
        }
    }

    override fun addOperatorToChannel(channelId: Long, operatorId: Long) :CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId)
                .thenApply {
                    val membersTree = SecureAVLTree(channelOperatorsStorage, defaultIdKey, channelId)
                    membersTree.put(IdKey(operatorId))
                }
    }

    override fun removeOperatorFromChannel(channelId: Long, operatorId: Long):CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId)
                .thenApply {
                    val membersTree = SecureAVLTree(channelOperatorsStorage, defaultIdKey, channelId)
                    membersTree.delete(IdKey(operatorId))
                }
    }


    /** CHANNEL COMPLEX STATISTICS **/
    override fun getNumberOfTotalChannelMessages() : CompletableFuture<Long> {
        return statisticsManager.getNumberOfChannelMessages()
    }

    override fun getTop10ChannelsByUsersCount(): CompletableFuture<List<String>> {
        return getTop10FromTree(TREE_CHANNELS_BY_USERS_COUNT)
    }

    override fun getTop10ChannelsByActiveUsersCount(): CompletableFuture<List<String>> {
        return getTop10FromTree(TREE_CHANNELS_BY_ACTIVE_USERS_COUNT)
    }

    override fun getTop10ChannelsByMsgsCount(): CompletableFuture<List<String>> {
        return getTop10FromTree(TREE_CHANNELS_MSG_COUNT)
    }

    /** PRIVATES **/
    /**
     * the method validates the channel id and return the channelId for continues use
     */
    private fun validateChannelIdFuture(channelId: Long): CompletableFuture<Long> {
        return isChannelValid(channelId = channelId)
                .thenApply {
                    if (!it) throw IllegalArgumentException("channel id is not valid")
                    else channelId
                }
    }

    // channel name exists if and only if it is mapped to a VALID channel id, i.e. channel id != CHANNEL_INVALID_ID
    // and its id_name is not mapped to CHANNEL_INVALID_NAME
    private fun isChannelValid(channelId: Long?): CompletableFuture<Boolean> {
        if (channelId != null && channelId != CHANNEL_INVALID_ID) {
            return channelStorage.getPropertyStringByChannelId(channelId, CHANNEL_NAME_PROPERTY)
                    .thenApply { name -> name != null && name != CHANNEL_INVALID_NAME }

        }
        return ImmediateFuture { false }
    }

    private fun isChannelValid(channelName: String?): CompletableFuture<Boolean> {

        if (channelName != null && channelName != CHANNEL_INVALID_NAME) {
            return channelStorage.getChannelIdByChannelName(channelName)
                    .thenApply { id -> id != null && id != CHANNEL_INVALID_ID }
        }
        return ImmediateFuture { false }
    }

    private fun addOperatorToChannelListFuture(list: List<Long>, operatorId: Long, channelId: Long): CompletableFuture<Unit> {
        return if (!list.contains(operatorId)) {
            val mutableList = ArrayList<Long>(list)
            mutableList.add(operatorId)
            channelStorage.setPropertyListToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_OPERATORS_LIST, mutableList)
        } else {
            ImmediateFuture { Unit } //operator id already exists in channel
        }
    }

    private fun invalidateChannelFuture(channelId: Long) :CompletableFuture<Unit> {
        return getChannelNameById(channelId)
                .thenCompose { channelName-> invalidateChannelFuture(channelId, channelName) }
                .exceptionally {  }

    }

    private fun invalidateChannelFuture(channelId: Long, channelName: String): CompletableFuture<Unit> {
        val invalidatedIdFuture=channelStorage.setChannelIdToChannelName(channelName, CHANNEL_INVALID_ID)
        val invalidateNameFuture=channelStorage.setPropertyStringToChannelId(channelId, CHANNEL_NAME_PROPERTY, CHANNEL_INVALID_NAME)
        return Future.allAsList(listOf(invalidateNameFuture,invalidatedIdFuture)).thenApply { Unit }
    }

    private fun addNewChannelToChannelTrees(channelId: Long) {
        val key = CountIdKey(count = 0, id = channelId)
        channelsByUsersCountTree.put(key) //TODO: remove future init after tree refactor
        channelsByActiveUsersCountTree.put(key) //TODO: remove future init after tree refactor
    }

    private fun removeChannelFromChannelTrees(channelId: Long): CompletableFuture<Unit> {
        val membersCountFuture = getNumberOfMembersInChannel(channelId)
                .thenApply{
                    val key = CountIdKey(id = channelId, count = it)
                    channelsByUsersCountTree.delete(key) //TODO: remove future init after tree refactor
                }
        val activeMembersCountFuture = getNumberOfActiveMembersInChannel(channelId)
                .thenApply {
                    val key = CountIdKey(id = channelId, count = it)
                    channelsByActiveUsersCountTree.delete(key) //TODO: remove future init after tree refactor
                }
        return Future.allAsList(listOf(membersCountFuture,activeMembersCountFuture)).thenApply { Unit }
    }

    private fun changeNumberOfActiveMembersInChannelBy(channelId: Long, count: Long): CompletableFuture<Unit> {
        return validateChannelIdFuture(channelId).thenCompose { getNumberOfActiveMembersInChannel(channelId) }
                .thenCompose {currentValue->
                    val newValue=count+currentValue
                    val nextValue= Pair(currentValue, newValue)
                    channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_ACTIVE_MEMBERS, newValue).thenApply { nextValue }
                }.thenApply {
                    (currentValue, newValue)->
                    updateKeyInTree(TREE_CHANNELS_BY_ACTIVE_USERS_COUNT, channelId, currentValue, newValue)
                }
    }

    private fun updateKeyInTree(treeName: String, channelId: Long, currentValue: Long, newValue: Long) {
        val tree = getTreeByName(treeName)
        val oldKey = CountIdKey(count = currentValue, id = channelId)
        tree.delete(oldKey)  //TODO: remove Future init after tree refactor
        val newKey = CountIdKey(count = newValue, id = channelId)
        tree.put(newKey) //TODO: remove Future init after tree refactor
    }

    private fun getTop10FromTree(treeName: String): CompletableFuture<List<String>> {
        val tree= getTreeByName(treeName)
        return getNumberOfChannels().thenCompose { buildTop10FutureListFromTree(tree,it) }
    }

    private fun buildTop10FutureListFromTree(tree: SecureAVLTree<CountIdKey>, numberOfChannels: Long): CompletableFuture<List<String>> {
        val higherChannelIndex=numberOfChannels-1
        val lowestChannelIndex= numberOfChannels- min(10, numberOfChannels)
        val mutableList= buildTop10FromHigherToLowerListFromTree(higherChannelIndex,lowestChannelIndex,tree)
        return mutableList.thenApply { it }
    }

    private fun buildTop10FromHigherToLowerListFromTree(higherChannelIndex: Long, lowestChannelIndex: Long, tree: SecureAVLTree<CountIdKey>): CompletableFuture<MutableList<String>> {
        return if(higherChannelIndex<lowestChannelIndex){
            ImmediateFuture{ mutableListOf<String>()}
        }else{
            //TODO: fix after tree refactoring (remove Future init)
            val channelIdFuture = ImmediateFuture {
                tree.select(lowestChannelIndex).getId()
            }
            val channelNameFuture=channelIdFuture.thenCompose { getChannelNameById(it) }
            buildTop10FromHigherToLowerListFromTree(higherChannelIndex,lowestChannelIndex+1,tree)
                    .thenCompose { list-> channelNameFuture.thenApply { name-> list.add(name); list } }
        }
    }

    private fun validateMemberInList(it: List<Long>, memberId: Long): List<Long> {
        if (!it.contains(memberId))
            throw IllegalAccessException("member id does not exists in channel"); return it
    }

    private fun validateMemberNotInList(it: List<Long>, memberId: Long): List<Long> {
        if (it.contains(memberId))
            throw IllegalAccessException("member id already exists in channel"); return it
    }

    private fun removeOperatorFromListFuture(list: List<Long>, operatorId: Long, channelId: Long ): CompletableFuture<Unit> {
        return if (list.contains(operatorId)) {
            val mutableList = ArrayList<Long>(list)
            mutableList.remove(operatorId)
            channelStorage.setPropertyListToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_OPERATORS_LIST, mutableList)
        } else {
            ImmediateFuture { Unit } //operator id already exists in channel
        }
    }

    private fun increaseNumberOfChannelsFuture(id: Long): CompletableFuture<Long> {
        return statisticsManager.increaseNumberOfChannelsBy().thenApply { id }
    }

    /**
     * the method updates the channel member list with the current list, and returns it's size
     */
    private fun updateChannelMemberList(channelId:Long,currentList:List<Long>):CompletableFuture<Long>{
        val futureList=channelStorage.setPropertyListToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_MEMBERS_LIST, currentList)
        val futureListSize=channelStorage.setPropertyLongToChannelId(channelId, MANAGERS_CONSTS.CHANNEL_NR_MEMBERS, currentList.size.toLong())
        return Future.allAsList(listOf(futureList,futureListSize)).map { currentList.size.toLong() }
    }

    private fun getTreeByName(treeName: String): SecureAVLTree<CountIdKey> {
        return when (treeName) {
            TREE_CHANNELS_BY_USERS_COUNT -> channelsByUsersCountTree
            TREE_CHANNELS_BY_ACTIVE_USERS_COUNT -> channelsByActiveUsersCountTree
            TREE_CHANNELS_MSG_COUNT -> channelsMsgsCountTree
            else -> throw IllegalAccessException("tree does not exist, should not get here")
        }
    }
}