
/** Use in place of List<TagToDataMap>  */
class SQItemList(val list: List<TagToDataMap>): List<TagToDataMap> by list {

    private fun TagToDataMap.dataMapToString(): String {
        val sb = StringBuilder()
        this.forEach{ (k, v) ->
            sb.append(v.toString())
            sb.append('\n')
        }
        return sb.toString()
    }
    /*
    private fun TagToDataMap.size(): Int {
        var sum = 0
        this.forEach{ (k, v) ->
            sum += v.len.toInt()
        }
        return sum
    }
    */
    /** prints all items. Items are in `{}` braces, separated by just a `;` */
    override fun toString(): String {
        val sb = StringBuilder()
        list.forEach {
            //sb.append("Item ${it.size()}:\n")
            sb.append("Item:\n")
            sb.append(it.dataMapToString())
            //sb.append("\n")
        }
        //sb.deleteCharAt(sb.length - 1) // removes last '\n'
        //sb.deleteCharAt(sb.length - 1) // removes last ','
        return "{\n$sb}"
    }
}