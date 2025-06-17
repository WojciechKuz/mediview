package transform3d

class ReARanger(val begin: Short, val end: Short) {
    val diff: Short; get() = (end - begin).toShort()
    fun okToMap() = diff != 0.toShort()
    fun valueToRange(value: Short, newRange: ReARanger): Short {
        return ((value - this.begin) * newRange.diff / this.diff + newRange.begin).toShort()
    }
    companion object {
        fun okToMap(begin: Short, end: Short) = (end - begin) != 0
    }
}