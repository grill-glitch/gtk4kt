# Saving The Content To A File - GNOME Developer Documentation

# Saving The Content To A File ¶

In this lesson you will learn how to add a menu entry with a key shortcut,
ask the user to select a file for saving the GtkTextBuffer contents, and
save a file asynchronously.

## Add the “Save As” menu item ¶

- Open the UI definition file for your window and find the primary_menu menu definition at the bottom of the file
- Remove the “Preferences” menu item, as we are not going to need it
- In place of the removed menu item, add the definition of the Save As menu item:

```
<menu
 
id=
"primary_menu"
>

  
<section>

    
<item>

      
<attribute
 
name=
"label"
 
translatable=
"yes"
>
_Save
 
as...
</attribute>

      
<attribute
 
name=
"action"
>
win.save-as
</attribute>

    
</item>

    
<item>

      
<attribute
 
name=
"label"
 
translatable=
"yes"
>
_Keyboard
 
Shortcuts
</attribute>

      
<attribute
 
name=
"action"
>
win.show-help-overlay
</attribute>

    
</item>

    
<item>

      
<attribute
 
name=
"label"
 
translatable=
"yes"
>
_About
 
{{name}}
</attribute>

      
<attribute
 
name=
"action"
>
app.about
</attribute>

    
</item>

  
</section>

</menu>
```

The “Save as” menu item is bound to the win.save-as action; this means
that activating the menu item will activate the save-as action registered
on the TextViewerWindow window.

## Add the “Save As” action ¶

- Open the text_viewer-window.c file, and find the instance
initialization function of the TextViewerWindow widget, text_viewer_window_init
- Create the save-as action, connect a callback to its activate signal, and add the action to the window

```
static
 
void

text_viewer_window__open_file_dialog
 
(
GAction
 
*
action
,

                                      
GVariant
 
*
param
,

                                      
TextViewerWindow
 
*
self
);

static
 
void

text_viewer_window__save_file_dialog
 
(
GAction
 
*
action
,

                                      
GVariant
 
*
param
,

                                      
TextViewerWindow
 
*
self
);

static
 
void

text_viewer_window_init
 
(
TextViewerWindow
 
*
self
)

{

  
gtk_widget_init_template
 
(
GTK_WIDGET
 
(
self
));

  
g_autoptr
 
(
GSimpleAction
)
 
open_action
 
=
 
g_simple_action_new
 
(
"open"
,
 
NULL
);

  
g_signal_connect
 
(
open_action
,
 
"activate"
,
 
G_CALLBACK
 
(
text_viewer_window__open_file_dialog
),
 
self
);

  
g_action_map_add_action
 
(
G_ACTION_MAP
 
(
self
),
 
G_ACTION
 
(
open_action
));

  
g_autoptr
 
(
GSimpleAction
)
 
save_action
 
=
 
g_simple_action_new
 
(
"save-as"
,
 
NULL
);

  
g_signal_connect
 
(
save_action
,
 
"activate"
,
 
G_CALLBACK
 
(
text_viewer_window__save_file_dialog
),
 
self
);

  
g_action_map_add_action
 
(
G_ACTION_MAP
 
(
self
),
 
G_ACTION
 
(
save_action
));

  
GtkTextBuffer
 
*
buffer
 
=
 
gtk_text_view_get_buffer
 
(
self
->
main_text_view
);

  
g_signal_connect
 
(
buffer
,

                    
"notify::cursor-position"
,

                    
G_CALLBACK
 
(
text_viewer_window__update_cursor_position
),

                    
self
);

}
```

- Open the window.py file, and find the instance initialization
method of the TextViewerWindow widget
- Create the save-as action, connect a callback to its activate signal, and add the action to the window

```
def
 
__init__
(
self
,
 
**
kwargs
):

    
super
()
.
__init__
(
**
kwargs
)

    
open_action
 
=
 
Gio
.
SimpleAction
(
name
=
"open"
)

    
open_action
.
connect
(
"activate"
,
 
self
.
open_file_dialog
)

    
self
.
add_action
(
open_action
)

    
save_action
 
=
 
Gio
.
SimpleAction
(
name
=
"save-as"
)

    
save_action
.
connect
(
"activate"
,
 
self
.
save_file_dialog
)

    
self
.
add_action
(
save_action
)

    
buffer
 
=
 
self
.
main_text_view
.
get_buffer
()

    
buffer
.
connect
(
"notify::cursor-position"
,
 
self
.
update_cursor_position
)
```

