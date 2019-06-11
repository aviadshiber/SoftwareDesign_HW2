package il.ac.technion.cs.softwaredesign.storage.utils

import com.google.common.primitives.Longs
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ConversionUtils {

    fun longToBytes(x: Long): ByteArray = Longs.toByteArray(x)

    fun bytesToLong(bytes: ByteArray): Long = Longs.fromByteArray(bytes)

    fun createPropertyKey(id: Long, property: String): ByteArray {
        val channelIdByteArray = longToBytes(id)
        val keySuffixByteArray = "${MANAGERS_CONSTS.DELIMITER}$property".toByteArray()
        return channelIdByteArray + keySuffixByteArray
    }

    fun localDateTimeToBytes(localDateTime: LocalDateTime):ByteArray{
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")
        return localDateTime.format(formatter).toByteArray()
    }

    fun byteArrayToLocalDateTime(bytes: ByteArray):LocalDateTime{
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS")
        return LocalDateTime.parse(String(bytes), formatter)
    }
}