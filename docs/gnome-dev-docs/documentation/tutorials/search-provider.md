# Writing a Search Provider - GNOME Developer Documentation

# Writing a Search Provider ¶

Search is a central concept in the GNOME user experience. The search entry in
the shell overview is the place to go for quick searches.

A search provider is a mechanism by which an application can expose its search
capabilities to GNOME Shell. When the user types anything in the shell’s search
entry, the text is forwarded to all known search providers, and the results are
relayed back for display.

In the shell overview, search hits are grouped against their respective
applications and a maximum of three is shown per application. The user can
either select an individual result, in which case the application SHOULD open
it; or she can select the application icon, in which case it COULD show an
in-app view of all the results from this specific application without any
limitation.

The exact meaning of open depends on the application in question. Files and
Documents offer a preview of the item’s content; Software shows an UI to install
the application; and Terminal windows are simply brought into focus. If
possible, the applications SHOULD offer a way to go ‘back’ to its search view,
which should be pre-populated with the same search that was done in the shell.
This lets the user continue to refine their search inside the application.

Applications should be prepared to handle repeated queries as the user types
more characters into the shell search entry.

## Basics ¶

For an application to become a search provider, it should implement the
following D-Bus interface:

```
<
node
>

  
<
interface
 
name
=
"org.gnome.Shell.SearchProvider2"
>

    
<
method
 
name
=
"GetInitialResultSet"
>

      
<
arg
 
type
=
"as"
 
name
=
"terms"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"as"
 
name
=
"results"
 
direction
=
"out"
 
/>

    
</
method
>

    
<
method
 
name
=
"GetSubsearchResultSet"
>

      
<
arg
 
type
=
"as"
 
name
=
"previous_results"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"as"
 
name
=
"terms"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"as"
 
name
=
"results"
 
direction
=
"out"
 
/>

    
</
method
>

    
<
method
 
name
=
"GetResultMetas"
>

      
<
arg
 
type
=
"as"
 
name
=
"identifiers"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"aa
{sv}
"
 
name
=
"metas"
 
direction
=
"out"
 
/>

    
</
method
>

    
<
method
 
name
=
"ActivateResult"
>

      
<
arg
 
type
=
"s"
 
name
=
"identifier"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"as"
 
name
=
"terms"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"u"
 
name
=
"timestamp"
 
direction
=
"in"
 
/>

    
</
method
>

    
<
method
 
name
=
"LaunchSearch"
>

      
<
arg
 
type
=
"as"
 
name
=
"terms"
 
direction
=
"in"
 
/>

      
<
arg
 
type
=
"u"
 
name
=
"timestamp"
 
direction
=
"in"
 
/>

    
</
method
>

  
</
interface
>

</
node
>
```

Note

You can also find a copy of this D-Bus interface definition at $(datadir)/dbus-1/interfaces/org.gnome.ShellSearchProvider2.xml

### Registering a new search provider ¶

In order to register the search provider with GNOME Shell, you must provide a
key/value file in $(datadir)/gnome-shell/search-providers for your provider.

Let’s assume that we have an application called “Foo Bar” that is D-Bus
activatable, where “Foo” is the project-wide namespace and “Bar” is the name of the
application. Then we can create a file called foo.bar.search-provider.ini as:

```
[
Shell
 
Search
 
Provider
]

DesktopId
=
foo
.
Bar
.
desktop

BusName
=
foo
.
Bar

ObjectPath
=/
foo
/
Bar
/
SearchProvider

Version
=
2
```

After you restart the Shell, the queries will now be forwarded to the search
provider using the given D-Bus name.

## Configuration ¶

The GNOME control-center has a settings panel that allows the user to configure
which search providers to use in the shell, and in what order to show their
results. This is handled completely between the control-center and the shell,
your application is not involved other than providing the name its desktop file
in the DesktopId key. Both the shell and the control-center use this key to find
the icon and name to use in the UI to represent your search provider.

## Details ¶

Keep in mind is that activating the search provider by entering text in the
shell search entry should not visibly launch the application. It is still
possible to implement the search provider in the same binary as the application
itself (which has the advantage that you can share the search implementation
between the search provider and the in-application search); just make sure that
activating the application starts it in ‘service’ mode. The startup() virtual function should not open any windows, that should only be done in activate() or open() .

Another point to keep in mind is that searching in the shell should not affect
the UI of your application if it is already running until the user explicitly
chooses to open (one or all) search results with the application. Once the user
does that, it is expected that you reuse the already open window and switch it
to the search view.

