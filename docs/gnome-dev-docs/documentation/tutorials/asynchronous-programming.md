# Asynchronous Programming - GNOME Developer Documentation

# Asynchronous Programming ¶

## Summary ¶

- Use asynchronous calls in preference to synchronous calls or explicit use of
threads
- Learn and follow the GLib pattern for declaring asynchronous APIs
- Place callbacks from asynchronous functions in order down the file, so control
flow is easy to follow
- Use the presence of a GTask or GCancellable to indicate whether an
operation is ongoing
- If running operations in parallel, track how many operations are yet to start,
and how many are yet to finish — the overall operation is complete once both
counts are zero
- Separate state for operations into ‘task data’ structures for GTask ,
allowing operations to be reused more easily without needing changes to
global state handling
- Consider how asynchronous methods on an object instance interact with
finalization of that instance

## Concepts ¶

GLib supports asynchronous programming , where long-running operations can be
started, run ‘in the background’, and a callback invoked when they are finished
and their results are available. This is in direct contrast to synchronous
long-running operations, which are a single function call which blocks program
control flow until complete.

As discussed in the Main Contexts and in the Threading tutorials, asynchronous operations
should be favoured over synchronous ones and over explicit use of threading.
They do not block the main context like sychronous operations do; and are
easier to use correctly than threads. They often also have a lower performance
penalty than spawning a thread and sending work to it.

## API patterns ¶

Asynchronous calls follow a standard pattern in GLib code. For an operation
named load_data on the File class in the Foo namespace, there will
be:

```
void

foo_file_load_data_async
 
(
FooFile
             
*
self
,

                          
// ...,

                          
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

gboolean

foo_file_load_data_finish
 
(
FooFile
       
*
self
,

                           
GAsyncResult
  
*
result
,

                           
// ...,

                           
GError
       
**
error
);
```

The … parameters to foo_file_load_data_async() are those specific to the
operation—in this case, perhaps the size of a buffer to load into. Similarly for foo_file_load_data_finish() they are the operation-specific return
values—perhaps a location to return a content type string in this case.

When foo_file_load_data_async() is called, it schedules the load operation
in the background (as a new file descriptor on the GMainContext or as a
worker thread, for example), then returns without blocking.

When the operation is complete, the callback is executed in the same GMainContext as the original asynchronous call. The callback is invoked
exactly once, whether the operation succeeded or failed.

From the callback, foo_file_load_data_finish() may be called by the user’s
code to retrieve return values and error details, passing the GAsyncResult instance which was passed to the callback.

```
Foo
.
File
.
load_data_async
(
self
,

                         
# *additional_arguments,

                         
cancellable
:
 
Gio
.
Cancellable
,

                         
callback
:
    
GAsyncReadyCallback
,

                         
user_data
)
 
->
 
None

Foo
.
File
.
load_data_finish
(
self
,

                          
result
:
 
Gio
.
AsyncResult

                          
)
 
->
 
bool
 
#, *additional_return_values
```

The *additional_arguments parameters to Foo.File.load_data_async() are
those specific to the operation—in this case, perhaps the size of a buffer to
load into. Similarly *additional_return_values for Foo.File.load_data_finish() are the operation-specific return values—perhaps a content type string in this case.

callback is a callable (e.g. a function) that will be called with three parameters
when the operation has been completed. Those parameters are:

- The Foo.File instance whose load_data_async() method we called
- A Gio.AsyncResult
- user_data

When Foo.File.load_data_async() is called, it schedules the load operation
in the background (as a new file descriptor on the GLib.MainContext or as a
worker thread, for example), then returns without blocking.

When the operation is complete, the callback is executed in the same GLib.MainContext as the original asynchronous call. The callback is invoked
exactly once, whether the operation succeeded or failed.

From the callback, Foo.File.load_data_finish() may be called by the user’s
code to retrieve return values and error details, passing the Gio.AsyncResult instance which was passed to the callback.

## Operation Lifetimes ¶

When writing asynchronous operations, it is common to write them as methods of a
class. In this case, it is important to define how ongoing operations on a class
instance interact with finalization of that instance. There are two approaches:

**Strong**

The ongoing operation keeps a reference to the class instance, forcing it to
remain alive for the duration of the operation. The class should provide some
kind of ‘close’ or ‘cancel’ method which can be used by other classes to force
cancellation of the operation and allow that instance to be finalized.

**Weak**

The ongoing operation does not keep a reference to the class instance, and
the class cancels the operation (using g_cancellable_cancel() ) in its
dispose function.

Which approach is used depends on the class’ design. A class which wraps a
particular operation (perhaps a MyFileTransfer class, for example) might
want to use the weak approach. A class which manages multiple network
connections and asynchronous operations on them may use the strong approach
instead. Due to incoming network connections, for example, it might not be in
complete control of the scheduling of its asynchronous calls, so the weak
approach would not be appropriate—any code dropping a reference to the object
could not be sure it was not accidentally killing a new network connection.

## Using asynchronous functions ¶

It is often the case that multiple asynchronous calls need to be used to
complete an operation. For example, opening a file for reading, then performing
a couple of reads, and then closing the file. Or opening several network sockets
in parallel and waiting until they are all open before continuing with other
work. Some examples of these situations are given below.

### Single operation ¶

A single asynchronous call requires two functions: one to start the operation,
and one to complete it. In C, the demanding part of performing an asynchronous
call is correctly storing state between these two functions, and handling
changes to that state in the time between those two functions being called. For
example, cancellation of an ongoing asynchronous call is a state change, and if
not implemented carefully, any UI updates (for example) made when cancelling an
operation will be undone by updates in the operation’s callback.

The example below demonstrates copying a file from one location in the file
system to another. The key principles demonstrated here are:

- Placing the copy_button_clicked_cb() (start) and copy_finish_cb() (finish) functions in order by using a forward declaration for copy_finish_cb() . This means the control flow continues linearly down the
file, rather than getting to the bottom of copy_button_clicked_cb() and
resuming in copy_finish_cb() somewhere else in the file.
- Using a GCancellable to allow cancelling the operation after it has
started. The code in cancel_button_clicked_cb() is very simple: as the copy_finish_cb() callback is guaranteed to be invoked when the operation
completes (even when completing early due to cancellation), all the UI and
state updates for cancellation can be handled there, rather than in cancel_button_clicked_cb() .
- An operation is ongoing exactly while MyObjectPrivate.copy_cancellable is
not NULL , making it easy to track running operations. Note that this
means only one file copy operation can be started via copy_button_clicked_cb() at a time. One GCancellable cannot easily be
used for multiple operations like this.

