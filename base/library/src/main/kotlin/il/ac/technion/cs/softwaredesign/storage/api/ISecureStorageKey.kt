package il.ac.technion.cs.softwaredesign.storage.api

import il.ac.technion.cs.softwaredesign.internals.IStorageConvertable

/**
 * This interface used to define Key operations that that must be comparable and serializable to byteArray
 * @param Key type for the data structure
 */
interface ISecureStorageKey<Key> :  Comparable<Key> , IStorageConvertable<Key>