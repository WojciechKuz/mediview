package transform3d

class WidthHeightDepth(val width: Int, val height: Int, val depth: Int) {
    companion object {
        fun unsignedConstruct(width: UInt, height: UInt, depth: UInt): WidthHeightDepth {
            return WidthHeightDepth(width.toInt(), height.toInt(), depth.toInt())
        }
    }
}