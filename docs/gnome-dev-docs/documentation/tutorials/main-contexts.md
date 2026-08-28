# Main Contexts - GNOME Developer Documentation

# Main Contexts ¶

## Summary ¶

- Use g_main_context_invoke_full() to invoke functions in other threads,
assuming every thread has a thread default main context which runs throughout
the lifetime of that thread
- Use GTask to run a function in the background without caring about the
specific thread used
- Liberally use assertions to check which context executes each function, and
add these assertions when first writing the code
- Explicitly document contexts a function is expected to be called in, a
callback will be invoked in, or a signal will be emitted in
- Beware of g_idle_add() and similar functions which implicitly use the
global-default main context

## What is GMainContext? ¶

GMainContext is a generalized implementation of an event loop, useful for
implementing polled file I/O or event-based widget systems (such as GTK). It is
at the core of almost every GLib application. To understand GMainContext requires understanding poll() and polled I/O.

A GMainContext has a set of sources which are ‘attached’ to it, each of
which can be thought of as an expected event with an associated callback
function which will be invoked when that event is received; or equivalently as a
set of file descriptors (FDs) to check. An event could be a timeout or data
being received on a socket, for example. One iteration of the event loop will:

- Prepare sources, determining if any of them are ready to dispatch immediately.
- Poll the sources, blocking the current thread until an event is received for
one of the sources.
- Check which of the sources received an event (several could have).
- Dispatch callbacks from those sources.

This is explained very well in the GLib documentation .

At its core, GMainContext is just a poll() loop, with the preparation,
check and dispatch stages of the loop corresponding to the normal preamble and
postamble in a typical poll() loop implementation. Typically, some
complexity is needed in non-trivial poll() -using applications to track the
lists of FDs which are being polled. Additionally, GMainContext adds a lot
of useful functionality which vanilla poll() doesn’t support. Most
importantly, it adds thread safety.

GMainContext is completely thread safe, meaning that a GSource can be
created in one thread and attached to a GMainContext running in another
thread. A typical use for this might be to allow worker threads to control which
sockets are being listened to by a GMainContext in a central I/O thread.
Each GMainContext is ‘acquired’ by a thread for each iteration it’s put
through. Other threads cannot iterate a GMainContext without acquiring it,
which guarantees that a GSource and its FDs will only be polled by one
thread at once (since each GSource is attached to at most one GMainContext ). A GMainContext can be swapped between threads across
iterations, but this is expensive.

GMainContext is used instead of poll() mostly for convenience, as it
transparently handles dynamically managing the array of FDs to pass to poll() , especially when operating over multiple threads. This is done by
encapsulating file descriptors inside a GSource , which decide whether those
FDs should be passed to the poll() call on each ‘prepare’ stage of the main
context iteration.

## What is GMainLoop? ¶

GMainLoop is essentially the following few lines of code, once reference
counting and locking have been removed:

```
loop
->
is_running
 
=
 
TRUE
;

while
 
(
loop
->
is_running
)

  
{

    
if
 
(
quit_condition
)

      
loop
->
is_running
 
=
 
FALSE
;

    
g_main_context_iteration
 
(
context
,
 
TRUE
);

  
}
```

Setting quit_condition to TRUE will cause the loop to terminate once the
current main context iteration ends.

Hence, GMainLoop is a convenient, thread-safe way of running a GMainContext to process events until a desired exit condition is met, at
which point g_main_loop_quit() should be called. Typically, in a UI program,
this will be the user clicking ‘exit’. In a socket handling program, this might
be the final socket closing.

It is important not to confuse main contexts with main loops. Main contexts do
the bulk of the work: preparing source lists, waiting for events, and
dispatching callbacks. A main loop simply iterates a context.

## Default Contexts ¶

One of the important features of GMainContext is its support for ‘default’
contexts. There are two levels of default context: the thread-default, and the
global-default. The global-default (accessed using g_main_context_default() ) is
run by GTK when g_application_run() is called. It’s also used for timeouts
( g_timeout_add() ) and idle callbacks ( g_idle_add() ) — these won’t be dispatched
unless the default context is running!

Thread-default contexts are generally used for I/O operations which need to run
and dispatch callbacks in a thread. By calling g_main_context_push_thread_default() before starting an I/O operation, the
thread-default context is set and the I/O operation can add its sources to that
context. The context can then be run in a new main loop in an I/O thread,
causing the callbacks to be dispatched on that thread’s stack rather than on the
stack of the thread running the global-default main context. This allows I/O
operations to be run entirely in a separate thread without explicitly passing a
specific GMainContext pointer around everywhere.

