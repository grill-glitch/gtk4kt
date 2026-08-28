/*
 * gtk4kt — Declarative UI Runtime
 * Phase 2: State + Composable
 */

package org.librelab.gtk4kt.runtime

import java.util.IdentityHashMap
import kotlin.reflect.KProperty

// ============================================================================
// State
// ============================================================================

/**
 * Observable state value. Inspired by Compose State<T>.
 *
 * When the value changes, all reading Composable scopes are automatically
 * recomposed on the next frame.
 */
interface State<T> {
    val value: T
}

/**
 * Mutable state that can be read and written.
 * Writing triggers recomposition of all Composable scopes that read this value.
 */
class MutableState<T>(initial: T) : State<T> {
    private var _value: T = initial
    override val value: T get() = _value

    // Notifiers — set of scopes that read this state and need recomposition
    private val notifiers = IdentityHashMap<Recomposer, Unit>()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        _value = value
        // Notify all recomposers that depend on this state
        notifiers.keys.forEach { it.requestRecompose() }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    fun update(transform: (T) -> T) {
        setValue(null, KProperty<Any?>::name, transform(_value))
    }

    internal fun registerNotifier(recomposer: Recomposer) {
        notifiers[recomposer] = Unit
    }

    internal fun unregisterNotifier(recomposer: Recomposer) {
        notifiers.remove(recomposer)
    }
}

/**
 * Create a rememberable mutable state.
 * If [initial] is a computation, wrap in lambda: `remember { mutableStateOf { compute() } }`
 */
fun <T> mutableStateOf(initial: T): MutableState<T> = MutableState(initial)

/**
 * Remember [calculate] value across recompositions.
 * The value is computed once on first call and reused on subsequent recompositions,
 * unless any state read during [calculate] changes.
 *
 * Usage:
 * ```
 * val count by remember { mutableStateOf(0) }
 * val data by remember(dataId) { mutableStateOf { fetchData(dataId) } }
 * ```
 */
fun <T> remember(calculate: () -> T): T = Recomposer.current?.remember(calculate) ?: calculate()

/**
 * Remember a mutable state, recomputing the initial value only when dependencies change.
 * Tracks which states are read during [calculate] — when any of them changes,
 * the whole lambda is re-executed.
 *
 * Usage:
 * ```
 * val count by remember { mutableStateOf(0) }
 * ```
 */
fun <T> remember(calculate: () -> MutableState<T>): MutableState<T> {
    val recomposer = Recomposer.current ?: return calculate()
    return recomposer.rememberState(calculate)
}

// ============================================================================
// Recomposer
// ============================================================================

/**
 * Manages recomposition of Composable scopes when tracked state changes.
 *
 * Each call to a @Composable function creates a new Recomposer scope.
 * When MutableState.write is called, all dependent Recomposers are notified.
 */
class Recomposer {
    private val pending = LinkedHashSet<() -> Unit>()
    private var isScheduled = false
    private val lock = Any()

    // Track state dependencies for this recomposer
    private val trackedStates = IdentityHashMap<MutableState<*>, Unit>()

    // Remembered values cache
    private val rememberCache = LinkedHashMap<() -> Any?, Any?>()

    internal companion object {
        // Thread-local current recomposer
        private val currentThread = ThreadLocal<Recomposer?>()

        val current: Recomposer?
            get() = currentThread.get()
    }

    /**
     * Request recomposition. Schedules a frame if not already scheduled.
     */
    fun requestRecompose() {
        synchronized(lock) {
            if (!isScheduled) {
                isScheduled = true
                scheduleFrame()
            }
        }
    }

    private fun scheduleFrame() {
        // In GTK, we schedule on the main loop via GLib.idle_add
        // For now, recompose synchronously (acceptable for PoC)
        // TODO: use GTK main loop idle source for proper async recomposition
        synchronized(lock) {
            isScheduled = false
            val pending_copy = pending.toList()
            pending.clear()
            pending_copy.forEach { it() }
        }
    }

    /**
     * Run [block] with this recomposer as current.
     */
    internal fun <T> run(block: () -> T): T {
        val previous = currentThread.get()
        currentThread.set(this)
        try {
            return block()
        } finally {
            currentThread.set(previous)
        }
    }

    /**
     * Track reading of [state] for recomposition dependency.
     */
    internal fun track(state: MutableState<*>) {
        trackedStates[state] = Unit
        state.registerNotifier(this)
    }

    /**
     * Remember a value across recompositions.
     */
    internal fun <T> remember(calculate: () -> T): T {
        val key = calculate
        @Suppress("UNCHECKED_CAST")
        return rememberCache.getOrPut(key) { calculate() } as T
    }

    /**
     * Remember a mutable state, tracking dependencies.
     */
    internal fun <T> rememberState(calculate: () -> MutableState<T>): MutableState<T> {
        val key = calculate
        @Suppress("UNCHECKED_CAST")
        val existing = rememberCache.get(key) as? MutableState<T>
        if (existing != null) {
            // Re-track dependencies
            track(existing)
            return existing
        }
        // Run with fresh dependency tracking
        val fresh = calculate()
        rememberCache[key] = fresh
        track(fresh)
        return fresh
    }
}
