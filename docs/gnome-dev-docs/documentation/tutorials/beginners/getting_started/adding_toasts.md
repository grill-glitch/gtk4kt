# Notifying The User With Toasts - GNOME Developer Documentation

# Notifying The User With Toasts ¶

Toasts , or
“in-app notifications”, are useful to communicate a change in state from within
the application, or to gather feedback from the user.

In this lesson, you will learn how to add a toast overlay to the text viewer
application, and how to display a toast when opening a file.

![../../../_images/adding_toasts.png](https://developer.gnome.org/documentation/_images/adding_toasts.png)

## Add a toast overlay ¶

Toasts are displayed by an overlay, which must contain the rest of the
application’s content area.

### Update the UI definition file ¶

- Find the UI definition file for TextViewerWindow
- Find the definition for the GtkScrolledWindow that contains the
main text area
- Insert the AdwToastOverlay widget as the child of the TextViewerWindow and the parent of the GtkScrolledWindow , and use the toast_overlay id

```
<property
 
name=
"content"
>

  
<object
 
class=
"AdwToastOverlay"
 
id=
"toast_overlay"
>

    
<property
 
name=
"child"
>

      
<object
 
class=
"GtkScrolledWindow"
>

        
<property
 
name=
"hexpand"
>
true
</property>

        
<property
 
name=
"vexpand"
>
true
</property>

        
<property
 
name=
"margin-top"
>
6
</property>

        
<property
 
name=
"margin-bottom"
>
6
</property>

        
<property
 
name=
"margin-start"
>
6
</property>

        
<property
 
name=
"margin-end"
>
6
</property>

        
<property
 
name=
"child"
>

          
<object
 
class=
"GtkTextView"
 
id=
"main_text_view"
>

            
<property
 
name=
"monospace"
>
true
</property>

          
</object>

        
</property>

      
</object>

    
</property>

  
</object>

</property>
```

### Bind the overlay in the source ¶

- You now must add a new member to the TextViewerWindow instance
structure for the toast_overlay widget:

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

  
AdwToastOverlay
 
*
toast_overlay
;

};
```

- Bind the newly added toast_overlay widget to the template in the
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

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
TextViewerWindow
,
 
toast_overlay
);

}
```

- Add the toast_overlay widget to the TextViewerWindow class

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

    
toast_overlay
 
=
 
Gtk
.
Template
.
Child
()
```

- Add the toast_overlay widget to the TextViewer.Window class

```
[
GtkTemplate
 
(
ui
 
=
 
"/org/example/app/window.ui"
)]

public
 
class
 
TextViewer
.
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

    
[
GtkChild
]

    
private
 
unowned
 
Adw
.
ToastOverlay
 
toast_overlay
;

    
// ...

}
```

- Add the toast_overlay widget to the TextViewerWindow class

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
,
 
'toast_overlay'
],

},
 
class
 
TextViewerWindow
 
extends
 
Adw
.
ApplicationWindow
 
{

    
// ...

});
```

## Show toasts ¶

Toasts are especially useful for notifying the user that an asynchronous
operation has terminated. Opening a file and saving it are two typical use
cases for a notification.

### Notify after opening a file ¶

- Find the open_file_complete function for TextViewerWindow
- Find the error handling blocks and replace them with a toast
- Add a toast at the end of the function

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

  
TextViewerWindow
 
*
self
 
=
 
user_data
;

  
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

  
// In case of error, show a toast

  
if
 
