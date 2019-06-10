package il.ac.technion.cs.softwaredesign

class UserListener(private val id: Long) {
    private var callbacks: MutableSet<ListenerCallback> = HashSet()
    /**
     * add a new callback
     * @param callback ListenerCallback
     * @return Boolean, true if callback added successfully and false if the callback is already contained in the set
     */
    fun listen(callback: ListenerCallback): Boolean {
        return callbacks.add(callback)
    }

    /**
     * remove callback
     * @param callback ListenerCallback
     * @return Boolean, true if callback removed successfully and false if the callback is not contained in the set
     */
    fun unlisten(callback: ListenerCallback): Boolean {
        return callbacks.remove(callback)
    }

    /**
     * check if there are no callbacks to this user
     * @return Boolean, true if there are no callbacks, false if there is at least one callback
     */
    fun isEmpty(): Boolean = callbacks.isEmpty()

    /**
     * getter for user id
     * @return Long
     */
    fun getId(): Long = id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserListener

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}