Conversely, by starting a long-running operation with a specific thread-default
context set, the calling code can guarantee that the operation’s callbacks will
be emitted in that context, even if the operation itself runs in a worker
thread. This is the principle behind GTask : when a new GTask is created, it
stores a reference to the current thread-default context, and dispatches its
completion callback in that context, even if the task itself is run using g_task_run_in_thread() .

For example, the code below will run a GTask which performs two writes in
parallel from a thread. The callbacks for the writes will be dispatched in the
worker thread, whereas the callback from the task as a whole will be dispatched
in the interesting_context.

```
typedef
 
struct
 
{

  
GMainLoop
 
*
main_loop
;

  
guint
 
n_remaining
;

}
 
WriteData
;

/* This is always called in the same thread as thread_cb() because

 * it’s always dispatched in the @worker_context. */

static
 
void

write_cb
 
(
GObject
      
*
source_object
,

          
GAsyncResult
 
*
result
,

          
gpointer
      
user_data
)

{

  
WriteData
 
*
data
 
=
 
user_data
;

  
GOutputStream
 
*
stream
 
=
 
G_OUTPUT_STREAM
 
(
source_object
);

  
GError
 
*
error
 
=
 
NULL
;

  
gssize
 
len
;

  
/* Finish the write. */

  
len
 
=
 
g_output_stream_write_finish
 
(
stream
,
 
result
,
 
&
error
);

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_error
 
(
"Error: %s"
,
 
error
->
message
);

      
g_error_free
 
(
error
);

    
}

  
/* Check whether all parallel operations have finished. */

  
write_data
->
n_remaining
--
;

  
if
 
(
write_data
->
n_remaining
 
==
 
0
)

    
{

      
g_main_loop_quit
 
(
write_data
->
main_loop
);

    
}

}

/* This is called in a new thread. */

static
 
void

thread_cb
 
(
GTask
        
*
task
,

           
gpointer
      
source_object
,

           
gpointer
      
task_data
,

           
GCancellable
 
*
cancellable
)

{

  
/* These streams come from somewhere else in the program: */

  
GOutputStream
 
*
output_stream1
,
 
*
output_stream
;

  
GMainContext
 
*
worker_context
;

  
GBytes
 
*
data
;

  
const
 
guint8
 
*
buf
;

  
gsize
 
len
;

  
/* Set up a worker context for the writes’ callbacks. */

  
worker_context
 
=
 
g_main_context_new
 
();

  
g_main_context_push_thread_default
 
(
worker_context
);

  
/* Set up the writes. */

  
write_data
.
n_remaining
 
=
 
2
;

  
write_data
.
main_loop
 
=
 
g_main_loop_new
 
(
worker_context
,
 
FALSE
);

  
data
 
=
 
g_task_get_task_data
 
(
task
);

  
buf
 
=
 
g_bytes_get_data
 
(
data
,
 
&
len
);

  
g_output_stream_write_async
 
(
output_stream1
,
 
buf
,
 
len
,

                               
G_PRIORITY_DEFAULT
,
 
NULL
,
 
write_cb
,

                               
&
write_data
);

  
g_output_stream_write_async
 
(
output_stream2
,
 
buf
,
 
len
,

                               
G_PRIORITY_DEFAULT
,
 
NULL
,
 
write_cb
,

                               
&
write_data
);

  
/* Run the main loop until both writes have finished. */

  
g_main_loop_run
 
(
write_data
.
main_loop
);

  
g_task_return_boolean
 
(
task
,
 
TRUE
);
  
/* ignore errors */

  
g_main_loop_unref
 
(
write_data
.
main_loop
);

  
g_main_context_pop_thread_default
 
(
worker_context
);

  
g_main_context_unref
 
(
worker_context
);

}

/* This can be called from any thread. Its @callback will always be

 * dispatched in the thread which currently owns

 * @interesting_context. */

void

parallel_writes_async
 
(
GBytes
              
*
data
,

                       
GMainContext
        
*
interesting_context
,

                       
GCancellable
        
*
cancellable
,

                       
GAsyncReadyCallback
  
callback
,

                       
gpointer
             
user_data
)

{

  
GTask
 
*
task
;

  
g_main_context_push_thread_default
 
(
interesting_context
);

  
task
 
=
 
g_task_new
 
(
NULL
,
 
cancellable
,
 
callback
,
 
user_data
);

  
g_task_set_task_data
 
(
task
,
 
data
,

                        
(
GDestroyNotify
)
 
g_bytes_unref
);

  
g_task_run_in_thread
 
(
task
,
 
thread_cb
);

  
g_object_unref
 
(
task
);

  
g_main_context_pop_thread_default
 
(
interesting_context
);

}
```

