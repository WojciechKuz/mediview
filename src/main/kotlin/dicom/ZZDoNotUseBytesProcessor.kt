package dicom

/*
/** returns number that has given amount of 1's in front and 0's on the back */
@Deprecated("")
fun useBits(nof1: Int, nof0: Int = 0): Int = ( (1 shl (nof1+1)) - 1 ) shl nof0 // nof1 + nof0 <= 32, nof1 < 32

@Deprecated("")
fun getBit(byte: Byte, whichBit: Int): Int = (1 shl whichBit) and byte.toU().toInt()

/** bytes max size is 4 !!! */
@Deprecated("")
fun interpretBytesAsInt(config: BytesConfig, bytes: ByteArray): Int {
    val usedBits = useBits(config.bitsStored, config.skipABit)
    val newInt = endianIntParser(bytes, config.order).toInt()
    return usedBits and newInt
}

// DO NOT USE
@Deprecated("")
fun byteArrayToIntArray(config: BytesConfig, bytes: ByteArray /*, bytesToInt: (ByteArray) -> Int*/): IntArray {
    if(config.bitsAllocated == 1) {
        val ints = IntArray(bytes.size * 8)
        return bytes.mapIndexed { index, b -> getBit(b, index%8) }.toIntArray()
    }

    // bitsAllocated here should be multiple of 8
    val bytesPerPixel = config.bitsAllocated / 8
    val ints = IntArray(bytes.size / bytesPerPixel).mapIndexed { index, _ ->

        // divide ByteArray into bytesPerPixel sized byte arrays
        val onePixelBytes = bytes[index * bytesPerPixel, (index+1) * bytesPerPixel] // 2nd index is exclusive

        // this small byte arrays to ints
        interpretBytesAsInt(config, onePixelBytes)
    }.toIntArray()

    return ints
}

 */