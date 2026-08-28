# Actions - GNOME Developer Documentation

# Actions ¶

A GAction is a representation of a single user-interesting action in an application.

To use GAction you should be using GtkApplication .

Note

It’s possible to use GAction without GtkApplication but this is not
discussed here.

## What it is, what it isn’t ¶

A GAction is essentially a way to tell the toolkit about a piece of
functionality in your program, and to give it a name.

Actions are purely functional. They do not contain any presentational information.

An action has four pieces of information associated with it:

- a name as an identifier (usually all-lowercase, untranslated English string)
- an enabled flag indicating if the action can be activated or not (like “sensitive”)
- an optional state value, for stateful actions (like a boolean for toggles)
- an optional parameter type, used when activating the action

An action supports two operations:

- activation, invoked with an optional parameter (of the correct type, see
above)
- state change request, invoked with a new requested state value (of the
correct type). Only supported for stateful actions.

Here are some rules about an action:

- the name is immutable (in the sense that it will never change) and it
is never NULL
- the enabled flag can change
- the parameter type is immutable
- the parameter type is optional: it can be NULL if the parameter type is NULL then action activation must be done
without a parameter (ie: a NULL GVariant pointer) if the parameter type is non NULL then the parameter must have this
type
- the state can change, but it cannot change type if the action was stateful when it was created, it will always have a
state and it will always have exactly the same type (such as boolean
or string) if the action was stateless when it was created, it can never have a state
- you can only request state changes on stateful actions and it is only
possible to request that the state change to a value of the same type as the
existing state

An action does not have any of the following:

- a label
- an icon
- a way of creating a widget corresponding to it
- any other sort of presentational information

## Action state and parameters ¶

Most actions in your application will be stateless actions with no parameters.
These typically appear as menu items with no special decoration. An example is
“quit”.

Stateful actions are used to represent an action which has a closely-associated
state of some kind. A good example is a “fullscreen” action. For this case,
you’d expect to see a checkmark next to the menu item when the fullscreen option
is active. This is usually called a toggle action, and it has a boolean state.
By convention, toggle actions have no parameter type for activation: activating
the action always toggles the state.

Another common case is to have an action representing a enumeration of possible
values of a given type (typically string). This is often called a radio action
and is usually represented in the user interface with radio buttons or radio
menu items, or sometimes a combobox. A good example is “text-justify” with
possible values “left”, “center”, and “right”. By convention, these types of
actions have a parameter type equal to their state type, and activating them
with a particular parameter value is equivalent to changing their state to that
value.

Note

This approach to handling radio buttons is different than many other action
systems. With GAction , there is only one action for “text-justify” and
“left”, “center” and “right” are possible states on that action. There are
not three separate “justify-left”, “justify-center” and “justify-right”
actions.

The final common type of action is a stateless action with a parameter. This is
typically used for actions like “open-bookmark” where the parameter to the
action would be the identifier of the bookmark to open.

## Action target and detailed names ¶

Because some types of actions cannot be invoked without a parameter, it is often
important to specify a parameter when referring to the action from a place where
it will be invoked (such as from a radio button that sets the state to a
particular value or from a menu item that opens a specific bookmark). From these
contexts, the value used for the action parameter is typically called the target
of the action.

Even though toggle actions have a state, they do not have a parameter.
Therefore, a target value is not needed when referring to them – they will
always be toggled on activation.

Most APIs that allow using a GAction (such as GMenuModel and GtkActionable ) allow use of detailed action names. This is a convenient way
of specifying an action name and an action target with a single string.

In the case that the action target is a string with no unusual characters (ie:
only alpha-numeric, plus ‘-’ and ‘.’) then you can use a detailed action name of
the form justify::left to specify the justify action with a target of left.

In the case that the action target is not a string, or contains unusual
characters, you can use the more general format action-name(5) , where the “5”
here is any valid text-format GVariant (ie: a string that can be parsed by g_variant_parse() ). Another example is open-bookmark('http://gnome.org/') .