## Implicit Use of the Global-Default Main Context ¶

Several functions implicitly add sources to the global-default main context.
They should not be used in threaded code. Instead, use g_source_attach() with the GSource created by the replacement function from the table below.

Implicit use of the global-default main context means the callback functions are
invoked in the main thread, typically resulting in work being brought back from
a worker thread into the main thread.

| Do not use | Use instead |
|---|---|
| g_timeout_add() | g_timeout_source_new() |
| g_idle_add() | g_idle_source_new() |
| g_child_watch_add() | g_child_watch_source_new() |

So to delay some computation in a worker thread, use the following code:

```
static
 
guint

schedule_computation
 
(
guint
 
delay_seconds
)

{

  
/* Get the calling context. */

  
GMainContext
 
*
context
 
=
 
g_main_context_get_thread_default
 
();

  
GSource
 
*
source
 
=
 
g_timeout_source_new_seconds
 
(
delay_seconds
);

  
g_source_set_callback
 
(
source
,
 
do_computation
,
 
NULL
,
 
NULL
);

  
guint
 
id
 
=
 
g_source_attach
 
(
source
,
 
context
);

  
g_source_unref
 
(
source
);

  
/* The ID can be used with the same @context to

   * cancel the scheduled computation if needed. */

  
return
 
id
;

}

static
 
void

do_computation
 
(
gpointer
 
user_data
)

{

  
// ...

}
```

## Using GMainContext in a Library ¶

At a high level, library code must not make changes to main contexts which could
affect the execution of an application using the library, for example by
changing when the application’s sources are dispatched. There are various best
practices which can be followed to aid this.

Never iterate a context created outside the library, including the
global-default or thread-default contexts. Otherwise, sources created in the
application may be dispatched when the application is not expecting it, causing
re-entrancy problems for the application code.

Always remove sources from a main context before dropping the library’s last
reference to the context, especially if it may have been exposed to the
application (for example, as a thread-default). Otherwise the application may
keep a reference to the main context and continue iterating it after the library
has returned, potentially causing unexpected source dispatches in the library.
This is equivalent to not assuming that dropping the library’s last reference to
a main context will finalize that context.

If the library is designed to be used from multiple threads, or in a
context-aware fashion, always document which context each callback will be
dispatched in. For example, “callbacks will always be dispatched in the context
which is the thread-default at the time of the object’s construction”.
Developers using the library’s API need to know this information.

Use g_main_context_invoke() to ensure callbacks are dispatched in the right
context. It’s much easier than manually using g_idle_source_new() to
transfer work between contexts.

Libraries should never use g_main_context_default() (or, equivalently, pass
NULL to a GMainContext -typed parameter). Always store and explicitly use a
specific GMainContext , even if it often points to some default context. This
makes the code easier to split out into threads in future, if needed, without
causing hard-to-debug problems caused by callbacks being invoked in the wrong
context.

Write things asynchronously internally (using GTask where appropriate), and
keep synchronous wrappers at the very top level of an API, where they can be
implemented by calling g_main_context_iteration() on a specific GMainContext . Again, this makes future refactoring easier. This is
demonstrated in the previous example: the thread uses g_output_stream_write_async() rather than g_output_stream_write() . A
worker thread may be used instead, and this can simplify the callback chain for
long series of asynchronous calls; but at the cost of increased complexity in
verifying the code is race-free.

Always match pushes and pops of the thread-default main context: g_main_context_push_thread_default() and g_main_context_pop_thread_default() .

## Ensuring Functions are Called in the Right Context ¶

The ‘right context’ is the thread-default main context of the thread the
function should be executing in. This assumes the typical case that every thread
has a single main context running in a main loop. A main context effectively
provides a work or message queue for the thread — something which the thread can
periodically check to determine if there is work pending from another thread.
Putting a message on this queue – invoking a function in another main context –
will result in it eventually being dispatched in that thread.

For example, if an application does a long and CPU-intensive computation it
should schedule this in a background thread so that UI updates in the main
thread are not blocked. The results of the computation, however, might need to
be displayed in the UI, so some UI update function must be called in the main
thread once the computation’s complete.