All methods of the SearchProvider interface should be implemented
asynchronously, in particular, you should handle overlapping subsearch requests,
as the user keeps typing in the shell search entry. A common way to deal with
this is to cancel the previous search request when a new one comes in.

## The SearchProvider interface ¶

### GetInitialResultSet :: (as) → (as) ¶

GetInitialResultSet is called when a new search is started. It gets an array
of search terms as arguments, and should return an array of result IDs.
gnome-shell will call GetResultMetas for (some) of these result IDs to get
details about the result that can be be displayed in the result list.

### GetSubsearchResultSet :: (as,as) → (as) ¶

GetSubsearchResultSet is called to refine the initial search results when
the user types more characters in the search entry. It gets the previous search
results and the current search terms as arguments, and should return an array of
result IDs, just like GetInitialResulSet .

### GetResultMetas :: (as) → (aa{sv}) ¶

GetResultMetas is called to obtain detailed information for results. It gets
an array of result IDs as arguments, and should return a matching array of
dictionaries (ie one a{sv} for each passed-in result ID). The following pieces
of information should be provided for each result:

- “id”: the result ID
- “name”: the display name for the result
- “icon”: a serialized GIcon (see g_icon_serialize() ), or alternatively,
- “gicon”: a textual representation of a GIcon (see g_icon_to_string() ), or alternatively,
- “icon-data”: a tuple of type (iiibiiay) describing a pixbuf with width,
height, rowstride, has-alpha, bits-per-sample, n-channels, and image data
- “description”: an optional short description (1-2 lines)
- “clipboardText”: an optional text to send to the clipboard on activation

### ActivateResult :: (s,as,u) → () ¶

ActivateResult is called when the user clicks on an individual result to
open it in the application. The arguments are the result ID, the current search
terms and a timestamp.

### LaunchSearch :: (as,u) → () ¶

LaunchSearch is called when the user clicks on the provider icon to display
more search results in the application. The arguments are the current search
terms and a timestamp.

## Implementation ¶

You can add a search provider to an application that is using GtkApplication by
exporting the GDBusInterfaceSkeleton that implementing the search provider
interface in the dbus_register() vfunc. Often the search provider is in the same
binary as the application itself because it has the advantage of being able to
share the search implementation, but this is not mandatory.

Let’s assume that we have an application called Foo Bar, where Foo is the
project-wide namespace and Bar is the name of the application.

You should generate the GDBusInterfaceSkeleton using gdbus-codegen through the “gnome” Meson module:

```
gnome
 
=
 
import
(
'gnome'
)

sp_sources
 
=
 
gnome
.
gdbus_codegen
(

  
'shell-search-provider-generated'
,

  
sources
:
 
'org.gnome.Shell.SearchProvider2.xml'
,

  
interface_prefix
 
:
 
'org.gnome.'
,

  
namespace
 
:
 
'Bar'
,

)
```

Then you will need to override the dbus_register() and dbus_unregister() virtual functions of your GtkApplication in order to export and unexport the
interface at the given path:

```
G_DEFINE_TYPE
 
(
BarApplication
,
 
bar_application
,
 
GTK_TYPE_APPLICATION
);

static
 
void

bar_application_class_init
 
(
BarApplicationClass
 
*
class
)

{

  
GApplicationClass
 
*
application_class
 
=
 
G_APPLICATION_CLASS
 
(
class
);

  
application_class
->
dbus_register
 
=
 
bar_application_dbus_register
;

  
application_class
->
dbus_unregister
 
=
 
bar_application_dbus_unregister
;

}

GtkApplication
 
*

bar_application_new
 
(
void
)

{

  
return
 
g_object_new
 
(
BAR_TYPE_APPLICATION
,

                       
"application-id"
,
 
"foo.bar"
,

                       
NULL
);

}

static
 
gboolean

bar_application_dbus_register
 
(
GApplication
 
*
application
,

                               
GDBusConnection
 
*
connection
,

                               
const
 
gchar
 
*
object_path
,

                               
GError
 
**
error
)

{

  
BarApplication
 
*
self
 
=
 
BAR_APPLICATION
 
(
application
);

  
// Chain up to the parent's implementation

  
if
 
(
!
G_APPLICATION_CLASS
 
(
bar_application_parent_class
)

         
->
dbus_register
 
(
application
,

                          
connection
,

                          
object_path
,

                          
error
))

    
return
 
FALSE
;

  
g_autofree
 
search_provider_path
 
=

    
g_strconcat
 
(
object_path
,
 
"/SearchProvider"
,
 
NULL
);

  
// Export the SearchProvider interface to the given path

  
if
 
(
!
bar_search_provider_dbus_export
 
(
self
->
search_provider
,

                                        
connection
,

                                        
search_provider_path
,

                                        
error
))

    
return
 
FALSE
;

  
return
 
TRUE
;

}

static
 
void

bar_application_dbus_unregister
 
(
GApplication
 
*
application
,

                                 
GDBusConnection
 
*
connection
,

                                 
const
 
gchar
 
*
object_path
)

{

  
BarApplication
 
*
self
 
=
 
BAR_APPLICATION
 
(
application
);

  
g_autofree
 
char
 
*
search_provider_path
 
=

    
g_strconcat
 
(
object_path
,
 
"/SearchProvider"
,
 
NULL
);

  
bar_search_provider_dbus_unexport
 
(
self
->
search_provider
,

                                     
connection
,

                                     
search_provider_path
);

  
G_APPLICATION_CLASS
 
(
bar_application_parent_class
)

    
->
dbus_unregister
 
(
application
,
 
connection
,
 
object_path
);

}
```

