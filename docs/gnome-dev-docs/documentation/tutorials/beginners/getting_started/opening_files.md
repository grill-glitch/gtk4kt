# Loading Content From A File - GNOME Developer Documentation

# Loading Content From A File ¶

In this lesson you will learn how to ask the user to select a file, load the
file’s contents, and then put those contents into the text area of our text
viewer.

![../../../_images/opening_files.png](https://developer.gnome.org/documentation/_images/opening_files.png)

## Add an “Open” button ¶

In order to open a file, you need to let the user select it. You can follow
these instructions to add a button to the window’s header bar that will open a file selection dialog.

### Update the UI definition ¶

- Open the text_viewer-window.ui file
- Find the object definition for the AdwHeaderBar widget
- Add an object definition for a GtkButton as a child of the header
bar, packing it at the leading edge of the window decoration using the start type:

```
<object
 
class=
"AdwHeaderBar"
 
id=
"header_bar"
>

  
<child
 
type=
"start"
>

    
<object
 
class=
"GtkButton"
 
id=
"open_button"
>

      
<property
 
name=
"label"
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

  
<child
 
type=
"end"
>

    
<object
 
class=
"GtkMenuButton"
>

      
<property
 
name=
"primary"
>
True
</property>

      
<property
 
name=
"icon-name"
>
open-menu-symbolic
</property>

      
<property
 
name=
"tooltip-text"
 
translatable=
"yes"
>
Menu
</property>

      
<property
 
name=
"menu-model"
>
primary_menu
</property>

    
</object>

  
</child>
```

- The button has the open_button identifier, so you can bind it in the window
template.
- The button also has an action-name property set to win.open ; this
action will be activated when the user presses the button.

### Bind the template in your source code ¶

- Open the text_viewer-window.c file
- Add the open_button widget to the instance structure of TextViewerWindow :

```
struct
 
_TextViewerWindow

{

  
AdwApplicationWindow
  
parent_instance
;

  
/* Template widgets */

  
AdwHeaderBar
 
*
header_bar
;

  
GtkTextView
 
*
main_text_view
;

  
GtkButton
 
*
open_button
;

};
```

- Bind the open_button widget in the class initialization for TextViewerWindow , text_viewer_window_class_init :

```
static
 
void

text_viewer_window_class_init
 
(
TextViewerWindowClass
 
*
klass
)

{

  
GtkWidgetClass
 
*
widget_class
 
=
 
GTK_WIDGET_CLASS
 
(
klass
);

  
gtk_widget_class_set_template_from_resource
 
(
widget_class
,
 
"/com/example/TextViewer/text_viewer-window.ui"
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,

                                        
TextViewerWindow
,

                                        
header_bar
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,

                                        
TextViewerWindow
,

                                        
main_text_view
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,

                                        
TextViewerWindow
,

                                        
open_button
);

}
```

- Open the window.py file
- Add the open_button widget to the instance structure of TextViewerWindow :

```
@Gtk
.
Template
(
resource_path
=
'/com/example/TextViewer/window.ui'
)

class
 
TextViewerWindow
(
Adw
.
ApplicationWindow
):

    
__gtype_name__
 
=
 
'TextViewerWindow'

    
main_text_view
 
=
 
Gtk
.
Template
.
Child
()

    
open_button
 
=
 
Gtk
.
Template
.
Child
()

    
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
```

- Open the window.vala file
- Add the open_button widget to the instance structure of TextViewer.Window :

```
namespace
 
TextViewer
 
{

    
[
GtkTemplate
 
(
ui
 
=
 
"/org/example/app/window.ui"
)]

    
public
 
class
 
Window
 
:
 
Adw
.
ApplicationWindow
 
{

        
[
GtkChild
]

        
private
 
unowned
 
Gtk
.
TextView
 
main_text_view
;

        
[
GtkChild
]

        
private
 
unowned
 
Gtk
.
Button
 
open_button
;

        
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

    
}

}
```

- Open the window.js file
- Add the open_button widget to the InternalChildren array of TextViewerWindow :

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

    
}

});
```

## Add the “Open” action ¶

Add the open action to the instance
initialization for TextViewerWindow .

Once you add the open action to the window, you can address it
as win.open :

- Modify the TextViewerWindow instance initialization function text_viewer_window_init to create a GSimpleAction and
add it to the window

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
parameter
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

}
```