Furthermore, if the computation function can be limited to a single thread, it
becomes easy to eliminate the need for locking a lot of the data it accesses.
This assumes that other threads are implemented similarly and hence most data is
only accessed by a single thread, with threads communicating by message passing.
This allows each thread to update its data at its leisure, which significantly
simplifies locking.

For some functions, there might be no reason to care which context they’re
executed in, perhaps because they’re asynchronous and hence do not block the
context. However, it is still advisable to be explicit about which context is
used, since those functions may emit signals or invoke callbacks, and for
reasons of thread safety it’s necessary to know which threads those signal
handlers or callbacks are going to be invoked in.

For example, the progress callback in g_file_copy_async() is documented as
being called in the thread-default main context at the time of the initial call.

## Principles of Invocation ¶

The core principle of invoking a function in a specific context is simple, and
is walked through below to explain the concepts. In practice the convenience
method, g_main_context_invoke_full() should be used instead.

A GSource has to be added to the target GMainContext , which will invoke
the function when it’s dispatched. This GSource should almost always be an
idle source created with g_idle_source_new() , but this doesn’t have to be
the case. It could be a timeout source so that the function is executed after a
delay, for example.

The GSource will be dispatched as soon as it’s ready, calling the function
on the thread’s stack. In the case of an idle source, this will be as soon as
all sources at a higher priority have been dispatched — this can be tweaked
using the idle source’s priority parameter with g_source_set_priority() . The
source will typically then be destroyed so the function is only executed once
(though again, this doesn’t have to be the case).

Data can be passed between threads as the user_data passed to the GSource ’s callback. This is set on the source using g_source_set_callback() , along with the callback function to invoke. Only a
single pointer is provided, so if multiple data fields need passing, they must
be wrapped in an allocated structure.

The example below demonstrates the underlying principles, but there are
convenience methods explained below which simplify things.

```
/* Main function for the background thread, thread1. */

static
 
gpointer

thread1_main
 
(
gpointer
 
user_data
)

{

  
GMainContext
 
*
thread1_main_context
 
=
 
user_data
;

  
GMainLoop
 
*
main_loop
;

  
/* Set up the thread’s context and run it forever. */

  
g_main_context_push_thread_default
 
(
thread1_main_context
);

  
main_loop
 
=
 
g_main_loop_new
 
(
thread1_main_context
,
 
FALSE
);

  
g_main_loop_run
 
(
main_loop
);

  
g_main_loop_unref
 
(
main_loop
);

  
g_main_context_pop_thread_default
 
(
thread1_main_context
);

  
g_main_context_unref
 
(
thread1_main_context
);

  
return
 
NULL
;

}

/* A data closure structure to carry multiple variables between

 * threads. */

typedef
 
struct
 
{

  
gchar
   
*
some_string
;
  
/* owned */

  
guint
    
some_int
;

  
GObject
 
*
some_object
;
  
/* owned */

}
 
MyFuncData
;

static
 
void

my_func_data_free
 
(
MyFuncData
 
*
data
)

{

  
g_free
 
(
data
->
some_string
);

  
g_clear_object
 
(
&
data
->
some_object
);

  
g_free
 
(
data
);

}

static
 
void

my_func
 
(
const
 
gchar
 
*
some_string
,

         
guint
        
some_int
,

         
GObject
     
*
some_object
)

{

  
/* Do something long and CPU intensive! */

}

/* Convert an idle callback into a call to my_func(). */

static
 
gboolean

my_func_idle
 
(
gpointer
 
user_data
)

{

  
MyFuncData
 
*
data
 
=
 
user_data
;

  
my_func
 
(
data
->
some_string
,
 
data
->
some_int
,
 
data
->
some_object
);

  
return
 
G_SOURCE_REMOVE
;

}

/* Function to be called in the main thread to schedule a call to

 * my_func() in thread1, passing the given parameters along. */

static
 
void

invoke_my_func
 
(
GMainContext
 
*
thread1_main_context
,

                
const
 
gchar
  
*
some_string
,

                
guint
         
some_int
,

                
GObject
      
*
some_object
)

{

  
GSource
 
*
idle_source
;

  
MyFuncData
 
*
data
;

  
/* Create a data closure to pass all the desired variables

   * between threads. */

  
data
 
=
 
g_new0
 
(
MyFuncData
,
 
1
);

  
data
->
some_string
 
=
 
g_strdup
 
(
some_string
);

  
data
->
some_int
 
=
 
some_int
;

  
data
->
some_object
 
=
 
g_object_ref
 
(
some_object
);

  
/* Create a new idle source, set my_func() as the callback with

   * some data to be passed between threads, bump up the priority

   * and schedule it by attaching it to thread1’s context. */

  
idle_source
 
=
 
g_idle_source_new
 
();

  
g_source_set_callback
 
(
idle_source
,
 
my_func_idle
,
 
data
,

                         
(
GDestroyNotify
)
 
my_func_data_free
);

  
g_source_set_priority
 
(
idle_source
,
 
G_PRIORITY_DEFAULT
);

  
g_source_attach
 
(
idle_source
,
 
thread1_main_context
);

  
g_source_unref
 
(
idle_source
);

}

/* Main function for the main thread. */

static
 
void

main
 
(
void
)

{

  
GThread
 
*
thread1
;

  
GMainContext
 
*
thread1_main_context
;

  
/* Spawn a background thread and pass it a reference to its

   * GMainContext. Retain a reference for use in this thread

   * too. */

  
thread1_main_context
 
=
 
g_main_context_new
 
();

  
g_thread_new
 
(
"thread1"
,
 
thread1_main
,

                
g_main_context_ref
 
(
thread1_main_context
));

  
/* Maybe set up your UI here, for example. */

  
/* Invoke my_func() in the other thread. */

  
invoke_my_func
 
(
thread1_main_context
,

                  
"some data which needs passing between threads"
,

                  
123456
,
 
some_object
);

  
/* Continue doing other work. */

}
```