You can convert between detailed action names and split-out action names and
target values using g_action_parse_detailed_action_name() and g_action_print_detailed_action_name() but usually you will not need to. Most
APIs will provide both ways of specifying actions with targets.

## Action scopes ¶

Actions are always scoped to a particular object on which they operate.

GTK allows you to create any number of scopes for actions, but will always have
two predefined scopes available:

- app , for actions global to the application
- win , for actions tied to an application window

Actions scoped to windows should be the actions that specifically impact that
window. These are actions like “fullscreen” and “close”, or in the case that a
window contains a document, “save” and “print”.

Actions that impact the application as a whole rather than one specific window
are scoped to the application. These are actions like “about” and “preferences”.

If a particular action is scoped to a window then it is scoped to a specific
window. Another way of saying this: if your application has a “fullscreen”
action that applies to windows and it has three windows, then it will have three
fullscreen actions: one for each window.

Having a separate action per-window allows for each window to have a separate
state for each instance of the action as well as being able to control the
enabled state of the action on a per-window basis.

Actions are added to their relevant scope (application or window) using the GActionMap interface.

## GActionMap ¶

GActionMap is an interface exposing a mapping of action names to actions. It
is implemented by GtkApplication and GtkApplicationWindow . Actions can
be added, removed, or looked up.

```
void
      
g_action_map_add_action
    
(
GActionMap
  
*
action_map
,

                                      
GAction
     
*
action
);

void
      
g_action_map_remove_action
 
(
GActionMap
  
*
action_map
,

                                      
const
 
gchar
 
*
action_name
);

GAction
 
*
 
g_action_map_lookup_action
 
(
GActionMap
  
*
action_map
,

                                      
const
 
gchar
 
*
action_name
);
```

```
class
 
ActionMap
:

    
def
 
add_action
(
self
,
 
action
:
 
Gio
.
Action
):

        
pass

    
def
 
remove_action
(
self
,
 
action_name
:
 
str
):

        
pass

    
def
 
lookup_action
(
self
,
 
action_name
:
 
str
)
 
->
 
Gio
.
Action
:

        
pass
```

```
public
 
interface
 
ActionMap
 
{

    
public
 
void
 
add_action
 
(
GLib
.
Action
 
action
);

    
public
 
void
 
remove_action
 
(
string
 
action_name
);

    
public
 
GLib
.
Action
 
lookup_action
 
(
string
 
action_name
);

}
```

```
action_map
.
add_action
(
action
);
 
// GLib.Action

action_map
.
remove_action
(
action_name
);
 
// string

action_map
.
lookup_action
(
action_name
);
 
// string
```

```
public
 
interface
 
ActionMap
 
{

    
default
 
void
 
addAction
 
(
Action
 
action
);

    
default
 
void
 
removeAction
 
(
String
 
actionName
);

    
default
 
@Nullable
 
Action
 
lookupAction
 
(
String
 
actionName
);

}
```

If you want to insert several actions at the same time, it is typically faster
and easier to use GActionEntry .

When referring to actions on a GActionMap only the name of the action itself
is used (ie: “quit”, not “app.quit”). The “app.quit” form is only used when
referring to actions from places like a GMenu or GtkActionable widget
where the scope of the action is not already known. Because you’re using the GtkApplication or GtkApplicationWindow as the GActionMap it is clear
which object your action is scoped to, so the prefix is not needed.

## GSimpleAction vs GAction ¶

GAction is an interface with several implementations. The one that you are
most likely to use directly is GSimpleAction .

A good way to think about the split between GAction and GSimpleAction is
that GAction is the “consumer interface” and GSimpleAction is the
“provider interface”. The GAction interface provides the functions that are
consumed by users/callers/displayers of the action (such as menus and widgets).
The GSimpleAction interface is only accessed by the code that provides the
implementation for the action itself.

Note that GActionMap takes a GAction . Your action will only be
“consumed” as a result of you putting it in a GActionMap .

Compare:

- GAction has a function for checking if an action is enabled
( g_action_get_enabled() ) but only the GSimpleAction API can enable or
disable an action ( g_simple_action_set_enabled() ).
- GAction has a function to query the state of the action
( g_action_get_state() ) and request changes to it
( g_action_change_state() ) but only GSimpleAction has the API to directly
set the state value ( g_simple_action_set_state() ).

If you want to provide a custom GAction implementation then you can have
your own mechanism to control access to state setting and enabled. The GSettings GAction implementation, for example, gets its state directly from the value
in GSettings and is enabled according to lockdown in effect on the key. It is
not possible to directly modify these values in any way (although it is possible
to indirectly affect the state by changing the value of the setting, if you have
permission to do so).

## Using GSimpleAction ¶

If you are implementing actions, probably you will do it with GSimpleAction .

GSimpleAction has two interesting signals: activate and change-state . These correspond directly to g_action_activate() and g_action_change_state() . You will almost certainly need to connect a handler
to the activate signal in order to handle the action being activated. The signal
handler takes a GVariant parameter which is the parameter that was passed to g_action_activate() .

If your action is stateful, you may also want to connect a change-state handler
to deal with state change requests. If your action is stateful and you do not
connect a handler for the change-state signal then the default is that all state
change requests will always change the state to the requested value. Even if you
always want the state to be set to the requested value, you will probably want
to connect a handler so that you can take some action in response to the state
being changed.

The default behaviour of setting the state in response to g_action_change_state() is disabled when connecting a handler to
change-state.  You therefore need to be sure to call g_simple_action_set_state() from your handler if you actually want the state
to change.

A convenient way to bulk-create all the GSimpleActions you need to add to a GActionMap is to use a GActionEntry array and g_action_map_add_action_entries() :

```
static
 
GActionEntry
 
app_entries
[]
 
=
 
{

  
{
 
"preferences"
,
 
preferences_activated
,
 
NULL
,
 
NULL
,
 
NULL
 
},

  
{
 
"quit"
,
 
quit_activated
,
 
NULL
,
 
NULL
,
 
NULL
 
}

};

static
 
void

example_app_startup
 
(
GApplication
 
*
app
)

{

  
// ...

  
g_action_map_add_action_entries
 
(
G_ACTION_MAP
 
(
app
),

                                
app_entries
,
 
G_N_ELEMENTS
 
(
app_entries
),

                                
app
);

  
// ...

}
```

```
public
 
class
 
Example
.
App
 
:
 
GLib
.
Application
 
{

    
public
 
override
 
void
 
startup
 
()
 
{

        
// ...

        
ActionEntry
 
app_entries
[]
 
=
 
{

            
{
 
"preferences"
,
 
this
.
preferences_activated
 
},

            
{
 
"quit"
,
 
this
.
quit_activated
 
}

        
};

        
this
.
add_action_entries
 
(
app_entries
,
 
this
);

        
// ...

    
}

    
private
 
void
 
preferences_activated
 
(
SimpleAction
 
action
,
 
Variant
?
 
parameter
)
 
{

        
// ...

    
}

    
private
 
void
 
quit_activated
 
(
SimpleAction
 
action
,
 
Variant
?
 
parameter
)
 
{

        
// ...

    
}

}
```

## Other kinds of actions ¶

Besides GSimpleAction , GIO provides some other implementations of GAction . One is GSettingsAction , which wraps a GSettings key with an
action that represents the value of the setting and lets you set the key to a
new value when activated. Another is GPropertyAction , which similarly
wraps a GObject property.

Both GSettingsAction and GPropertyAction implement toggle-on-activate
behaviour for boolean states - note that GSimpleAction does not, you have
to implement an activate handler yourself for that.

## Adding actions to your GtkApplication ¶

You can add a GAction to anything implementing the GActionMap interface,
including GtkApplication . This is done with g_action_map_add_action() and g_action_map_add_action_entries() .

Typically, you will want to do this during the startup phase of your
application.