- Open the window.vala file, and find the instance intialization
method of the TextViewer.Window widget
- Create the save-as action, connect a callback to its activate signal, and add the action to the window

```
namespace
 
TextViewer
 
{

    
public
 
class
 
Window
 
:
 
Gtk
.
ApplicationWindow
 
{

        
// ...

        
public
 
Window
 
(
Gtk
.
Application
 
app
)
 
{

            
Object
 
(
application
:
 
app
);

        
}

        
construct
 
{

            
var
 
open_action
 
=
 
new
 
SimpleAction
 
(
"open"
,
 
null
);

            
open_action
.
activate
.
connect
 
(
this
.
open_file_dialog
);

            
this
.
add_action
 
(
open_action
);

            
var
 
save_action
 
=
 
new
 
SimpleAction
 
(
"save-as"
,
 
null
);

            
save_action
.
activate
.
connect
 
(
this
.
save_file_dialog
);

            
this
.
add_action
 
(
save_action
);

            
Gtk
.
TextBuffer
 
buffer
 
=
 
this
.
text_view
.
buffer
;

            
buffer
.
notify
[
"cursor-position"
].
connect
 
(
this
.
update_cursor_position
);

        
}

    
}

}
```

- Open the window.js file, and find the constructor
of the TextViewer.Window widget
- Create the save-as action, connect a callback to its activate signal, and add the action to the window

```
export
 
const
 
TextViewerWindow
 
=
 
GObject
.
registerClass
({

    
GTypeName
:
 
'TextViewerWindow'
,

    
Template
:
 
'resource:///com/example/TextViewer/window.ui'
,

    
InternalChildren
:
 
[
'main_text_view'
,
 
'open_button'
,
 
'cursor_pos'
],

},
 
class
 
TextViewerWindow
 
extends
 
Adw
.
ApplicationWindow
 
{

    
constructor
(
application
)
 
{

        
super
({
 
application
 
});

        
const
 
openAction
 
=
 
new
 
Gio
.
SimpleAction
({
name
:
 
'open'
});

        
openAction
.
connect
(
'activate'
,
 
()
 
=>
 
this
.
openFileDialog
());

        
this
.
add_action
(
openAction
);

        
const
 
saveAction
 
=
 
new
 
Gio
.
SimpleAction
({
name
:
 
'save-as'
});

        
saveAction
.
connect
(
'activate'
,
 
()
 
=>
 
this
.
saveFileDialog
());

        
this
.
add_action
(
saveAction
);

        
const
 
buffer
 
=
 
this
.
_main_text_view
.
buffer
;

        
buffer
.
connect
(
"notify::cursor-position"
,
 
this
.
updateCursorPosition
.
bind
(
this
));

    
}
```

## Select a file ¶

- In the activate callback for the save-as action, create a file
selection dialog using the GTK_FILE_CHOOSER_ACTION_SAVE action, and
connect to its response signal

```
static
 
void

text_viewer_window__save_file_dialog
 
(
GAction
          
*
action
 
G_GNUC_UNUSED
,

                                      
GVariant
         
*
param
 
G_GNUC_UNUSED
,

                                      
TextViewerWindow
 
*
self
)

{

  
g_autoptr
 
(
GtkFileDialog
)
 
dialog
 
=

    
gtk_file_dialog_new
 
();

  
gtk_file_dialog_save
 
(
dialog
,

                        
GTK_WINDOW
 
(
self
),

                        
NULL
,

                        
on_save_response
,

                        
self
);

}
```

```
def
 
save_file_dialog
(
self
,
 
action
,
 
_
):

    
native
 
=
 
Gtk
.
FileDialog
()

    
native
.
save
(
self
,
 
None
,
 
self
.
on_save_response
)
```

```
private
 
void
 
save_file_dialog
 
(
Variant
?
 
parameter
)
 
{

    
var
 
filechooser
 
=
 
new
 
Gtk
.
FileChooserNative
 
(
"Save File As"
,

                                                 
this
,

                                                 
Gtk
.
FileChooserAction
.
SAVE
,

                                                 
"_Save"
,

                                                 
"_Cancel"
);

    
filechooser
.
response
.
connect
 
(
  
);

    
filechooser
.
show
 
();

}
```

