package il.ac.technion.cs.softwaredesign.internals

import java.util.concurrent.CompletableFuture

interface ISequenceGenerator {
    fun next():CompletableFuture<Long>
}