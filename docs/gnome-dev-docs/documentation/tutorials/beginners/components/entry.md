# Entries - GNOME Developer Documentation

# Entries ¶

Entries are used for single line text entry and editing.

- Interface guidelines

## Activation ¶

Entries will emit the GtkEntry::activate signal every time the user presses
the Return key.

```
GtkWidget
 
*
entry
 
=
 
gtk_entry_new
 
();

// "on_entry_activate" is defined elsewhere

g_signal_connect
 
(
entry
,
 
"activate"
,
 
G_CALLBACK
 
(
on_entry_activate
),
 
NULL
);
```

```
entry
 
=
 
Gtk
.
Entry
()

# "on_entry_activate" is defined elsewhere

entry
.
connect
(
"activate"
,
 
on_entry_activate
)
```

```
var
 
entry
 
=
 
new
 
Gtk
.
Entry
 
();

// "on_entry_activate" is defined elsewhere

entry
.
activate
.
connect
 
(
on_entry_activate
);
```

```
const
 
entry
 
=
 
new
 
Gtk
.
Entry
();

// "on_entry_activate" is defined elsewhere

entry
.
connect
(
"activate"
,
 
on_entry_activate
);
```

```
var
 
entry
 
=
 
new
 
Entry
();

// "onEntryActivate" is defined elsewhere

entry
.
onActivate
(
this
::
onEntryActivate
);
```

The entry can also activate the default widget in the same window, by setting
the GtkEntry:activates-default property:

```
GtkWidget
 
*
entry
 
=
 
gtk_entry_new
 
();

gtk_entry_set_activates_default
 
(
GTK_ENTRY
 
(
entry
),
 
TRUE
);
```

```
entry
 
=
 
Gtk
.
Entry
(
activates_default
=
True
)
```

```
var
 
entry
 
=
 
new
 
Gtk
.
Entry
 
()
 
{

    
activates_default
 
=
 
true

};
```

```
const
 
entry
 
=
 
new
 
Gtk
.
Entry
({

  
activates_default
:
 
true
,

});
```

```
var
 
entry
 
=
 
Entry
.
builder
()

        
.
setActivatesDefault
(
true
)

        
.
build
();
```

## Buffers ¶

Entries store their contents inside separate GtkEntryBuffer objects. You
can use entry buffers to share content between different entry widgets:

```
GtkEntryBuffer
 
*
buffer
 
=
 
gtk_entry_buffer_new
 
(
"Live, Laugh, Love"
,
 
-1
);

// All three entries will show "Live, Laugh, Love"

GtkWidget
 
*
live
 
=
 
gtk_entry_new_with_buffer
 
(
buffer
);

GtkWidget
 
*
laugh
 
=
 
gtk_entry_new_with_buffer
 
(
buffer
);

GtkWidget
 
*
love
 
=
 
gtk_entry_new_with_buffer
 
(
buffer
);
```

```
buffer
 
=
 
Gtk
.
EntryBuffer
(
text
=
"Live, Laugh, Love"
)

# All three entries will show "Live, Laugh, Love"

live
 
=
 
Gtk
.
Entry
(
buffer
=
buffer
)

laugh
 
=
 
Gtk
.
Entry
(
buffer
=
buffer
)

love
 
=
 
Gtk
.
Entry
(
buffer
=
buffer
)
```

```
var
 
buffer
 
=
 
new
 
Gtk
.
EntryBuffer
 
(
"Live, Laugh, Love"
);

// All three entries will show "Live, Laugh, Love"

var
 
live
 
=
 
new
 
Gtk
.
Entry
.
with_buffer
 
(
buffer
);

var
 
laugh
 
=
 
new
 
Gtk
.
Entry
.
with_buffer
 
(
buffer
);

var
 
love
 
=
 
new
 
Gtk
.
Entry
.
with_buffer
 
(
buffer
);
```

```
const
 
buffer
 
=
 
new
 
Gtk
.
EntryBuffer
({

  
text
:
 
"Live, Laugh, Love"
,

});

// All three entries will show "Live, Laugh, Love"

const
 
live
 
=
 
new
 
Gtk
.
Entry
({
 
buffer
 
});

const
 
laugh
 
=
 
new
 
Gtk
.
Entry
({
 
buffer
 
});

const
 
love
 
=
 
new
 
Gtk
.
Entry
({
 
buffer
 
});
```

```
var
 
buffer
 
=
 
new
 
EntryBuffer
(
"Live, Laugh, Love"
);

// All three entries will show "Live, Laugh, Love"

var
 
live
 
=
 
Entry
.
withBuffer
(
buffer
);

var
 
laugh
 
=
 
Entry
.
withBuffer
(
buffer
);

var
 
love
 
=
 
Entry
.
withBuffer
(
buffer
);
```

## Useful methods for the component ¶

- You should use the set_input_purpose() method to specify the purpose of
the text entry; for instance, if the entry is only supposed to be used to
input phone numbers, or email addresses.
- You should use the set_input_hint() method to include additional hints
as to the content of the text entry, especially for input methods and
internationalization.
- If you want to show a hint when the entry is empty and not focused, you
can use the GtkEntry:placeholder-text property.

## API references ¶

In the examples we used the following classes:

- GtkEntry
- GtkEntryBuffer
