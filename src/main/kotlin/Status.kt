
class Status(val message: String, val status: St) {
    constructor(message: String = "") : this(message, if(message.isEmpty()) St.OK else St.INFO) {}

    val canProceed: Boolean = when(this.status) {
        St.OK, St.INFO, St.WARNING -> true
        else -> false
    }
    enum class St {
        OK,
        INFO,
        WARNING,
        ERROR // Nah, just throw exception in this case
    }
}