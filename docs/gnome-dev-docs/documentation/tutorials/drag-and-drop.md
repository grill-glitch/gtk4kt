# Drag and Drop - GNOME Developer Documentation

# Drag and Drop ¶

Drag and drop is an essential part of applications that deal with many files,
and very easy to add to an application.

## Adding a drop target ¶

You will need to create a GtkDropTarget , which is an event controller that,
connected to a widget, emits the “drop” signal if the dragged object is dropped
on the widget. To receive the data, connect to that signal.

For this first example, we will setup the program to receive files from another
program, such as Nautilus. Signal connections are included in this snippet for
ease of copy-and-paste.

```
GtkDropTarget
 
*
target
 
=
 
gtk_drop_target_new
 
(
G_TYPE_INVALID
,
 
GDK_ACTION_COPY
);

gtk_drop_target_set_gtypes
 
(
target
,
 
(
GType
[
1
])
 
{
 
GDK_TYPE_FILE_LIST
,
 
},
 
1
);

g_signal_connect
 
(
target
,
 
"drop"
,
 
G_CALLBACK
 
(
on_drop
),
 
NULL
);

g_signal_connect
 
(
target
,
 
"enter"
,
 
G_CALLBACK
 
(
on_enter
),
 
my_window
);

g_signal_connect
 
(
target
,
 
"leave"
,
 
G_CALLBACK
 
(
on_leave
),
 
my_window
);

gtk_widget_add_controller
 
(
GTK_WIDGET
 
(
my_window
),
 
GTK_EVENT_CONTROLLER
 
(
target
));
```

```
content
 
=
 
Gdk
.
ContentFormats
.
new_for_gtype
(
Gdk
.
FileList
)

target
 
=
 
Gtk
.
DropTarget
(
formats
=
content
,
 
actions
=
Gdk
.
DragAction
.
COPY
)

target
.
connect
(
'drop'
,
 
on_drop
)

target
.
connect
(
'enter'
,
 
on_enter
)

target
.
connect
(
'leave'
,
 
on_leave
)

# self here is a widget.

self
.
add_controller
(
target
)
```

```
var
 
target
 
=
 
new
 
Gtk
.
DropTarget
 
(
typeof
 
(
Gdk
.
FileList
),
 
COPY
);

target
.
on_drop
.
connect
 
(
on_drop
);

// Visually emphasize the user that they are dropping a widget.

target
.
enter
.
connect
 
(
on_enter
);

target
.
leave
.
connect
 
(
on_leave
);

((
Gtk
.
Widget
)
 
this
).
add_controller
 
(
target
);
```

```
var
 
target
 
=
 
new
 
DropTarget
(
FileList
.
getType
(),
 
DragAction
.
COPY
);

target
.
onDrop
(
this
::
handleDrop
);

target
.
onEnter
(
this
::
handleEnter
);

target
.
onLeave
(
this
::
handleLeave
);

((
Widget
)
 
this
).
addController
(
target
);
```

We have told GTK that this widget is ready to be used for dragging and dropping.
If you drag a file from your file manager, it will show the drag and drop icon.
Next, we have to retrieve the data.

```
static
 
gboolean

on_drop
 
(
GtkDropTarget
 
*
target
,

         
const
 
GValue
 
*
value
,

         
double
 
x
,

         
double
 
y
,

         
gpointer
 
data
)

{

  
/* GdkFileList is a boxed value so we use the boxed API. */

  
GdkFileList
 
*
file_list
 
=
 
g_value_get_boxed
 
(
value
);

  
GSList
 
*
list
 
=
 
gdk_file_list_get_files
 
(
file_list
);

  
/* Loop through the files and print their names. */

  
for
 
(
GSList
 
*
l
 
=
 
list
;
 
l
 
!=
 
NULL
;
 
l
 
=
 
l
->
next
)

    
{

      
GFile
*
 
file
 
=
 
l
->
data
;

      
g_print
 
(
"%s
\n
"
,
 
g_file_get_path
 
(
file
));

    
}

  
return
 
TRUE
;

}
```

```
def
 
on_drop
(
self
,
 
drop_target
,
 
value
:
 
Gdk
.
FileList
,
 
x
,
 
y
,
 
user_data
=
None
):

    
files
:
 
List
[
Gio
.
File
]
 
=
 
value
.
get_files
()

    
# Loop through the files and print their names.

    
for
 
file
 
in
 
files
:

        
print
(
file
.
get_path
())
```

```
private
 
bool
 
on_drop
 
(
Value
 
value
,
 
double
 
x
,
 
double
 
y
)
 
{

   
var
 
file_list
 
=
 
(
Gdk
.
FileList
)
 
value
;

   
// Loop through the files and print their names.

   
foreach
 
(
var
 
file
 
in
 
file_list
.
get_files
 
())
 
{

      
print
 
(
"%s
\n
"
,
 
file
.
get_path
 
());

   
}

   
return
 
true
;

}
```

```
private
 
boolean
 
handleDrop
(
@Nullable
 
Value
 
value
,
 
double
 
x
,
 
double
 
y
)
 
{

   
var
 
fileList
 
=
 
new
 
FileList
(
value
.
getBoxed
());

   
// Loop through the files and print their names.

   
for
 
(
var
 
file
 
:
 
fileList
.
getFiles
())

      
System
.
out
.
println
(
file
.
getPath
());

   
return
 
true
;

}
```

You can perform some custom operation when the pointer enters the widget during
a drag operation:

```
static
 
GdkDragAction

on_enter
 
(
GtkDropTarget
 
*
target
,

          
double
 
x
,

          
double
 
y
,

          
gpointer
 
data
)

{

  
/* Custom code... */

  
/* Tell the callee to continue. */

  
return
 
GDK_ACTION_COPY
;

}

static
 
void

on_leave
 
(
GtkDropTarget
 
*
target
,

          
gpointer
 
data
)

{

  
/* Custom code... */

}
```

```
def
 
on_enter
(
self
,
 
drop_target
,
 
x
,
 
y
):

    
# Custom code...

    
# Tell the callee to continue

    
return
 
Gdk
.
DragAction
.
COPY

def
 
on_leave
(
self
,
 
drop_target
):

    
# Custom code...
```

```
private
 
Gdk
.
DragAction
 
on_enter
 
(
double
 
x
,
 
double
 
y
)
 
{

   
// Custom code...

   
// Tell the callee to continue.

   
return
 
COPY
;

}

private
 
void
 
on_leave
 
()
 
{

   
// Custom code...

}
```

```
private
 
Set
<
DragAction
>
 
handleEnter
(
double
 
x
,
 
double
 
y
)
 
{

   
// Custom code...

   
// Tell the callee to continue.

   
return
 
Set
.
of
(
DragAction
.
COPY
);

}

private
 
void
 
handleLeave
 
()
 
{

   
// Custom code...

}
```

Note

GTK already provides sets the :drop(active) pseudoclass on a widget
containing the cursor during a drag operation, so you don’t need custom
code to handle a style change.

It is possible to use any type supported by the GObject type system for drag and drop
operations within the same application, including complex types.

If you want to support drag and drop of complex types across applications, you will
have to use the GdkContentSerializer and GdkContentDeserializer API.
