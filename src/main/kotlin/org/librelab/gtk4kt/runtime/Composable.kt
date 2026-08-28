package org.librelab.gtk4kt.runtime

/**
 * Marks a function as a Composable function — a function that describes UI.
 *
 * Composable functions:
 * - Can only be called from other Composable functions
 * - Are re-executed (recomposed) when their input State changes
 * - Should be pure: same inputs → same output
 *
 * Usage:
 * ```
 * @Composable
 * fun Greeting(name: String) {
 *     Label("Hello, $name!")
 * }
 * ```
 */
@DslMarker
annotation class Composable

/**
 * Annotation to mark a lambda parameter as a Composable scope.
 */
@DslMarker
annotation class ComposableLambda

/**
 * A composable function type.
 * Usage in DSL: `@Composable { label("hello") }`
 */
@ComposableLambda
annotation class ComposableLambdaImpl