You will need to create a search provider object as the implementation of the
interface:

```
G_DECLARE_FINAL_TYPE
 
(
BarSearchProvider
,
 
bar_search_provider
,
 
BAR
,
 
SEARCH_PROVIDER
,
 
GObject
)

struct
 
_BarSearchProvider
 
{

  
GObject
 
parent_instance
;

  
ShellSearchProvider2
 
*
skeleton
;

};

G_DEFINE_TYPE
 
(
BarSearchProvider
,
 
bar_search_provider
,
 
G_TYPE_OBJECT
)

static
 
void

bar_search_provider_init
 
(
BarSearchProvider
 
*
self
)

{

  
self
->
skeleton
 
=
 
shell_search_provider2_skeleton_new
 
();

  
g_signal_connect_swapped
 
(
self
->
skeleton
,

                            
"handle-activate-result"
,

                            
G_CALLBACK
 
(
bar_search_provider_activate_result
),

                            
self
);

  
g_signal_connect_swapped
 
(
self
->
skeleton
,

                            
"handle-get-initial-result-set"
,

                            
G_CALLBACK
 
(
bar_search_provider_get_initial_result_set
),

                            
self
);

  
g_signal_connect_swapped
 
(
self
->
skeleton
,

                            
"handle-get-subsearch-result-set"
,

                            
G_CALLBACK
 
(
bar_search_provider_get_subsearch_result_set
),

                            
self
);

  
g_signal_connect_swapped
 
(
self
->
skeleton
,

                            
"handle-get-result-metas"
,

                            
G_CALLBACK
 
(
bar_search_provider_get_result_metas
),

                            
self
);

  
g_signal_connect_swapped
 
(
self
->
skeleton
,

                            
"handle-launch-search"
,

                            
G_CALLBACK
 
(
bar_search_provider_launch_search
),

                            
self
);

}
```

Important

If your D-Bus method handlers are asynchronous, then you should hold a
reference to BarApplication using g_application_hold() and then release
the reference using g_application_release() when you are done. Use the
corresponding shell_search_provider2_complete* methods to indicate
completion and send back results, if any.

Here’s a pair of convenience wrapper methods for exporting and unexporting the
skeleton:

```
gboolean

bar_search_provider_dbus_export
 
(
BarSearchProvider
 
*
self
,

                                 
GDBusConnection
 
*
connection
,

                                 
const
 
gchar
 
*
object_path
,

                                 
GError
 
**
error
)

{

  
return
 
g_dbus_interface_skeleton_export
 
(

    
G_DBUS_INTERFACE_SKELETON
 
(
self
->
skeleton
),

    
connection
,

    
object_path
,

    
error
);

}

void

bar_search_provider_dbus_unexport
 
(
BarSearchProvider
 
*
self
,

                                   
GDBusConnection
 
*
connection
,

                                   
const
 
gchar
 
*
object_path
)

{

  
if
 
(
g_dbus_interface_skeleton_has_connection
 
(

        
G_DBUS_INTERFACE_SKELETON
 
(
self
->
skeleton
),

        
connection
))

    
{

      
g_dbus_interface_skeleton_unexport_from_connection
 
(

        
G_DBUS_INTERFACE_SKELETON
 
(
self
->
skeleton
),

        
connection
);

    
}

}
```
