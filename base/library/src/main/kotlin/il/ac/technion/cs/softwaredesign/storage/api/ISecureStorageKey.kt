package il.ac.technion.cs.softwaredesign.storage.api

/**
 * This interface used to define Key operations that going to be used in SecureAvlTree
 * @param Key
 */
interface ISecureStorageKey<Key> :  Comparable<Key> , IStorageConvertable<Key>