```
static
 
void

copy_finish_cb
 
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
);

static
 
void

copy_button_clicked_cb
 
(
GtkButton
 
*
button

                        
gpointer
   
user_data
)

{

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
source
 
=
 
NULL
,
 
*
destination
 
=
 
NULL
;
  
/* owned */

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
user_data
));

  
/* Operation already in progress? */

  
if
 
(
priv
->
copy_cancellable
 
!=
 
NULL
)

    
{

      
g_debug
 
(
"Copy already in progress."
);

      
return
;

    
}

  
/* Build source and destination file paths. */

  
source
 
=
 
g_file_new_for_path
 
(
/* some path generated from UI */
);

  
destination
 
=
 
g_file_new_for_path
 
(
/* some other path generated from UI */
);

  
/* Set up a cancellable. */

  
priv
->
copy_cancellable
 
=
 
g_cancellable_new
 
();

  
g_file_copy_async
 
(
source
,
 
destination
,
 
G_FILE_COPY_NONE
,
 
G_PRIORITY_DEFAULT
,

                     
priv
->
copy_cancellable
,
 
NULL
,
 
NULL
,

                     
copy_finish_cb
,
 
user_data
);

  
g_object_unref
 
(
destination
);

  
g_object_unref
 
(
source
);

  
/* Update UI to show copy is in progress. */

}

static
 
void

copy_finish_cb
 
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

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
source
;
  
/* unowned */

  
GError
 
*
error
 
=
 
NULL
;

  
source
 
=
 
G_FILE
 
(
source_object
);

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
user_data
));

  
/* Handle completion of the operation. */

  
g_file_copy_finish
 
(
source
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
 
&&

      
!
g_error_matches
 
(
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_CANCELLED
))

    
{

      
/* Should update the UI to signal failure.

       * Ignore failure due to cancellation. */

      
g_warning
 
(
"Failed to copy file: %s"
,
 
error
->
message
);

    
}

  
g_clear_error
 
(
&
error
);

  
/* Clear the cancellable to signify the operation has finished. */

  
g_clear_object
 
(
&
priv
->
copy_cancellable
);

  
/* Update UI to show copy as complete. */

}

static
 
void

cancel_button_clicked_cb
 
(
GtkButton
 
*
button
,

                          
gpointer
   
user_data
)

{

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
source
 
=
 
NULL
,
 
*
destination
 
=
 
NULL
;
  
/* owned */

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
user_data
));

  
/* Operation in progress? No-op if @copy_cancellable is %NULL. */

  
g_cancellable_cancel
 
(
priv
->
copy_cancellable
);

}

static
 
void

my_object_dispose
 
(
GObject
 
*
obj
)

{

  
MyObjectPrivate
 
*
priv
;

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
obj
));

  
/* Cancel any ongoing copy operation.

   *

   * This ensures that if #MyObject is disposed part-way through a copy, the

   * callback doesn’t get invoked with an invalid #MyObject pointer. */

  
g_cancellable_cancel
 
(
priv
->
copy_cancellable
);

  
/* Do other dispose calls here. */

  
/* Chain up. */

  
G_OBJECT_CLASS
 
(
my_object_parent_class
)
->
dispose
 
(
obj
);

}
```

```
import
 
logging

from
 
gi.repository
 
import
 
GLib

from
 
gi.repository
 
import
 
Gio

from
 
gi.repository
 
import
 
Gtk

class
 
MyWidget
(
Gtk
.
Widget
):

    
copy_cancellable
 
=
 
None

    
def
 
copy_button_clicked_cb
(
self
,
 
button
):

        
# Operation already in progress?

        
if
 
self
.
copy_cancellable
 
is
 
not
 
None
:

            
logging
.
debug
(
"Copy already in progress."
)

            
return

        
# Calculate source and destination paths from the UI.

        
source
 
=
 
Gio
.
File
.
new_for_path
(
source_path
)

        
destination
 
=
 
Gio
.
File
.
new_for_path
(
destination_path
)

        
# Set up a cancellable.

        
self
.
copy_cancellable
 
=
 
Gio
.
Cancellable
()

        
source
.
copy_async
(
destination
,
 
Gio
.
FileCopyFlags
.
NONE
,

                          
GLib
.
PRIORITY_DEFAULT
,
 
self
.
copy_cancellable
,

                          
callback
=
copy_finish_cb
)

        
# Update UI to show copy is in progress.

    
def
 
copy_finish_cb
(
self
,
 
source
,
 
result
,
 
user_data
=
None
):

        
# Handle completion of the operation.

        
try
:

            
source
.
copy_finish
(
result
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
if
 
err
.
matches
(
Gio
.
io_error_quark
(),
 
Gio
.
IOErrorEnum
.
CANCELLED
):

                
# Should update the UI to signal failure.

                
# Ignore failure due to cancellation.

                
logging
.
warning
(
"Failed to copy file: 
%s
"
,
 
err
.
message
)

            
else
:

                
raise

        
# Clear the cancellable to signify the operation has finished.

        
self
.
copy_cancellable
 
=
 
None

        
# Update UI to show copy as complete.

    
def
 
cancel_button_clicked_cb
(
self
,
 
button
):

        
self
.
copy_cancellable
.
cancel
()
```

For comparison, here is the same code implemented using the synchronous version
of g_file_copy() . Note how the order of statements is almost identical.  The
UI is blocked from updating and receiving user input, which also means that
cancellation cannot be supported. This is the main reason why this code should
not be used in practice:

```
static
 
void

copy_button_clicked_cb
 
(
GtkButton
 
*
button

                        
gpointer
   
user_data
)

{

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
source
 
=
 
NULL
,
 
*
destination
 
=
 
NULL
;
  
/* owned */

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
user_data
));

  
/* Build source and destination file paths. */

  
source
 
=
 
g_file_new_for_path
 
(
/* some path generated from UI */
);

  
destination
 
=
 
g_file_new_for_path
 
(
/* some other path generated from UI */
);

  
g_file_copy
 
(
source
,
 
destination
,
 
G_FILE_COPY_NONE
,

               
NULL
  
/* cancellable */
,
 
NULL
,
 
NULL
,

               
&
error
);

  
g_object_unref
 
(
destination
);

  
g_object_unref
 
(
source
);

  
/* Handle completion of the operation. */

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Should update the UI to signal failure.

       * Ignore failure due to cancellation. */

      
g_warning
 
(
"Failed to copy file: %s"
,
 
error
->
message
);

    
}

  
g_clear_error
 
(
&
error
);

  
/* Update UI to show copy as complete. */

}
```