```
saveFileDialog
()
 
{

    
const
 
fileDialog
 
=
 
new
 
Gtk
.
FileDialog
();

    
fileDialog
.
save
(
this
,
 
null
,
 
async
 
(
self
,
 
result
)
 
=>
 
{

        
// we'll implement it in the next step

    
});

}
```

- In the callback, retrieve the GFile for the location selected by the user, and call
the save_file() function

```
static
 
void

on_save_response
 
(
GObject
      
*
source
,

                  
GAsyncResult
 
*
result
,

                  
gpointer
      
user_data
)

{

  
GtkFileDialog
 
*
dialog
 
=
 
GTK_FILE_DIALOG
 
(
source
);

  
TextViewerWindow
 
*
self
 
=
 
user_data
;

  
g_autoptr
 
(
GFile
)
 
file
 
=

    
gtk_file_dialog_save_finish
 
(
dialog
,
 
result
,
 
NULL
);

  
if
 
(
file
 
!=
 
NULL
)

    
save_file
 
(
self
,
 
file
);

}
```

```
def
 
on_save_response
(
self
,
 
dialog
,
 
result
):

  
file
 
=
 
dialog
.
save_finish
(
result
)

  
if
 
file
 
is
 
not
 
None
:

      
self
.
save_file
(
file
)
```

```
private
 
void
 
save_file_dialog
 
(
Variant
?
 
parameter
)
 
{

    
var
 
filechooser
 
=
 
new
 
Gtk
.
FileChooserNative
 
(
"Save File As"
,

                                                 
this
,

                                                 
Gtk
.
FileChooserAction
.
SAVE
,

                                                 
"_Save"
,

                                                 
"_Cancel"
);

    
filechooser
.
response
.
connect
 
((
dialog
,
 
response
)
 
{

        
if
 
(
response
 
==
 
Gtk
.
ResponseType
.
ACCEPT
)
 
{

            
File
 
file
 
=
 
filechooser
.
get_file
 
();

            
this
.
save_file
 
(
file
);

        
}

    
});

    
filechooser
.
show
 
();

}
```

```
saveFileDialog
()
 
{

    
const
 
fileDialog
 
=
 
new
 
Gtk
.
FileDialog
();

    
fileDialog
.
save
(
this
,
 
null
,
 
async
 
(
self
,
 
result
)
 
=>
 
{

        
try
 
{

            
const
 
file
 
=
 
self
.
save_finish
(
result
);

            
if
 
(
file
)
 
{

                
await
 
this
.
saveFile
(
file
);
 
// we will define this method soon

            
}

        
}
 
catch
(
_
)
 
{

            
// user closed the dialog without selecting any file

        
}

    
});

}
```

## Save the contents of the text buffer ¶

- In the save_file function, retrieve the contents of the GtkTextBuffer using the start and end GtkTextIter as the
bounds of the buffer, then start an asynchronous operation to save
the data in the location pointed by the GFile

```
 
static
 
void

 
save_file
 
(
TextViewerWindow
 
*
self
,

            
GFile
            
*
file
)

 
{

   
GtkTextBuffer
 
*
buffer
 
=
 
gtk_text_view_get_buffer
 
(
self
->
main_text_view
);

   
// Retrieve the iterator at the start of the buffer

   
GtkTextIter
 
start
;

   
gtk_text_buffer_get_start_iter
 
(
buffer
,
 
&
start
);

   
// Retrieve the iterator at the end of the buffer

   
GtkTextIter
 
end
;

   
gtk_text_buffer_get_end_iter
 
(
buffer
,
 
&
end
);

   
// Retrieve all the visible text between the two bounds

   
char
 
*
text
 
=
 
gtk_text_buffer_get_text
 
(
buffer
,
 
&
start
,
 
&
end
,
 
FALSE
);

   
// If there is nothing to save, return early

   
if
 
(
text
 
==
 
NULL
)

     
return
;

   
g_autoptr
(
GBytes
)
 
bytes
 
=
 
g_bytes_new_take
 
(
text
,
 
strlen
 
(
text
));

   
// Start the asynchronous operation to save the data into the file

   
g_file_replace_contents_bytes_async
 
(
file
,

                                        
bytes
,

                                        
NULL
,

                                        
FALSE
,

                                        
G_FILE_CREATE_NONE
,

                                        
NULL
,

                                        
save_file_complete
,

                                        
self
);

}
```

- In the save_file_complete function, finish the asynchronous
operation and report any error

