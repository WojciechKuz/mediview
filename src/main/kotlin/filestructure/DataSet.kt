package filestructure

import DataType
import filestructure.groups.DeviceGroup
import filestructure.groups.GroupBase
import filestructure.groups.PixelGroup
import filestructure.groups.StudyGroup

object DataSet: GroupBase() {
    val tagNames: Map<UInt, DataType> = listOf(
        "Instance Number (0020,0013)" * "IS", // image number. unique in directory
        "Pixel Data Element (7FE0,0010)" * "OB",
        +"Last Data element (FFFC,FFFC)",
        "(2020,0110) Basic Grayscale Image Sequence" * "SQ", // Basic Grayscale Image Sequence SQ 1 M/M
        "(2020,0010) Image Position" * "US",
        "(2020,0020) Polarity" * "CS",
        "(FFFE,E000) Seq Item" * "  ",
        "(FFFE,E00D) Seq Item Delimiter" * "  ",
        "(FFFE,E0DD) Seq Delimiter" * "  ",
    ).associateBy { it.tag } + PixelGroup.pixelDataTagNames + StudyGroup.dataTagNames + DeviceGroup.deviceDataTagNames
}