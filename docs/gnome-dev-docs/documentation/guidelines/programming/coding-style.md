# C Coding Style - GNOME Developer Documentation

# C Coding Style ¶

This document presents the preferred coding style for C programs in GNOME.

While coding style is very much a matter of taste, in GNOME we favor a coding
style that promotes consistency, readability, and maintainability.

We present examples of good coding style as well as examples of bad style that
is not acceptable in GNOME. Please try to submit patches that conform to GNOME’s
coding style; this indicates that you have done your homework to respect the
project’s goal of long-term maintainability. Patches with GNOME’s coding style
will also be easier to review!

Important

This document is intended for C code. Unlike C, other programming languages
have their own official coding style recommendations; we encourage you to
follow them wherever applicable.

These guidelines are heavily inspired by the GTK coding style document; the
Linux kernel coding style; and the GNU coding standards. These are slight
variations of each other, with particular modifications for each project’s
particular needs and culture, and GNOME’s version is no different.

## The single most important rule ¶

The single most important rule when writing code is this: check the surrounding
code and try to imitate it .

As a maintainer it is dismaying to receive a patch that is obviously in a
different coding style to the surrounding code. This is disrespectful, like
someone tromping into a spotlessly-clean house with muddy shoes.

So, whatever this document recommends, if there is already written code and you
are contributing to it, keep its current style consistent even if it is not your
favorite style.

Most importantly, do not make your first contribution to a project a change in
the coding style to suit your taste. That is incredibly disrespectful.

## Line width ¶

Try to use lines of code between 80 and 120 characters long. This amount of text
is easy to fit in most monitors with a decent font size. Lines longer than that
become hard to read, and they mean that you should probably restructure your
code. If you have too many levels of indentation, it means that you should fix
your code anyway.

## Indentation ¶

In general there are two preferred indentation styles for code in GNOME:

- Linux Kernel style. Tabs with a length of 8 characters are used for the
indentation, with K&R brace placement:

```
for
 
(
i
 
=
 
0
;
 
i
 
<
 
num_elements
;
 
i
++
)
 
{

        
foo
[
i
]
 
=
 
foo
[
i
]
 
+
 
42
;

        
if
 
(
foo
[
i
]
 
<
 
35
)
 
{

                
printf
 
(
"Foo!"
);

                
foo
[
i
]
--
;

        
}
 
else
 
{

                
printf
 
(
"Bar!"
);

                
foo
[
i
]
++
;

        
}

}
```

- GNU style. Each new level is indented by 2 spaces, braces go on a line by
themselves, and they are indented as well:

```
for
 
(
i
 
=
 
0
;
 
i
 
<
 
num_elements
;
 
i
++
)

  
{

    
foo
[
i
]
 
=
 
foo
[
i
]
 
+
 
42
;

    
if
 
(
foo
[
i
]
 
<
 
35
)

      
{

        
printf
 
(
"Foo!"
);

        
foo
[
i
]
--
;

      
}

    
else

      
{

        
printf
 
(
"Bar!"
);

        
foo
[
i
]
++
;

      
}

  
}
```

Both styles have their pros and cons. The most important things is to be
consistent with the surrounding code . For example, the GTK library, which is
GNOME’s widget toolkit, is written with the GNU style. Nautilus, GNOME’s file
manager, is written in Linux kernel style. Both styles are perfectly readable
and consistent when you get used to them.

Your first feeling when having to study or work on a piece of code that doesn’t
have your preferred indentation style may be, how shall we put it,
gut-wrenching. You should resist your inclination to reindent everything, or to
use an inconsistent style for your patch. Remember the first rule: be consistent
and respectful of that code’s customs, and your patches will have a much higher
chance of being accepted without a lot of arguing about the right indentation
style.

### Tab characters ¶

Do not ever change the size of tabs in your editor ; leave them as 8 spaces.
Changing the size of tabs means that code that you didn’t write yourself will be
perpetually misaligned.

Instead, set the indentation size as appropriate for the code you are editing.
When writing in something other than Linux kernel style, you may even want to
tell your editor to automatically convert all tabs to 8 spaces, so that there is
no ambiguity about the intended amount of space.

## Braces ¶

Curly braces should not be used for single statement blocks:

```
/* valid */

if
 
(
condition
)

        
single_statement
 
();

else

        
another_single_statement
 
(
arg1
);
```

The “no block for single statements” rule has only four exceptions:

