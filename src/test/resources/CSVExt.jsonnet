local csvFile = std.parseJson(PortX.CSV.readExt(payload, false, "'", "|", "\\", "\\n"));
{
    fName: csvFile[0][0],
    num: csvFile[0][3]
}
