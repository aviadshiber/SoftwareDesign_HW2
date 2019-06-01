package il.ac.technion.cs.softwaredesign.storage.utils

import com.google.common.primitives.Longs
import java.util.concurrent.CompletableFuture

object ConversionUtils {

    fun longToBytes(x: Long): ByteArray = Longs.toByteArray(x)

    fun bytesToLong(bytes: ByteArray): Long? = Longs.fromByteArray(bytes)
}