This invocation is uni-directional: it calls my_func() in thread1 , but
there’s no way to return a value to the main thread. To do that, the same
principle needs to be used again, invoking a callback function in the main
thread. It’s a straightforward extension which isn’t covered here.

To maintain thread safety, data which is potentially accessed by multiple
threads must make those accesses mutually exclusive using a mutex. Data
potentially accessed by multiple threads: thread1_main_context , passed in
the fork call to thread1_main ; and some_object , a reference to which is
passed in the data closure. Critically, GLib guarantees that GMainContext is
thread safe, so sharing thread1_main_context between threads is safe. The
example assumes that other code accessing some_object is thread safe.

Note that some_string and some_int cannot be accessed from both threads,
because copies of them are passed to thread1 , rather than the originals.
This is a standard technique for making cross-thread calls thread safe without
requiring locking. It also avoids the problem of synchronizing freeing some_string .

Similarly, a reference to some_object is transferred to thread1 , which
works around the issue of synchronizing destruction of the object.

g_idle_source_new() is used rather than the simpler g_idle_add() so the GMainContext to attach to can be specified.

## Convenience Method: g_main_context_invoke_full() ¶

This is simplified greatly by the convenience method, g_main_context_invoke_full() . It invokes a callback so that the specified GMainContext is owned during the invocation. Owning a main context is almost
always equivalent to running it, and hence the function is invoked in the thread
for which the specified context is the thread-default.

g_main_context_invoke() can be used instead if the user data does not need
to be freed by a GDestroyNotify callback after the invocation returns.

Modifying the earlier example, the invoke_my_func() function can be replaced
by the following:

```
static
 
void

invoke_my_func
 
(
GMainContext
 
*
thread1_main_context
,

                
const
 
gchar
  
*
some_string
,

                
guint
         
some_int
,

                
GObject
      
*
some_object
)

{

  
MyFuncData
 
*
data
;

  
/* Create a data closure to pass all the desired variables

   * between threads. */

  
data
 
=
 
g_new0
 
(
MyFuncData
,
 
1
);

  
data
->
some_string
 
=
 
g_strdup
 
(
some_string
);

  
data
->
some_int
 
=
 
some_int
;

  
data
->
some_object
 
=
 
g_object_ref
 
(
some_object
);

  
/* Invoke the function. */

  
g_main_context_invoke_full
 
(
thread1_main_context
,

                              
G_PRIORITY_DEFAULT
,
 
my_func_idle
,

                              
data
,

                              
(
GDestroyNotify
)
 
my_func_data_free
);

}
```

Consider what happens if invoke_my_func() were called from thread1 ,
rather than from the main thread. With the original implementation, the idle
source would be added to thread1 ’s context and dispatched on the context’s
next iteration (assuming no pending dispatches with higher priorities). With the
improved implementation, g_main_context_invoke_full() will notice that the
specified context is already owned by the thread (or ownership can be acquired
by it), and will call my_func_idle() directly, rather than attaching a
source to the context and delaying the invocation to the next context iteration.

