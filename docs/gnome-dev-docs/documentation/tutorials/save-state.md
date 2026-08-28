# Saving and Loading Window State - GNOME Developer Documentation

# Saving and Loading Window State ¶

## Use cases ¶

Saving and restoring the window state across sessions is a useful feature for
some applications.

Since applications know best what their state and geometry are—and whether, or
how, those should be applied once the application is launched—it’s better to
have this function near the code that creates the application window itself.

## Where should the state be stored ¶

The are two options for storing the window state:

- a file under $XDG_CACHE_HOME/your.application.id (or simply $XDG_CACHE_HOME when running inside a Flatpak application)
- GSettings

Both options have their own advantages and trade offs.

The first option requires you to write the serialization and deserialization
code, but it’s portable and does not require ancillary files to be present at
run time, like a GSettings schema. This means that you need to write more code,
but it allows, for instance, to run your application uninstalled.

The second option gives you the serialization and deserialization code, but it
requires you to define the setting keys schema and install it in a known
location. This means that you will keep your code to a minimum, but it will also
require installation.

It is generally preferred, everything being equal, to use GSettings over a
custom file. The GSettings API is nicer, more reliable, and faster.

## What kind of state should be saved? ¶

There are few window states that should generally be saved:

- the window size
- whether the window is maximized, if the window supports it
- whether the window is full screen, if the window supports it

The position of the window is best left to the window manager. Other states can
be saved along with the ones above, using the same technique, so they will be
left as an exercise for the reader.

Tip

If your application uses a GtkPaned widget, you may also want to save the
position of the handle, by tracking the GtkPaned:position property.

The window geometry should be updated by getting notifications from the GtkWindow:default-width and GtkWindow:default-height properties; window
states, like “maximized” and “fullscreen”, are also mapped to GtkWindow
properties.

## Saving and restoring state into GSettings ¶

First of all, you will need a GSettings schema, to define the keys, the type of
the values, and default value for each key:

```
<
schemalist
>

  
<
schema
 
id
=
"com.example.YourApp.State"
 
path
=
"/com/example/YourApp/State/"
>

    
<
key
 
name
=
"width"
 
type
=
"i"
>

      
<
default
>
640
</
default
>

    
</
key
>

    
<
key
 
name
=
"height"
 
type
=
"i"
>

      
<
default
>
480
</
default
>

    
</
key
>

    
<
key
 
name
=
"is-maximized"
 
type
=
"b"
>

      
<
default
>
false
</
default
>

    
</
key
>

    
<
key
 
name
=
"is-fullscreen"
 
type
=
"b"
>

      
<
default
>
false
</
default
>

    
</
key
>

  
</
schema
>

</
schemalist
>
```

Once you have a schema, you can map window properties to the GSettings keys
defined by that schema:

```
static
 
void

my_application_window_init
 
(
MyApplicationWindow
 
*
self
)

{

  
GSettings
 
*
settings
 
=
 
g_settings_new
 
(
"com.example.YourApp.State"
);

  
// update the settings when the properties change and vice versa

  
g_settings_bind
 
(
settings
,
 
"width"
,

                   
self
,
 
"default-width"
,

                   
G_SETTINGS_BIND_DEFAULT
);

  
g_settings_bind
 
(
settings
,
 
"height"
,

                   
self
,
 
"default-height"
,

                    
G_SETTINGS_BIND_DEFAULT
);

  
g_settings_bind
 
(
settings
,
 
"is-maximized"
,

                   
self
,
 
"maximized"
,

                   
G_SETTINGS_BIND_DEFAULT
);

  
g_settings_bind
 
(
settings
,
 
"is-fullscreen"
,

                   
self
,
 
"fullscreened"
,

                   
G_SETTINGS_BIND_DEFAULT
);

}
```