- Open the text_viewer-application.c source file and find the TextViewerApplication instance initialization function text_viewer_application_init
- Add Ctrl + O as the accelerator shortcut for the win.open action static void text_viewer_application_init ( TextViewerApplication * self ) { // ... gtk_application_set_accels_for_action ( GTK_APPLICATION ( self ), "win.open" , ( const char * []) { "<Ctrl>o" , NULL , }); }

- Import the Gio module and modify the TextViewerWindow instance
initialization to create a GSimpleAction

```
from
 
gi.repository
 
import
 
Adw
,
 
Gio
,
 
Gtk

@Gtk
.
Template
(
resource_path
=
'/com/example/TextViewer/window.ui'
)

class
 
TextViewerWindow
(
Gtk
.
ApplicationWindow
):

  
__gtype_name__
 
=
 
'TextViewerWindow'

  
main_text_view
 
=
 
Gtk
.
Template
.
Child
()

  
open_button
 
=
 
Gtk
.
Template
.
Child
()

  
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

  
def
 
open_file_dialog
(
self
,
 
action
,
 
_
):

      
pass
```

- Open the main.py source file and find the Application instance initialization function
- Add Ctrl + O as the accelerator shortcut for the win.open action class Application ( Adw . Application ): def __init__ ( self ): super () . __init__ ( application_id = 'com.example.TextViewer' , flags = Gio . ApplicationFlags . FLAGS_NONE ) # ... self . set_accels_for_action ( 'win.open' , [ '<Ctrl>o' ])

- Add a construct block to TextViewer.Window to create
a SimpleAction and add it to the window

```
namespace
 
TextViewer
 
{

    
[
GtkTemplate
 
(
ui
 
=
 
"/org/example/app/window.ui"
)]

    
public
 
class
 
Window
 
:
 
Gtk
.
ApplicationWindow
 
{

        
[
GtkChild
]

        
private
 
unowned
 
Gtk
.
TextView
 
main_text_view
;

        
[
GtkChild
]

        
private
 
unowned
 
Gtk
.
Button
 
open_button
;

        
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

        
}

    
}

}
```

- Open the application.vala source file and
find the instance initialization function
- Add Ctrl + O as the accelerator shortcut for the win.open action

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
"win.open"
,
 
{
 
"<Ctrl>o"
 
});

}
```

- Extend the constructor of TextViewerWindow to create
a SimpleAction and add it to the window. The action, when
activated, will invoke a new function called openFileDialog

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

   
}

    
openFileDialog
()
 
{

        
// We'll write some logic here in a moment

    
}

});
```

The Gio.SimpleAction class comes from the Gio library. You will have to
import it:

```
import
 
GObject
 
from
 
'gi://GObject'
;

import
 
Gtk
 
from
 
'gi://Gtk'
;

import
 
Adw
 
from
 
'gi://Adw'
;

import
 
Gio
 
from
 
'gi://Gio'
;
```

- Open the main.js source file and
find the TextViewerApplication constructor
- Add Ctrl + O as the accelerator shortcut for the win.open action

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

    
// ...

}
```

## Select a file ¶

Now that you have added action, you must define the function that will be
called when the action is activated.

- Inside the text_viewer_window__open_file_dialog function you
create a GtkFileDialog object, which will present a file selection dialog to the user:

```
static
 
void

text_viewer_window__open_file_dialog
 
(
GAction
          
*
action
 
G_GNUC_UNUSED
,

                                      
GVariant
         
*
parameter
 
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

  
gtk_file_dialog_open
 
(
dialog
,

                        
GTK_WINDOW
 
(
self
),

                        
NULL
,

                        
on_open_response
,

                        
self
);

}
```

- The on_open_response function handles the response of the user
once they have selected the file and closed the dialog, or simply closed
the dialog without selecting a file:

```
static
 
void

on_open_response
 
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

    
gtk_file_dialog_open_finish
 
(
dialog
,
 
result
,
 
NULL
);

  
// If the user selected a file, open it

  
if
 
(
file
 
!=
 
NULL
)

    
open_file
 
(
self
,
 
file
);

}
```

- Inside the open_file_dialog method you create a GtkFileDialog object, which will present a file selection dialog to the user:

```
def
 
open_file_dialog
(
self
,
 
action
,
 
parameter
):

    
# Create a new file selection dialog, using the "open" mode

    
native
 
=
 