- In GNU style, if either side of an if … else statement has braces, both
sides should, to match up indentation:

```
/* valid */

if
 
(
condition
)

  
{

    
foo
 
();

    
bar
 
();

  
}

else

  
{

    
baz
 
();

  
}
```

- If the single statement covers multiple lines, e.g. for functions with many
arguments, and it is followed by else or else if :

```
/* valid Linux kernel style */

if
 
(
condition
)
 
{

        
a_single_statement_with_many_arguments
 
(
some_lengthy_argument
,

                                                
another_lengthy_argument
,

                                                
and_another_one
,

                                                
plus_one
);

}
 
else

        
another_single_statement
 
(
arg1
,
 
arg2
);

/* valid GNU style */

if
 
(
condition
)

  
{

    
a_single_statement_with_many_arguments
 
(
some_lengthy_argument
,

                                            
another_lengthy_argument
,

                                            
and_another_one
,

                                            
plus_one
);

  
}

else

  
{

    
another_single_statement
 
(
arg1
,
 
arg2
);

  
}
```

- If the condition is composed of many lines:

```
/* valid Linux kernel style */

if
 
(
condition1
 
||

    
(
condition2
 
&&
 
condition3
)
 
||

    
condition4
 
||

    
(
condition5
 
&&
 
(
condition6
 
||
 
condition7
)))
 
{

        
a_single_statement
 
();

}

/* valid GNU style */

if
 
(
condition1
 
||

    
(
condition2
 
&&
 
condition3
)
 
||

    
condition4
 
||

    
(
condition5
 
&&
 
(
condition6
 
||
 
condition7
)))

  
{

    
a_single_statement
 
();

  
}
```

Note

Such long conditions are usually hard to understand. A good practice is to
set the condition to a boolean variable, with a good name for that variable.
Another way is to move the long condition to a function.

- Nested if , in which case the block should be placed on the outermost if :

```
/* valid Linux kernel style */

if
 
(
condition
)
 
{

        
if
 
(
another_condition
)

                
single_statement
 
();

        
else

                
another_single_statement
 
();

}

/* valid GNU style */

if
 
(
condition
)

  
{

    
if
 
(
another_condition
)

      
single_statement
 
();

    
else

      
another_single_statement
 
();

  
}
```

In general, new blocks should be placed on a new indentation level, like this:

```
int
 
retval
 
=
 
0
;

statement_1
 
();

statement_2
 
();

{

        
int
 
var1
 
=
 
42
;

        
gboolean
 
res
 
=
 
FALSE
;

        
res
 
=
 
statement_3
 
(
var1
);

        
retval
 
=
 
res
 
?
 
-1
 
:
 
1
;

}
```

While curly braces for function definitions should rest on a new line they
should not add an indentation level:

```
/* valid Linux kernel style*/

static
 
void

my_function
 
(
int
 
argument
)

{

        
do_my_things
 
();

}

/* valid GNU style*/

static
 
void

my_function
 
(
int
 
argument
)

{

  
do_my_things
 
();

}
```

## Conditions ¶

Do not check boolean values for equality. By using implicit comparisons, the
resulting code can be read more like conversational English. Another rationale
is that a ‘true’ value may not be necessarily equal to whatever the TRUE macro uses. For example:

```
if
 
(
found
)

  
do_foo
 
();

if
 
(
!
found
)

  
do_bar
 
();
```

The C language uses the value 0 for many purposes. As a numeric value, the
end of a string, a null pointer and the FALSE boolean. To make the code
clearer, you should write code that highlights the specific way 0 is used.
So when reading a comparison, it is possible to know the variable type. For
boolean variables, an implicit comparison is appropriate because it’s already a
logical expression. Other variable types are not logical expressions by
themselves, so an explicit comparison is better:

```
if
 
(
some_pointer
 
==
 
NULL
)

        
do_blah
 
();

if
 
(
number
 
==
 
0
)

        
do_foo
 
();

if
 
(
str
 
!=
 
NULL
 
&&
 
*
str
 
!=
 
'\0'
)

        
do_bar
 
();
```

## Functions ¶

Functions should be declared by placing the returned value on a separate line from the function name:

```
void

my_function
 
(
void
)

{

        
// ...

}
```

The argument list must be broken into a new line for each argument, with the
argument names right aligned, taking into account pointers:

```
void

my_function
 
(
some_type_t
      
type
,

             
another_type_t
  
*
a_pointer
,

             
double_ptr_t
   
**
double_pointer
,

             
final_type_t
     
another_type
)

{

        
// ...

}
```