```
static
 
void

save_file_complete
 
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

  
GFile
 
*
file
 
=
 
G_FILE
 
(
source_object
);

  
g_autoptr
 
(
GError
)
 
error
 
=
  
NULL
;

  
g_file_replace_contents_finish
 
(
file
,
 
result
,
 
NULL
,
 
&
error
);

  
// Query the display name for the file

  
g_autofree
 
char
 
*
display_name
 
=
 
NULL
;

  
g_autoptr
 
(
GFileInfo
)
 
info
 
=

  
g_file_query_info
 
(
file
,

                     
"standard::display-name"
,

                     
G_FILE_QUERY_INFO_NONE
,

                     
NULL
,

                     
NULL
);

  
if
 
(
info
 
!=
 
NULL
)

    
{

      
display_name
 
=

        
g_strdup
 
(
g_file_info_get_attribute_string
 
(
info
,
 
"standard::display-name"
));

    
}

  
else

    
{

      
display_name
 
=
 
g_file_get_basename
 
(
file
);

    
}

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_printerr
 
(
"Unable to save “%s”: %s
\n
"
,

                  
display_name
,

                  
error
->
message
);

    
}

}
```

- Import the GLib module alongside Adw, Gio, and Gtk

```
from
 
gi.repository
 
import
 
Adw
,
 
Gio
,
 
GLib
,
 
Gtk
```

- In the save_file function, retrieve the contents of the GtkTextBuffer using the start and end GtkTextIter as the
bounds of the buffer, then start an asynchronous operation to save
the data in the location pointed by the GFile

```
def
 
save_file
(
self
,
 
file
):

    
buffer
 
=
 
self
.
main_text_view
.
get_buffer
()

    
# Retrieve the iterator at the start of the buffer

    
start
 
=
 
buffer
.
get_start_iter
()

    
# Retrieve the iterator at the end of the buffer

    
end
 
=
 
buffer
.
get_end_iter
()

    
# Retrieve all the visible text between the two bounds

    
text
 
=
 
buffer
.
get_text
(
start
,
 
end
,
 
False
)

    
# If there is nothing to save, return early

    
if
 
not
 
text
:

        
return

    
bytes
 
=
 
GLib
.
Bytes
.
new
(
text
.
encode
(
'utf-8'
))

    
# Start the asynchronous operation to save the data into the file

    
file
.
replace_contents_bytes_async
(
bytes
,

                                      
None
,

                                      
False
,

                                      
Gio
.
FileCreateFlags
.
NONE
,

                                      
None
,

                                      
self
.
save_file_complete
)
```

- In the save_file_complete function, finish the asynchronous
operation and report any error

```
def
 
save_file_complete
(
self
,
 
file
,
 
result
):

    
res
 
=
 
file
.
replace_contents_finish
(
result
)

    
info
 
=
 
file
.
query_info
(
"standard::display-name"
,

                           
Gio
.
FileQueryInfoFlags
.
NONE
)

    
if
 
info
:

        
display_name
 
=
 
info
.
get_attribute_string
(
"standard::display-name"
)

    
else
:

        
display_name
 
=
 
file
.
get_basename
()

    
if
 
not
 
res
:

        
print
(
f
"Unable to save 
{
display_name
}
"
)
```

- In the save_file function, retrieve the contents of the Gtk.TextBuffer using the start and end Gtk.TextIter as the
bounds of the buffer, then start an asynchronous operation to save
the data in the location pointed by the File

```
private
 
void
 
save_file
 
(
File
 
file
)
 
{

    
Gtk
.
TextBuffer
 
buffer
 
=
 
this
.
main_text_view
.
buffer
;

    
// Retrieve the iterator at the start of the buffer

    
Gtk
.
TextIter
 
start
;

    
buffer
.
get_start_iter
 
(
out
 
start
);

    
// Retrieve the iterator at the end of the buffer

    
Gtk
.
TextIter
 
end
;

    
buffer
.
get_end_iter
 
(
out
 
end
);

    
// Retrieve all the visible text between the two bounds

    
string
?
 
text
 
=
 
buffer
.
get_text
 
(
start
,
 
end
,
 
false
);

    
if
 
(
text
 
==
 
null
 
||
 
text
.
length
 
==
 
0
)

        
return
;

    
var
 
bytes
 
=
 
new
 
Bytes
.
take
 
(
text
.
data
);

    
file
.
replace_contents_bytes_async
.
begin
 
(
bytes
,

                                             
null
,

                                             
false
,

                                             
FileCreateFlags
.
NONE
,

                                             
null
,

                                             
(
object
,
 
result
)
 
=>
 
{
 
});

}
```

