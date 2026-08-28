# Themed Icons - GNOME Developer Documentation

# Themed Icons ¶

The GtkIconTheme API gives access to icons that are shipped with the icon
theme.

Icon themes are sets of icons that share a common look and feel; a theme is a
mapping between a name and an icon file and size.

Note

For more information on icon themes, you can read the freedesktop.org icon
theme specification

## Extending the icon theme ¶

Some times, applications need icons that are too domain-specific to be included
in a generic icon theme.

There are various ways to ship icon assets as named icons:

- resources (recommended)
- files

### Resources ¶

It is possible to include application-specific icons directly in the application
binary as resources, instead of installing them in the file system. By using
resources you reduce the performance penalty of seeking files on the file
system, and you improve the portability and reliability of your application. The
downsides are that changing icons requires rebuilding your application, and that
the size of the binary grows.

To use resources, place your icons in a directory structure that matches the
hicolor icon theme:

```
<gresources>

  
<gresource
 
prefix=
"/my/resources/icons/scalable/actions"
>

    
<file>
app-remove-symbolic.svg
</file>

    
<file>
edit-select-all-symbolic.svg
</file>

  
</gresource>

  
<gresource
 
prefix=
"/my/resources/icons/16x16/actions"
>

    
<file>
app-remove.png
</file>

    
<file>
edit-select-all.png
</file>

    
...

  
</gresource>

</gresources>
```

Note

When using resources, you don’t need to include ‘hicolor’ in the path.

Then, tell GtkIconTheme about the resource path where your icons are located:

```
GtkIconTheme
 
*
theme
 
=
 
gtk_icon_theme_get_for_display
 
(
gdk_display_get_default
 
());

gtk_icon_theme_add_resource_path
 
(
theme
,
 
"/my/resources/icons"
);
```

```
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
(
Gdk
.
Display
.
get_default
())

theme
.
add_resource_path
(
"/my/resources/icons"
)
```

```
var
 
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
 
(
Gdk
.
Display
.
get_default
 
());

theme
.
add_resource_path
 
(
"/my/resources/icons"
);
```

```
const
 
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
(
Gdk
.
Display
.
get_default
());

theme
.
add_resource_path
(
"/my/resources/icons"
);
```

```
var
 
theme
 
=
 
IconTheme
.
getForDisplay
(
Display
.
getDefault
());

theme
.
addResourcePath
(
"/my/resources/icons"
);
```

Finally, you can refer to the icons using their name:

```
GtkWidget
 
*
image
 
=
 
gtk_image_new_from_icon_name
 
(
"app-remove-symbolic"
);
```

```
image
 
=
 
Gtk
.
Image
(
icon_name
=
"app-remove-symbolic"
)
```

```
var
 
image
 
=
 
Gtk
.
Image
.
new_from_icon_name
 
(
"app-remove-symbolic"
);
```

```
const
 
image
 
=
 
Gtk
.
Image
({
 
icon_name
:
 
"app-remove-symbolic"
 
});
```

```
var
 
image
 
=
 
Image
.
fromIconName
(
"app-remove-symbolic"
);
```

```
<object
 
class=
"GtkImage"
>

  
<property
 
name=
"icon-name"
>
app-remove-symbolic
</property>

</object>
```

Important

GtkApplication automatically sets up a resource path based on the
application id of your application. If your applications id is ‘org.my.App’,
then icons will be looked for under “/org/my/App/icons”. Please, refer to
the GtkApplication documentation for more information on automatic resources.

### Files ¶

You can install the icons in a directory structure that matches the hicolor icon theme. Typically, the icons will be located in your application’s data
directory, e.g. /usr/share/org.gnome.YourApp .

For instance, you can install icons from your Meson build file:

```
# Define PKG_DATADIR as a global symbol

pkg_datadir
 
=
 
get_option
(
'prefix'
)
 
/
 
get_option
(
'datadir'
)
 
/
 
meson
.
project_name
()

add_project_arguments
(
'-DPKG_DATADIR=@0@'
.
format
(
datadir
),
 
language
:
 
'c'
)

# List the icons to install

action_icons_dir
 
=
 
pkg_datadir
 
/
 
'icons/hicolor/16x16/actions'

action_icons
 
=
 
[

  
'action-name-1.png'
,

  
'action-name-2.png'
,

]

# Install the icons

install_data
(
action_icons
,
 
install_dir
:
 
action_icons_dir
)

# Ensure that the directory matches the expected icon theme definition

meson
.
add_install_script
(

  
'gtk-update-icon-cache'
,

  
'-q'
,

  
'-t'
,

  
'-f'
,

  
customdir
,

  
skip_if_destdir
:
 
true
,

)
```

