# Dealing With Deprecations - GNOME Developer Documentation

# Dealing With Deprecations ¶

Deprecations are commonly used to allow APIs to evolve over time, by phasing out
no-longer-fashionable API functions and replacing them with newer, cooler
replacements. This page explains how libraries in the GNOME stack do
deprecations, and what mechanisms they provide for an application
developer—you—to deal with them.

One important thing to keep in mind is that ‘deprecated’ does not mean ‘broken’,
or ‘unusable’. There is no need to rush into replacing every deprecated function
by its replacement right away. The next major release of the library in question
(which is the point at which deprecated API may be dropped) may be years away.
Until that happens, it is perfectly fine to keep using the functions.

## Basics ¶

How does one recognize that a function is deprecated? Since a few years, the
GNOME libraries annotates deprecations in their headers, like this:

```
GLIB_DEPRECATED_IN_2_26

void
         
g_completion_clear_items
   
(
GCompletion
*
    
cmp
);

GLIB_DEPRECATED_IN_2_40_FOR
(
g_settings_schema_key_get_range
)

GVariant
 
*
   
g_settings_get_range
        
(
GSettings
   
*
settings
,

                                          
const
 
gchar
 
*
key
);

GDK_DEPRECATED_IN_3_10

void
     
gtk_window_reshow_with_initial_size
 
(
GtkWindow
 
*
window
);
```

These annotations are defined as macros in the GLib headers, which expand to
function attributes that cause the C compiler to warn when these functions are
used.

In addition, the doc comments for deprecated functions contain a Deprecated: tag:

```
/**

 * g_completion_clear_items:

 * @cmp: the #GCompletion.

 *

 * Removes all items from the #GCompletion. The items are not freed, so if the

 * memory was dynamically allocated, it should be freed after calling this

 * function.

 *

 * Deprecated:2.26: Rarely used API

 */

/**

 * g_settings_get_range:

 * @settings: a #GSettings

 * @key: the key to query the range of

 *

 * Queries the range of a key.

 *

 * Since: 2.28

 *

 * Deprecated: 2.40: Use g_settings_schema_key_get_range() instead.

 */

/**

 * gtk_window_reshow_with_initial_size:

 * @window: a #GtkWindow

 *

 * Hides @window, then reshows it, resetting the

 * default size and position of the window. Used

 * by GUI builders only.

 *

 * Deprecated: 3.10: GUI builders can call gtk_widget_hide(),

 *   gtk_widget_unrealize() and then gtk_widget_show() on @window

 *   themselves, if they still need this functionality.

 */
```

Note

If you are a library maintainer: it is important that the Deprecated tag
contains not just the version in which the symbol was deprecated, but also
an explanation of how to replace the symbol with non-deprecated API, if
a replacement is possible.

## Deprecation warnings ¶

Ultimatively, the GDK_DEPRECATED_IN_3_10 macro expands to a compiler
attribute — like __attribute__((__deprecated__)) , in GCC — which causes a
reasonably modern compiler to emit warnings when you compile your program; for
instance:

```
$ make testheaderbar
CC       testheaderbar.o
testheaderbar.c: In function ‘main’:
testheaderbar.c:192:3: warning: ‘gtk_window_reshow_with_initial_size’ is deprecated (declared at ../gtk/gtkwindow.h:419) [-Wdeprecated-declarations]
  gtk_window_reshow_with_initial_size (GTK_WINDOW (window));
  ^
CCLD     testheaderbar
```

Note that this is just a warning: the program was still successfully built.

Tip

When you decide to port your code away from deprecated symbols, you can ask
the compiler to turn deprecation warnings into errors; for instance, with
GCC, you can add -Werror=deprecated-declarations to your compiler flags.

## Ignoring all deprecation warnings ¶

If you want to ignore deprecation warnings you can either tell your compiler to
not emit warnings—for instance, with GCC, you can add -Wno-deprecated-declarations to your compiler flags. Or you can disable deprecation warnings for the
libraries that you’re using—for instance, by adding to your meson.build file:

```
add_project_arguments
([

    
'-DGLIB_DISABLE_DEPRECATION_WARNINGS'
,

    
'-DGTK_DISABLE_DEPRECATION_WARNINGS'
,

  
],

  
language
:
 
'c'
,

)
```

Every library that provides deprecation annotations should also provide a way to
disable them.

## Selectively ignoring deprecation warnings ¶

Sometimes it is impractical to avoid certain deprecated functions, even though
your code is otherwise deprecation-free. In this case, you probably don’t want
to disable all deprecation warnings, and instead just selectively mark the code
sections in which the deprecated functions are used:

```
G_GNUC_BEGIN_IGNORE_DEPRECATIONS

gtk_window_reshow_with_initial_size
 
(
window
);

G_GNUC_END_IGNORE_DEPRECATIONS
```

## Targeting specific library versions ¶

If your application was developed against a certain version of a library, say
GLib 2.56, and you want to ensure that it continues to work on systems with this
version of GLib, as well as newer versions, there are two things you need to
worry about.

Newer versions of GLib may deprecate API that you are using. There is no reason
for you to stop using it, since it was not deprecated in 2.56 (and the
replacements will probably not be available in 2.56), but you also want to build
your application with newer versions of GLib without warnings.

Even if your development system has a newer version of GLib, you want avoid
using GLib API that was introduced after 2.56, to keep your application working
on systems with GLib 2.56.

The deprecations and API additions in the GNOME stack are versioned, which lets
you achieve these goals by defining the range of versions of the APIs that your
application is expected to work with. This is done with a pair of macros, which
are typically defined in your meson.build file:

```
add_project_arguments
([

    
'-DGLIB_VERSION_MIN_REQUIRED=GLIB_VERSION_2_56'
,

    
'-DGLIB_VERSION_MAX_ALLOWED=GLIB_VERSION_2_56'
,

  
],

  
language
:
 
'c'
,

)
```

Each library has their own macros. For GLib, they are GLIB_VERSION_MIN_REQUIRED and GLIB_VERSION_MAX_ALLOWED . For GTK, they
are GDK_VERSION_MIN_REQUIRED and GDK_VERSION_MAX_ALLOWED .

The definition for these macros must encode the major and minor version in a
binary format. You can either use predefined values such as GLIB_VERSION_2_56 , GDK_VERSION_3_10 , or use the G_ENCODE_VERSION macro like this:

```
#define GLIB_VERSION_MIN_REQUIRED G_ENCODE_VERSION(2,56)
```

Important

Both the MIN_REQUIRED and MAX_ALLOWED macros must be defined before including the header for the corresponding library.

## Additional references ¶

- Compiling GLib applications
- Compiling GTK applications
