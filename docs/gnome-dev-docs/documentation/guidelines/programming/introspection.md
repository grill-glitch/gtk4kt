# Introspection - GNOME Developer Documentation

# Introspection ¶

GObject introspection (abbreviated G-I) is a system which extracts APIs from C
code and produces binary type libraries which can be used by non-C language
bindings, and other tools, to introspect or wrap the original C libraries. It
uses a system of annotations in documentation comments in the C code to expose
extra information about the APIs which is not machine readable from the code
itself.

## Using Introspection ¶

The first step for using introspection is to add it to the build system. This
should be done early in the life of a project, as introspectability affects API
design. When using the Meson build system, you can use the generate_gir() function provided by the gnome module :

```
lib
 
=
 
library
(
...
)

gir_files
 
=
 
gnome
.
generate_gir
(
lib
,

  
# The list of files to be parsed

  
sources
:
 
[
public_headers
,
 
sources
],

  
# The namespace of the library

  
namespace
:
 
ns
,

  
# The version of the API

  
nsversion
:
 
ns_version
,

  
# The pkg-config file exported by the library

  
export_packages
:
 
'your-library-1'
,

  
# The GIR files of the dependencies of the library

  
includes
:
 
gir_dependencies
,

  
# The public header of the library

  
header
:
 
'your-library.h'
,

  
extra_args
:
 
[
'--quiet'
,
 
'--warn-all'
],

  
install
:
 
true
,

)
```

This should result in a .gir and .typelib files being generated for the
project.

The GIR file is human readable, and can be inspected manually to see if the API
has been introspected correctly (although the GIR compilation process will print
error messages and warnings for any missing annotations or other problems). The
GIR file is typically used by bindings that generate code, or to generate the
API reference for your project. The typelib file is an efficient binary
representation of the GIR data, which can be opened at run time by dynamic
languages.

Important

APIs with introspectable=”0” will not be exposed to language bindings as they
are missing annotations or are otherwise not representable in the GIR file.

## Annotations ¶

The next step is to add annotations to the documentation comments for every
piece of public API. If a particular piece of API should not be exposed in the
GIR file, use the (skip) annotation. Documentation on the available
annotations is available on the G-I website .

If annotating the code for a program, a good approach is to split the bulk of
the code out into an internal, private convenience library. An internal API
reference manual can be built from its documentation comments. The library is
then not installed, but is linked in to the program which is itself installed.
This approach for generating internal API documentation is especially useful for
large projects where the internal code may be large and hard to navigate.

Annotations do not have to be added exhaustively: GIR has a set of default
annotations which it applies based on various conventions. For example, a const char* parameter or return value does not need an explicit (transfer none) annotation, because the const modifier implies this already.
Learning the defaults for annotations is a matter of practice.

## API Design ¶

In order to be introspectable without too many annotations, APIs must follow
certain conventions, such as the standard GObject naming conventions, and the
conventions for bindable APIs. This is necessary because of the flexibility of
C: code can be written to behave in any way imaginable, but higher level
languages don’t allow this kind of freedom. So in order for a C API to be
representable in a higher level language, it has to conform to the behaviors
supported by that language.

Additionally, the same C API will be accessed by multiple languages, each with
potentially different behaviors; adhering to the conventions and best practices
of GObject will ensure that the API remains consistent across different
languages.

A quick list of conventions to follow:

The introspection scanner only understands macros that evaluates to a
constant value; if you have complex functionality, always use a real
function.

Variadic arguments are not supported by every language, so you should
ensure you have a vector-based variant of any variadic arguments or va_list based function in your API

`g_object_new()`

You should not have constructor functions that set internal details
of an instance, as most dynamic languages will call g_object_new() directly. An exception are functions that return a singleton or
work as factory constructors, but those typically are classified as
static type functions.

Bindings for various programming languages expose properties and methods
in the same way, and will lead to collisions.

For a complete list of conventions to follow, please see the G-I website .

The g-ir-scanner tool will emit warnings whenever it encounters code it
cannot understand. You should make sure to pass --warn-all to see a full
list of all potential warnings.

Hint

You may want to set fatal_warnings: get_option('werror') in the generate_gir() arguments to ensure that any introspection warning
will stop the build in your continuous integration pipeline, just like
any compiler warning would; this is useful to make sure that all newly
added API is introspectable.
