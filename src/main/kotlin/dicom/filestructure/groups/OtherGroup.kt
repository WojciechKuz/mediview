package dicom.filestructure.groups

import dicom.DataType

object OtherGroup: GroupBase() {
    val otherTagNames: Map<UInt, DataType> = listOf(
        +"Red Palette Color Lookup Table Data (0028,1201)" * "OW",
        +"Green Palette Color Lookup Table Data (0028,1202)" * "OW",
        +"Blue Color Palette Lookup Table Data (0028,1203)" * "OW",
        +"Alpha Palette Color Lookup Table Data (0028,1204)" * "OW",
        +"LUT Data (0028,3006)" * "US",
        +"LUT Descriptor (0028,3002)" * "US",
        +"Blending Lookup Table Data (0028,1408)" * "OW",
        +"Track Point Index List (0066,0129)" * "OL",

        +"(0002,0000) File Meta Information Group Length" * "UL",
        +"(0002,0001) File Meta Information Version" * "OB",
        +"(0002,0002) Media Storage SOP Class UID" * "UI",
        +"(0002,0003) Media Storage SOP Instance UID" * "UI",
        +"(0002,0010) Transfer Syntax UID" * "UI",
        +"(0002,0012) Implementation Class UID" * "UI",
        +"(0002,0013) Implementation Version Name" * "SH",
        +"(0018,0050) Slice Thickness" * "DS",
        +"(0018,0090) Data Collection Diameter" * "DS",
        +"(0018,1100) Reconstruction Diameter" * "DS",
        +"(0018,1120) Gantry/Detector Tilt" * "DS",
        +"(0018,1130) Table Height" * "DS",
        +"(0018,1140) Rotation Direction" * "CS",
        +"(0018,1170) Generator Power" * "IS",
        +"(0018,1210) Convolution Kernel" * "SH",
        +"(0018,5100) Patient Position" * "CS",
        +"(0020,1040) Position Reference Indicator" * "LO",
        +"(0020,1041) Slice location" * "DS",

        +"(0008,9215) Sequence" * "SQ",
        +"(0020,0032) Image Position (patient)" * "DS",
        +"(0020,0037) Image Orientation (patient)" * "DS",
    ).associateBy { it.tag }
}
