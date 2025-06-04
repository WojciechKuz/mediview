package dicom.filestructure.groups

import dicom.DataType

object DeviceGroup: GroupBase() {
    val deviceDataTagNames: Map<UInt, DataType> = listOf(
        "(0008,0070) Manufacturer" * "LO",
        "(0008,1090) Manufacturer's Model Name" * "LO",
        "(0018,1000) Device Serial Number" * "LO",
        "(0018,1020) Software Version" * "LO",
        "(0018,1200) Date of Last Calibration" * "DA",
        "(0018,1201) Time of Last Calibration" * "TM",
    ).associateBy { it.tag }
}
/*
(0008,0070) Manufacturer LO 1 U/U
(0008,1090) Manufacturer's Model Name LO 1 U/U
(0018,1000) Device Serial Number LO 1 U/U
(0018,1020) Software Version LO 1 U/U
(0018,1200) Date of Last Calibration DA 1-n U/U
(0018,1201) Time of Last Calibration TM 1-n U/U
 */