Gtk
.
FileDialog
()

    
native
.
open
(
self
,
 
None
,
 
self
.
on_open_response
)
```

- The on_open_response method handles the response of the user
once they have selected the file and closed the dialog, or simply
closed the dialog without selecting a file:

```
def
 
on_open_response
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
open_finish
(
result
)

    
# If the user selected a file...

    
if
 
file
 
is
 
not
 
None
:

        
# ... open it

        
self
.
open_file
(
file
)

def
 
open_file
(
self
,
 
file
):

    
pass
```

- Inside the open_file_dialog method in TextViewer.Window in window.vala ,
you create a Gtk.FileChooserNative object, which will present
a file selection dialog to the user

```
private
 
void
 
open_file_dialog
 
(
Variant
?
 
parameter
)
 
{

    
// Create a new file selection dialog, using the "open" mode

    
// and keep a reference to it

    
var
 
filechooser
 
=
 
new
 
Gtk
.
FileChooserNative
 
(
"Open File"
,
 
null
,
 
Gtk
.
FileChooserAction
.
OPEN
,
 
"_Open"
,
 
"_Cancel"
)
 
{

        
transient_for
 
=
 
this

    
};

    
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

- When the filechooser emits the response signal, the following code in the lambda gets executed.
This happens once the user has selected the file and closed the dialog, or simply
closed the dialog without selecting a file:

```
private
 
void
 
open_file_dialog
 
(
Variant
?
 
parameter
)
 
{

    
// Create a new file selection dialog, using the "open" mode

    
// and keep a reference to it

    
var
 
filechooser
 
=
 
new
 
Gtk
.
FileChooserNative
 
(
"Open File"
,
 
null
,
 
Gtk
.
FileChooserAction
.
OPEN
,
 
"_Open"
,
 
"_Cancel"
)
 
{

        
transient_for
 
=
 
this

    
};

    
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
 
=>
 
{

        
// If the user selected a file...

        
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

            
// ... retrieve the location from the dialog and open it

            
this
.
open_file
 
(
filechooser
.
get_file
 
());

        
}

    
});

    
filechooser
.
show
 
();

}
```

- Inside the openFileDialog method in TextViewerWindow in window.js ,
create a Gtk.FileDialog object, which will present
a file selection dialog to the user

```
openFileDialog
()
 
{

   
// Create a new file selection dialog

   
const
 
fileDialog
 
=
 
new
 
Gtk
.
FileDialog
();

   
// Open the dialog and handle user's selection

   
fileDialog
.
open
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
open_finish
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
openFile
(
file
);
 
// We will define this method soon

         
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

## Read the contents of a file ¶

Reading the contents of a file can take an arbitrary amount of time, and
block the application’s control flow. For this reason, it’s recommended that
you load the file asynchronously. This requires starting the “read” operation
in the open_file function:

```
static
 
void

open_file
 
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

  
g_file_load_contents_async
 
(
file
,

                              
NULL
,

                              
(
GAsyncReadyCallback
)
 
open_file_complete
,

                              
self
);

}
```

```
def
 
open_file
(
self
,
 
file
):

    
file
.
load_contents_async
(
None
,
 
self
.
open_file_complete
)
```

```
private
 
void
 
open_file
 
(
File
 
file
)
 
{

    
file
.
load_contents_async
.
begin
 
(
null
,
 
(
object
,
 
result
)
 
=>
 
{});

}
```

First, let’s make it so that we will be able to use the async / await syntax, to make our code cleaner.
Open the main.js from the src directory, and add the following code,
below the imports:

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

// ...
```

We’ve used the Promisify Helper to turn the load_contents_async function into a Promise -returning one.

Let’s now call that function, back in the openFile method of the TextViewerWindow class:

```
async
 
openFile
(
file
)
 
{

    
let
 
contentsBytes
;

    
try
 
{

        
// Retrieve contents asynchronously

        
// The first index of the returned array contains a byte

        
// array of the contents

        
contentsBytes
 
=
 
(
await
 
file
.
load_contents_async
(
null
))[
0
];

    
}
 
catch
 
(
e
)
 
{

        
logError
(
e
,
 
`Unable to open 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

}
```

Once the asynchronous operation is complete, or if there has been an error,
the open_file_complete function will be called, and you will need to
complete the asynchronous loading operation:

```
static
 
void

open_file_complete
 
