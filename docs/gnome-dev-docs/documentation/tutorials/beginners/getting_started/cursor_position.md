# Showing The Cursor Position - GNOME Developer Documentation

# Showing The Cursor Position ¶

In this lesson you will learn how to use GtkTextBuffer to be notified of
the position of the cursor inside the text area widget, and update a label
in the header bar of the text viewer application.

![../../../_images/cursor_position.png](https://developer.gnome.org/documentation/_images/cursor_position.png)

## Add the cursor position indicator ¶

### Update the UI definition ¶

- Add a GtkLabel as the child of the AdwHeaderBar in the UI definition
file for the TextViewerWindow class; the label must be packed as a child
of type end , and placed after the GtkMenuButton
- The label has the cursor_pos identifier that is going to be used to
bind it in the TextViewerWindow template
- The label has an initial content of Ln 0, Col 0 set using the label property
- Additionally, the label has two style classes: dim-label , to reduce the contrast in the default theme numeric , which will use tabular numbers in the font used by the
label

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

  
<child
 
type=
"end"
>

    
<object
 
class=
"GtkLabel"
 
id=
"cursor_pos"
>

      
<property
 
name=
"label"
>
Ln
 
0,
 
Col
 
0
</property>

      
<style>

        
<class
 
name=
"dim-label"
/>

        
<class
 
name=
"numeric"
/>

      
</style>

    
</object>

  
</child>

</object>
```

### Bind the template in your source code ¶

- You now must add a new member to the TextViewerWindow instance
structure for the cursor_pos label:

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

  
GtkLabel
 
*
cursor_pos
;

};
```

- Bind the newly added cursor_pos widget to the template in the
class initialization function text_viewer_window_class_init of
the TextViewerWindow type:

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

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
TextViewerWindow
,
 
cursor_pos
);

}
```

- Add the cursor_pos widget to the TextViewerWindow class

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

    
cursor_pos
 
=
 
Gtk
.
Template
.
Child
()
```

- Add the cursor_pos widget to the TextViewer.Window class

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

        
[
GtkChild
]

        
private
 
unowned
 
Gtk
.
Label
 
cursor_pos
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

        
// ...

    
}

}
```

Add the cursor_pos widget to the TextViewer.Window class

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

    
}

    
// ...

});
```

## Update the cursor position label ¶

- Retrieve the GtkTextBuffer from the main_text_view widget and
connect a callback to the notify::cursor-position signal to receive
a notification every time the cursor-position property changes:

```
static
 
void

text_viewer_window__update_cursor_position
 
(
GtkTextBuffer
 
*
buffer
,

                                            
GParamSpec
 
*
pspec
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
text_viewer_window__open
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

    
cursor_pos
 
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

```
namespace
 
TextViewer
 
{

    
public
 
class
 
Window
 
:
 
Adw
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

        
// ...

    
}

}
```

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

    
// ...

});
```

- Define the notify::cursor-position callback to retrieve the position
of the cursor from the GtkTextBuffer object, and update the contents
of the cursor_pos label:

```
static
 
void

text_viewer_window__update_cursor_position
 
(
GtkTextBuffer
    
*
buffer
,

                                            
GParamSpec
       
*
pspec
 
G_GNUC_UNUSED
,

                                            
TextViewerWindow
 
*
self
)

{

  
int
 
cursor_pos
 
=
 
0
;

  
// Retrieve the value of the "cursor-position" property

  
g_object_get
 
(
buffer
,
 
"cursor-position"
,
 
&
cursor_pos
,
 
NULL
);

  
// Construct the text iterator for the position of the cursor

  
GtkTextIter
 
iter
;

  
gtk_text_buffer_get_iter_at_offset
 
(
buffer
,
 
&
iter
,
 
cursor_pos
);

  
// Set the new contents of the label

  
g_autofree
 
char
 
*
cursor_str
 
=

    
g_strdup_printf
 
(
"Ln %d, Col %d"
,

                     
gtk_text_iter_get_line
 
(
&
iter
)
 
+
 
1
,

                     
gtk_text_iter_get_line_offset
 
(
&
iter
)
 
+
 
1
);

  
gtk_label_set_text
 
(
self
->
cursor_pos
,
 
cursor_str
);

}
```

```
def
 
update_cursor_position
(
self
,
 
buffer
,
 
_
):

    
# Retrieve the value of the "cursor-position" property

    
cursor_pos
 
=
 
buffer
.
props
.
cursor_position

    
# Construct the text iterator for the position of the cursor

    
iter
 
=
 
buffer
.
get_iter_at_offset
(
cursor_pos
)

    
line
 
=
 
iter
.
get_line
()
 
+
 
1

    
column
 
=
 
iter
.
get_line_offset
()
 
+
 
1

    
# Set the new contents of the label

    
self
.
cursor_pos
.
set_text
(
f
"Ln 
{
line
}
, Col 
{
column
}
"
)
```

```
private
 
void
 
update_cursor_position
 
(
Object
 
source_object
,
 
ParamSpec
 
pspec
)
 
{

    
var
 
buffer
 
=
 
source_object
 
as
 
Gtk
.
TextBuffer
;

    
int
 
cursor_position
 
=
 
buffer
.
cursor_position
;

    
Gtk
.
TextIter
 
iter
;

    
buffer
.
get_iter_at_offset
 
(
out
 
iter
,
 
cursor_position
);

    
this
.
cursor_pos
.
label
 
=
 
@"Ln $(iter.get_line ()), Col $(iter.get_line_offset ())"
;

}
```

```
updateCursorPosition
(
buffer
)
 
{

    
const
 
iterator
 
=
 
buffer
.
get_iter_at_offset
(
buffer
.
cursor_position
);

    
this
.
_cursor_pos
.
label
 
=
 
`Ln 
${
iterator
.
get_line
()
}
, Col 
${
iterator
.
get_line_offset
()
}
`
;

}
```

The objective of this lesson is to update the contents of a GtkLabel widget
every time the position of the cursor in the GtkTextView widget changes by
using the property notification mechanism provided by GObject .
