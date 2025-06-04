package dicom
/** get sub-array copy */
operator fun ByteArray.get(a: Int, b: Int): ByteArray = this.copyOfRange(a, b)
/** get sub-array copy */
operator fun ByteArray.get(a: UInt, b: UInt): ByteArray = this[a.toInt(), b.toInt()]

/** get sub-array copy. */
operator fun ByteArray.get(range: IntRange): ByteArray = this.copyOfRange(range.first, range.last)