```
import
 
logging

from
 
gi.repository
 
import
 
GLib

from
 
gi.repository
 
import
 
Gio

from
 
gi.repository
 
import
 
Gtk

class
 
MyWidget
(
Gtk
.
Widget
):

    
def
 
copy_button_clicked_cb
(
self
,
 
button
):

        
# Calculate source and destination paths from the UI.

        
source
 
=
 
Gio
.
File
.
new_for_path
(
source_path
)

        
destination
 
=
 
Gio
.
File
.
new_for_path
(
destination_path
)

        
try
:

            
source
.
copy
(
destination
,
 
Gio
.
FileCopyFlags
.
NONE
,
 
None
,
 
None
,
 
None
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
if
 
err
.
matches
(
Gio
.
io_error_quark
(),
 
Gio
.
IOErrorEnum
.
CANCELLED
):

                
# Should update the UI to signal failure.

                
# Ignore failure due to cancellation.

                
logging
.
warning
(
"Failed to copy file: 
%s
"
,
 
err
.
message
)

            
else
:

                
raise

        
# Update UI to show copy as complete.
```

### Operations in series ¶

A common situation is to run multiple asynchronous operations in series, when
each operation depends on the previous one completing.

In this example, the application reads a socket address from a file, opens a
connection to that address, reads a message, and then finishes.

Key points in this example are:

- Each callback is numbered consistently, and they are all placed in order in
the file so the code follows sequentially.
- As in the single-call example, a single GCancellable indicates that the
series of operations is ongoing. Cancelling it aborts the entire sequence.
- As in the single-call example, the pending operation is cancelled if the
owning MyObject instance is disposed, to prevent callbacks being called
later with an invalid MyObject pointer.

```
static
 
void

connect_to_server_cb1
 
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
);

static
 
void

connect_to_server_cb2
 
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
);

static
 
void

connect_to_server_cb3
 
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
);

static
 
void

connect_to_server
 
(
MyObject
 
*
self
)

{

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
address_file
 
=
 
NULL
;
  
/* owned */

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
if
 
(
priv
->
connect_cancellable
 
!=
 
NULL
)

    
{

      
/* Already connecting. */

      
return
;

    
}

  
/* Set up a cancellable. */

  
priv
->
connect_cancellable
 
=
 
g_cancellable_new
 
();

  
/* Read the socket address. */

  
address_file
 
=
 
build_address_file
 
();

  
g_file_load_contents_async
 
(
address_file
,
 
priv
->
connect_cancellable
,

                              
connect_to_server_cb1
,
 
self
);

  
g_object_unref
 
(
address_file
);

}

static
 
void

connect_to_server_cb1
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
address_file
;
  
/* unowned */

  
gchar
 
*
address
 
=
 
NULL
;
  
/* owned */

  
gsize
 
address_size
 
=
 
0
;

  
GInetAddress
 
*
inet_address
 
=
 
NULL
;
  
/* owned */

  
GInetSocketAddress
 
*
inet_socket_address
 
=
 
NULL
;
  
/* owned */

  
guint16
 
port
 
=
 
123
;

  
GSocketClient
 
*
socket_client
 
=
 
NULL
;
  
/* owned */

  
GError
 
*
error
 
=
 
NULL
;

  
address_file
 
=
 
G_FILE
 
(
source_object
);

  
self
 
=
 
MY_OBJECT
 
(
user_data
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish loading the address. */

  
g_file_load_contents_finish
 
(
address_file
,
 
result
,
 
&
address
,

                               
&
address_size
,
 
NULL
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

      
goto
 
done
;

    
}

  
/* Parse the address. */

  
inet_address
 
=
 
g_inet_address_new_from_string
 
(
address
);

  
if
 
(
inet_address
 
==
 
NULL
)

    
{

      
/* Error. */

      
g_set_error
 
(
&
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_INVALID_ARGUMENT
,

                   
"Invalid address ‘%s’."
,
 
address
);

      
goto
 
done
;

    
}

  
inet_socket_address
 
=
 
g_inet_socket_address_new
 
(
inet_address
,
 
port
);

  
/* Connect to the given address. */

  
socket_client
 
=
 
g_socket_client_new
 
();

  
g_socket_client_connect_async
 
(
socket_client
,

                                 
G_SOCKET_CONNECTABLE
 
(
inet_socket_address
),

                                 
priv
->
connect_cancellable
,

                                 
connect_to_server_cb2
,

                                 
self
);

done
:

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Stop the operation. */

      
if
 
(
!
g_error_matches
 
(
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_CANCELLED
))

        
{

          
g_warning
 
(
"Failed to load server address: %s"
,
 
error
->
message
);

        
}

      
g_clear_object
 
(
&
priv
->
connect_cancellable
);

      
g_error_free
 
(
error
);

    
}

  
g_free
 
(
address
);

  
g_clear_object
 
(
&
inet_address
);

  
g_clear_object
 
(
&
inet_socket_address
);

  
g_clear_object
 
(
&
socket_client
);

}

static
 
void

connect_to_server_cb2
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GSocketClient
 
*
socket_client
;
  
/* unowned */

  
GSocketConnection
 
*
connection
 
=
 
NULL
;
  
/* owned */

  
GInputStream
 
*
input_stream
;
  
/* unowned */

  
GError
 
*
error
 
=
 
NULL
;

  
socket_client
 
=
 
G_SOCKET_CLIENT
 
(
source_object
);

  
self
 
=
 
MY_OBJECT
 
(
user_data
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish connecting to the socket. */

  
connection
 
=
 
g_socket_client_connect_finish
 
(
socket_client
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

      
goto
 
done
;

    
}

  
/* Store a reference to the connection so it is kept open while we read from

   * it: #GInputStream does not keep a reference to a #GIOStream which contains

   * it. */

  
priv
->
connection
 
=
 
g_object_ref
 
(
connection
);

  
/* Read a message from the connection. This uses a single buffer stored in

   * #MyObject, meaning that only one connect_to_server() operation can run at

   * any time. The buffer could instead be allocated dynamically if this is a

   * problem. */

  
input_stream
 
=
 
g_io_stream_get_input_stream
 
(
G_IO_STREAM
 
(
connection
));

  
g_input_stream_read_async
 
(
input_stream
,

                             
priv
->
message_buffer
,

                             
sizeof
 
(
priv
->
message_buffer
),

                             
G_PRIORITY_DEFAULT
,
 
priv
->
connect_cancellable
,

                             
connect_to_server_cb3
,
 
self
);

done
:

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Stop the operation. */

      
if
 
(
!
g_error_matches
 
(
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_CANCELLED
))

        
{

          
g_warning
 
(
"Failed to connect to server: %s"
,
 
error
->
message
);

        
}

      
g_clear_object
 
(
&
priv
->
connect_cancellable
);

      
g_clear_object
 
(
&
priv
->
connection
);

      
g_error_free
 
(
error
);

    
}

  
g_clear_object
 
(
&
connection
);

}

static
 
void

connect_to_server_cb3
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GInputStream
 
*
input_stream
;
  
/* unowned */

  
gssize
 
len
 
=
 
0
;

  
GError
 
*
error
 
=
 
NULL
;

  
input_stream
 
=
 
G_INPUT_STREAM
 
(
source_object
);

  
self
 
=
 
MY_OBJECT
 
(
user_data
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish reading from the socket. */

  
len
 
=
 
g_input_stream_read_finish
 
(
input_stream
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

      
goto
 
done
;

    
}

  
/* Handle the message. */

  
g_assert_cmpint
 
(
len
,
 
>=
,
 
0
);

  
g_assert_cmpuint
 
((
gsize
)
 
len
,
 
<=
,
 
sizeof
 
(
priv
->
message_buffer
));

  
handle_received_message
 
(
self
,
 
priv
->
message_buffer
,
 
len
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

      
goto
 
done
;

    
}

done
:

  
/* Unconditionally mark the operation as finished.

   *

   * The streams should automatically close as this

   * last reference is dropped. */

  
g_clear_object
 
(
&
priv
->
connect_cancellable
);

  
g_clear_object
 
(
&
priv
->
connection
);

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Warn about the error. */

      
if
 
(
!
g_error_matches
 
(
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_CANCELLED
))

        
{

          
g_warning
 
(
"Failed to read from the server: %s"
,
 
error
->
message
);

        
}

      
g_error_free
 
(
error
);

    
}

}

static
 
void

my_object_dispose
 
(
GObject
 
*
obj
)

{

  
MyObjectPrivate
 
*
priv
;

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
obj
));

  
/* Cancel any ongoing connection operations.

   *

   * This ensures that if #MyObject is disposed part-way through the

   * connect_to_server() sequence of operations, the sequence gets cancelled and

   * doesn’t continue with an invalid #MyObject pointer. */

  
g_cancellable_cancel
 
(
priv
->
connect_cancellable
);

  
/* Do other dispose calls here. */

  
/* Chain up. */

  
G_OBJECT_CLASS
 
(
my_object_parent_class
)
->
dispose
 
(
obj
);

}
```

