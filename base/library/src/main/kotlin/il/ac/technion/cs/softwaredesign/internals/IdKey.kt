package il.ac.technion.cs.softwaredesign.internals

import il.ac.technion.cs.softwaredesign.storage.api.ISecureStorageKey
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils
import il.ac.technion.cs.softwaredesign.storage.utils.MANAGERS_CONSTS.MESSAGE_INVALID_ID

/**
 * Key that contains [id] as primary key
 * @property id Long
 * @constructor
 */
class IdKey(private var id: Long = MESSAGE_INVALID_ID) : ISecureStorageKey<IdKey> {
    override fun compareTo(other: IdKey): Int {
        return id.compareTo(other.id)
    }

    override fun toByteArray(): ByteArray {
        return ConversionUtils.longToBytes(id)
    }

    override fun fromByteArray(value: ByteArray) {
        id = ConversionUtils.bytesToLong(value)
    }

    fun getId() : Long = id
}
