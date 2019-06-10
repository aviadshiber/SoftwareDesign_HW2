package il.ac.technion.cs.softwaredesign.storage.datastructures

import il.ac.technion.cs.softwaredesign.internals.IPointer
import il.ac.technion.cs.softwaredesign.storage.utils.ConversionUtils

/**
 * Pointer is used in SecureAvlTree and contains a Long value
 * @property pointer Long
 */
class Pointer : IPointer {
    override fun getAddress(): Long {
        return pointer
    }

    private var pointer : Long = 0

    //copy ctor
    constructor(p : Long) {
        this.pointer=p
    }

    override fun toByteArray():ByteArray = ConversionUtils.longToBytes(pointer)

    override fun fromByteArray(value:ByteArray) {
        pointer = ConversionUtils.bytesToLong(value)
    }
}