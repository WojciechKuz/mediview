package filestructure.groups

import DataType
import filestructure.DataSet.tagToPair

object StudyGroup: GroupBase() {
    val dataTagNames: Map<UInt, DataType> = listOf(
        "(0010,0010) Patient's Name" * "PN",
        "(0010,0020) Patient ID" * "LO",
        "(0010,0030) Patient's Birth Date" * "DA",
        "(0010,0032) Patient's Birth Time" * "TM",
        "(0010,0040) Patient's Sex" * "CS",
        "(0010,1000) Other Patient IDs" * "LO",
        "(0010,1001) Other Patient Names" * "PN",
        "(0010,2160) Ethnic Group" * "SH",
        "(0010,4000) Patient Comments" * "LT",

        "(0020,000D) Study Instance UID" * "UI",
        "(0020,0010) Study ID" * "SH",
        "(0008,0020) Study Date" * "DA",
        "(0008,0030) Study Time" * "TM",
        "(0008,0090) Referring Physician's Name" * "PN",
        "(0008,0050) Accession Number" * "SH",
        "(0008,1030) Study Description" * "LO",
        "(0008,1048) Physician(s) of Record" * "PN",
        "(0008,1060) Physician(s) Reading the Study" * "PN",
        "(0032,1033) Requesting Service" * "LO",
    ).associateBy { it.tag }
}
/*
(0010,0010) Patient's Name PN 1 2
(0010,0020) Patient ID LO 1 2
(0010,0030) Patient's Birth Date DA 1 2
(0010,0040) Patient's Sex CS 1 2
(0010,0032) Patient's Birth Time TM 1 3
(0010,1000) Other Patient IDs LO 1 3
(0010,1001) Other Patient Names PN 1 3
(0010,2160) Ethnic Group SH 1 3
(0010,4000) Patient Comments LT 1 3

(0020,000D) Study Instance UID UI 1 1
(0008,0020) Study Date DA 1 2
(0008,0030) Study Time TM 1 2
(0008,0090) Referring Physician's Name PN 1 2
(0020,0010) Study ID SH 1 2
(0008,0050) Accession Number SH 1 2
(0008,1030) Study Description LO 1 3
(0008,1048) Physician(s) of Record PN 1 3
(0008,1060) Physician(s) Reading the Study PN 1 3
(0032,1033) Requesting Service LO 1 3
 */