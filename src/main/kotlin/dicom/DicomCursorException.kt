package dicom
/** Throw when file is too short to read some value.
 * @param what What was supposed to be read.
 * Example: "File is too short to read **preamble**." */
class DicomCursorException(what: String): Exception(
    "File is too short to read $what."
)