- In the lambda that gets called when the asynchronous function ends,
finish the asynchronous operation and report any error

```
private
 
void
 
save_file
 
(
File
 
file
)
 
{

    
Gtk
.
TextBuffer
 
buffer
 
=
 
this
.
main_text_view
.
buffer
;

    
// Retrieve the iterator at the start of the buffer

    
Gtk
.
TextIter
 
start
;

    
buffer
.
get_start_iter
 
(
out
 
start
);

    
// Retrieve the iterator at the end of the buffer

    
Gtk
.
TextIter
 
end
;

    
buffer
.
get_end_iter
 
(
out
 
end
);

    
// Retrieve all the visible text between the two bounds

    
string
?
 
text
 
=
 
buffer
.
get_text
 
(
start
,
 
end
,
 
false
);

    
if
 
(
text
 
==
 
null
 
||
 
text
.
length
 
==
 
0
)

        
return
;

    
var
 
bytes
 
=
 
new
 
Bytes
.
take
 
(
text
.
data
);

    
file
.
replace_contents_bytes_async
.
begin
 
(
bytes
,

                                             
null
,

                                             
false
,

                                             
FileCreateFlags
.
NONE
,

                                             
null
,

                                             
(
object
,
 
result
)
 
=>
 
{

        
string
 
display_name
;

        
// Query the display name for the file

        
try
 
{

            
FileInfo
 
info
 
=
 
file
.
query_info
 
(
"standard::display-name"
,

                                             
FileQueryInfoFlags
.
NONE
);

            
display_name
 
=
 
info
.
get_attribute_string
 
(
"standard::display-name"
);

        
}
 
catch
 
(
Error
 
e
)
 
{

            
display_name
 
=
 
file
.
get_basename
 
();

        
}

        
try
 
{

            
file
.
replace_contents_async
.
end
 
(
result
,
 
null
);

        
}
 
catch
 
(
Error
 
e
)
 
{

            
stderr
.
printf
 
(
"Unable to save “%s”: %s
\n
"
,
 
display_name
,
 
e
.
message
);

        
}

    
});

}
```

- Just like we did with the load_contents_async function when we were dealing
with opening the file, let’s turn the Gio’s built-in replace_contents_bytes_async function into a Promise returning one.
That way, we’re going to be able to utilize the convenient async / await syntax.
Open main.js , and add the following lines:

```
import
 
GObject
 
from
 
'gi://GObject'
;

import
 
Gio
 
from
 
'gi://Gio'
;

import
 
Gtk
 
from
 
'gi://Gtk?version=4.0'
;

import
 
Adw
 
from
 
'gi://Adw?version=1'
;

import
 
GLib
 
from
 
'gi://GLib'
;

import
 
{
 
TextViewerWindow
 
}
 
from
 
'./window.js'
;

Gio
.
_promisify
(
Gio
.
File
.
prototype
,

    
'load_contents_async'
,

    
'load_contents_finish'
);

Gio
.
_promisify
(
Gio
.
File
.
prototype
,

    
'replace_contents_bytes_async'
,

    
'replace_contents_finish'
);

// ...
```

- Back in window.js , we need to define a new saveFile function since it is
going to be called when user selects a file to save the contents to.
In the function, we need to retrieve the contents of the GtkTextBuffer using the start and end GtkTextIter as the
bounds of the buffer. Then, start an asynchronous operation to save
the data in the location pointed by the GFile .

```
async
 
saveFile
(
file
)
 
{

    
const
 
buffer
 
=
 
this
.
_main_text_view
.
buffer
;

    
// Retrieve the start and end iterators

    
const
 
startIterator
 
=
 
buffer
.
get_start_iter
();

    
const
 
endIterator
 
=
 
buffer
.
get_end_iter
();

    
// Retrieve all the visible text between the two bounds

    
const
 
text
 
=
 
buffer
.
get_text
(
startIterator
,
 
endIterator
,
 
false
);

    
if
 
(
text
 
===
 
null
 
||
 
text
.
length
 
===
 
0
)
 
{

        
logWarning
(
"Text is empty, ignoring"
);

        
return
;

    
}

    
// Get file's name, which will be needed in case of an error

    
let
 
fileName
;

    
try
 
{

        
const
 
fileInfo
 
=
 
file
.
query_info
(
"standard::display-name"
,
 
FileQueryInfoFlags
.
NONE
);

        
fileName
 
=
 
fileInfo
.
get_attribute_string
(
"standard::display-name"
);

    
}
 
catch
(
_
)
 
{

        
fileName
 
=
 
file
.
get_basename
();

    
}

    
try
 
{

        
// Save the file (asynchronously)

        
await
 
file
.
replace_contents_bytes_async
(

            
new
 
GLib
.
Bytes
(
text
),

            
null
,

            
false
,

            
Gio
.
FileCreateFlags
.
NONE
,

            
null
);

    
}
 
catch
(
e
)
 
{

        
logError
(
`Unable to save 
${
fileName
}
: 
${
e
.
message
}
`
)

    
}

}
```