```
import
 
logging

from
 
gi.repository
 
import
 
GObject

from
 
gi.repository
 
import
 
GLib

from
 
gi.repository
 
import
 
Gio

class
 
MyObject
(
GObject
.
Object
):

    
connect_cancellable
 
=
 
None

    
def
 
connect_to_server
(
self
):

        
if
 
self
.
connect_cancellable
 
is
 
not
 
None
:

            
# Already connecting.

            
return

        
# Set up a cancellable.

        
self
.
connect_cancellable
 
=
 
Gio
.
Cancellable
()

        
# Read the socket address.

        
address_file
 
=
 
build_address_file
()

        
address_file
.
load_contents_async
(
self
.
connect_cancellable
,

                                         
connect_to_server_cb1
)

    
def
 
connect_to_server_cb1
(
self
,
 
address_file
,
 
result
,
 
user_data
=
None
):

        
try
:

            
# Finish loading the address.

            
address
,
 
etags
 
=
 
address_file
.
load_contents_finish
(
result
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
logging
.
error
(
'Failed to connect to the server. '
 
+
 
err
.
message
)

            
self
.
connect_cancellable
 
=
 
None

            
return

        
try
:

            
# Parse the address.

            
inet_address
 
=
 
Gio
.
InetAddress
.
new_from_string
(
address
)

        
except
 
TypeError
 
as
 
err
:

            
logging
.
error
(
"Invalid address ‘
%s
’."
,
 
address
)

            
self
.
connect_cancellable
 
=
 
None

            
return

        
port
 
=
 
123

        
inet_socket_address
 
=
 
Gio
.
InetSocketAddress
.
new
(
inet_address
,
 
port
)

        
# Connect to the given address.

        
socket_client
 
=
 
Gio
.
SocketClient
()

        
socket_client
.
connect_async
(
inet_socket_address
,

                                    
self
.
connect_cancellable
,

                                    
connect_to_server_cb2
)

    
def
 
connect_to_server_cb2
(
self
,
 
socket_client
,
 
result
,
 
user_data
=
None
):

        
try
:

            
# Finish connecting to the socket.

            
self
.
connection
 
=
 
socket_client
.
connect_finish
(
result
)

            
# Read a message from the connection.

            
input_stream
 
=
 
self
.
connection
.
get_input_stream
()

            
self
.
message_buffer
 
=
 
input_stream
.
read_async
(
GLib
.
PRIORITY_DEFAULT
,

                                                          
self
.
connect_cancellable
,

                                                          
connect_to_server_cb3
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
# Stop the operation.

            
self
.
connect_cancellable
 
=
 
None

            
self
.
connection
 
=
 
None

            
if
 
err
.
matches
(
Gio
.
io_error_quark
(),
 
GLib
.
IOErrorEnum
.
CANCELLED
):

                
logging
.
error
(
"Failed to connect to server: 
%s
"
,
 
error
->
message
)

    
def
 
connect_to_server_cb3
(
self
,
 
input_stream
,
 
result
,
 
user_data
=
None
):

        
# Finish reading from the socket.

        
try
:

            
length
 
=
 
input_stream
.
read_finish
(
result
);

            
# Handle the message.

            
assert
 
0
 
<=
 
length
 
<=
 
len
(
data
.
message_buffer
)

            
self
.
handle_received_message
(
self
.
message_buffer
,
 
length
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
# Unconditionally mark the operation as finished.

            
#

            
# The streams should automatically close as this

            
# last reference is dropped.

            
self
.
connect_cancellable
 
=
 
None

            
self
.
connection
 
=
 
None

            
# Warn about the error.

            
if
 
err
.
matches
(
Gio
.
io_error_quark
(),
 
GLib
.
IOErrorEnum
.
CANCELLED
):

                
logging
.
error
(
"Failed to read from the server: 
%s
"
,
 
err
.
message
)
```

