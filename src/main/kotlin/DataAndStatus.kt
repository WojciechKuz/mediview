import dicom.DicomDataElement

class DataAndStatus<T>(val data: T, val status: Status) {
    constructor(data: T): this(data, Status())
}

fun DicomDataElement<out Any>.withStatus(status: Status = Status()): DataAndStatus<DicomDataElement<out Any>> = DataAndStatus(this, status)