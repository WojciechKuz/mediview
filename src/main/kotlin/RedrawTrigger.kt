
@Deprecated("does not work as intended. Just use value.")
/** Declare as `var name by remember { mutableStateOf(RedrawTrigger()) }` in @Composable function.
 *  To trigger UI redraw call [trigger]. */
class RedrawTrigger {
    private var value = 0
    /** trigger UI redraw */
    fun trigger() {
        value++
    }
}