### Operations in parallel ¶

Another common situation is to run multiple asynchronous operations in parallel,
considering the overall operation complete when all its constituents are
complete.

In this example, the application deletes multiple files in parallel.

Key points in this example are:

- The number of pending asynchronous operations (ones which have started but not
yet finished) is tracked as n_deletions_pending . The delete_files_cb() callback only considers the entire operation complete once this reaches zero.
- n_deletions_to_start tracks deletion operations being started, in case g_file_delete_async() manages to use a fast path and complete
synchronously (without blocking).
- As in the single-call example, all pending deletions are cancelled if the
owning MyObject instance is disposed, to prevent callbacks being called
later with an invalid MyObject pointer.

```
static
 
void

delete_files_cb
 
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
);

static
 
void

delete_files
 
(
MyObject
 
*
self
,

              
GPtrArray
/*<owned GFile*>>*/
 
*
files
)

{

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
address_file
 
=
 
NULL
;
  
/* owned */

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Set up a cancellable if no operation is ongoing already. */

  
if
 
(
priv
->
delete_cancellable
 
==
 
NULL
)

    
{

      
priv
->
delete_cancellable
 
=
 
g_cancellable_new
 
();

      
priv
->
n_deletions_pending
 
=
 
0
;

      
priv
->
n_deletions_total
 
=
 
0
;

    
}

  
/* Update internal state, and temporarily set @n_deletions_to_start. This is

   * used in delete_files_cb() to avoid indicating the overall operation has

   * completed while deletions are still being started. This can happen if

   * g_file_delete_async() completes synchronously, for example if there’s a

   * non-blocking fast path for the given file system. */

  
priv
->
n_deletions_pending
 
+=
 
files
->
len
;

  
priv
->
n_deletions_total
 
+=
 
files
->
len
;

  
priv
->
n_deletions_to_start
 
=
 
files
->
len
;

  
/* Update the UI to indicate the files are being deleted. */

  
update_ui_to_show_progress
 
(
self
,

                              
priv
->
n_deletions_pending
,

                              
priv
->
n_deletions_total
);

  
/* Start all the deletion operations in parallel. They share the same

   * #GCancellable. */

  
for
 
(
i
 
=
 
0
;
 
i
 
<
 
files
->
len
;
 
i
++
)

    
{

      
GFile
 
*
file
 
=
 
files
->
pdata
[
i
];

      
priv
->
n_deletions_to_start
--
;

      
g_file_delete_async
 
(
file
,
 
G_PRIORITY_DEFAULT
,
 
priv
->
delete_cancellable
,

                           
delete_files_cb
,
 
self
);

    
}

}

static
 
void

delete_files_cb
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GFile
 
*
file
;
  
/* unowned */

  
GError
 
*
error
 
=
 
NULL
;

  
file
 
=
 
G_FILE
 
(
source_object
);

  
self
 
=
 
MY_OBJECT
 
(
user_data
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish deleting the file. */

  
g_file_delete_finish
 
(
file
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
 
&&

      
!
g_error_matches
 
(
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_CANCELLED
))

    
{

      
g_warning
 
(
"Error deleting file: %s"
,
 
error
->
message
);

    
}

  
g_clear_error
 
(
&
error
);

  
/* Update the internal state. */

  
g_assert_cmpuint
 
(
priv
->
n_deletions_pending
,
 
>
,
 
0
);

  
priv
->
n_deletions_pending
--
;

  
/* Update the UI to show progress. */

  
update_ui_to_show_progress
 
(
self
,

                              
priv
->
n_deletions_pending
,

                              
priv
->
n_deletions_total
);

  
/* If all deletions have completed, and no more are being started,

   * update the UI to show completion. */

  
if
 
(
priv
->
n_deletions_pending
 
==
 
0
 
&&
 
priv
->
n_deletions_to_start
 
==
 
0
)

    
{

      
update_ui_to_show_completion
 
(
self
);

      
/* Clear the operation state. */

      
g_clear_object
 
(
&
priv
->
delete_cancellable
);

      
priv
->
n_deletions_total
 
=
 
0
;

    
}

}

static
 
void

my_object_dispose
 
(
GObject
 
*
obj
)

{

  
MyObjectPrivate
 
*
priv
;

  
priv
 
=
 
my_object_get_instance_private
 
(
MY_OBJECT
 
(
obj
));

  
/* Cancel any ongoing deletion operations.

   *

   * This ensures that if #MyObject is disposed part-way through the

   * delete_files() set of operations, the set gets cancelled and

   * doesn’t continue with an invalid #MyObject pointer. */

  
g_cancellable_cancel
 
(
priv
->
delete_cancellable
);

  
/* Do other dispose calls here. */

  
/* Chain up. */

  
G_OBJECT_CLASS
 
(
my_object_parent_class
)
->
dispose
 
(
obj
);

}
```

