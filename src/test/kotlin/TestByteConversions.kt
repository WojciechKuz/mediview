import filestructure.DataSet.tagToPair
import filestructure.Header
import filestructure.informationGroupLength
import java.io.File
import kotlin.test.Test

class TestByteConversions {
    fun strHex(u: UInt, pad: Int = 4): String {
//        if (u != 0u)
//            return "0x" + u.toString(16)
//        return "0x0000"
        return "0x" + u.toString(16).padStart(pad, '0') // same as "0x" + hexString(u)
    }
    @Test
    fun testTagName() {
        val tagName = "Bits Allocated (0028,0100)"
        val pair = tagToPair(tagName)
        println("0x" + strHex(pair.first, 8))
        println(pair.second.description)
        assert(pair.first == 0x00280100u)
        assert(pair.second.description == "Bits Allocated")
    }
    @Test
    fun testEndianness() {
        val bytearr = byteArrayOf(0x10, 0x00, 0x00, 0x00) // 0x10, 0x00 expected to be 0x0010
        val cursor = DicomCursor(bytearr)
        val nxtInt = cursor.readNextInt(2)
        println(strHex(nxtInt))
        assert(nxtInt == 0x0010u)
    }
    @Test
    fun test4Byte() {
        val bytearr = byteArrayOf(0x10, 0x20, 0x30, 0x40) // expected to be 0x40302010
        val cursor = DicomCursor(bytearr)
        val nxtInt = cursor.readNextInt(4)
        println(strHex(nxtInt))
        assert(nxtInt == 0x40302010u)
    }
    @Test
    fun testCharToByte() {
        fun test(c: Char, b: Int) {
            println(charToByte(c).toString(16))
            assert(charToByte(c) == b.toByte())
        }
        test('0', 0x0)
        test('1', 0x1)
        test('2', 0x2)
        test('9', 0x9)
        test('A', 0xA)
        test('F', 0xF)
        test('a', 0xa)
        test('f', 0xf)
    }
    @Test
    fun testTagAsUInt() {
        val strTag = "(0028,0b00)"
        val tagAsU = tagAsUInt(strTag)
        val mTag = mergeUInt(0x0028u, 0x0b00u)
        println(strTag + ": " + strHex(tagAsU, 8) + " should be " + strHex(mTag, 8))
        assert(tagAsU == mTag)
    }
    @Test
    fun mergeUIntTest() {
        val mrg = mergeUInt(0x7FE0u, 0x0010u)
        println("0x" + mrg.toString(16))
        assert(0x7FE00010u == mrg)
    }

    @Test
    fun testUIntConversion() {
        val byte = (196).toByte() // byte 0b11000100 can mean either -60 or 196
        println("To ubyte " + byte.toUByte())
        println("To uint " + byte.toUInt())
        println("To ubyte, then to uint " + byte.toUByte().toUInt())
        println("My toU(), like above. " + byte.toU())
        assert(byte.toU() == 196u)
    }
}