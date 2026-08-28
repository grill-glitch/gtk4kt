# Menu Buttons - GNOME Developer Documentation

# Menu Buttons ¶

Menu buttons let you show a menu whenever the user activates the button.

Menus are defined using GMenu , and each item in the
menu will activate an action associated to it.

- Interface guidelines

```
GtkWidget
 
*
button
 
=
 
gtk_menu_button_new
 
();

gtk_menu_button_set_icon_name
 
(
GTK_MENU_BUTTON
 
(
button
),
 
"open-menu-symbolic"
);

// "menu" is defined elsewhere

gtk_menu_button_set_menu_model
 
(
GTK_MENU_BUTTON
 
(
button
),
 
menu
);
```

```
# "menu" is defined elsewhere

button
 
=
 
Gtk
.
MenuButton
(
icon_name
=
"open-menu-symbolic"
,
 
menu_model
=
menu
)
```

```
// "menu" is defined elsewhere

var
 
button
 
=
 
new
 
Gtk
.
MenuButton
 
()
 
{

    
icon_name
 
=
 
"open-menu-symbolic"
,

    
menu_model
 
=
 
menu

};
```

```
// "menu" is defined elsewhere

const
 
button
 
=
 
new
 
Gtk
.
MenuButton
({

  
icon_name
:
 
"open-menu-symbolic"
,

  
menu_model
:
 
menu
,

});
```

```
var
 
button
 
=
 
new
 
MenuButton
();

button
.
setIconName
(
"open-menu-symbolic"
);

// "menu" is defined elsewhere

button
.
setMenuModel
(
menu
);
```

```
<object
 
class=
"GtkMenuButton"
 
id=
"primary-menu-button"
>

  
<property
 
name=
"icon-name"
>
open-menu-symbolic
</property>

  
<property
 
name=
"menu-model"
>
primary_menu_model
</property>

</object>
```

## Useful methods for the component ¶

- By default, a GtkMenuButton will only display a downward arrow icon; you can
use set_label() and set_icon_name() to specify a label or an icon,
respectively.
- If you want to show a more complex button, you can use the set_child() method with a custom widget.
- If you display a label, an icon, or even a custom widget as the button’s
child, you can still request GtkMenuButton to show an arrow by using the set_always_show_arrow() method.
- If the menu button should act as the primary menu button for your window,
and respond to the F10 keyboard shortcut, you can use the set_primary() method.

## API references ¶

In the examples we used the following classes:

- GtkMenuButton
- GMenuModel