This subtle behavior difference doesn’t matter in most cases, but is worth
bearing in mind since it can affect blocking behavior ( invoke_my_func() would go from taking negligible time, to taking the same amount of time as my_func() before returning).

## Checking Threading ¶

It is useful to document which thread each function should be called in, in the form of an assertion:

```
g_assert
 
(
g_main_context_is_owner
 
(
expected_main_context
));
```

If that’s put at the top of each function, any assertion failure will highlight
a case where a function has been called from the wrong thread. It is much easier
to write these assertions when initially developing code, rather than debugging
race conditions which can easily result from a function being called in the
wrong thread.

This technique can also be applied to signal emissions and callbacks, improving
type safety as well as asserting the right context is used. Note that signal
emission via g_signal_emit() is synchronous, and doesn’t involve a main
context at all.

For example, instead of using the following when emitting a signal:

```
guint
 
param1
;
  
/* arbitrary example parameters */

gchar
 
*
param2
;

guint
 
retval
 
=
 
0
;

g_signal_emit_by_name
 
(
my_object
,

                       
"some-signal"
,

                       
param1
,

                       
param2
,

                       
&
retval
);
```

The following can be used:

```
static
 
guint

emit_some_signal
 
(
GObject
     
*
my_object
,

                  
guint
        
param1
,

                  
const
 
gchar
 
*
param2
)

{

  
guint
 
retval
 
=
 
0
;

  
g_assert
 
(
g_main_context_is_owner
 
(
expected_main_context
));

  
g_signal_emit_by_name
 
(
my_object
,
 
"some-signal"
,

                         
param1
,
 
param2
,
 
&
retval
);

  
return
 
retval
;

}
```

## GTask ¶

GTask provides a slightly different approach to invoking functions in other
threads, which is more suited to the case where a function should be executed in
some background thread, but not a specific one.

GTask takes a data closure and a function to execute, and provides ways to
return the result from this function. It handles everything necessary to run
that function in an arbitrary thread belonging to some thread pool internal to
GLib.

By combining g_main_context_invoke_full() and GTask , it is possible to
run a task in a specific context and effortlessly return its result to the
current context:

```
/* This will be invoked in thread1. */

static
 
gboolean

my_func_idle
 
(
gpointer
 
user_data
)

{

  
GTask
 
*
task
 
=
 
G_TASK
 
(
user_data
);

  
MyFuncData
 
*
data
;

  
gboolean
 
retval
;

  
/* Call my_func() and propagate its returned boolean to

   * the main thread. */

  
data
 
=
 
g_task_get_task_data
 
(
task
);

  
retval
 
=
 
my_func
 
(
data
->
some_string
,
 
data
->
some_int
,

                    
data
->
some_object
);

  
g_task_return_boolean
 
(
task
,
 
retval
);

  
return
 
G_SOURCE_REMOVE
;

}

/* Whichever thread this is invoked in, the @callback will be

 * invoked in, once my_func() has finished and returned a result. */

static
 
void

invoke_my_func_with_result
 
(
GMainContext
        
*
thread1_main_context
,

                            
const
 
gchar
         
*
some_string
,

                            
guint
                
some_int
,

                            
GObject
             
*
some_object
,

                            
GAsyncReadyCallback
  
callback
,

                            
gpointer
             
user_data
)

{

  
MyFuncData
 
*
data
;

  
/* Create a data closure to pass all the desired variables

   * between threads. */

  
data
 
=
 
g_new0
 
(
MyFuncData
,
 
1
);

  
data
->
some_string
 
=
 
g_strdup
 
(
some_string
);

  
data
->
some_int
 
=
 
some_int
;

  
data
->
some_object
 
=
 
g_object_ref
 
(
some_object
);

  
/* Create a GTask to handle returning the result to the current

   * thread-default main context. */

  
task
 
=
 
g_task_new
 
(
NULL
,
 
NULL
,
 
callback
,
 
user_data
);

  
g_task_set_task_data
 
(
task
,
 
data
,

                        
(
GDestroyNotify
)
 
my_func_data_free
);

  
/* Invoke the function. */

  
g_main_context_invoke_full
 
(
thread1_main_context
,

                              
G_PRIORITY_DEFAULT
,
 
my_func_idle
,

                              
task
,

                              
(
GDestroyNotify
)
 
g_object_unref
);

}
```
