# Password Entries - GNOME Developer Documentation

# Password Entries ¶

A password entry is a text field meant to be used for entering secrets. A
password entry will not show its contents by default.

- Interface guidelines

```
GtkWidget
 
*
entry
 
=
 
gtk_password_entry_new
 
();
```

```
entry
 
=
 
Gtk
.
PasswordEntry
()
```

```
var
 
entry
 
=
 
new
 
Gtk
.
PasswordEntry
 
();
```

```
const
 
entry
 
=
 
new
 
Gtk
.
PasswordEntry
();
```

```
var
 
entry
 
=
 
new
 
PasswordEntry
();
```

## Useful methods for the component ¶

- You can use the GtkPasswordEntry:placeholder-text property to show a
placeholder text when the entry is not focused.
- You can disable the “peek contents” icon in the entry by setting the GtkPasswordEntry:show-peek-icon property to FALSE .

## API references ¶

In the examples we used the following classes:

- GtkPasswordEntry
