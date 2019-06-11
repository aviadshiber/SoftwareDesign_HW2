package il.ac.technion.cs.softwaredesign.internals

/**
 * This interface defines an object that can be converted into byte array and vice versa
 * @param T
 */
interface IStorageConvertable<T> {
    /**
     * return ByteArray representation of current object
     * @return ByteArray
     */
    fun toByteArray():ByteArray

    /**
     * transform this object into a new one according the accepted ByteArray
     * @param value ByteArray
     */
    fun fromByteArray(value:ByteArray)
}