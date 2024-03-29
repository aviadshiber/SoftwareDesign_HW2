@file:JvmName("SecureStorageConstants")
package il.ac.technion.cs.softwaredesign.storage.utils

import il.ac.technion.cs.softwaredesign.storage.utils.STATISTICS_KEYS.INIT_INDEX_VAL

object MANAGERS_CONSTS{
    internal const val DELIMITER = "_"
    const val INVALID_USER_ID=-1L
    const val USERNAME_PROPERTY="username"
    const val PASSWORD_PROPERTY="password"
    const val STATUS_PROPERTY="status"
    const val PRIVILEGE_PROPERTY="privilage"
    const val LAST_READ_MSG_ID_PROPERTY="last_read"

    const val LIST_PROPERTY="list"
    const val SIZE_PROPERTY="size"

    const val CHANNEL_NAME_PROPERTY="channel_name"
    const val CHANNEL_NR_ACTIVE_MEMBERS="nr_active_members"
    const val CHANNEL_NR_MESSAGES="nr_msgs"
    const val CHANNEL_NR_MEMBERS="nr_members"
    const val CHANNEL_MEMBERS_LIST="members_list"
    const val CHANNEL_OPERATORS_LIST="operators_list"
    const val CHANNEL_INVALID_NAME=""
    const val CHANNEL_INVALID_ID=INIT_INDEX_VAL

    const val MESSAGE_INVALID_ID=-1L
    const val MESSAGE_MEDIA_TYPE="media"
    const val MESSAGE_CONTENTS="content"
    const val MESSAGE_CREATED_TIME="created"
    const val MESSAGE_RECEIVED_TIME="received"
    const val MESSAGE_TYPE="type"
    const val MESSAGE_SOURCE="source"
    const val MESSAGE_COUNTER="counter"
    const val MESSAGE_CHANNEL_ID="channelId"
    const val MESSAGE_DEST_USER_ID="destUserId"
}

object TREE_CONST {
    internal const val DELIMITER = "_"
    internal const val SECURE_AVL_STORAGE_NUM_PROPERTIES = 5
    internal const val ROOT_KEY = "root"
    internal const val ROOT_INIT_INDEX=0L
    internal const val LAST_GENERATED_ID="LAST_GENERATED_ID"
}

object DB_NAMES {
    internal const val USER_ID = "USER_ID"
    internal const val TOKEN = "TOKEN"
    internal const val USER_DETAILS = "USER_DETAILS"
    internal const val CHANNEL_ID = "CHANNEL_ID"
    internal const val CHANNEL_DETAILS = "CHANNEL_DETAILS"
    internal const val MESSAGE_DETAILS = "MESSAGE_DETAILS"
    internal const val STATISTICS = "STATISTICS"
    //DATA BASES FOR TREES

    internal const val TREE_USERS_BY_CHANNELS_COUNT="USERS_BY_CHANNELS_COUNT"
    internal const val TREE_CHANNELS_BY_USERS_COUNT="CHANNELS_BY_USERS_COUNT"
    internal const val TREE_CHANNELS_BY_ACTIVE_USERS_COUNT="CHANNELS_BY_ACTIVE_USERS_COUNT"
    internal const val TREE_CHANNELS_MSG_COUNT="CHANNELS_BY_MSG_COUNT"
    internal const val TREE_CHANNEL_MEMBERS="CHANNEL_MEMBERS"
    internal const val TREE_CHANNEL_OPERATORS="CHANNEL_OPERATORS"

    internal const val USERS_MSGS_TREES ="USERS_MSGS_TREES"
    internal const val CHANNELS_MSGS_TREES ="CHANNELS_MSGS_TREES"

}


object STATISTICS_KEYS{
    internal const val USER_MESSAGE_ID="userMessageId"
    internal const val NUMBER_OF_USERS="numberOfUsers"
    internal const val NUMBER_OF_LOGGED_IN_USERS="numberOfLoggenInUsers"
    internal const val NUMBER_OF_CHANNELS="numberOfChannels"
    internal const val NUMBER_OF_CHANNEL_MESSAGES="nrChannelMsgs"
    internal const val NUMBER_OF_PENDING_MESSAGES="nrPendingMsgs"
    internal const val MAX_CHANNEL_INDEX="maxChannelIndex"

    internal const val INIT_INDEX_VAL=0L
}