It’s possible to add or remove actions at any time, but doing it before startup
is wasteful in case the application is a remote instance (and will just exit
anyway). It is also possible to dynamically add and remove actions any time
after startup, when the application is running.

## Adding actions to your GtkApplicationWindow ¶

GtkApplicationWindow also implements GActionMap . You will typically
want to add most actions to your window when it is constructed. It is possible
to add and remove actions at any time while the window exists.

For example:

```
static
 
void

on_save_activate
 
(
GAction
 
*
action
,

                  
GVariant
 
*
param
)

{

  
g_print
 
(
"You are welcome"
);

}

static
 
void

on_app_activate
 
(
GApplication
 
*
app
)

{

  
GtkWidget
 
*
window
 
=
 
gtk_application_window_new
 
(
GTK_APPLICATION
 
(
app
));

  
gtk_window_present
 
(
GTK_WINDOW
 
(
window
));

  
GAction
 
*
action
 
=
 
g_simple_action_new
 
(
"save"
,
 
NULL
);

  
g_signal_connect
 
(
action
,
 
"activate"
,
 
G_CALLBACK
 
(
on_save_activate
),
 
NULL
);

  
g_action_map_add_action
 
(
G_ACTION_MAP
 
(
window
),
 
action
);

  
GtkWidget
 
*
button
 
=
 
gtk_button_new_with_label
 
(
"Save"
);

  
gtk_window_set_child
 
(
GTK_WINDOW
 
(
window
),
 
button
);

  
gtk_actionable_set_action_name
 
(
GTK_ACTIONABLE
 
(
button
),
 
"win.save"
);

}

// ...

int

main
 
(
int
 
argc
,

      
char
 
*
argv
[])

{

  
GtkApplication
 
*
app
 
=

    
gtk_application_new
 
(
"com.example.App"
,
 
G_APPLICATION_FLAGS_NONE
);

  
g_signal_connect
 
(
app
,
 
"activate"
,
 
G_CALLBACK
 
(
on_app_activate
),
 
NULL
);

  
return
 
g_application_run
 
(
G_APPLICATION
 
(
app
),
 
argc
,
 
argv
);

}
```

```
from
 
gi.repository
 
import
 
Gio
,
 
Gtk

def
 
on_save_activate
(
action
,
 
_
):

    
print
 
"You are welcome"

def
 
on_app_activate
(
app
):

    
window
 
=
 
Gtk
.
ApplicationWindow
(
application
=
app
)

    
window
.
present
()

    
action
 
=
 
Gio
.
SimpleAction
(
name
=
"save"
)

    
action
.
connect
(
"activate"
,
 
on_save_activate
)

    
window
.
add_action
(
action
)

    
button
 
=
 
Gtk
.
Button
(
label
=
"Save"
)

    
window
.
set_child
(
button
)

    
button
.
set_action_name
(
"win.save"
)

app
 
=
 
Gtk
.
Application
()

app
.
connect
(
"activate"
,
 
on_app_activate
)

app
.
run
([])
```

```
public
 
class
 
Example
.
App
 
:
 
Gtk
.
Application
 
{

    
public
 
App
 
()
 
{

        
Object
 
(
application_id
:
 
"com.example.App"
,

        
flags
:
 
ApplicationFlags
.
FLAGS_NONE
);

    
}

    
public
 
override
 
void
 
activate
 
()
 
{

        
var
 
window
 
=
 
new
 
Gtk
.
ApplicationWindow
 
(
this
);

        
window
.
present
 
();

        
var
 
action
 
=
 
new
 
SimpleAction
 
(
"save"
,
 
null
);

        
action
.
activate
.
connect
 
(()
 
=>
 
{

            
stdout
.
printf
 
(
"You are welcome
\n
"
);

        
});

        
window
.
add_action
 
(
action
);

        
var
 
button
 
=
 
new
 
Gtk
.
Button
 
(
"Save"
);

        
window
.
child
 
=
 
button
;

        
button
.
action_name
 
=
 
"win.save"
;

    
}

    
public
 
static
 
int
 
main
 
(
string
[]
 
args
)
 
{

        
var
 
app
 
=
 
new
 
Example
.
App
 
();

        
return
 
app
.
run
 
(
args
);

    
}

}
```