(
GObject
          
*
source_object
,

                    
GAsyncResult
     
*
result
,

                    
TextViewerWindow
 
*
self
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

  
g_autofree
 
char
 
*
contents
 
=
 
NULL
;

  
gsize
 
length
 
=
 
0
;

  
g_autoptr
 
(
GError
)
 
error
 
=
 
NULL
;

  
// Complete the asynchronous operation; this function will either

  
// give you the contents of the file as a byte array, or will

  
// set the error argument

  
g_file_load_contents_finish
 
(
file
,

                               
result
,

                               
&
contents
,

                               
&
length
,

                               
NULL
,

                               
&
error
);

  
// In case of error, print a warning to the standard error output

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_printerr
 
(
"Unable to open “%s”: %s
\n
"
,

                  
g_file_peek_path
 
(
file
),

                  
error
->
message
);

      
return
;

    
}

 
}
```

```
def
 
open_file_complete
(
self
,
 
file
,
 
result
):

    
contents
 
=
 
file
.
load_contents_finish
(
result
)

    
if
 
not
 
contents
[
0
]:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to open 
{
path
}
: 
{
contents
[
1
]
}
"
)

        
return
```

```
private
 
void
 
open_file
 
(
File
 
file
)
 
{

    
file
.
load_contents_async
.
begin
 
(
null
,
 
(
object
,
 
result
)
 
=>
 
{

      
uint8
[]
 
contents
;

      
try
 
{

         
// Complete the asynchronous operation; this function will either

         
// give you the contents of the file as a byte array, or will

         
// raise an exception

         
file
.
load_contents_async
.
end
 
(
result
,
 
out
 
contents
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

         
// In case of an error, print a warning to the standard error output

         
stderr
.
printf
 
(
"Unable to open “%s“: %s"
,
 
file
.
peek_path
 
(),
 
e
.
message
);

      
}

    
});

}
```

Since we’ve used the async / await syntax, we do not need to do
anything else to finish the file read operation. That wouldn’t be the case
if you went with the “traditional” asynchronous operation handling

## Show the contents inside the text area ¶

Now that you have the contents of the file, you can display them in the GtkTextView widget.

- Verify that the contents of the file are encoded using UTF-8, as that
is what GTK requires for all its text widgets

```
static
 
void

open_file_complete
 
(
GObject
          
*
source_object
,

                    
GAsyncResult
     
*
result
,

                    
TextViewerWindow
 
*
self
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

  
g_autofree
 
char
 
*
contents
 
=
 
NULL
;

  
gsize
 
length
 
=
 
0
;

  
g_autoptr
 
(
GError
)
 
error
 
=
 
NULL
;

  
// Complete the asynchronous operation; this function will either

  
// give you the contents of the file as a byte array, or will

  
// set the error argument

  
g_file_load_contents_finish
 
(
file
,

                               
result
,

                               
&
contents
,

                               
&
length
,

                               
NULL
,

                               
&
error
);

  
// In case of error, print a warning to the standard error output

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_printerr
 
(
"Unable to open the “%s”: %s
\n
"
,

                  
g_file_peek_path
 
(
file
),

                  
error
->
message
);

      
return
;

    
}

  
// Ensure that the file is encoded with UTF-8

  
if
 
(
!
g_utf8_validate
 
(
contents
,
 
length
,
 
NULL
))

    
{

      
g_printerr
 
(
"Unable to load the contents of “%s”: "

                  
"the file is not encoded with UTF-8
\n
"
,

                  
g_file_peek_path
 
(
file
));

      
return
;

    
}

}
```

- Modify the open_file_complete function to retrieve the GtkTextBuffer instance that the GtkTextView widget uses
to store the text, and set its contents

```
static
 
void

open_file_complete
 
(
GObject
          
*
source_object
,

                    
GAsyncResult
     
*
result
,

                    
TextViewerWindow
 
*
self
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

  
g_autofree
 
char
 
*
contents
 
=
 
NULL
;

  
gsize
 
length
 
=
 
0
;

  
g_autoptr
 
(
GError
)
 
error
 
=
 
NULL
;

  
// Complete the asynchronous operation; this function will either

  
// give you the contents of the file as a byte array, or will

  
// set the error argument

  
g_file_load_contents_finish
 
(
file
,

                               
result
,

                               
&
contents
,

                               
&
length
,

                               
NULL
,

                               
&
error
);

  
// In case of error, print a warning to the standard error output

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_printerr
 
(
"Unable to open the “%s”: %s
\n
"
,

                  
g_file_peek_path
 
(
file
),

                  
error
->
message
);

      
return
;

    
}

  
// Ensure that the file is encoded with UTF-8

  
if
 
