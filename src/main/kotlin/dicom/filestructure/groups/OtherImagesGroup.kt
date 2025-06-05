package dicom.filestructure.groups

import dicom.DataType
import dicom.filestructure.groups.PixelGroup.times
import dicom.filestructure.groups.PixelGroup.unaryPlus

object OtherImagesGroup: GroupBase() {
    val imageTagNames: List<UInt> = listOf(
        +"(0020,0011) Series Number" * "IS",
        +"(0020,0012) Acquisition Number" * "IS",
        +"(0020,0013) Instance Number" * "IS",      // for now ok for ordering images

        +"(0028,0010) Rows" * "US",              // Rows US 1 M/M
        +"(0028,0011) Columns" * "US",           // Columns US 1 M/M

        +"(0028,0030) Pixel Spacing" * "DS",
        +"(0028,0034) Pixel Aspect Ratio" * "IS",
        //
        +"Bits Allocated (0028,0100)" * "US",    // The size of the Pixel Cell, number of allocated bits
        +"Bits Stored (0028,0101)" * "US",       // Total number of allocated bits that will be used to represent a pixel
        +"High Bit (0028,0102)" * "US",
        +"Data Element Pixel Representation (0028,0103)" * "US", // if binary 2's complement integer (u2 encoding) or an unsigned integer
        +"Smallest Image Pixel Value (0028,0106)",
        +"Largest Image Pixel Value (0028,0107)",
        //
        +"\"Window Center\" (0028,1050)" * "DS",
        +"\"Window Width\" (0028,1051)" * "DS",
        +"\"Rescale Intercept\" (0028,1052)" * "DS",
        +"\"Rescale Slope\" (0028,1053)" * "DS",
        +"Rescale Type (0028,1054)" * "LO",
        +"\"Window Center & Width Explanation\" (0028,1055)",
    ).map { dataType -> dataType.tag }
}