```
import
 
logging

from
 
gi.repository
 
import
 
GObject

from
 
gi.repository
 
import
 
GLib

from
 
gi.repository
 
import
 
Gio

class
 
MyObject
(
GObject
.
Object
):

    
delete_cancellable
 
=
 
None

    
def
 
delete_files
(
self
,
 
files
:
 
list
[
Gio
.
File
]):

        
# Set up a cancellable if no operation is ongoing already.

        
if
 
self
.
delete_cancellable
 
is
 
None
:

            
self
.
delete_cancellable
 
=
 
Gio
.
Cancellable
();

            
self
.
n_deletions_pending
 
=
 
0
;

            
self
.
n_deletions_total
 
=
 
0
;

        
# Update internal state, and temporarily set @n_deletions_to_start. This is

        
# used in delete_files_cb() to avoid indicating the overall operation has

        
# completed while deletions are still being started. This can happen if

        
# Gio.File.delete_async() completes synchronously, for example if there’s a

        
# non-blocking fast path for the given file system.

        
self
.
n_deletions_pending
 
+=
 
len
(
files
)

        
self
.
n_deletions_total
 
+=
 
len
(
files
)

        
self
.
n_deletions_to_start
 
=
 
len
(
files
)

        
# Update the UI to indicate the files are being deleted.

        
self
.
update_ui_to_show_progress
(
self
.
n_deletions_pending
,

                                        
self
.
n_deletions_total
)

        
# Start all the deletion operations in parallel. They share the same

        
# #GCancellable.

        
for
 
i
 
in
 
range
(
len
(
files
)):

            
file
 
=
 
files
[
i
]

            
self
.
n_deletions_to_start
 
-=
 
1

            
file
.
delete_async
(
GLib
.
PRIORITY_DEFAULT
,
 
self
.
delete_cancellable
,

                              
delete_files_cb
)

    
def
 
delete_files_cb
(
self
,
 
file
,
 
result
,
 
user_data
=
None
):

        
try
:

            
# Finish deleting the file.

            
file
.
delete_finish
(
result
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
if
 
err
.
matches
(
Gio
.
io_error_quark
(),
 
GLig
.
IOErrorEnum
.
CANCELLED
):

                
logging
.
warning
(
"Error deleting file: 
%s
"
,
 
err
.
message
)

            
else
:

                
raise

        
# Update the internal state.

        
assert
 
self
.
n_deletions_pending
 
>
 
0

        
self
.
n_deletions_pending
 
-=
 
1

        
# Update the UI to show progress.

        
self
.
update_ui_to_show_progress
(
self
.
n_deletions_pending
,

                                        
self
.
n_deletions_total
)

        
# If all deletions have completed, and no more are being started,

        
# update the UI to show completion.

        
if
 
not
 
self
.
n_deletions_pending
 
and
 
not
 
self
.
n_deletions_to_start
:

            
self
.
update_ui_to_show_completion
()

            
# Clear the operation state.

            
self
.
delete_cancellable
 
=
 
None

            
self
.
n_deletions_total
 
=
 
0
```

### Wrapping with GTask ¶

Often when an asynchronous operation (or set of operations) becomes more
complex, it needs associated state. This is typically stored in a custom
structure — but defining a new structure to store the standard callback, user
data and cancellable tuple is laborious. GTask eases this by providing a
standardized way to wrap all three, plus extra custom ‘task data’.

The use of a GTask can replace the use of a GCancellable for
indicating whether an operation is ongoing.

This example is functionally the same as the operations in series example,
but refactored to use a GTask to wrap the sequence of operations.

Key points in this example are:

- State which was in MyObjectPrivate in the series example is now in the ConnectToServerData closure, which is set as the ‘task data’ of the GTask representing the overall operation. This means it’s automatically
freed after the operation returns.
- Furthermore, this means that manipulations of MyObjectPrivate state are
limited to the start and end of the sequence of operations, so reusing the
task in different situations becomes possible—for example, it is now a lot
easier to support running multiple such tasks in parallel.
- As the GTask holds a reference to MyObject , it is impossible for the
object to be disposed while the sequence of operations is ongoing, so the my_object_dispose() code has been removed. Instead, a my_object_close() method exists to allow any pending operations can be
cancelled so MyObject can be disposed when desired.

