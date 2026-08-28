# Spinners - GNOME Developer Documentation

# Spinners ¶

A spinner is a placeholder for a long-running action happening in the
background.

- Interface guidelines

```
static
 
gboolean

stop_spinner
 
(
gpointer
 
data
)

{

  
GtkSpinner
 
*
spinner
 
=
 
data
;

  
gtk_spinner_stop
 
(
spinner
);

  
return
 
G_SOURCE_REMOVE
;

}

GtkWidget
 
*
spinner
 
=
 
gtk_spinner_new
 
();

gtk_spinner_start
 
(
GTK_SPINNER
 
(
spinner
));

// Stop spinner after 5 seconds

g_timeout_add_seconds
 
(
5
,
 
stop_spinner
,
 
spinner
);
```

```
spinner
 
=
 
Gtk
.
Spinner
()

spinner
.
start
()

def
 
stop_spinner
(
spinner
):

    
spinner
.
stop
()

    
return
 
GLib
.
SOURCE_REMOVE

GLib
.
timeout_add_seconds
(
5
,
 
stop_spinner
,
 
spinner
)
```

```
var
 
spinner
 
=
 
new
 
Gtk
.
Spinner
 
();

spinner
.
start
 
();

// Stop spinner after 5 seconds

Timeout
.
add_seconds
 
(
5
,
 
()
 
=>
 
{

    
spinner
.
stop
 
();

    
return
 
Source
.
REMOVE
;

});
```

```
const
 
spinner
 
=
 
new
 
Gtk
.
Spinner
();

spinner
.
start
();

// Stop spinner after 5 seconds

GLib
.
timeout_add_seconds
(
GLib
.
PRIORITY_DEFAULT
,
 
5
,
 
()
 
=>
 
{

  
spinner
.
stop
();

  
return
 
GLib
.
SOURCE_REMOVE
;

});
```

```
var
 
spinner
 
=
 
new
 
Spinner
();

spinner
.
start
();

// Stop spinner after 5 seconds

GLib
.
timeoutAddSeconds
(
GLib
.
PRIORITY_DEFAULT
,
 
5
,
 
()
 
->
 
{

    
spinner
.
stop
();

    
return
 
GLib
.
SOURCE_REMOVE
;

});
```

## API references ¶

In the examples we used the following classes:

- GtkSpinner
