package it.matteoleggio.osustalker.Logic

val osuMods: MutableMap<String, Int> = LinkedHashMap()

fun parseMods(bitVal: String): String? {
    osuMods["NF"] = 1
    osuMods["EZ"] = 2
    osuMods["HD"] = 8
    osuMods["HR"] = 16
    osuMods["SD"] = 32
    osuMods["DT"] = 64
    osuMods["RL"] = 128
    osuMods["HT"] = 256
    osuMods["NC"] = 512
    osuMods["FL"] = 1024
    osuMods["AT"] = 2048
    osuMods["SO"] = 4096
    osuMods["AP"] = 8192
    osuMods["PF"] = 16384
    osuMods["K4"] = 32768
    osuMods["K5"] = 65536
    osuMods["K6"] = 131072
    osuMods["K7"] = 262144
    osuMods["K8"] = 524288
    osuMods["FadeIn"] = 1048576
    osuMods["Random"] = 2097152
    osuMods["Cinema"] = 4194304
    osuMods["Target"] = 8388608
    osuMods["K9"] = 16777216
    osuMods["KC"] = 33554432
    osuMods["K1"] = 67108864
    osuMods["K3"] = 134217728
    osuMods["K2"] = 268435456
    osuMods["V2"] = 536870912
    osuMods["Mirror"] = 1073741824

    var mods = ""
    var i = bitVal.toInt()
    val iter: ListIterator<Map.Entry<String, Int>> =
        ArrayList(osuMods.entries).listIterator(osuMods.size)

    // go through list backwards for proper display order
    while (iter.hasPrevious()) {
        val entry = iter.previous()
        if (i >= entry.value) {
            i -= entry.value

            // handle repeated implied mods (e.g. NC implies DT)
            if (entry.key == "NC") {
                i -= osuMods["DT"]!!
            }
            mods = entry.key + mods
        }
    }
    if(mods == "") {
        mods = "NoMod"
    }
    return mods
}