```
static
 
void

connect_to_server_cb1
 
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
);

static
 
void

connect_to_server_cb2
 
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
);

static
 
void

connect_to_server_cb3
 
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
);

typedef
 
struct
 
{

  
GSocketConnection
 
*
connection
;
  
/* nullable; owned */

  
guint8
 
message_buffer
[
128
];

}
 
ConnectToServerData
;

static
 
void

connect_to_server_data_free
 
(
ConnectToServerData
 
*
data
)

{

  
g_clear_object
 
(
&
data
->
connection
);

}

void

my_object_connect_to_server_async
 
(
MyObject
            
*
self
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

  
MyObjectPrivate
 
*
priv
;

  
GTask
 
*
task
 
=
 
NULL
;
  
/* owned */

  
ConnectToServerData
 
*
data
 
=
 
NULL
;
  
/* owned */

  
GFile
 
*
address_file
 
=
 
NULL
;
  
/* owned */

  
g_return_if_fail
 
(
MY_IS_OBJECT
 
(
self
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

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
if
 
(
priv
->
connect_task
 
!=
 
NULL
)

    
{

      
g_task_report_new_error
 
(
self
,
 
callback
,
 
user_data
,
 
NULL
,

                               
G_IO_ERROR
,
 
G_IO_ERROR_PENDING
,

                               
"Already connecting to the server."
);

      
return
;

    
}

  
/* Set up a cancellable. */

  
if
 
(
cancellable
 
!=
 
NULL
)

    
{

      
g_object_ref
 
(
cancellable
);

    
}

  
else

    
{

      
cancellable
 
=
 
g_cancellable_new
 
();

    
}

  
/* Set up the task. */

  
task
 
=
 
g_task_new
 
(
self
,
 
cancellable
,
 
callback
,
 
user_data
);

  
g_task_set_check_cancellable
 
(
task
,
 
FALSE
);

  
data
 
=
 
g_malloc0
 
(
sizeof
 
(
ConnectToServerData
));

  
g_task_set_task_data
 
(
task
,
 
data
,

                        
(
GDestroyNotify
)
 
connect_to_server_data_free
);

  
g_object_unref
 
(
cancellable
);

  
priv
->
connect_task
 
=
 
g_object_ref
 
(
task
);

  
/* Read the socket address. */

  
address_file
 
=
 
build_address_file
 
();

  
g_file_load_contents_async
 
(
address_file
,
 
g_task_get_cancellable
 
(
task
),

                              
connect_to_server_cb1
,
 
g_object_ref
 
(
task
));

  
g_object_unref
 
(
address_file
);

  
g_clear_object
 
(
&
task
);

}

static
 
void

connect_to_server_cb1
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GTask
 
*
task
 
=
 
NULL
;
  
/* owned */

  
GFile
 
*
address_file
;
  
/* unowned */

  
gchar
 
*
address
 
=
 
NULL
;
  
/* owned */

  
gsize
 
address_size
 
=
 
0
;

  
GInetAddress
 
*
inet_address
 
=
 
NULL
;
  
/* owned */

  
GInetSocketAddress
 
*
inet_socket_address
 
=
 
NULL
;
  
/* owned */

  
guint16
 
port
 
=
 
123
;

  
GSocketClient
 
*
socket_client
 
=
 
NULL
;
  
/* owned */

  
GError
 
*
error
 
=
 
NULL
;

  
address_file
 
=
 
G_FILE
 
(
source_object
);

  
task
 
=
 
G_TASK
 
(
user_data
);

  
self
 
=
 
g_task_get_source_object
 
(
task
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish loading the address. */

  
g_file_load_contents_finish
 
(
address_file
,
 
result
,
 
&
address
,

                               
&
address_size
,
 
NULL
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

      
goto
 
done
;

    
}

  
/* Parse the address. */

  
inet_address
 
=
 
g_inet_address_new_from_string
 
(
address
);

  
if
 
(
inet_address
 
==
 
NULL
)

    
{

      
/* Error. */

      
g_set_error
 
(
&
error
,
 
G_IO_ERROR
,
 
G_IO_ERROR_INVALID_ARGUMENT
,

                   
"Invalid address ‘%s’."
,
 
address
);

      
goto
 
done
;

    
}

  
inet_socket_address
 
=
 
g_inet_socket_address_new
 
(
inet_address
,
 
port
);

  
/* Connect to the given address. */

  
socket_client
 
=
 
g_socket_client_new
 
();

  
g_socket_client_connect_async
 
(
socket_client
,

                                 
G_SOCKET_CONNECTABLE
 
(
inet_socket_address
),

                                 
g_task_get_cancellable
 
(
task
),

                                 
connect_to_server_cb2
,

                                 
g_object_ref
 
(
task
));

done
:

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Stop the operation and propagate the error. */

      
g_clear_object
 
(
&
priv
->
connect_task
);

      
g_task_return_error
 
(
task
,
 
error
);

    
}

  
g_free
 
(
address
);

  
g_clear_object
 
(
&
inet_address
);

  
g_clear_object
 
(
&
inet_socket_address
);

  
g_clear_object
 
(
&
socket_client
);

  
g_clear_object
 
(
&
task
);

}

static
 
void

connect_to_server_cb2
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GTask
 
*
task
 
=
 
NULL
;
  
/* owned */

  
ConnectToServerData
 
*
data
;
  
/* unowned */

  
GSocketClient
 
*
socket_client
;
  
/* unowned */

  
GSocketConnection
 
*
connection
 
=
 
NULL
;
  
/* owned */

  
GInputStream
 
*
input_stream
;
  
/* unowned */

  
GError
 
*
error
 
=
 
NULL
;

  
socket_client
 
=
 
G_SOCKET_CLIENT
 
(
source_object
);

  
task
 
=
 
G_TASK
 
(
user_data
);

  
data
 
=
 
g_task_get_task_data
 
(
task
);

  
self
 
=
 
g_task_get_source_object
 
(
task
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish connecting to the socket. */

  
connection
 
=
 
g_socket_client_connect_finish
 
(
socket_client
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

      
goto
 
done
;

    
}

  
/* Store a reference to the connection so it is kept open while we read from

   * it: #GInputStream does not keep a reference to a #GIOStream which contains

   * it. */

  
data
->
connection
 
=
 
g_object_ref
 
(
connection
);

  
/* Read a message from the connection. As the buffer is allocated as part of

   * the per-task @data, multiple tasks can run concurrently. */

  
input_stream
 
=
 
g_io_stream_get_input_stream
 
(
G_IO_STREAM
 
(
connection
));

  
g_input_stream_read_async
 
(
input_stream
,

                             
data
->
message_buffer
,

                             
sizeof
 
(
data
->
message_buffer
),

                             
G_PRIORITY_DEFAULT
,
 
g_task_get_cancellable
 
(
task
),

                             
connect_to_server_cb3
,
 
g_object_ref
 
(
task
));

done
:

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Stop the operation and propagate the error. */

      
g_clear_object
 
(
&
priv
->
connect_task
);

      
g_task_return_error
 
(
task
,
 
error
);

    
}

  
g_clear_object
 
(
&
connection
);

  
g_clear_object
 
(
&
task
);

}

static
 
void

connect_to_server_cb3
 
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

  
MyObject
 
*
self
;

  
MyObjectPrivate
 
*
priv
;

  
GTask
 
*
task
 
=
 
NULL
;
  
/* owned */

  
ConnectToServerData
 
*
data
;
  
/* unowned */

  
GInputStream
 
*
input_stream
;
  
/* unowned */

  
gssize
 
len
 
=
 
0
;

  
GError
 
*
error
 
=
 
NULL
;

  
input_stream
 
=
 
G_INPUT_STREAM
 
(
source_object
);

  
task
 
=
 
G_TASK
 
(
user_data
);

  
data
 
=
 
g_task_get_task_data
 
(
task
);

  
self
 
=
 
g_task_get_source_object
 
(
task
);

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
/* Finish reading from the socket. */

  
len
 
=
 
g_input_stream_read_finish
 
(
input_stream
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

      
goto
 
done
;

    
}

  
/* Handle the message. */

  
g_assert_cmpint
 
(
len
,
 
>=
,
 
0
);

  
g_assert_cmpuint
 
((
gsize
)
 
len
,
 
<=
,
 
sizeof
 
(
data
->
message_buffer
));

  
handle_received_message
 
(
self
,
 
data
->
message_buffer
,
 
len
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

      
goto
 
done
;

    
}

  
/* Success! */

  
g_task_return_boolean
 
(
task
,
 
TRUE
);

done
:

  
/* Unconditionally mark the operation as finished.

   *

   * The streams should automatically close as this

   * last reference is dropped. */

  
g_clear_object
 
(
&
priv
->
connect_task
);

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
/* Stop the operation and propagate the error. */

      
g_task_return_error
 
(
task
,
 
error
);

    
}

  
g_clear_object
 
(
&
task
);

}

void

my_object_connect_to_server_finish
 
(
MyObject
      
*
self
,

                                    
GAsyncResult
  
*
result
,

                                    
GError
       
**
error
)

{

  
g_return_if_fail
 
(
MY_IS_OBJECT
 
(
self
));

  
g_return_if_fail
 
(
g_task_is_valid
 
(
result
,
 
self
));

  
g_return_if_fail
 
(
error
 
==
 
NULL
 
||
 
*
error
 
==
 
NULL
);

  
g_task_propagate_boolean
 
(
G_TASK
 
(
result
),
 
error
);

}

void

my_object_close
 
(
MyObject
 
*
self
)

{

  
MyObjectPrivate
 
*
priv
;

  
g_return_if_fail
 
(
MY_IS_OBJECT
 
(
self
));

  
priv
 
=
 
my_object_get_instance_private
 
(
self
);

  
if
 
(
priv
->
connect_task
 
!=
 
NULL
)

    
{

      
GCancellable
 
*
cancellable
 
=
 
g_task_get_cancellable
 
(
priv
->
connect_task
);

      
g_cancellable_cancel
 
(
cancellable
);

    
}

}
```

