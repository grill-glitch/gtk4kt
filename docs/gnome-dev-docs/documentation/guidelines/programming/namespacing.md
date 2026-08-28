# Namespacing - GNOME Developer Documentation

# Namespacing ¶

Consistent and complete namespacing of symbols (functions and types) and files
is important for two key reasons:

- Establishing a convention which means developers have to learn fewer symbol
names to use the library — they can guess them reliably instead.
- Ensuring symbols from two projects do not conflict if included in the same
file.

The second point is important — imagine what would happen if every project
exported a function called create_object() . The headers defining them could not
be included in the same file, and even if that were overcome, the programmer
would not know which project each function comes from. Namespacing eliminates
these problems by using a unique, consistent prefix for every symbol and
filename in a project, grouping symbols into their projects and separating them
from others.

The conventions below should be used for namespacing all symbols. They are used
in all GLib-based projects, so should be familiar to a lot of developers:

- Functions should use lower_case_with_underscores (also known as snake
case ).
- Structures, types and objects should use CamelCaseWithoutUnderscores .
- Macros and constants should use UPPER_CASE_WITH_UNDERSCORES .
- All symbols should be prefixed with a short (2–4 characters) version of the
namespace. This is shortened purely for ease of typing, but should still be
unique.
- All methods of a class should also be prefixed with the class name.

Additionally, public headers should be included from a subdirectory, effectively
namespacing the header files. For example, instead of #include <abc.h> , a
project should allow its users to use #include <namespace/abc.h> .

Some projects namespace their headers within this subdirectory — for example, #include <namespace/ns-abc.h> instead of #include <namespace/abc.h> .
This is redundant, but harmless.

For example, for a project called ‘Walbottle’, the short namespace ‘Wbl’ would
be chosen. If it has a ‘schema’ class and a ‘writer’ class, it would install
headers:

- $(includedir)/walbottle-$API_MAJOR/walbottle/schema.h
- $(includedir)/walbottle-$API_MAJOR/walbottle/writer.h

(The use of $API_MAJOR above is for parallel installability.)

For the schema class, the following symbols would be exported (amongst others),
following GObject conventions:

- WblSchema structure
- WblSchemaClass structure
- WBL_TYPE_SCHEMA macro
- WBL_IS_SCHEMA macro
- wbl_schema_get_type function
- wbl_schema_new function
- wbl_schema_load_from_data function