(
!
g_utf8_validate
 
(
contents
,
 
length
,
 
NULL
))

    
{

      
g_printerr
 
(
"Unable to load the contents of “%s”: "

                  
"the file is not encoded with UTF-8
\n
"
,

                  
g_file_peek_path
 
(
file
));

      
return
;

    
}

  
// Retrieve the GtkTextBuffer instance that stores the

  
// text displayed by the GtkTextView widget

  
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

  
// Set the text using the contents of the file

  
gtk_text_buffer_set_text
 
(
buffer
,
 
contents
,
 
length
);

  
// Reposition the cursor so it's at the start of the text

  
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

  
gtk_text_buffer_place_cursor
 
(
buffer
,
 
&
start
);

}
```

- Verify that the contents of the file are encoded using UTF-8, as that
is what GTK requires for all its text widgets

```
def
 
open_file_complete
(
self
,
 
file
,
 
result
):

    
contents
 
=
 
file
.
load_contents_finish
(
result
)

    
if
 
not
 
contents
[
0
]:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to open 
{
path
}
: 
{
contents
[
1
]
}
"
)

        
return

    
try
:

        
text
 
=
 
contents
[
1
]
.
decode
(
'utf-8'
)

    
except
 
UnicodeError
 
as
 
err
:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to load the contents of 
{
path
}
: the file is not encoded with UTF-8"
)

        
return
```

- Modify the open_file_complete function to retrieve the GtkTextBuffer instance that the GtkTextView widget uses
to store the text, and set its contents

```
def
 
open_file_complete
(
self
,
 
file
,
 
result
):

    
contents
 
=
 
file
.
load_contents_finish
(
result
)

    
if
 
not
 
contents
[
0
]:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to open 
{
path
}
: 
{
contents
[
1
]
}
"
)

        
return

    
try
:

        
text
 
=
 
contents
[
1
]
.
decode
(
'utf-8'
)

    
except
 
UnicodeError
 
as
 
err
:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to load the contents of 
{
path
}
: the file is not encoded with UTF-8"
)

        
return

    
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
set_text
(
text
)

    
start
 
=
 
buffer
.
get_start_iter
()

    
buffer
.
place_cursor
(
start
)
```

- Verify that the contents of the file are encoded using UTF-8, as that
is what GTK requires for all its text widgets

```
private
 
void
 
open_file
 
(
File
 
file
)
 