You will also need to tell GtkIconTheme to look in that directory:

```
GtkIconTheme
 
*
theme
 
=
 
gtk_icon_theme_get_for_display
 
(
gdk_display_get_default
 
());

gtk_icon_theme_append_search_path
 
(
theme
,
 
PKG_DATADIR
 
"/icons"
);
```

```
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
(
Gdk
.
Display
.
get_default
())

theme
.
append_search_path
(
os
.
path
.
join
(
PKG_DATADIR
,
 
"icons"
))
```

```
var
 
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
 
(
Gdk
.
Display
.
get_default
 
());

theme
.
append_search_path
 
(
PKG_DATADIR
 
+
 
"/icons"
);
```

```
const
 
theme
 
=
 
Gtk
.
IconTheme
.
get_for_display
(
Gdk
.
Display
.
get_default
());

theme
.
append_search_path
(
PKG_DATADIR
 
+
 
"/icons"
);
```

```
var
 
theme
 
=
 
IconTheme
.
getForDisplay
(
Display
.
getDefault
());

theme
.
appendSearchPath
(
PKG_DATADIR
 
+
 
"/icons"
);
```

It is recommended to follow the principles of the icon naming specification even for these private icons.

This approach to icon theme extension has some advantages over others:

- the application-specific icons don’t pollute the shared namespace for themed
icons, and other applications won’t unintentionally pick up an icon that was
only meant for your application
- since the icons are installed into a directory below ‘hicolor’, the theme can
override the icons to make your application fit in with the rest of the system

## Flipped icons ¶

Some icons benefit from being ‘flipped’ in right-to-left (RTL) locales. GTK does
this automatically, by passing a GTK_ICON_LOOKUP_DIR_RTL or GTK_ICON_LOOKUP_DIR_LTR flag to GtkIconTheme when loading icons.

If you are loading icons manually using the GtkIconTheme API , you may want to do
the same for icons where flipping is relevant.

Of course, RTL variants must be present in the icon theme for this to make any
difference. If you have an icon with an RTL variant, you should append the -rtl suffix to the icon’s file base name.

## Symbolic icons ¶

Symbolic icons have a simple form, and can be used much like text. They will be
recolored according to the context in which they are used. By convention,
symbolic icons are named with a -symbolic suffix.

Passing an icon name like “pan-start-symbolic” to GTK functions like gtk_image_set_from_icon_name() will automatically do the right thing. When
you are manually loading a symbolic icon using the GtkIconTheme APIs, make sure
to use the ‘symbolic’ variants, such as gtk_icon_info_load_symbolic() to ensure
that the icon is properly colored.

Important

Only symbolic icons loaded via their icon name through GtkIconTheme will
be recolored using    the style colors. Icons loaded directly from a file or
a resource, or stored inside a location that does not match the icon theme
directory structure, will not be automatically recolored by GTK.

an svg (the traditional form in which symbolic icons are created) or use the

gtk-encode-symbolic-svg utility to convert the icon into specially crafted .symbolic.png files, which can be installed into the fixed-size subdirectories
of the icon theme:

```
gtk
-
encode
-
symbolic
-
svg
 
-
o
 
/
usr
/
share
/
icons
/
hicolor
/
48
x48
/
apps
 
my
-
app
-
symbolic
.
svg
 
48
x48
```

## Application icons ¶

The icon theme specification defines a universal fallback theme, called hicolor,
in which applications can install icons that need to be known by the rest of the
system.

The main example for this is the application icon that is used in the
applications desktop file. This icon should be named to match the application
name, and be installed in /usr/share/icons/hicolor/48x48/apps/ . Other sizes are
optional, but a 256×256 icon is the default size used by GNOME in its
application grid, so you’re strongly encouraged to provide one.

Applications are also encouraged to install a symbolic version of the
application icon into the hicolor theme, with the same name and a -symbolic suffix. The symbolic icon will be used in the HighContrast theme. Symbolic icons
can be installed as SVG, in the /usr/share/icons/hicolor/symbolic/apps directory
(which was added to hicolor in 0.15), or as .symbolic.png files in /usr/share/icons/hicolor/48x48/apps/ . Other sizes are optional.

Note

Unlike other icons, application icons cannot be added to a GResource, as they
are referenced by the desktop file and loaded by the desktop.
