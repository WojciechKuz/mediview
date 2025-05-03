
/** Powiadom, że użytkownik chce zobaczyć inny fragment skanu.
 *  Wysyłane przez suwaki lub onClick obrazka. Ustaw ujemne, jeśli któreś wartości się nie zmieniły. */
interface NotifyChangePoint {
    fun pointChange(red: UInt, green: UInt, blue: UInt)
}

// potrzebne iest jeszcze w drugą stronę - zwracanie bitmapy