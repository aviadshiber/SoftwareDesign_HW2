package il.ac.technion.cs.softwaredesign.storage.utils

import com.google.common.primitives.Longs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter



object ConversionUtils {

    fun longToBytes(x: Long): ByteArray = Longs.toByteArray(x)

    fun bytesToLong(bytes: ByteArray): Long = Longs.fromByteArray(bytes)

    fun localDateTimeToBytes(localDateTime: LocalDateTime):ByteArray{
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")
        return localDateTime.format(formatter).toByteArray()
    }

    fun ByteArrayToLocalDateTime(bytes: ByteArray):LocalDateTime{
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")
        return LocalDateTime.parse(String(bytes), formatter)
    }

}