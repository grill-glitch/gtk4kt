# Boxes - GNOME Developer Documentation

# Boxes ¶

A box is a layout container that provides scaffolding for your application. You
can use boxes inside other boxes to structure the UI, and control how each
widget expands and aligns itself with regards to its parent container.

Boxes have an orientation, either horizontal or vertical, and arrange their
children on the same row or column, respectively.

## Homogeneous boxes ¶

Boxes by default will give each child their desired size depending on the
orientation. If you want each child to have the same size, you can use the GtkBox:homogeneous property:

```
GtkWidget
 
*
box
 
=
 
gtk_box_new
 
(
GTK_ORIENTATION_HORIZONTAL
,
 
6
);

GtkWidget
 
*
hello
 
=
 
gtk_button_new_with_label
 
(
"Hello"
);

GtkWidget
 
*
gbye
 
=
 
gtk_button_new_with_label
 
(
"Goodbye"
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
hello
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
gbye
);

gtk_box_set_homogeneous
 
(
GTK_BOX
 
(
box
),
 
TRUE
);
```

```
box
 
=
 
Gtk
.
Box
(
orientation
=
Gtk
.
Orientation
.
HORIZONTAL
,
 
spacing
=
6
)

hello
 
=
 
Gtk
.
Button
(
label
=
"Hello"
)

gbye
 
=
 
Gtk
.
Button
(
label
=
"Goodbye"
)

box
.
append
(
hello
)

box
.
append
(
gbye
)

box
.
props
.
homogeneous
 
=
 
True
```

```
var
 
box
 
=
 
new
 
Gtk
.
Box
 
(
Gtk
.
Orientation
.
HORIZONTAL
,
 
6
);

var
 
hello
 
=
 
new
 
Gtk
.
Button
.
with_label
 
(
"Hello"
);

var
 
gbye
 
=
 
new
 
Gtk
.
Button
.
with_label
 
(
"Goodbye"
);

box
.
append
 
(
hello
);

box
.
append
 
(
gbye
);

box
.
homogeneous
 
=
 
true
;
```

```
const
 
box
 
=
 
new
 
Gtk
.
Box
({

  
orientation
:
 
Gtk
.
Orientation
.
HORIZONTAL
,

  
spacing
:
 
6
,

});

const
 
hello
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Hello"
 
});

const
 
gbye
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Goodbye"
 
});

box
.
append
(
hello
);

box
.
append
(
gbye
);

box
.
homogeneous
 
=
 
true
;
```

```
var
 
box
 
=
 
new
 
Box
(
Orientation
.
HORIZONTAL
,
 
6
);

var
 
hello
 
=
 
Button
.
withLabel
(
"Hello"
);

var
 
gbye
 
=
 
Button
.
withLabel
(
"Goodbye"
);

box
.
append
(
hello
);

box
.
append
(
gbye
);

box
.
setHomogeneous
(
true
);
```

## Expanding boxes ¶

Boxes will provide extra space to a child if the child is set to expand:

```
GtkWidget
 
*
box
 
=
 
gtk_box_new
 
(
GTK_ORIENTATION_HORIZONTAL
,
 
6
);

GtkWidget
 
*
foo
 
=
 
gtk_button_new_with_label
 
(
"Live"
);

GtkWidget
 
*
bar
 
=
 
gtk_button_new_with_label
 
(
"Laugh"
);

GtkWidget
 
*
baz
 
=
 
gtk_button_new_with_label
 
(
"Love"
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
foo
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
bar
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
baz
);

// We set the middle button to expand, which will make the

// box expand, if given more space

gtk_widget_set_hexpand
 
(
bar
,
 
TRUE
);
```

```
box
 
=
 
Gtk
.
Box
(
orientation
=
Gtk
.
Orientation
.
HORIZONTAL
,
 
spacing
=
6
)

foo
 
=
 
Gtk
.
Button
(
label
=
"Live"
)

# We set the middle button to expand, which will make the box

# expand, if given more space

bar
 
=
 
Gtk
.
Button
(
label
=
"Laugh"
,
 
hexpand
=
True
)

baz
 
=
 
Gtk
.
Button
(
label
=
"Love"
)

box
.
append
(
foo
)

box
.
append
(
bar
)

box
.
append
(
baz
)
```

```
var
 
box
 
=
 
new
 
Gtk
.
Box
 
(
Gtk
.
Orientation
.
HORIZONTAL
,
 
6
);

var
 
foo
 
=
 
new
 
Gtk
.
Button
.
with_label
 
(
"Live"
);

var
 
bar
 
=
 
new
 
Gtk
.
Button
.
with_label
 
(
"Laugh"
);

var
 
baz
 
=
 
new
 
Gtk
.
Button
.
with_label
 
(
"Love"
);

box
.
append
 
(
foo
);

box
.
append
 
(
bar
);

box
.
append
 
(
baz
);

// We set the middle button to expand. which will make the box

// expand, if given more space

bar
.
hexpand
 
=
 
true
;
```

```
const
 
box
 
=
 
new
 
Gtk
.
Box
({

  
orientation
:
 
Gtk
.
Orientation
.
HORIZONTAL
,

  
spacing
:
 
6
,

});

const
 
foo
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Live"
 
});

const
 
bar
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Laugh"
 
});

const
 
baz
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Love"
 
});

box
.
append
(
foo
);

box
.
append
(
bar
);

box
.
append
(
baz
);

// We set the middle button to expand. which will make the box

// expand, if given more space

bar
.
hexpand
 
=
 
true
;
```

```
var
 
box
 
=
 
new
 
Box
 
(
Orientation
.
HORIZONTAL
,
 
6
);

var
 
foo
 
=
 
new
 
Button
.
withLabel
(
"Live"
);

var
 
bar
 
=
 
new
 
Button
.
withLabel
(
"Laugh"
);

var
 
baz
 
=
 
new
 
Button
.
withLabel
(
"Love"
);

box
.
append
(
foo
);

box
.
append
(
bar
);

box
.
append
(
baz
);

// We set the middle button to expand. which will make the box

// expand, if given more space

bar
.
setHexpand
(
true
);
```

```
<object
 
class=
"GtkBox"
>

  
<property
 
name=
"orientation"
>
horizontal
</property>

  
<property
 
name=
"spacing"
>
6
</property>

  
<child>

    
<object
 
class=
"GtkButton"
 
id=
"foo"
>

      
<property
 
name=
"label"
>
Live
</property>

    
</object>

  
</child>

  
<child>

    
<object
 
class=
"GtkButton"
 
id=
"bar"
>

      
<property
 
name=
"label"
>
Laugh
</property>

      
<property
 
name=
"hexpand"
>
true
</property>

    
</object>

  
</child>

  
<child>

    
<object
 
class=
"GtkButton"
 
id=
"baz"
>

      
<property
 
name=
"label"
>
Love
</property>

    
</object>

  
</child>

</object>
```

## Useful methods for the component ¶

- The set_baseline_position() method controls the base line alignment of
the children of a box; you can specify if the alignment of the children in
case the box receive more space by their parent.

## API references ¶

In the examples we used the following classes:

- GtkBox
- GtkButton