```
class
 
MyApplicationWindow
(
Gtk
.
ApplicationWindow
):

    
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

        
self
.
settings
 
=
 
Gio
.
Settings
(
schema_id
=
"com.example.YourApp.State"
)

        
self
.
settings
.
bind
(
"width"
,
 
self
,
 
"default-width"
,

                           
Gio
.
SettingsBindFlags
.
DEFAULT
)

        
self
.
settings
.
bind
(
"height"
,
 
self
,
 
"default-height"
,

                           
Gio
.
SettingsBindFlags
.
DEFAULT
)

        
self
.
settings
.
bind
(
"is-maximized"
,
 
self
,
 
"maximized"
,

                           
Gio
.
SettingsBindFlags
.
DEFAULT
)

        
self
.
settings
.
bind
(
"is-fullscreen"
,
 
self
,
 
"fullscreened"
,

                           
Gio
.
SettingsBindFlags
.
DEFAULT
)
```

```
My
.
ApplicationWindow
 
:
 
Gtk
.
ApplicationWindow
 
{

  
public
 
void
 
init
 
()
 
{

    
var
 
settings
 
=
 
new
 
Settings
 
(
"com.example.YourApp.State"
);

    
// update the settings when the properties change and vice versa

    
settings
.
bind
 
(
"width"
,
 
this
,

                   
"default-width"
,
 
SettingsBindFlags
.
DEFAULT
);

    
settings
.
bind
 
(
"height"
,
 
this
,

                   
"default-height"
,
 
SettingsBindFlags
.
DEFAULT
);

    
settings
.
bind
 
(
"is-maximized"
,
 
this
,

                   
"maximized"
,
 
SettingsBindFlags
.
DEFAULT
);

    
settings
.
bind
 
(
"is-fullscreen"
,
 
this
,

                   
"fullscreened"
,
 
SettingsBindFlags
.
DEFAULT
);

  
}

}
```

```
const
 
settings
 
=
 
new
 
Gio
.
Settings
({

  
schema
:
 
"com.example.YourApp.State"
,

});

const
 
window
 
=
 
new
 
Adw
.
ApplicationWindow
({
 
application
 
});

// update the settings when the properties change and vice versa

settings
.
bind
(

  
"width"
,

  
window
,

  
"default-width"
,

  
Gio
.
SettingsBindFlags
.
DEFAULT
,

);

settings
.
bind
(

  
"height"
,

  
window
,

  
"default-height"
,

  
Gio
.
SettingsBindFlags
.
DEFAULT
,

);

settings
.
bind
(

  
"is-maximized"
,

  
window
,

  
"maximized"
,

  
Gio
.
SettingsBindFlags
.
DEFAULT
,

);

settings
.
bind
(

  
"is-fullscreen"
,

  
window
,

  
"fullscreened"
,

  
Gio
.
SettingsBindFlags
.
DEFAULT
,

);
```

```
public
 
class
 
MyApplicationWindow
 
extends
 
ApplicationWindow
 
{

    
@InstanceInit

    
public
 
void
 
init
()
 
{

        
var
 
settings
 
=
 
new
 
org
.
gnome
.
gio
.
Settings
(
"com.example.YourApp.State"
);

        
// update the settings when the properties change and vice versa

        
settings
.
bind
(
"width"
,
 
this
,

                      
"default-width"
,
 
SettingsBindFlags
.
DEFAULT
);

        
settings
.
bind
(
"height"
,
 
this
,

                      
"default-height"
,
 
SettingsBindFlags
.
DEFAULT
);

        
settings
.
bind
(
"is-maximized"
,
 
this
,

                      
"maximized"
,
 
SettingsBindFlags
.
DEFAULT
);

        
settings
.
bind
(
"is-fullscreen"
,
 
this
,

                      
"fullscreened"
,
 
SettingsBindFlags
.
DEFAULT
);

    
}

}
```

The code above will automatically set the properties to the value in the
corresponding GSettings keys, and will update the GSettings keys with the value
of the properties whenever they change.
