package il.ac.technion.cs.softwaredesign

abstract class Observable<T> {
    private val listeners = HashSet<(T) -> Unit>()

    fun listen(listener: (T) -> Unit) {
        listeners.add(listener)
    }
    fun unlisten(listener: (T) -> Unit) {
        listeners.remove(listener)
    }
    protected fun onChange(t: T) {
        listeners.forEach { it(t) }
    }
}