Tip

If you use Emacs, you can use M-x align to do this kind of alignment
automatically. Just put the point and mark around the function’s prototype,
and invoke that command.

The alignment also holds when invoking a function without breaking the line
length limit:

```
align_function_arguments
 
(
first_argument
,

                          
second_argument
,

                          
third_argument
);
```

## Whitespace ¶

Always put a space before an opening parenthesis but never after:

```
if
 
(
condition
)

        
do_my_things
 
();

switch
 
(
condition
)
 
{

}
```

When declaring a structure type use newlines to separate logical sections of the
structure:

```
struct
 
_GtkWrapBoxPrivate

{

  
GtkOrientation
 
orientation
;

  
GtkWrapAllocationMode
 
mode
;

  
GtkWrapBoxSpreading
 
horizontal_spreading
;

  
GtkWrapBoxSpreading
 
vertical_spreading
;

  
guint16
 
spacing
[
2
];

  
guint16
 
minimum_line_children
;

  
guint16
 
natural_line_children
;

  
GList
 
*
children
;

};
```

Do not eliminate whitespace and newlines just because something would fit on
a single line:

```
/* invalid */

if
 
(
condition
)
 
foo
 
();
 
else
 
bar
 
();
```

Do eliminate trailing whitespace on any line, preferably as a separate patch or
commit. Never use empty lines at the beginning or at the end of a file.

## The switch statement ¶

A switch should open a block on a new indentation level, and each case
should start on the same indentation level as the curly braces, with the case
block on a new indentation level:

```
/* valid Linux kernel style */

switch
 
(
condition
)
 
{

case
 
FOO
:

        
do_foo
 
();

        
break
;

case
 
BAR
:

        
do_bar
 
();

        
break
;

}

/* valid GNU style */

switch
 
(
condition
)

  
{

  
case
 
FOO
:

    
do_foo
 
();

    
break
;

  
case
 
BAR
:

    
do_bar
 
();

    
break
;

  
default
:

    
do_default
 
();

  
}
```

Tip

It is preferable, though not mandatory, to separate the various cases with a
newline.

Tip

The break statement for the default case is not mandatory.

If switching over an enumerated type, a case statement must exist for every
member of the enumerated type. For members you do not want to handle, alias
their case statements to default :

```
switch
 
(
enumerated_condition
)
 
{

case
 
HANDLED_1
:

        
do_foo
 
();

        
break
;

case
 
HANDLED_2
:

        
do_bar
 
();

        
break
;

case
 
IGNORED_1
:

case
 
IGNORED_2
:

default
:

        
do_default
 
();

}
```

Tip

If most members of the enumerated type should not be handled, consider using an if … else if statement instead of a switch .

If a case block needs to declare new variables, the same rules as the inner
blocks apply (see above); the break statement should be placed outside of
the inner block:

```
switch
 
(
condition
)

  
{

  
case
 
FOO
:

    
{

      
int
 
foo
;

      
foo
 
=
 
do_foo
 
();

    
}

    
break
;

  
// ...

  
}
```

## Header files ¶

The only major rule for headers is that the function definitions should be
vertically aligned in three columns:

```
return_type
          
function_name
           
(
type
   
argument
,

                                              
type
   
argument
,

                                              
type
   
argument
);
```

The maximum width of each column is given by the longest element in the column:

```
void
        
gtk_type_set_property
 
(
GtkType
     
*
type
,

                                   
const
 
char
  
*
value
,

                                   
GError
     
**
error
);

const
 
char
 
*
gtk_type_get_property
 
(
GtkType
     
*
type
);
```

It is also possible to align the columns to the next tab, to avoid having to
reformat headers every time you add a new function:

```
void
          
gtk_type_set_prop
           
(
GtkType
 
*
type
,

                                           
float
    
value
);

float
         
gtk_type_get_prop
           
(
GtkType
 
*
type
);

int
           
gtk_type_update_foobar
      
(
GtkType
 
*
type
);
```

If you are creating a public library, try to export a single public header file
that in turn includes all the smaller header files into it. This is so that
public headers are never included directly; rather a single include is used in
applications. For example, GTK uses the following in its header files that
should not be included directly by applications:

