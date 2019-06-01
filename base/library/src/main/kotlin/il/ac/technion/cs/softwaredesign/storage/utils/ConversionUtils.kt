package il.ac.technion.cs.softwaredesign.storage.utils

import com.google.common.primitives.Longs
import java.util.concurrent.CompletableFuture

object ConversionUtils {

    fun longToBytes(x: CompletableFuture<Long?>): ByteArray = Longs.toByteArray(x.get()!!)

    fun bytesToLong(bytes: CompletableFuture<ByteArray?>): CompletableFuture<Long?> = bytes.thenApply { if(it==null) null else Longs.fromByteArray(it) }
}