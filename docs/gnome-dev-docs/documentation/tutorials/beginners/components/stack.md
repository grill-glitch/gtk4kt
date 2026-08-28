# Stacks - GNOME Developer Documentation

# Stacks ¶

A stack is a container which only shows one of its children at a time.

```
<object
 
class=
"GtkStack"
 
id=
"stack"
>

  
<child>

    
<object
 
class=
"GtkStackPage"
>

      
<property
 
name=
"name"
>
beginning
</property>

      
<property
 
name=
"title"
 
translatable=
"yes"
>
In
 
the
 
beginning…
</property>

      
<property
 
name=
"child"
>

        
<object
 
class=
"GtkLabel"
>

          
<property
 
name=
"label"
 
translatable=
"yes"
>
It
 
was
 
dark
</property>

        
</object>

      
</property>

    
</object>

  
</child>

  
<child>

    
<object
 
class=
"GtkStackPage"
>

      
<property
 
name=
"name"
>
end
</property>

      
<property
 
name=
"title"
 
translatable=
"yes"
>
The
 
End
</property>

      
<property
 
name=
"child"
>

        
<object
 
class=
"GtkLabel"
>

          
<property
 
name=
"label"
 
translatable=
"yes"
>
They
 
all
 
lived
 
happily
 
ever
 
after.
</property>

        
</object>

      
</property>

    
</object>

  
</child>

</object>
```

The currently visible child can only be controlled programmatically; you can
use widgets like GtkStackSwitcher or GtkStackSidebar to let the user
control the visible child.

```
<object
 
class=
"GtkStackSwitcher"
 
id=
"stack-switcher"
>

  
<!-- "stack" is the GtkStack we defined above -->

  
<property
 
name=
"stack"
>
stack
</property>

</object>
```

## Changing the visible page ¶

You can change the visible page either through the GtkStackPage name, or
using the child widget you want to show:

```
gtk_stack_set_visible_child_name
 
(
GTK_STACK
 
(
stack
),
 
"beginning"
);
```

```
stack
.
set_visible_child_name
(
"beginning"
)
```

```
stack
.
visible_child_name
 
=
 
"beginning"
;
```

```
stack
.
visible_child_name
 
=
 
"beginning"
;
```

```
stack
.
setVisibleChildName
(
"beginning"
);
```

## Transitions ¶

By default, pages are shown immediately when made visible, but you can define
a transition type and duration to animate this step. You can set the transition
type for all pages, or for a specific one.

```
// Default transition

gtk_stack_set_transition_type
 
(
GTK_STACK
 
(
stack
),

                               
GTK_STACK_TRANSITION_TYPE_CROSSFADE
);

// Show the "end" page using a 3D transition

gtk_stack_set_visible_child_full
 
(
GTK_STACK
 
(
stack
),
 
"end"
,

                                  
GTK_STACK_TRANSITION_TYPE_ROTATE_RIGHT
);
```

```
# Default transition

stack
.
props
.
transition_type
 
=
 
Gtk
.
StackTransitionType
.
CROSSFADE

# Show the "end" page using a 3D transition

stack
.
set_visible_child_full
(
"end"
,
 
Gtk
.
StackTransitionType
.
ROTATE_RIGHT
)
```

```
// Default transition

stack
.
transition_type
 
=
 
Gtk
.
StackTransitionType
.
CROSSFADE
;

// Show the "end" page using a 3D transition

stack
.
set_visible_child_full
 
(
"end"
,
 
Gtk
.
StackTransitionType
.
ROTATE_RIGHT
);
```

```
// Default transition

stack
.
transition_type
 
=
 
Gtk
.
StackTransitionType
.
CROSSFADE
;

// Show the "end" page using a 3D transition

stack
.
set_visible_child_full
(
"end"
,
 
Gtk
.
StackTransitionType
.
ROTATE_RIGHT
);
```

```
// Default transition

stack
.
setTransitionType
(
StackTransitionType
.
CROSSFADE
);

// Show the "end" page using a 3D transition

stack
.
setVisibleChildFull
(
"end"
,
 
StackTransitionType
.
ROTATE_RIGHT
);
```

## Pages ¶

Each stack page has metadata that can be used by other widgets like GtkStackSidebar , like:

- a user readable title
- an icon
- whether the page is trying to raise attention

You can retrieve the GtkStackPage instance for any given child of the
stack using the get_page() method.

## Useful methods for the component ¶

- The get_pages() method will give you a GListModel with all the stack
pages; you can use it to observe pages added, removed, or selected in the
stack.

## API references ¶

In the examples we used the following classes:

- GtkStack
- GtkStackPage
- GtkStackSwitcher
- GtkStackSidebar
