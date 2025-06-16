package transform3d

import dicom.DicomCursor
import dicom.endianIntParser
import dicom.toU
import java.nio.ByteOrder

class BytesConfig(val bitsAllocated: Int, val bitsStored: Int, val highBit: Int, val signed: Boolean = false) {
    companion object {
        // this can't be actual constructor as uint and int are the same on JVM, and both constructors would have same signature
        /** Alternative constructor */
        fun constructor(bitsAllocated: UInt, bitsStored: UInt, highBit: UInt, signed: Boolean = false) =
                BytesConfig(bitsAllocated.toInt(), bitsStored.toInt(), highBit.toInt(), signed)

        /** returns number that has given amount of 1's in front and 0's on the back */
        private fun useBits(nof1: Int, nof0: Int = 0): Int = ( (1 shl (nof1+1)) - 1 ) shl nof0 // nof1 + nof0 <= 32, nof1 < 32

        /*var said = 0
        fun sayOnce(say: () -> Unit) {
            if(said == 0) {
                say()
                said++
            }
        }*/
    }

    override fun toString(): String {
        return "BytesConfig: allocated $bitsAllocated, stored $bitsStored, high bit $highBit, sign? $signed."
    }

    val order: ByteOrder
        get() = ByteOrder.LITTLE_ENDIAN
        /*get() {
            if(highBit >= bitsStored - 1) {
                return ByteOrder.LITTLE_ENDIAN
            }
            if(highBit <= bitsNotUsed) {
                return ByteOrder.BIG_ENDIAN
            }
            // else WTF - highBit seems to be in the middle of bitsStored
            return ByteOrder.LITTLE_ENDIAN
        }*/

    val bitsNotUsed: Int = (bitsAllocated - bitsStored)

    /** number of not used bytes at the back of allocated bits */
    val skipABit: Int
        get() {
            if(highBit >= bitsStored - 1) {
                // the empty space is in the back
                return bitsAllocated - 1 - highBit
            }
            if(highBit <= bitsNotUsed) {
                // the empty space is in the front
                return highBit + bitsStored
            }
            // else WTF - highBit seems to be in the middle of bitsStored
            return 0
        }

    /** Reads value to int as configured in BytesConfig. */
    fun getValue(upTo4Bytes: ByteArray): Short {
        // .toInt() does not change bin representation
        val bytesAsInt = endianIntParser(upTo4Bytes, order).toInt()
        val signPosition = useBits(0, highBit) // sign is at high bit
        if(signed) {
            // signed case (+ -), complement's twos code (U2)
            val isNegative = (bytesAsInt and signPosition) != 0
            val newSign = if(isNegative) 1 shl 31 else 0 // sign at Int's sign position
            val valueBits = bytesAsInt and useBits(bitsStored - 1, skipABit) // -1 because without sign
            return (newSign or valueBits).toShort()
        }
        else {
            // unsigned case (alw +)
            val uint = bytesAsInt and useBits(bitsStored, skipABit)
            return uint.toShort()
        }
    }
}

/** ByteArray -> ShortArray as configured in BytesConfig */
fun pixelsToShort(bytes: ByteArray, config: BytesConfig): ShortArray {
    if (config.bitsAllocated == 1) {
        return bytes.readAll1BitValues()
    }
    else {
        return bytes.readAllByteValues(config)
    }
}

/** Can't process 1 bit values!!!
 * ByteArray -> ShortArray as configured in BytesConfig */
private fun ByteArray.oldReadAllByteValues(config: BytesConfig): ShortArray {
    val cursor = DicomCursor(this)
    val byteLength = config.bitsAllocated / 8
    val shArr = ShortArray(this.size / byteLength)
    /*BytesConfig.sayOnce {
        println(config)
        println("Meanwhile, BYTE length is $byteLength")
        println("${this.size} long byte array will become ${shArr.size} long short array")
    }*/
    var arrIndex = 0
    while(cursor.hasNext(byteLength)) {
        shArr[arrIndex] = config.getValue(cursor.readNextByteField(byteLength))
        arrIndex++
    }
    return shArr
}

/** Can't process 1 bit values!!!
 * ByteArray -> ShortArray as configured in BytesConfig */
private fun ByteArray.readAllByteValues(config: BytesConfig): ShortArray {
    val cursor = DicomCursor(this)
    val byteLength = config.bitsAllocated / 8
    val shArr = ShortArray(this.size / byteLength) { arrIndex ->
        if(cursor.hasNext(byteLength))
            config.getValue(cursor.readNextByteField(byteLength))
        else 0
    }
    /*BytesConfig.sayOnce {
        println(config)
        println("Meanwhile, BYTE length is $byteLength")
        println("${this.size} long byte array will become ${shArr.size} long short array")
    }*/
    return shArr
}
/** Processes only 1 bit values!!!
 * ByteArray bits -> ShortArray */
private fun ByteArray.readAll1BitValues(): ShortArray {
    fun Byte.toBitArray(): List<Short> {
        fun getBit(byte: Byte, whichBit: Int): Int = if(
            (  (1 shl whichBit) and byte.toU().toInt()  ) != 0
        ) 1 else 0
        return List<Short>(8) { index -> getBit(this, index).toShort() }
    }

    return this.flatMap { b -> b.toBitArray() }.toShortArray()
}