```
import
 
Gtk
 
from
 
"gi://Gtk?version=4.0"
;

import
 
Adw
 
from
 
"gi://Adw?version=1"
;

import
 
Gio
 
from
 
"gi://Gio"
;

function
 
on_save_activate
(
action
,
 
param
)
 
{

  
log
(
"You are welcome"
);

}

function
 
on_app_activate
(
app
)
 
{

  
const
 
window
 
=
 
new
 
Gtk
.
ApplicationWindow
({
 
application
 
});

  
window
.
present
();

  
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
 
"save"
 
});

  
action
.
connect
(
"activate"
,
 
on_save_activate
);

  
window
.
add_action
(
action
);

  
const
 
button
 
=
 
new
 
Gtk
.
Button
({
 
label
:
 
"Save"
 
});

  
window
.
set_child
(
button
);

  
button
.
set_action_name
(
"win.save"
);

}

const
 
application
 
=
 
new
 
Gtk
.
Application
();

application
.
connect
(
"activate"
,
 
on_app_activate
);

application
.
run
([]);
```

```
import
 
org.gnome.gio.ApplicationFlags
;

import
 
org.gnome.gio.SimpleAction
;

import
 
org.gnome.gtk.*
;

public
 
class
 
ExampleApp
 
extends
 
Application
 
{

    
public
 
ExampleApp
 
()
 
{

        
setApplicationId
(
"com.example.App"
);

        
setFlags
(
ApplicationFlags
.
DEFAULT_FLAGS
);

    
}

    
@Override

    
public
 
void
 
activate
 
()
 
{

        
var
 
window
 
=
 
new
 
ApplicationWindow
(
this
);

        
window
.
present
();

        
var
 
action
 
=
 
new
 
SimpleAction
(
"save"
,
 
null
);

        
action
.
onActivate
(
_
 
->
 
System
.
out
.
println
 
(
"You are welcome"
));

        
window
.
addAction
(
action
);

        
var
 
button
 
=
 
Button
.
withLabel
(
"Save"
);

        
window
.
setChild
(
button
);

        
button
.
setActionName
(
"win.save"
);

    
}

    
public
 
static
 
void
 
main
 
(
String
[]
 
args
)
 
{

        
var
 
app
 
=
 
new
 
ExampleApp
();

        
app
.
run
(
args
);

    
}

}
```

## Accelerators (keybindings) for actions ¶

Use gtk_application_add_accelerator() inside your application’s startup
implementation. For example:

```
gtk_application_set_accels_for_action
 
(
app
,
 
"win.new_tab"
,

                                       
(
const
 
char
 
*
[])
 
{

                                         
"<Control><Shift>T"
,

                                         
NULL
,

                                       
});
```

```
app
.
set_accels_for_action
(
"win.new_tab"
,
 
[
"<Control><Shift>T"
])
```

```
this
.
set_accels_for_action
 
(
"win.new_tab"
,
 
{
 
"<Control><Shift>T"
,
 
null
 
});
```

```
app
.
set_accels_for_action
(
"win.new_tab"
,
 
[
"<Control><Shift>T"
]);
```

```
this
.
setAccelsForAction
(
"win.new_tab"
,
 
new
 
String
[]
 
{
 
"<Control><Shift>T"
 
});
```

## What can be done with actions ¶

GActions that you add to your application or window can be used in
several different ways.

- used with GMenu
- used with GtkActionable widgets
- used with shortcuts
- remotely activated from a remote GApplication instance (only for
application actions)
- listed as “Additional application actions” in desktop files (only for
application actions)
- remotely activated from other D-Bus callers (such as Ubuntu’s HUD)
- used with GNotification notifications (only for application actions)
