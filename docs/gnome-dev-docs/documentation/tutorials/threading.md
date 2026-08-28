# Threading - GNOME Developer Documentation

# Threading ¶

## Summary ¶

- Do not use threads if at all possible.
- If threads have to be used, use GTask or GThreadPool and isolate the
threaded code as much as possible.
- Use g_thread_join() to avoid leaking thread resources if using GThread manually.
- Be careful about the GMainContext which code is executed in if using
threads. Executing code in the wrong context can cause race conditions, or
block the main loop.

## When to use threading ¶

When writing projects using GLib, the default approach should be to never use
threads. Instead, make proper use of the GLib main context which, through the
use of asynchronous operations, allows most blocking I/O operations to continue
in the background while the main context continues to process other events.
Analysis, review and debugging of threaded code becomes very hard, very quickly.

Threading should only be necessary when using an external library which has
blocking functions which need to be called from GLib code. If the library
provides a non-blocking alternative, or one which integrates with a poll() loop, that should be used in preference. If the blocking function really must be
used, a thin wrapper should be written for it to convert it to the normal GAsyncResult style of GLib asynchronous function, running the blocking
operation in a worker thread.

For example, the following blocking function:

```
int

some_blocking_function
 
(
void
 
*
param1
,

                        
void
 
*
param2
);
```

Should be wrapped by this pair of functions:

```
void

some_blocking_function_async
 
(
void
                 
*
param1
,

                              
void
                 
*
param2
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
);

int

some_blocking_function_finish
 
(
GAsyncResult
        
*
result
,

                               
GError
             
**
error
);
```

With an implementation like:

```
/* Closure for the call’s parameters. */

typedef
 
struct
 
{

  
void
 
*
param1
;

  
void
 
*
param2
;

}
 
SomeBlockingFunctionData
;

static
 
void

some_blocking_function_data_free
 
(
SomeBlockingFunctionData
 
*
data
)

{

  
free_param
 
(
data
->
param1
);

  
free_param
 
(
data
->
param2
);

  
g_free
 
(
data
);

}

static
 
void

some_blocking_function_thread_cb
 
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

  
SomeBlockingFunctionData
 
*
data
 
=
 
task_data
;

  
int
 
retval
;

  
/* Handle cancellation. */

  
if
 
(
g_task_return_error_if_cancelled
 
(
task
))

    
{

      
return
;

    
}

  
/* Run the blocking function. */

  
retval
 
=
 
some_blocking_function
 
(
data
->
param1
,
 
data
->
param2
);

  
g_task_return_int
 
(
task
,
 
retval
);

}

void

some_blocking_function_async
 
(
void
                 
*
param1
,

                              
void
                 
*
param2
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
 
=
 
NULL
;
  
/* owned */

  
SomeBlockingFunctionData
 
*
data
 
=
 
NULL
;
  
/* owned */

  
g_return_if_fail
 
(
validate_param
 
(
param1
));

  
g_return_if_fail
 
(
validate_param
 
(
param2
));

  
g_return_if_fail
 
(
cancellable
 
==
 
NULL
 
||
 
G_IS_CANCELLABLE
 
(
cancellable
));

  
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

  
g_task_set_source_tag
 
(
task
,
 
some_blocking_function_async
);

  
/* Cancellation should be handled manually using mechanisms specific to

   * some_blocking_function(). */

  
g_task_set_return_on_cancel
 
(
task
,
 
FALSE
);

  
/* Set up a closure containing the call’s parameters. Copy them to avoid

   * locking issues between the calling thread and the worker thread. */

  
data
 
=
 
g_new0
 
(
SomeBlockingFunctionData
,
 
1
);

  
data
->
param1
 
=
 
copy_param
 
(
param1
);

  
data
->
param2
 
=
 
copy_param
 
(
param2
);

  
g_task_set_task_data
 
(
task
,
 
data
,
 
some_blocking_function_data_free
);

  
/* Run the task in a worker thread and return immediately while that continues

   * in the background. When it’s done it will call @callback in the current

   * thread default main context. */

  
g_task_run_in_thread
 
(
task
,
 
some_blocking_function_thread_cb
);

  
g_object_unref
 
(
task
);

}

int

some_blocking_function_finish
 
(
GAsyncResult
  
*
result
,

                               
GError
       
**
error
)

{

  
g_return_val_if_fail
 
(
g_task_is_valid
 
(
result
,

                                         
some_blocking_function_async
),
 
-1
);

  
g_return_val_if_fail
 
(
error
 
==
 
NULL
 
||
 
*
error
 
==
 
NULL
,
 
-1
);

  
return
 
g_task_propagate_int
 
(
G_TASK
 
(
result
),
 
error
);

}
```

See the GTask documentation for more details.

## Using threading ¶

If GTask is not suitable for writing the worker thread, a more low-level
approach must be used. This should be considered very carefully, as it is very
easy to get threading code wrong in ways which will unpredictably cause bugs at
runtime, cause deadlocks, or consume too many resources and terminate the
program.

A full manual on writing threaded code is beyond the scope of this document, but
here are a number of guidelines to follow which should reduce the potential for
bugs in threading code. The overriding principle is to reduce the amount of code
and data which can be affected by threading—for example, reducing the number of
threads, the complexity of worker thread implementation, and the amount of data
shared between threads.

- Use GThreadPool instead of manually creating threads if possible. GThreadPool supports a work queue, limits on the number of spawned
threads, and automatically joins finished threads so they are not leaked.
- If it is not possible to use a GThreadPool (which is rarely the case): Use g_thread_try_new() to spawn threads, instead of g_thread_new() ,
so errors due to the system running out of threads can be handled
gracefully rather than unconditionally aborting the program. Explicitly join threads using g_thread_join() to avoid leaking the
thread resources.
- Use message passing to transfer data between threads, rather than manual
locking with mutexes. GThreadPool explicitly supports this with g_thread_pool_push() .
- If mutexes must be used: Isolate threading code as much as possible, keeping mutexes private
within classes, and tightly bound to very specific class members. All mutexes should be clearly commented beside their declaration,
indicating which other structures or variables they protect access to.
Similarly, those variables should be commented saying that they should
only be accessed with that mutex held.
- Be careful about interactions between main contexts and threads. For example, g_timeout_add_seconds() adds a timeout to be executed in the global
default main context , which is being run in the main thread, not necessarily
the current thread. Getting this wrong can mean that work intended for a
worker thread accidentally ends up being executed in the main thread anyway.

## Debugging ¶

Debugging threading issues is tricky, both because they are hard to reproduce,
and because they are hard to reason about. This is one of the big reasons for
avoiding using threads in the first place.

However, if a threading issue does arise, Valgrind’s drd and helgrind tools are useful.
