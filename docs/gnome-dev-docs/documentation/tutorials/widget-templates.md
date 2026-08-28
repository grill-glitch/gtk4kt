# Widget Templates - GNOME Developer Documentation

# Widget Templates ¶

GTK provides a way to describe UIs using definition files and the GtkBuilder API. Typically
you will need to create a GtkBuilder object instance and load the UI
definition XML file, extract the generated objects, and store references to
them inside your own types and data structures.

To automate this work, GTK also provides templates : a way to automatically
load the UI definition of a GTK widget type, bind the objects described in the
XML to fields in the widget instance, and automatically manage their lifetime.

Note

UI definitions can describe any object type that inherits from GObject.
Only widgets can have templates, but templates can contain any object.

## Using a template ¶

Templates are bound to a type, and are loaded whenever a new instance of that
type is created.

In order to use a template you will need to register it at class initialization
time. Typically you will have your UI definition file bundled with your binary
using GResource , in order
to reliably access it from your project.

As an example, we have a composite widget type that contains two children:

- an entry
- a button

Its UI definition file is going to be:

```
<interface>

  
<template
 
class=
"ExampleWidget"
 
parent=
"GtkWidget"
>

    
<child>

      
<object
 
class=
"GtkEntry"
 
id=
"entry"
>

      
</object>

    
</child>

    
<child>

      
<object
 
class=
"GtkButton"
 
id=
"button"
>

        
<property
 
name=
"label"
>
Hello
</property>

      
</object>

    
</child>

  
</template>

</interface>
```

and it is going to be saved as a GResource under the /com/example/widget.ui path.

- Add the template registration to your class_init function

```
G_DECLARE_FINAL_TYPE
 
(
ExampleWidget
,
 
example_widget
,
 
EXAMPLE
,
 
WIDGET
,
 
GtkWidget
)

struct
 
_ExampleWidget

{

  
GtkWidget
 
parent_type
;

  
GtkWidget
 
*
entry
;

  
GtkWidget
 
*
button
;

};

G_DEFINE_TYPE
 
(
ExampleWidget
,
 
example_widget
,
 
GTK_TYPE_WIDGET
)

static
 
void

example_widget_class_init
 
(
ExampleWidgetClass
 
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

                                               
"/com/example/widget.ui"
);

}
```

- Bind the widgets defined inside the template file to the corresponding
members of the widget’s instance data structure

```
static
 
void

example_widget_class_init
 
(
ExampleWidgetClass
 
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

                                               
"/com/example/widget.ui"
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
ExampleWidget
,
 
entry
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
ExampleWidget
,
 
button
);

}
```

- Initialize the template children when initializing the widget instance

```
static
 
void

example_widget_init
 
(
ExampleWidget
 
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

  
// It is now possible to access self->entry and self->button

}
```

- Clear the template children when disposing the widget instance

```
static
 
void

example_widget_dispose
 
(
GObject
 
*
gobject
)

{

  
gtk_widget_dispose_template
 
(
GTK_WIDGET
 
(
gobject
),
 
EXAMPLE_TYPE_WIDGET
);

  
G_OBJECT_CLASS
 
(
example_widget_parent_class
)
->
dispose
 
(
gobject
);

}

static
 
void

example_widget_class_init
 
(
ExampleWidgetClass
 
*
klass
)

{

  
G_OBJECT_CLASS
 
(
klass
)
->
dispose
 
=
 
example_widget_dispose
;

  
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

                                               
"/com/example/widget.ui"
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
ExampleWidget
,
 
entry
);

  
gtk_widget_class_bind_template_child
 
(
widget_class
,
 
ExampleWidget
,
 
button
);

}
```

- Add the template initialization with the @Gtk.Template class
decorator

```
@Gtk
.
Template
(
resource_path
=
"/com/example/widget.ui"
)

class
 
ExampleWidget
(
Gtk
.
Widget
):

    
__gtype_name__
 
=
 
"ExampleWidget"
```

- Bind the widgets defined inside the template file to the corresponding
attributes of the widget’s class using the Gtk.Template.Child constructor

