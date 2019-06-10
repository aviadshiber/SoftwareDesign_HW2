package il.ac.technion.cs.softwaredesign

import java.util.concurrent.CompletableFuture

interface Observable<S, M> {
    fun listen(listener: (S, M) -> CompletableFuture<Unit>)
    fun unlisten(listener: (S, M) -> CompletableFuture<Unit>)
}