```
// The __GTK_H_INSIDE__ symbol is defined in the gtk.h header

// The GTK_COMPILATION symbol is defined only when compiling

// GTK itself

#if !defined (__GTK_H_INSIDE__) && !defined (GTK_COMPILATION)

#error "Only <gtk/gtk.h> can be included directly."

#endif
```

For libraries, all headers should have inclusion guards (for internal usage) and
C++ guards. These provide the extern “C” magic that C++ requires to include
plain C headers:

```
#pragma once

#include
 
<gtk/gtk.h>

G_BEGIN_DECLS

// ...

G_END_DECLS
```

Tip

Instead of the once pragma you can use an explicit symbol-based
inclusion guard: #ifndef FILE_H #define FILE_H ... #endif .

## GObject classes ¶

GObject class definitions and implementations require some additional coding
style notices, and should always be correctly namespaced.

Type declarations should be placed at the beginning of the file:

```
typedef
 
struct
 
_GtkBoxedStruct
       
GtkBoxedStruct
;

typedef
 
struct
 
_GtkMoreBoxedStruct
   
GtkMoreBoxedStruct
;
```

This includes enumeration types:

```
typedef
 
enum

{

  
GTK_SIZE_REQUEST_WIDTH_FOR_HEIGHT
,

  
GTK_SIZE_REQUEST_HEIGHT_FOR_WIDTH

}
 
GtkSizeRequestMode
;
```

And callback types:

```
typedef
 
void
 
(
*
 
GtkCallback
)
 
(
GtkWidget
 
*
widget
,

                              
gpointer
   
user_data
);
```

Instance structures should be declared using the G_DECLARE_FINAL_TYPE() or G_DECLARE_DERIVABLE_TYPE() macros:

```
#define GTK_TYPE_FOO (gtk_foo_get_type ())

G_DECLARE_FINAL_TYPE
 
(
GtkFoo
,
 
gtk_foo
,
 
GTK
,
 
FOO
,
 
GtkWidget
)
```

For final types, private data can be stored in the object struct, which should
be defined in the C file:

```
struct
 
_GtkFoo

{

  
GObject
   
parent_instance
;

  
guint
     
private_data
;

  
gpointer
  
more_private_data
;

};
```

For derivable types, private data must be stored in a private struct in the C
file, configured using G_DEFINE_TYPE_WITH_PRIVATE() and accessed using the
generated _get_instance_private() function:

```
#define GTK_TYPE_FOO gtk_foo_get_type()

G_DECLARE_DERIVABLE_TYPE
 
(
GtkFoo
,
 
gtk_foo
,
 
GTK
,
 
FOO
,
 
GtkWidget
)

struct
 
_GtkFooClass

{

  
GtkWidgetClass
 
parent_class
;

  
void
 
(
*
 
handle_frob
)
  
(
GtkFrobber
 
*
frobber
,

                         
guint
       
n_frobs
);

  
// Padding, for ABI compatible expansion of the class

  
gpointer
 
padding
[
12
];

};
```

Always use the G_DEFINE_TYPE() , G_DEFINE_TYPE_WITH_PRIVATE() , and G_DEFINE_TYPE_WITH_CODE() macros, or their abstract variants G_DEFINE_ABSTRACT_TYPE() , G_DEFINE_ABSTRACT_TYPE_WITH_PRIVATE() , and G_DEFINE_ABSTRACT_TYPE_WITH_CODE() ; also, use the similar macros for
defining interfaces and boxed types.

Interfaces should be declared using the G_DECLARE_INTERFACE() macro:

```
#define GTK_TYPE_FOOABLE gtk_fooable_get_type()

G_DECLARE_INTERFACE
 
(
GtkFooable
,
 
gtk_fooable
,
 
GTK
,
 
FOOABLE
,
 
GObject
)
```

## Memory allocation ¶

When dynamically allocating data on the heap use g_new() .

Public structure types should always be returned after being zero-ed, either
explicitly for each member, or by using g_new0() .

## Macros ¶

Try to avoid private macros unless strictly necessary. Remember to #undef them at the end of a block or a series of functions needing them.

Inline functions are usually preferable to private macros.

Public macros should not be used unless they evaluate to a constant.

## Public API ¶

Avoid exporting variables as public API, since this is cumbersome on some
platforms. It is always preferable to add getters and setters instead. Also,
beware global variables in general.

To avoid exposing private API in the shared library, it is recommended to
default to a hidden symbol visibility, and explicitly annotate public
symbols in the header file.

Non-exported functions that are only needed in one source file should be
declared static to that file.
