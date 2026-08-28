# Using Notifications - GNOME Developer Documentation

# Using Notifications ¶

GNOME applications should use notifications to inform the user that something
has happened that requires their attention.

Notifications should not be intrusive, or distracting.

For more information on when to use notifications, you should follow the Human
Interface Guidelines .

Important

Remember that users can disable notifications for specific applications, or
globally. You should not rely exclusively on notifications.

## Prerequisites ¶

In order to use notifications in GNOME you will need to:

- use GApplication or GtkApplication
- provide a valid desktop file with the same name as your application id
- ensure that your application can be activated via D-Bus

The desktop file and D-Bus activation provide notification persistence, allowing
the desktop to associate the notification to the application even if the
application is not running.

## Anatomy of a notification ¶

A typical notification has a number of ingredients:

- a one-line title
- a longer, descriptive message body (optional)
- an icon (optional)
- actions , each with a label for use in a button (optional)
- additionally, notifications may be marked as urgent

## Creating a notification ¶

To send a notification, first create a GNotification object, and add the
data for your notification to it:

```
g_autoptr
(
GNotification
)
 
notification
 
=
 
g_notification_new
 
(
"Lunch is ready"
);

g_notification_set_body
 
(
notification
,
 
"Today we have pancakes and salad, and fruit and cake for dessert"
);

g_autoptr
(
GFile
)
 
file
 
=
 
g_file_new_for_path
 
(
"fruitbowl.png"
);

g_autoptr
(
GIcon
)
 
icon
 
=
 
g_file_icon_new
 
(
file
);

// The notification instance will acquire a reference on the icon

g_notification_set_icon
 
(
notification
,
 
G_ICON
 
(
icon
));
```

```
notification
 
=
 
Gio
.
Notification
()

notification
.
set_title
(
"Lunch is ready"
)

notification
.
set_body
(
"Today we have pancakes and salad, and fruit and cake for dessert"
)

file
 
=
 
Gio
.
File
.
new_for_path
(
"fruitbowl.png"
)

icon
 
=
 
Gio
.
FileIcon
(
file
=
file
)

notification
.
set_icon
(
icon
)
```

```
var
 
notification
 
=
 
new
 
Notification
 
(
"Lunch is ready"
);

notification
.
set_body
 
(
"Today we have pancakes and salad, and fruit and cake for dessert"
);

var
 
file
 
=
 
new
 
File
.
for_path
 
(
"fruitbowl.png"
);

var
 
icon
 
=
 
new
 
FileIcon
 
(
file
);

// The notification instance will acquire a reference on the icon

notification
.
set_icon
 
(
icon
);
```

```
const
 
notification
 
=
 
new
 
Gio
.
Notification
();

notification
.
set_title
(
"Lunch is ready"
);

notification
.
set_body
(

  
"Today we have pancakes and salad, and fruit and cake for dessert"
,

);

const
 
file
 
=
 
Gio
.
File
.
new_for_path
(
"fruitbowl.png"
);

const
 
icon
 
=
 
new
 
Gio
.
FileIcon
({
file
});

// The notification instance will acquire a reference on the icon

notification
.
set_icon
(
icon
);
```

```
var
 
notification
 
=
 
new
 
Notification
(
"Lunch is ready"
);

notification
.
setBody
(
"Today we have pancakes and salad, and fruit and cake for dessert"
);

var
 
file
 
=
 
File
.
newForPath
(
"fruitbowl.png"
);

var
 
icon
 
=
 
new
 
FileIcon
(
file
);

// The notification instance will acquire a reference on the icon

notification
.
setIcon
(
icon
);
```

Note that the title should be short; the body can be longer, say a paragraph.
The icon may be displayed at a small size (say, 24×24), so choose an icon that
is remains readable at small size.

To show your notification to the user, use the GApplication function for this
purpose:

```
// The application instance will acquire a reference on the

// notification object

g_application_send_notification
 
(
application
,
 
"lunch-is-ready"
,
 
notification
);
```

```
# The Application instance will acquire a reference on the

# notification object

application
.
send_notification
(
"lunch-is-ready"
,
 
notification
)
```

```
// The application instance will acquire a reference on the

// notification object

application
.
send_notification
 
(
"lunch-is-ready"
,
 
notification
);
```

```
// The application instance will acquire a reference on the

// notification object

application
.
send_notification
(
"lunch-is-ready"
,
 
notification
);
```

```
// The application instance will acquire a reference on the

// notification object

application
.
sendNotification
(
"lunch-is-ready"
,
 
notification
);
```

You need to provide an id for your notification here. This can be used if you
want to make updates to an existing notification: simply send a notification
with the same id. Note that the GNotification object does not have to be kept
around after sending the notification; you can unref it right away. It is not a
‘live’ object that is associated with the visible notification.

## Adding actions ¶

Often, you want the user to be able to react to the notification in some way,
other than just dismissing it. GNotification lets you do this by associating
actions with your notification. These will typically be presented as buttons in
the popup. One action has a special role, it is the ‘default’ action that gets
activated when the user clicks on the notification, not on a particular button.

```
g_notification_set_default_action
 
(
notification
,
 
"app.go-to-lunch"
);

g_notification_add_button
 
(
notification
,
 
"5 minutes"
,
 
"app.reply-5-minutes"
);

g_notification_add_button
 
(
notification
,
 
"Order takeout"
,
 
"app.order-takeout"
);
```

```
notification
.
set_default_action
(
"app.go-to-lunch"
)

notification
.
add_button
(
"5 minutes"
,
 
"app.reply-5-minutes"
)

notification
.
add_button
(
"Order takeout"
,
 
"app.order-takeout"
)
```