(
error
 
!=
 
NULL
)

    
{

      
g_autofree
 
char
 
*
msg
 
=

        
g_strdup_printf
 
(
"Unable to open “%s”"
,
 
display_name
);

      
adw_toast_overlay_add_toast
 
(
self
->
toast_overlay
,
 
adw_toast_new
 
(
msg
));

      
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

      
g_autofree
 
char
 
*
msg
 
=

        
g_strdup_printf
 
(
"Invalid text encoding for “%s”"
,
 
display_name
);

      
adw_toast_overlay_add_toast
 
(
self
->
toast_overlay
,
 
adw_toast_new
 
(
msg
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

  
// Show a toast for the successful loading

  
g_autofree
 
char
 
*
msg
 
=

    
g_strdup_printf
 
(
"Opened “%s”"
,
 
display_name
);

  
adw_toast_overlay_add_toast
 
(
self
->
toast_overlay
,
 
adw_toast_new
 
(
msg
));

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

        
self
.
toast_overlay
.
add_toast
(
Adw
.
Toast
(
title
=
f
"Unable to open “
{
display_name
}
”"
))

        
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

        
self
.
toast_overlay
.
add_toast
(
Adw
.
Toast
(
title
=
f
"Invalid text encoding for “
{
display_name
}
”"
))

        
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

    
self
.
toast_overlay
.
add_toast
(
Adw
.
Toast
(
title
=
f
"Opened “
{
display_name
}
”"
))
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
"standard::displayname"
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
"standard::displayname"
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

            
// In case of an error, show a toast

            
this
.
toast_overlay
.
add_toast
 
(
new
 
Adw
.
Toast
 
(
@"Unable to open “$display_name“"
));

        
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

            
this
.
toast_overlay
.
add_toast
 
(
new
 
Adw
.
Toast
 
(
@"Invalid text encoding for “$display_name“"
));

        
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

        
// Show a toast for the successful loading

        
this
.
toast_overlay
.
add_toast
 
(
new
 
Adw
.
Toast
 
(
@"Opened “$display_name“"
));

    
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

        
this
.
_toast_overlay
.
add_toast
(
Adw
.
Toast
.
new
(
`Unable to open 
${
file
.
peek_path
()
}
`
));

        
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

        
this
.
_toast_overlay
.
add_toast
(
Adw
.
Toast
.
new
(
`Invalid text encoding for 
${
file
.
peek_path
()
}
`
));

        
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

    
// Set the window title using the loaded file's name

    
this
.
title
 
=
 
fileName
;

    
// Show a toast for the successful loading

    
this
.
_toast_overlay
.
add_toast
(
Adw
.
Toast
.
new
(
`Opened 
${
file
.
peek_path
()
}
`
));

}
```

### Notify after saving to a file ¶

- In the save_file_complete function you can use a toast to notify the
user that the operation succeeded or failed

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

  
TextViewerWindow
 
*
self
 
=
 
user_data
;

  
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

  
g_autofree
 
char
 
*
msg
 
=
 
NULL
;

  
if
 
(
error
 
!=
 
NULL
)

    
msg
 
=
 
g_strdup_printf
 
(
"Unable to save as “%s”"
,
 
display_name
);

  
else

    
msg
 
=
 
g_strdup_printf
 
(
"Saved as “%s”"
,
 
display_name
);

  
adw_toast_overlay_add_toast
 
(
self
->
toast_overlay
,
 
adw_toast_new
 
(
msg
));

}
```

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

    
# Query the display name for the file

    
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

        
msg
 
=
 
f
"Unable to save as “
{
display_name
}
”"

    
else
:

        
msg
 
=
 
f
"Saved as “
{
display_name
}
”"

    
self
.
toast_overlay
.
add_toast
(
Adw
.
Toast
(
title
=
msg
))
```

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

        
string
 
message
;

        
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

            
message
 
=
 
@"Unable to save as “$display_name“"
;

        
}
 
catch
 
(
Error
 
e
)
 
{

            
message
 
=
 
@"Saved as “$display_name“"
;

        
}

        
this
.
toast_overlay
.
add_toast
 
(
new
 
Adw
.
Toast
 
(
message
));

    
});

}
```

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
)

        
return
;

    
}

    
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

        
this
.
_toast_overlay
.
add_toast
(
Adw
.
Toast
.
new
(
`Saved as "
${
fileName
}
"`
));

    
}
 
catch
(
e
)
 
{

        
this
.
_toast_overlay
.
add_toast
(
Adw
.
Toast
.
new
(
`Unable to save as "
${
fileName
}
"`
));

    
}

}
```

In this lesson you learned how to notify the user of a long running operation
that either succeeded or failed using toasts.