## Add a key shortcut for the “Save As” action ¶

- Open the text_viewer-application.c source file and find the TextViewerApplication instance initialization function text_viewer_application_init
- Add Ctrl + Shift + S as the accelerator shortcut
for the win.save-as action

```
static
 
void

text_viewer_application_init
 
(
TextViewerApplication
 
*
self
)

{

  
// ...

  
gtk_application_set_accels_for_action
 
(
GTK_APPLICATION
 
(
self
),

                                         
"win.save-as"
,

                                         
(
const
 
char
 
*
[])
 
{

                                           
"<Ctrl><Shift>s"
,

                                           
NULL
,

                                         
});

}
```

- Open the main.py source file and find the instance initialization
function for the Application class
- Add Ctrl + Shift + S as the accelerator shortcut
for the win.save-as action

```
class
 
Application
(
Adw
.
Application
):

    
def
 
__init__
(
self
):

        
super
()
.
__init__
(
application_id
=
'com.example.PyTextViewer'
,

                         
flags
=
Gio
.
ApplicationFlags
.
FLAGS_NONE
)

        
# ...

        
self
.
set_accels_for_action
(
'win.save-as'
,
 
[
'<Ctrl><Shift>s'
])
```

- Open the application.vala source file and find the instance initialization
function for the TextViewer.Application class
- Add Ctrl + Shift + S as the accelerator shortcut
for the win.save-as action

```
public
 
Application
 
()
 
{

    
Object
 
(
application_id
:
 
"com.example.TextViewer"
,

            
flags
:
 
ApplicationFlags
.
FLAGS_NONE
);

}

construct
 
{

    
// ...

    
this
.
set_accels_for_action
 
(
"win.save-as"
,
 
{
 
"<Ctrl><Shift>s"
 
});

}
```

- Open the main.js source file and find the constructor
for the TextViewerApplication class
- Add Ctrl + Shift + S as the accelerator shortcut
for the win.save-as action

```
constructor
()
 
{

   
super
({
application_id
:
 
'com.example.TextViewer'
,
 
flags
:
 
Gio
.
ApplicationFlags
.
FLAGS_NONE
});

   
this
.
set_accels_for_action
(
'win.open'
,
 
[
 
'<Ctrl>o'
 
]);

   
this
.
set_accels_for_action
(
'win.save-as'
,
 
[
 
'<Ctrl><Shift>s'
 
]);

   
// ...

}
```

## Add the “Save As” shortcut to the Keyboard Shortcuts help ¶

- Find the help-overlay.ui file in the sources directory
- Find the GtkShortcutsGroup definition
- Add a new GtkShortcutsShortcut definition for the win.save action
in the shortcuts group

```
<object
 
class=
"GtkShortcutsGroup"
>

  
<property
 
name=
"title"
 
translatable=
"yes"
 
context=
"shortcut window"
>
General
</property>

  
<child>

    
<object
 
class=
"GtkShortcutsShortcut"
>

      
<property
 
name=
"title"
 
translatable=
"yes"
 
context=
"shortcut window"
>
Open
</property>

      
<property
 
name=
"action-name"
>
win.open
</property>

    
</object>

  
</child>

  
<child>

    
<object
 
class=
"GtkShortcutsShortcut"
>

      
<property
 
name=
"title"
 
translatable=
"yes"
 
context=
"shortcut window"
>
Save
 
As
</property>

      
<property
 
name=
"action-name"
>
win.save-as
</property>

    
</object>

  
</child>
```

At the end of this lesson, you should be able to:

- select the “Save As” menu item from the primary menu
- select a file from a dialog
- save the contents of the text viewer in the selected file