```
@Gtk
.
Template
(
resource_path
=
"/com/example/widget.ui"
)

class
 
ExampleWidget
(
Gtk
.
Widget
):

    
__gtype_name__
 
=
 
"ExampleWidget"

    
entry
 
=
 
Gtk
.
Template
.
Child
()

    
button
 
=
 
Gtk
.
Template
.
Child
()
```

- Add the template initialization with the GtkTemplate attribute

```
[
GtkTemplate
 
(
ui
 
=
 
"/com/example/widget.ui"
)]

class
 
ExampleWidget
 
:
 
Gtk
.
Widget
 
{

  
private
 
Gtk
.
Entry
 
entry
;

  
private
 
Gtk
.
Button
 
button
;

}
```

- Bind the widgets defined inside the template file to the corresponding
members of the widget’s instance data structure

```
[
GtkTemplate
 
(
ui
 
=
 
"/com/example/widget.ui"
)]

class
 
ExampleWidget
 
:
 
Gtk
.
Widget
 
{

  
[
GtkChild
]

  
private
 
unowned
 
Gtk
.
Entry
 
entry
;

  
[
GtkChild
]

  
private
 
unowned
 
Gtk
.
Button
 
button
;

}
```

Note

It is not necessary to bind all the template children defined in the UI
file. You can use gtk_widget_get_template_child() to access a named
template child at run time. If you find yourself accessing the same
template child multiple times, it is more efficient to store the reference
inside your instance data structure.

## When to use templates ¶

Templates are useful as a way to keep code maintainable, and reduce the size
of UI definition files. If your UI definitions become increasingly complex or
include too many levels of nested widgets, then you should consider moving
blocks of functionality and related widgets into their own composite template,
and then instantiate the template widget from their parent’s UI definition. For
instance, given this UI definition:

```
<object
 
class=
"GtkBox"
>

  
<child>

    
<object
 
class=
"GtkBox"
>

      
<child>

        
<object
 
class=
"GtkStack"
>

          
<child>

            
<object
 
class=
"GtkStackPage"
>

              
<property
 
name=
"child"
>

                
<object
 
class=
"GtkBox"
>

                  
<property
 
name=
"orientation"
>
vertical
</property>

                  
<child>

                    
<object
 
class=
"GtkButton"
>

                      
<!-- ... -->
```

You may want to move the GtkBox that is the child of the stack page into
its own composite widget:

```
<template
 
class=
"ButtonsPage"
 
parent=
"GtkBox"
>

  
<property
 
name=
"orientation"
>
vertical
</property>

  
<child>

    
<object
 
class=
"GtkButton"
>

      
<!-- ... -->
```

And then reference it from the main UI definition:

```
<object
 
class=
"GtkBox"
>

  
<child>

    
<object
 
class=
"GtkBox"
>

      
<child>

        
<object
 
class=
"GtkStack"
>

          
<child>

            
<object
 
class=
"GtkStackPage"
>

              
<property
 
name=
"child"
>

                
<object
 
class=
"ButtonsPage"
>

                  
<!-- ... -->
```

After that, you may want to move the box with the stack into its own composite
widget:

```
<template
 
class=
"StackBox"
 
parent=
"GtkBox"
>

  
<child>

    
<object
 
class=
"GtkStack"
>

      
<child>

        
<object
 
class=
"GtkStackPage"
>

          
<property
 
name=
"child"
>

            
<object
 
class=
"ButtonsPage"
>

              
<!-- ... -->
```

And reference it from the main UI definition:

```
<object
 
class=
"GtkBox"
>

  
<child>

    
<object
 
class=
"StackBox"
>
```

This way we have replace one big UI definition file with three smaller ones.

Note

When using a custom widget as a child of a composite widget template
you must ensure that the custom widget’s type is known before the
template has been initialized. You can do that by calling the g_type_ensure() function with the type of the custom widget in
the parent’s instance initialization function, before calling gtk_widget_init_template() . For instance, in the example above,
the StackBox references a ButtonPage custom widget; this mean
that when the template of the StackBox instance is initialized,
the ButtonPage type must already be registered in the GObject type
system.