{

    
file
.
load_contents_async
.
begin
 
(
null
,
 
(
object
,
 
result
)
 
=>
 
{

        
uint8
[]
 
contents
;

        
try
 
{

            
// Complete the asynchronous operation; this function will either

            
// give you the contents of the file as a byte array, or will

            
// raise an exception

            
file
.
load_contents_async
.
end
 
(
result
,
 
out
 
contents
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

            
// In case of an error, print a warning to the standard error output

            
stderr
.
printf
 
(
"Unable to open “%s“: %s"
,
 
file
.
peek_path
 
(),
 
e
.
message
);

        
}

        
// Ensure that the file is encoded with UTF-8

        
if
 
(
!
((
string
)
 
contents
).
validate
 
())

            
stderr
.
printf
 
(
"Unable to load the contents of “%s”: "
+

                           
"the file is not encoded with UTF-8
\n
"
,

                           
file
.
peek_path
 
());

    
});

}
```

- Modify the handler of the end of loading to retrieve the Gtk.TextBuffer instance that the Gtk.TextView widget uses to store the text, and
set its contents

```
private
 
void
 
open_file
 
(
File
 
file
)
 
{

    
file
.
load_contents_async
.
begin
 
(
null
,
 
(
object
,
 
result
)
 
=>
 
{

        
uint8
[]
 
contents
;

        
try
 
{

            
// Complete the asynchronous operation; this function will either

            
// give you the contents of the file as a byte array, or will

            
// raise an exception

            
file
.
load_contents_async
.
end
 
(
result
,
 
out
 
contents
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

            
// In case of an error, print a warning to the standard error output

            
stderr
.
printf
 
(
"Unable to open “%s“: %s"
,
 
file
.
peek_path
 
(),
 
e
.
message
);

        
}

        
// Ensure that the file is encoded with UTF-8

        
if
 
(
!
((
string
)
 
contents
).
validate
 
())

            
stderr
.
printf
 
(
"Unable to load the contents of “%s”: "
+

                           
"the file is not encoded with UTF-8
\n
"
,

                           
file
.
peek_path
 
());

        
// Retrieve the GtkTextBuffer instance that stores the

        
// text displayed by the GtkTextView widget

        
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

        
// Set the text using the contents of the file

        
buffer
.
text
 
=
 
(
string
)
 
contents
;

        
// Reposition the cursor so it's at the start of the text

        
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

        
buffer
.
place_cursor
 
(
start
);

    
});

}
```

- Import GLib , since it contains a function that we’re going to
need - utf8_validate

```
import
 
GObject
 
from
 
'gi://GObject'
;

import
 
Gtk
 
from
 
'gi://Gtk'
;

import
 
Adw
 
from
 
'gi://Adw'
;

import
 
Gio
 
from
 
'gi://Gio'
;

import
 
GLib
 
from
 
'gi://GLib'
;
```

- Verify that the contents of the file are encoded using UTF-8, as that
is what GTK requires for all its text widgets

```
async
 
openFile
(
file
)
 
{

    
let
 
contentsBytes
;

    
try
 
{

        
contentsBytes
 
=
 
(
await
 
file
.
load_contents_async
(
null
))[
0
];

    
}
 
catch
 
(
e
)
 
{

        
logError
(
e
,
 
`Unable to open 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

    
if
 
(
!
GLib
.
utf8_validate
(
contentsBytes
))
 
{

        
logError
(
`Invalid text encoding for 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

}
```

- Retrieve the Gtk.TextBuffer instance that the Gtk.TextView widget
uses to store the text, and set its contents

```
async
 
openFile
(
file
)
 
{

    
let
 
contentsBytes
;

    
try
 
{

        
contentsBytes
 
=
 
(
await
 
file
.
load_contents_async
(
null
))[
0
];

    
}
 
catch
 
(
e
)
 
{

        
logError
(
e
,
 
`Unable to open 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

    
if
 
(
!
GLib
.
utf8_validate
(
contentsBytes
))
 
{

        
logError
(
`Invalid text encoding for 
${
file
.
peek_path
()
}
`
);

    
}

    
// Convert a UTF-8 bytes array into a String

    
const
 
contentsText
 
=
 
new
 
TextDecoder
(
'utf-8'
).
decode
(
contentsBytes
);

    
// Retrieve the GtkTextBuffer instance that stores the

    
// text displayed by the GtkTextView widget

    
const
 
buffer
 
=
 
this
.
_main_text_view
.
buffer
;

    
// Set the text using the contents of the file

    
buffer
.
text
 
=
 
contentsText
;

    
// Reposition the cursor so it's at the start of the text

    
const
 
startIterator
 
=
 
buffer
.
get_start_iter
();

    
buffer
.
place_cursor
(
startIterator
);

}
```

## Update the title of the window ¶

Since the application now is showing the contents of a specific file, you
should ensure that the user interface reflects this new state. One way to
do this is to update the title of the window with the name of the file.

Since the name of the file uses the raw encoding for files provided by the
operating system, we need to query the file for its display name .

- Modify the open_file_complete function to query the “display name” file
attribute
- Set the title of the window using the display name

```
static
 
void

open_file_complete
 
(
GObject
          
*
source_object
,

                    
GAsyncResult
     
*
result
,

                    
TextViewerWindow
 
*
self
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

  
g_autofree
 
char
 
*
contents
 
=
 
NULL
;

  
gsize
 
length
 
=
 
0
;

  
g_autoptr
 
(
GError
)
 
error
 
=
 
NULL
;

  
// Complete the asynchronous operation; this function will either

  
// give you the contents of the file as a byte array, or will

  
// set the error argument

  
g_file_load_contents_finish
 
(
file
,

                               
result
,

                               
&
contents
,

                               
&
length
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

  
// In case of error, print a warning to the standard error output

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_printerr
 
(
"Unable to open “%s”: %s
\n
"
,

                  
g_file_peek_path
 
(
file
),

                  
error
->
message
);

      
return
;

    
}

  
// Ensure that the file is encoded with UTF-8

  
if
 
(
!
g_utf8_validate
 
(
contents
,
 
length
,
 
NULL
))

    
{

      
g_printerr
 
(
"Unable to load the contents of “%s”: "

                  
"the file is not encoded with UTF-8
\n
"
,

                  
g_file_peek_path
 
(
file
));

      
return
;

    
}

  
// Retrieve the GtkTextBuffer instance that stores the

  
// text displayed by the GtkTextView widget

  
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

  
// Set the text using the contents of the file

  
gtk_text_buffer_set_text
 
(
buffer
,
 
contents
,
 
length
);

  
// Reposition the cursor so it's at the start of the text

  
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

  
gtk_text_buffer_place_cursor
 
(
buffer
,
 
&
start
);

  
// Set the title using the display name

  
gtk_window_set_title
 
(
GTK_WINDOW
 
(
self
),
 
display_name
);

}
```

```
def
 
open_file_complete
(
self
,
 
file
,
 
result
):

    
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

    
contents
 
=
 
file
.
load_contents_finish
(
result
)

    
if
 
not
 
contents
[
0
]:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to open 
{
path
}
: 
{
contents
[
1
]
}
"
)

        
return

    
try
:

        
text
 
=
 
contents
[
1
]
.
decode
(
'utf-8'
)

    
except
 
UnicodeError
 
as
 
err
:

        
path
 
=
 
file
.
peek_path
()

        
print
(
f
"Unable to load the contents of 
{
path
}
: the file is not encoded with UTF-8"
)

        
return

    
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
set_text
(
text
)

    
start
 
=
 
buffer
.
get_start_iter
()

    
buffer
.
place_cursor
(
start
)

    
self
.
set_title
(
display_name
)
```

```
private
 
void
 
open_file
 
(
File
 
file
)
 
{

    
file
.
load_contents_async
.
begin
 
(
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
?
 
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

        
uint8
[]
 
contents
;

        
try
 
{

            
// Complete the asynchronous operation; this function will either

            
// give you the contents of the file as a byte array, or will

            
// raise an exception

            
file
.
load_contents_async
.
end
 
(
result
,
 
out
 
contents
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

            
// In case of an error, print a warning to the standard error output

            
stderr
.
printf
 
(
"Unable to open “%s“: %s"
,
 
file
.
peek_path
 
(),
 
e
.
message
);

        
}

        
// Ensure that the file is encoded with UTF-8

        
if
 
(
!
((
string
)
 
contents
).
validate
 
())

            
stderr
.
printf
 
(
"Unable to load the contents of “%s”: "
+

                           
"the file is not encoded with UTF-8
\n
"
,

                           
file
.
peek_path
 
());

        
// Retrieve the GtkTextBuffer instance that stores the

        
// text displayed by the GtkTextView widget

        
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

        
// Set the text using the contents of the file

        
buffer
.
text
 
=
 
(
string
)
 
contents
;

        
// Reposition the cursor so it's at the start of the text

        
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

        
buffer
.
place_cursor
 
(
start
);

        
// Set the title using the display name

        
this
.
title
 
=
 
display_name
;

    
});

}
```

```
async
 
openFile
(
file
)
 
{

    
// Get the name of the file

    
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

    
let
 
contentsBytes
;

    
try
 
{

        
contentsBytes
 
=
 
(
await
 
file
.
load_contents_async
(
null
))[
0
];

    
}
 
catch
 
(
e
)
 
{

        
logError
(
e
,
 
`Unable to open 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

    
if
 
(
!
GLib
.
utf8_validate
(
contentsBytes
))
 
{

        
logError
(
`Invalid text encoding for 
${
file
.
peek_path
()
}
`
);

        
return
;

    
}

    
const
 
contentsText
 
=
 
new
 
TextDecoder
(
'utf-8'
).
decode
(
contentsBytes
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
text
 
=
 
contentsText
;

    
const
 
startIterator
 
=
 
buffer
.
get_start_iter
();

    
buffer
.
place_cursor
(
startIterator
);

    
// Set the window title using the loaded file's name

    
this
.
title
 
=
 
fileName
;

}
```

## Add the “Open” shortcut to the Keyboard Shortcuts help ¶

The Keyboard Shortcuts help dialog is part of the GNOME application template
in GNOME Builder. GTK automatically handles its creation and the action that
presents it to the user.

- Find the help-overlay.ui file in the sources directory
- Find the GtkShortcutsGroup definition
- Add a new GtkShortcutsShortcut definition for the win.open action
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
```

You should now be able to run the application, press the Open button or Ctrl + O , and select a text file in your system. For instance,
you can navigate to the text viewer project directory, and select the COPYING file in the sources:

![../../../_images/opening_files_main.png](https://developer.gnome.org/documentation/_images/opening_files_main.png)