```
import
 
logging

from
 
gi.repository
 
import
 
GObject

from
 
gi.repository
 
import
 
GLib

from
 
gi.repository
 
import
 
Gio

from
 
dataclasses
 
import
 
dataclass

@dataclass

class
 
ConnectToServerData
:

    
connection
:
 
Gio
.
SocketConnection
 
=
 
None

    
message_buffer
:
 
list
 
=
 
None

class
 
MyObject
(
GObject
.
Object
):

    
connect_task
 
=
 
None

    
def
 
connect_to_server_async
(
self
,
 
cancellable
,
 
callback
,
 
user_data
):

        
if
 
self
.
connect_task
 
is
 
not
 
None
:

            
error
 
=
 
GLib
.
Error
(
"Already connecting to the server."
,

                               
Gio
.
io_error_quark
(),

                               
Gio
.
IOErrorEnum
.
PENDING
)

            
Gio
.
Task
.
report_error
(
self
,
 
callback
,
 
user_data
,
 
None
,
 
error
)

            
return

        
# Set up a cancellable.

        
cancellable
 
=
 
cancellable
 
or
 
Gio
.
Cancellable
()

        
# Python bindings for Gio.Task do not pass user_data to the callback.

        
# So, we need to manually pass those to the callback by using a new

        
# function as callback to `Gio.Task.new()`. This new function will call

        
# the original callback with appropriate user_data.

        
original_callback
 
=
 
callback

        
def
 
callback
(
source_object
,
 
result
,
 
not_user_data
):

            
original_callback
(
source_object
,
 
result
,
 
user_data
)

        
# Set up the task.

        
task
 
=
 
Gio
.
Task
.
new
(
self
,
 
cancellable
,
 
callback
,
 
user_data
)

        
task
.
set_check_cancellable
(
False
)

        
# We cannot use Gio.Task.set_task_data() for any data other than integers.

        
# So, to set task data, we simply just define task.task_data attribute.

        
task
.
task_data
 
=
 
ConnectToServerData
()

        
self
.
connect_task
 
=
 
task

        
# Read the socket address.

        
address_file
 
=
 
build_address_file
()

        
address_file
.
load_contents_async
(
cancellable
,

                                         
connect_to_server_cb1
,

                                         
task
)

    
def
 
connect_to_server_cb1
(
self
,
 
address_file
,
 
result
,
 
task
):

        
try
:

            
# Finish loading the address.

            
address
,
 
etags
 
=
 
address_file
.
load_contents_finish
(
result
)

            
try
:

                
# Parse the address.

                
inet_address
 
=
 
Gio
.
InetAddress
.
new_from_string
(
address
)

            
except
 
TypeError
:

                
raise
 
GLib
.
Error
(
"Invalid address ‘
%s
’."
 
%
 
address
,

                                 
Gio
.
io_error_quark
(),

                                 
Gio
.
IOErrorEnum
.
INVALID_ARGUMENT
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
# Stop the operation and propagate the error.

            
self
.
connect_task
 
=
 
None

            
task
.
return_error
(
err
)

            
return

        
port
 
=
 
123

        
inet_socket_address
 
=
 
Gio
.
InetSocketAddress
.
new
(
inet_address
,
 
port
)

        
# Connect to the given address.

        
socket_client
 
=
 
Gio
.
SocketClient
()

        
socket_client
.
connect_async
(
inet_socket_address
,

                                    
task
.
get_cancellable
(),

                                    
connect_to_server_cb2
,

                                    
task
)

    
def
 
connect_to_server_cb2
(
self
,
 
socket_client
,
 
result
,
 
task
):

        
data
 
=
 
task
.
task_data

        
try
:

            
# Finish connecting to the socket.

            
connection
 
=
 
socket_client
.
connect_finish
(
result
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
# Stop the operation and propagate the error.

            
self
.
connect_task
 
=
 
None

            
task
.
return_error
(
err
)

            
return

        
# Store a reference to the connection so it is kept open while we read from

        
# it: #GInputStream does not keep a reference to a #GIOStream which contains

        
# it.

        
data
.
connection
 
=
 
connection

        
# Read a message from the connection. As the buffer is allocated as part of

        
# the per-task @data, multiple tasks can run concurrently.

        
input_stream
 
=
 
connection
.
get_input_stream
()

        
data
.
message_buffer
 
=
 
input_stream
.
read_async
(
GLib
.
PRIORITY_DEFAULT
,

                                                      
task
.
get_cancellable
(),

                                                      
connect_to_server_cb3
,

                                                      
task
)

    
def
 
connect_to_server_cb3
(
self
,
 
input_stream
,
 
result
,
 
task
):

        
data
 
=
 
task
.
task_data

        
try
:

            
# Finish reading from the socket.

            
length
 
=
 
input_stream
.
read_finish
(
result
)

            
# Handle the message.

            
assert
 
0
 
<=
 
length
 
<=
 
len
(
data
.
message_buffer
)

            
self
.
handle_received_message
(
data
.
message_buffer
,
 
length
)

            
# Success!

            
task
.
return_boolean
(
True
)

        
except
 
GLib
.
Error
 
as
 
err
:

            
# Unconditionally mark the operation as finished.

            
#

            
# The streams should automatically close as this

            
# last reference is dropped.

            
self
.
connect_task
 
=
 
None

            
# Stop the operation and propagate the error.

            
task
.
return_error
(
err
)

    
def
 
connect_to_server_finish
(
self
,
 
result
):

        
if
 
not
 
Gio
.
Task
.
is_valid
(
result
,
 
self
):

            
return
 
False

        
return
 
task
.
propagate_boolean
()
```
