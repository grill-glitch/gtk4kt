# Labels - GNOME Developer Documentation

# Labels ¶

![../../../_images/label.png](https://developer.gnome.org/documentation/_images/label.png)

GtkLabel displays limited amounts of text.

Labels can contain plain text or markup text .

The text rendering is controlled by Pango attributes .

```
// A plain text label

GtkWidget
 
*
label
 
=
 
gtk_label_new
 
(
"Hello GNOME!"
);

// A markup label

GtkWidget
 
*
label
 
=
 
gtk_label_new
 
(
"<small>Hello</small> <b>GNOME</b>!"
);

gtk_label_set_use_markup
 
(
GTK_LABEL
 
(
label
),
 
TRUE
);
```

```
# A plain text label

label
 
=
 
Gtk
.
Label
(
text
=
"Hello GNOME!"
)

# A markup label

label
 
=
 
Gtk
.
Label
(
markup
=
"<small>Hello</small> <b>GNOME</b>!"
)
```

```
// A plain text label

var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"Hello GNOME!"
);

// A Label that uses markup

var
 
markup_label
 
=
 
new
 
Gtk
.
Label
 
(
"<small>Hello</small> <b>GNOME</b>!"
)
 
{

   
use_markup
 
=
 
true

};
```

```
// A plain text label

const
 
label
 
=
 
new
 
Gtk
.
Label
({
 
label
:
 
"Hello GNOME!"
 
});

// A Label that uses markup

const
 
markup_label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"<small>Hello</small> <b>GNOME</b>!"
,

  
use_markup
:
 
true
,

});
```

```
// A plain text label

var
 
label
 
=
 
new
 
Label
(
"Hello GNOME!"
);

// A markup label

var
 
markupLabel
 
=
 
new
 
Label
(
"<small>Hello</small> <b>GNOME</b>!"
);

markupLabel
.
setUseMarkup
(
true
);
```

## Wrapping labels ¶

Two properties control the wrapping of the labels text:

- GtkLabel:wrap-mode : Controls whether to wrap only at word boundaries, or
anywhere
- GtkLabel:wrap : If set, wrap lines if the text becomes too wide

One normally only sets the wrap property. It is safe to leave the wrap-mode property at its default value, Pango.WrapMode.WORD .

When wrapping is enabled, the same label can appear with multiple different
height/width combinations. How far the label actually wraps depends on the
context in which it is used, and on the geometry management of its parent
container. There are two properties that allow you to influence the amount of
wrapping:

- GtkLabel::width-chars : Specifies the minimum width of a wrapping label
- GtkLabel::max-width-chars : Specified the natural width of a wrapping label

Note that both of these properties are using characters as unit. They are
converted to pixels using the average character width of the current font.

## Ellipsized labels ¶

Sometimes, it is convenient to only show as much of text as fits, and indicate
that there is more by using an ellipsis (…). GtkLabel supports this with
this property:

- GtkLabel:ellipsize : The preferred place to ellipsize the string

Setting it to a value other than the default PANGO_ELLIPSIZE_NONE enables
ellipsization. For the usual ellipsization at the end, use PANGO_ELLIPSIZE_END .

To control how much of the label can be ellipsized away, use the GtkLabel:width-chars property:

```
GtkWidget
 
*
label
 
=

  
gtk_label_new
 
(
"This is a long text that might need to get ellipsized"
);

gtk_label_set_ellipsize
 
(
GTK_LABEL
 
(
label
),
 
PANGO_ELLIPSIZE_END
);

gtk_label_set_width_chars
 
(
GTK_LABEL
 
(
label
),
 
15
);
```

```
label
 
=
 
Gtk
.
Label
(
text
=
"This is a long text that might need to get ellipsized"
)

label
.
props
.
ellipsize
 
=
 
Pango
.
EllipsizeMode
.
END

label
.
props
.
width_chars
 
=
 
15
```

```
var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"This is a long text that"
 
+

                           
"might need to get ellipsized"
)
 
{

                               
ellipsize
 
=
 
Pango
.
EllipsizeMode
.
END
,

                               
width_chars
 
=
 
15

                           
};
```

```
const
 
label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"This is a long text that"
 
+
 
"might need to get ellipsized"
,

  
ellipsize
:
 
Pango
.
EllipsizeMode
.
END
,

  
width_chars
:
 
15
,

});
```

```
var
 
label
 
=
 
new
 
Label
(
"This is a long text that might need to get ellipsized"
);

label
.
setEllipsize
(
EllipsizeMode
.
END
);

label
.
setWidthChars
(
15
);
```

Note

The width-chars property is based on the average character with of the font,
and thus the remaining text may not exactly be the number of characters you
specified.

You can also use the GtkLabel:max-width-chars property to limit the natural
size requested by the label widget.

## Fixed number of lines ¶

Sometimes it is necessary to keep text to a certain number of lines. One
approach is to turn off automatic wrapping and rely on inserting line breaks in
the text manually:

```
GtkWidget
 
*
label
 
=

  
gtk_label_new
 
(
"This is a long text
\n
that might need
\n
to be wrapped"
);
```

```
label
 
=
 
Gtk
.
Label
(
text
=
"This is a long text
\n
that might need
\n
to be wrapped"
)
```

```
var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"This is a long text
\n
"
+

                           
"that might need
\n
to be wrapped"
);
```

```
const
 
label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"This is a long text\n"
 
+
 
"that might need\nto be wrapped"
,

});
```

```
var
 
label
 
=
 
Label
(
"This is a long text\nthat might need\nto be wrapped"
);
```

This can be problematic because you have to balance line length yourself, and
translators may inadvertently change the number of lines by removing or adding
line breaks in their translations; also, text with fixed breaks is ellipsized in
each line, which may look unexpected.

A better alternative is to go back to automatic wrapping, and tell the label how
many lines of text you want, using the set_lines() method:

```
GtkWidget
 
*
label
 
=

  
gtk_label_new
 
(
"This is a long text that might need to be wrapped"
);

gtk_label_set_wrap
 
(
GTK_LABEL
 
(
label
),
 
TRUE
);

gtk_label_set_ellipsize
 
(
GTK_LABEL
 
(
label
),
 
PANGO_ELLIPSIZE_END
);

gtk_label_set_lines
 
(
GTK_LABEL
 
(
label
),
 
3
);
```

```
label
 
=
 
Gtk
.
Label
(
text
=
"This is a long text that might need to be wrapped"
)

label
.
props
.
wrap
 
=
 
True

label
.
props
.
ellipsize
 
=
 
Pango
.
EllipsizeMode
.
END

label
.
props
.
lines
 
=
 
3
```

```
var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"This is a long text that might need to be wrapped"
);

label
.
wrap
 
=
 
true
;

label
.
ellipsize
 
=
 
Pango
.
EllipsizeMode
.
END
;

label
.
lines
 
=
 
3
;
```

```
const
 
label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"This is a long text that might need to be wrapped"
,

  
wrap
:
 
true
,

  
ellipsize
:
 
Pango
.
EllipsizeMode
.
END
,

  
lines
:
 
3
,

});
```

```
var
 
label
 
=
 
new
 
Label
(
"This is a long text that might need to be wrapped"
);

label
.
setWrap
(
true
);

label
.
setEllipsize
(
EllipsizeMode
.
END
);

label
.
setLines
(
3
);
```

With this configuration, GtkLabel will automatically wrap lines to the right
width and only ellipsize the last one, if needed.

## Mnemonics ¶

Labels can be used to describe other widgets, for instance:

```
// Add a switch to enable a feature

GtkWidget
 
*
box
 
=
 
gtk_box_new
 
(
GTK_ORIENTATION_HORIZONTAL
,
 
12
);

GtkWidget
 
*
sw
 
=
 
gtk_switch_new
 
();

GtkWidget
 
*
label
 
=
 
gtk_label_new
 
(
"Enable feature"
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
label
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
sw
);
```

```
# Add a switch to enable a feature

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
12
)

switch
 
=
 
Gtk
.
Switch
()

label
 
=
 
Gtk
.
Label
(
text
=
"Enable feature"
)

box
.
append
(
label
)

box
.
append
(
switch
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
 
12
);

var
 
switch
 
=
 
new
 
Gtk
.
Switch
 
();

var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"Enable feature"
);

box
.
append
 
(
label
);

box
.
append
 
(
switch
);
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
 
12
,

});

const
 
sw
 
=
 
new
 
Gtk
.
Switch
();

const
 
label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"Enable feature"
,

});

box
.
append
(
label
);

box
.
append
(
sw
);
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
 
12
);

var
 
switch
 
=
 
new
 
Switch
();

var
 
label
 
=
 
new
 
Label
(
"Enable feature"
);

box
.
append
(
label
);

box
.
append
(
switch
);
```

To simplify keyboard navigation, labels can include “mnemonics”: underlined
characters that are used as shortcuts for activating the widget that is
described by the label:

```
// Add a switch to enable a feature

GtkWidget
 
*
box
 
=
 
gtk_box_new
 
(
GTK_ORIENTATION_HORIZONTAL
,
 
12
);

GtkWidget
 
*
sw
 
=
 
gtk_switch_new
 
();

GtkWidget
 
*
label
 
=
 
gtk_label_new
 
(
"Enable _feature"
);

gtk_label_set_use_underline
 
(
GTK_LABEL
 
(
label
),
 
TRUE
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
label
);

gtk_box_append
 
(
GTK_BOX
 
(
box
),
 
sw
);

// Bind the switch to the label's mnemonic

gtk_label_set_mnemonic_widget
 
(
GTK_LABEL
 
(
label
),
 
switch
);
```

```
# Add a switch to enable a feature

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
12
)

switch
 
=
 
Gtk
.
Switch
()

label
 
=
 
Gtk
.
Label
(
text
=
"Enable _feature"
,
 
use_underline
=
True
)

box
.
append
(
label
)

box
.
append
(
switch
)

# Bind the switch to the label's mnemonic

label
.
set_mnemonic_widget
(
switch
)
```

```
// Add a switch to enable a feature

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
 
12
);

var
 
switch
 
=
 
new
 
Gtk
.
Switch
 
();

var
 
label
 
=
 
new
 
Gtk
.
Label
 
(
"Enable _feature"
);

label
.
use_underline
 
=
 
true
;

box
.
append
 
(
label
);

box
.
append
 
(
switch
);

// Bind the switch to the label's mnemonic

label
.
mnemonic_widget
 
=
 
switch
;
```

```
// Add a switch to enable a feature

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
 
12
,

});

const
 
sw
 
=
 
new
 
Gtk
.
Switch
();

const
 
label
 
=
 
new
 
Gtk
.
Label
({

  
label
:
 
"Enable _feature"
,

  
use_underline
:
 
true
,

  
mnemonic_widget
:
 
sw
,

});

box
.
append
(
label
);

box
.
append
(
sw
);

// Bind the switch to the label's mnemonic

label
.
mnemonic_widget
 
=
 
sw
;
```

```
// Add a switch to enable a feature

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
 
12
);

var
 
switch
 
=
 
new
 
Switch
();

var
 
label
 
=
 
new
 
Label
(
"Enable _feature"
);

label
.
setUseUnderline
(
true
);

box
.
append
(
label
);

box
.
append
(
switch
);

// Bind the switch to the label's mnemonic

label
.
setMnemonicWidget
(
switch
);
```

Now, pressing Alt + F will toggle the switch.

## Useful methods for a Label widget ¶

- set_selectable() will mark the contents of the label as user selectable;
the contents of selectable labels can also be copied in the clipboard.

## API references ¶

In this sample we used the following:

- GtkLabel
- GtkSwitch
- GtkBox
