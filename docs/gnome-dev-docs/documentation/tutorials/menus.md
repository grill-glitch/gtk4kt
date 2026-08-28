# Menus - GNOME Developer Documentation

# Menus ¶

In order to create menus in GTK, you
have to describe the menu structure using the GMenu API; each menu item is
bound to a GAction .

It is possible to construct a menu model manually, using the GMenu APIs
directly. This is a somewhat painful:

```
GMenu
 
*
menu
 
=
 
g_menu_new
 
();

// The "Evocations" section

GMenu
 
*
section
 
=
 
g_menu_new
 
();

// g_menu_append() creates a menu item for you

g_menu_append
 
(
section
,
 
"Magic Missile"
,
 
"app.magicmissile"
);

g_menu_append_section
 
(
menu
,
 
"Evocations"
,
 
section
);

g_object_unref
 
(
section
);

// The "Cantrips" section

section
 
=
 
g_menu_new
 
();

// An explicit menu item

GMenuItem
 
*
item
 
=
 
g_menu_item_new
 
(
"Fire Bolt"
,
 
"app.firebolt"
);

// "cantrips_icon" is a GIcon and is defined elsewhere

g_menu_item_set_icon
 
(
item
,
 
cantrips_icon
);

g_menu_append_item
 
(
section
,
 
item
);

g_menu_append_section
 
(
menu
,
 
"Cantrips"
,
 
section
);

g_object_unref
 
(
section
);
```

```
menu
 
=
 
Gio
.
Menu
()

# The "Evocations" section

section
 
=
 
Gio
.
Menu
()

# Gio.Menu.append() creates a menu item for you

section
.
append
(
"Magic Missile"
,
 
"app.magicmissile"
)

menu
.
append_section
(
"Evocations"
,
 
section
)

# The "Cantrips" section

section
 
=
 
Gio
.
Menu
()

# An explicit menu item

item
 
=
 
Gio
.
MenuItem
()

item
.
set_label
(
"Fire Bolt"
)

item
.
set_detailed_action
(
"app.firebolt"
)

# "cantrips_icon" is a Gio.Icon and is defined elsewhere

item
.
set_icon
(
cantrips_icon
)

section
.
append_item
(
item
)

menu
.
append_section
(
"Cantrips"
,
 
section
)
```

```
const
 
menu
 
=
 
new
 
Gio
.
Menu
();

// The "Evocations" section

let
 
section
 
=
 
new
 
Gio
.
Menu
();

// Gio.Menu.append() creates a menu item for you

section
.
append
(
"Magic Missile"
,
 
"app.magicmissile"
);

menu
.
append_section
(
"Evocations"
,
 
section
);

// The "Cantrips" section

section
 
=
 
new
 
Gio
.
Menu
();

// An explicit menu item

const
 
item
 
=
 
new
 
Gio
.
MenuItem
();

item
.
set_label
(
"Fire Bolt"
);

item
.
set_detailed_action
(
"app.firebolt"
);

// "cantrips_icon" is a Gio.Icon and is defined elsewhere

item
.
set_icon
(
cantrips_icon
);

section
.
append_item
(
item
);

menu
.
append_section
(
"Cantrips"
,
 
section
);
```

```
var
 
menu
 
=
 
new
 
Menu
 
();

// The "Evocations" section

var
 
section
 
=
 
new
 
Menu
 
();

section
.
append
 
(
"Magic Missile"
,
 
"app.magicmissile"
);

menu
.
append_section
 
(
"Evocations"
,
 
section
);

// The "Cantrips" section

section
 
=
 
new
 
Menu
 
();

// an explicit menu item

var
 
item
 
=
 
new
 
MenuItem
 
()
 
{

  
label
 
=
 
"Fire Bolt"
,

  
detailed_action
 
=
 
"app.firebolt"

};

// "cantrips_icon" is a GLib.Icon and is defined elsewhere

item
.
set_icon
 
(
cantrips_icon
);

menu
.
append_section
 
(
"Cantrips"
,
 
section
);
```

```
var
 
menu
 
=
 
new
 
Menu
();

// The "Evocations" section

var
 
section
 
=
 
new
 
Menu
();

section
.
append
(
"Magic Missile"
,
 
"app.magicmissile"
);

menu
.
appendSection
(
"Evocations"
,
 
section
);

// The "Cantrips" section

section
 
=
 
new
 
Menu
();

// an explicit menu item

var
 
item
 
=
 
new
 
MenuItem
();

item
.
setLabel
(
"Fire Bolt"
);

item
.
setDetailedAction
(
"app.firebolt"
);

// "cantripsIcon" is a GLib Icon and is defined elsewhere

item
.
setIcon
 
(
cantripsIcon
);

menu
.
appendSection
 
(
"Cantrips"
,
 
section
);
```

Alternatively, you can use a UI definition:

```
<interface>

  
<menu
 
id=
"menu"
>

    
<section>

      
<item>

        
<attribute
 
name=
"label"
 
translatable=
"yes"
>
Magic
 
Missile
</attribute>

        
<attribute
 
name=
"action"
>
app.magicmissile
</attribute>

      
</item>

    
</section>

    
<section>

      
<attribute
 
name=
"label"
 
translatable=
"yes"
>
Cantrips
</attribute>

      
<item>

        
<attribute
 
name=
"label"
 
translatable=
"yes"
>
Fire
 
Bolt
</attribute>

        
<attribute
 
name=
"action"
>
app.firebolt
</attribute>

        
<attribute
 
name=
"icon"
>
/usr/share/my-app/cantrips.png
</attribute>

      
</item>

    
</section>

  
</menu>

</interface>
```

Load it with GtkBuilder and retrieve it as GMenuModel :

```
GtkBuilder
 
*
builder
 
=

  
gtk_builder_new_from_resource
 
(
"/my/favourite/spells/menu.ui"
);

GMenuModel
 
*
menu
 
=

  
G_MENU_MODEL
 
(
gtk_builder_get_object
 
(
"menu"
);
```

```
builder
 
=
 
Gtk
.
Builder
.
new_from_resource
(
"/my/favourite/spells/menu.ui"
)

menu
 
=
 
builder
.
get_object
(
"menu"
)
```

```
const
 
builder
 
=
 
Gtk
.
Builder
.
new_from_resource
(
"/my/favourite/spells/menu.ui"
);

const
 
menu
 
=
 
builder
.
get_object
(
"menu"
);
```

```
var
 
builder
 
=
 
new
 
Gtk
.
Builder
.
from_resource
 
(
"/my/favourite/spells/menu.ui"
);

Menu
 
menu
 
=
 
builder
.
get_object
 
(
"menu"
);
```

```
var
 
builder
 
=
 
GtkBuilder
.
fromResource
(
"/my/favourite/spells/menu.ui"
);

var
 
menu
 
=
 
(
Menu
)
 
builder
.
getObject
(
"menu"
);
```

## Primary menus ¶

A common pattern in modern user interfaces is to place a button, often with a
gears icon, in some prominent place (top-right), which will open a menu when
clicked. GTK comes with the GtkMenuButton for this use case:

```
GtkBuilder
 
*
builder
 
=

  
gtk_builder_new_from_resource
 
(
"/my/favourite/spells/menu.ui"
);

GMenu
 
*
menu
 
=

  
G_MENU_MODEL
 
(
gtk_builder_get_object
 
(
builder
,
 
"menu"
));

// "menu_button" is defined elsewhere

gtk_menu_button_set_menu_model
 
(
GTK_MENU_BUTTON
 
(
menu_button
),
 
menu
);

gtk_menu_button_set_primary
 
(
GTK_MENU_BUTTON
 
(
menu_button
),
 
TRUE
);

g_object_unref
 
(
builder
);
```

```
builder
 
=
 
Gtk
.
Builder
.
new_from_resource
(
"/my/favourite/spells/menu.ui"
)

menu
 
=
 
builder
.
get_object
(
"menu"
)

# "menu_button" is defined elsewhere

menu_button
.
set_menu_model
(
menu
)

menu_button
.
set_primary
(
True
)
```

```
const
 
builder
 
=
 
Gtk
.
Builder
.
new_from_resource
(
"/my/favourite/spells/menu.ui"
);

const
 
menu
 
=
 
builder
.
get_object
(
"menu"
);

// "menu_button" is defined elsewhere

menu_button
.
set_menu_model
(
menu
);

menu_button
.
set_primary
(
true
);
```

```
var
 
builder
 
=
 
new
 
Gtk
.
Builder
.
from_resource
 
(
"/my/favourite/spells/menu.ui"
);

Menu
 
menu
 
=
 
builder
.
get_object
 
(
"menu"
);

// "menu_button" is defined elsewhere

menu_button
.
menu_model
 
=
 
menu
;

menu_button
.
primary
 
=
 
true
;
```

```
var
 
builder
 
=
 
GtkBuilder
.
fromResource
(
"/my/favourite/spells/menu.ui"
);

var
 
menu
 
=
 
(
Menu
)
 
builder
.
getObject
(
"menu"
);

// "menuButton" is defined elsewhere

menuButton
.
setMenuModel
(
menu
);

menuButton
.
setPrimary
(
true
);
```

## Menu bars ¶

While uncommon in GNOME applications, it is still possible to create menu bars
by using GtkPopoverMenuBar .

## Context menus ¶

It is possible to create a ‘freestanding’ menu from a menu model, using gtk_popover_menu_new_from_model() . This menu can then be used as a context
menu on any widget. The actions you refer to in such a menu can come from the
application or window scopes. You can also introduce a more localized scope,
using gtk_widget_insert_action_group() . The actions from such a local scope
can be used in any menu that is attached below this local scope.

## Icons ¶

In order to add an icon to a menu item, you must specify the icon attribute
on the items in your menu model; when the menu is constructed, the icon will be
properly placed in the UI. The expected value for this attribute is a serialized
GIcon. Luckily, the GIcon serialization format is convenient for this use—for
instance:

```
<attribute
 
name=
"icon"
>
/path/to/my/icon.png
</attribute>
```

is a valid serialization for a GFileIcon for the file at the given path, and:

```
<attribute
 
name=
"icon"
>
preferences-desktop-locale-symbolic
</attribute>
```

is a valid serialization for a GThemedIcon for the preferences-desktop-locale-symbolic symbolic icon.
