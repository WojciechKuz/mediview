package transform3d

import dicom.tagAsUInt
import dicom.uIntAsTag

fun throwWrongTag(expected: UInt, got: UInt) {
    throw Error("Wrong tag, expected ${uIntAsTag(expected)}, got ${uIntAsTag(got)}")
}
fun tagNotFoundErr(tag: UInt) =
    Error("${uIntAsTag(tag)} tag not found in data map")
fun tagNotFoundErr(tag: String) = tagNotFoundErr(tagAsUInt(tag))