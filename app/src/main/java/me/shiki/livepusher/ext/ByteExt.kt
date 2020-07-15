package me.shiki.livepusher.ext

fun ByteArray.byteToHex(): String {
    val stringBuffer = StringBuffer()
    for (i in this.indices) {
        val hex = Integer.toHexString(get(i).toInt())
        if (hex.length == 1) {
            stringBuffer.append("0$hex")
        } else {
            stringBuffer.append(hex)
        }
        if (i > 20) {
            break
        }
    }
    return stringBuffer.toString()
}
