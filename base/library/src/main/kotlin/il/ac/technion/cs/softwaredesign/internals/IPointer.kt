package il.ac.technion.cs.softwaredesign.internals

internal interface IPointer : IStorageConvertable<IPointer> {
    fun getAddress() : Long
}