# Check Boxes - GNOME Developer Documentation

# Check Boxes ¶

Check boxes are buttons that display a “checked” indicator next to their
label.

- Interface guidelines

```
GtkWidget
 
*
check
 
=
 
gtk_check_button_new_with_label
 
(
"Show title"
);

// "on_toggled" is defined elsewhere

g_signal_connect
 
(
check
,
 
"toggled"
,
 
G_CALLBACK
 
(
on_toggled
),
 
NULL
);
```

```
check
 
=
 
Gtk
.
CheckButton
(
label
=
"Show title"
)

# "on_toggled" is defined elsewhere

check
.
connect
(
"toggled"
,
 
on_toggled
)
```

```
var
 
check
 
=
 
new
 
Gtk
.
CheckButton
.
with_label
 
(
"Show title"
);

// "on_toggled" is defined elsewhere

check
.
toggled
.
connect
 
(
on_toggled
);
```

```
const
 
check
 
=
 
new
 
Gtk
.
CheckButton
({
 
label
:
 
"Show title"
 
});

// "on_toggled" is defined elsewhere

check
.
connect
(
"toggled"
,
 
on_toggled
);
```

```
var
 
check
 
=
 
CheckButton
.
withLabel
(
"Show title"
);

// "onToggled" is defined elsewhere

check
.
onToggled
(
this
::
onToggled
);
```

## Useful methods for the component ¶

- Check boxes can be set to an “inconsistent” state: a state that is neither
active nor inactive. You can use the set_inconsistent() method to set
this state.
- If you want to enable a mnemonic shortcut for your check box, you can use
the new_with_mnemonic() constructor, or the set_use_underline() method.

## API references ¶

In the examples we used the following classes:

- GtkCheckButton