```
notification
.
set_default_action
 
(
"app.go-to-lunch"
);

notification
.
add_button
 
(
"5 minutes"
,
 
"app.reply-5-minutes"
);

notification
.
add_button
 
(
"Order takeout"
,
 
"app.order-takeout"
);
```

```
notification
.
set_default_action
(
"app.go-to-lunch"
);

notification
.
add_button
(
"5 minutes"
,
 
"app.reply-5-minutes"
);

notification
.
add_button
(
"Order takeout"
,
 
"app.order-takeout"
);
```

```
notification
.
setDefaultAction
(
"app.go-to-lunch"
);

notification
.
addButton
(
"5 minutes"
,
 
"app.reply-5-minutes"
);

notification
.
addButton
(
"Order takeout"
,
 
"app.order-takeout"
);
```

The actions are referred to here with their ‘app.’ prefixed name. This indicates
that the actions have to be added to your GApplication . You can not use any
other actions in GNotifications (window-specific actions with a ‘win.’ prefix,
or key shorcuts using other prefixes will not work).

## Actions with parameters ¶

A common pattern is to pass a ‘target’ parameter to the action that contains
sufficient details about the notification to let your application react in a
meaningful way.

As an example, here is how a notification about a newly installed application
could provide a launch button:

```
g_autofree
 
char
 
*
title
 
=
 
g_strdup_printf
 
(
"%s is now installed"
,
 
appname
);

g_autoptr
(
GNotification
)
 
notification
 
=
 
g_notification_new
 
(
title
);

g_notification_add_button_with_target
 
(
notification
,
 
"Launch"
,
 
"app.launch"
,
 
"s"
,
 
appid
);

g_application_send_notification
 
(
application
,
 
"app-installed"
,
 
notification
);
```

```
notification
 
=
 
Gio
.
Notification
()

notification
.
set_title
(
f
"
{
appname
}
 is now installed"
)

notification
.
add_button_with_target
(
"Launch"
,
 
"app.launch"
,
 
"s"
,
 
appid
)

application
.
send_notification
(
"app-installed"
,
 
notification
)
```

```
string
 
title
 
=
 
appname
 
+
 
" is now installed"
;

var
 
notification
 
=
 
new
 
Notification
 
(
title
);

notification
.
add_button_with_target
 
(
"Launch"
,
 
"app.launch"
,
 
"s"
,
 
appid
);

application
.
send_notification
 
(
"app-installed"
,
 
notification
);
```

```
const
 
title
 
=
 
appname
 
+
 
" is now installed"
;

const
 
notification
 
=
 
new
 
Gio
.
Notification
();

notification
.
set_title
(
title
);

notification
.
add_button_with_target
(

  
"Launch"
,

  
"app.launch"
,

  
GLib
.
Variant
.
new_string
(
appid
),

);

application
.
send_notification
(
"app-installed"
,
 
notification
);
```

```
String
 
title
 
=
 
appname
 
+
 
" is now installed"
;

var
 
notification
 
=
 
new
 
Notification
(
title
);

notification
.
addButtonWithTarget
(
"Launch"
,
 
"app.launch"
,
 
Variant
.
pack
(
appid
));

application
.
sendNotification
(
"app-installed"
,
 
notification
);
```

To make this work, your application needs to have a suitable ‘launch’ action
that takes the application id as a string parameter:

```
static
 
GActionEntry
 
actions
[]
 
=
 
{

  
{
 
"launch"
,
 
launch_application
,
 
"s"
,
 
NULL
,
 
NULL
 
},

    
// ...

};

g_action_map_add_action_entries
 
(
G_ACTION_MAP
 
(
application
),

                                 
actions
,
 
G_N_ELEMENTS
 
(
actions
),

                                 
application
);
```

```
# The "launch_application" function is defined elsewhere

action
 
=
 
Gio
.
SimpleAction
(
name
=
"launch"
)

action
.
connect
(
"activate"
,
 
launch_application
)

application
.
add_action
(
action
)
```

```
// the "launch_application" function is defined elsewhere

ActionEntry
 
actions
[]
 
=
 
{

  
{
 
"launch"
,
 
launch_application
 
},

  
// ...

};

application
.
add_action_entries
 
(
actions
,
 
application
);
```

```
// the "launch_application" function is defined elsewhere

const
 
action
 
=
 
new
 
Gio
.
SimpleAction
({
 
name
:
 
"launch"
 
});

action
.
connect
(
"activate"
,
 
launch_application
);

application
.
add_action
(
action
);
```

```
// The "launchApplication" function is defined elsewhere

var
 
action
 
=
 
new
 
SimpleAction
(
"launch"
,
 
null
);

action
.
onActivate
(
this
::
launchApplication
);

application
.
addAction
(
action
);
```

## Stale notifications ¶

Sometimes, a notification is no longer relevant and should not persist any
longer. In those cases, you can explicitly withdraw it, like this:

```
if
 
(
now_is_tea_time
 
())

  
g_application_withdraw_notification
 
(
application
,
 
"lunch-is-ready"
);
```

```
if
 
now_is_tea_time
():

    
application
.
withdraw_notification
(
"lunch-is-ready"
)
```

```
if
 
(
now_is_tea_time
 
())

  
application
.
withdraw_notification
 
(
"lunch-is-ready"
);
```

```
if
 
(
now_is_tea_time
())
 
{

   
application
.
withdraw_notification
(
"lunch-is-ready"
);

}
```

```
if
 
(
nowIsTeaTime
 
())

  
application
.
withdrawNotification
(
"lunch-is-ready"
);
```

## Disabling notifications ¶

If your application uses notifications, you should allow users to disable them.

GNOME has a blanket “do not disturb” mode, but each application can be
individually controlled through the “Notifications” settings panel.

In order to make your application appear in the panel, add the following line to
your desktop file:

```
X
-
GNOME
-
UsesNotifications
=
true
```
