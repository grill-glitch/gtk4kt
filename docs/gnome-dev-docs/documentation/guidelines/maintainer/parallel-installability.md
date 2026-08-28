# Parallel Installability - GNOME Developer Documentation

# Parallel Installability ¶

All public libraries should be designed to be parallel installed to ease API
breaks later in the life of the library. If a library is used by multiple
projects, and wants to break API, either all of the projects must be ported to
the new API in parallel, or some of them will no longer be installable at the
same time as the others, due to depending on conflicting versions of this
library.

This is unmaintainable, and asking all the projects to port to a new API at the
same time is hard to organize and demoralizing, as most API breaks do not bring
large new features which would motivate porting.

The solution is to ensure that all libraries are parallel installable, allowing
the old and new versions of the API to be installed and compiled against at the
same time, without conflicts. Building in support for this kind of parallel
installation is much easier to do at the start of a project than it is to do
retroactively.

This eliminates the ‘chicken and egg’ problem of porting a collection of
applications from one version of a library to the next, and makes breaking API a
lot simpler for library maintainers, which can allow for more rapid iteration
and development of new features if they desire.

The alternative, and equally valid, solution is for the library to never break
API — the approach taken by libc.

## How parallel installability works ¶

The solution to the problem is essentially to rename the library, and in most
cases the nicest way to do so is to include the version number in the path of
every file it installs. This means multiple versions of the library can be
installed at the same time.

For example, say that library “Foo” traditionally installs these files:

- /usr/include/foo.h
- /usr/include/foo-utils.h
- /usr/lib/libfoo.so
- /usr/lib/pkgconfig/foo.pc
- /usr/share/doc/foo/foo-manual.txt
- /usr/bin/foo-utility

You might modify “Foo” version 4 to install these files instead:

- /usr/include/foo-4/foo/foo.h
- /usr/include/foo-4/foo/utils.h
- /usr/lib/libfoo-4.so
- /usr/lib/pkgconfig/foo-4.pc
- /usr/share/doc/foo-4/foo-manual.txt
- /usr/bin/foo-utility-4

“Foo” version 5 could then be installed alongside version 4 without breaking
existing projects:

- /usr/include/foo-5/foo/foo.h
- /usr/include/foo-5/foo/utils.h
- /usr/lib/libfoo-5.so
- /usr/lib/pkgconfig/foo-5.pc
- /usr/share/doc/foo-5/foo-manual.txt
- /usr/bin/foo-utility-5

This is easily supported using pkg-config: foo-4.pc would add /usr/include/foo-4 to the include path and libfoo-4.so to the list of
libraries to link; foo-5.pc would add /usr/include/foo-5 and libfoo-5.so .

## Version numbers ¶

The version number that goes in filenames is an ABI/API version. It should not
be the full version number of your package — just the part which signifies an
API break. If using the standard major.minor.micro scheme for project
versioning, the API version is typically the major version number.

Minor releases (typically where API is added but not changed or removed) and
micro releases (typically bug fixes) do not affect API backwards compatibility
so do not require moving all the files.

## C header files ¶

Header files should always be installed in a versioned subdirectory that
requires an -I flag to the C compiler. For example, if my header is foo.h , and applications do this:

```
#include
 
<foo/foo.h>
```

then I should install these files:

- Version 4: /usr/include/foo-4/foo/foo.h
- Version 5: /usr/include/foo-5/foo/foo.h

Applications should pass the flag -I/usr/include/foo-4 or -I/usr/include/foo-5 to the C compiler depending on the version of “Foo”
they intend to use. Again, this is facilitated by using pkg-config .

## Shared libraries ¶

Library object files should have a versioned name. For example:

- Version 4: /usr/lib/libfoo-4.so
- Version 5: /usr/lib/libfoo-5.so

This allows applications to get exactly the one they want at compile time, and
ensures that versions 4 and 5 have no files in common.

### Library sonames ¶

Library sonames only address the problem of runtime linking previously-compiled
applications. They don’t address the issue of compiling applications that
require a previous version, and they don’t address anything other than
libraries.

For this reason, sonames should be used, but in addition to versioned names for
libraries. The two solutions address different problems.

## pkg-config files ¶

pkg-config files should have a versioned name. For example:

- Version 4: /usr/lib/pkgconfig/foo-4.pc
- Version 5: /usr/lib/pkgconfig/foo-5.pc

Since each pkg-config file contains versioned information about the library
name and include paths, any project which depends on the library should be able
to switch from one version to another simply by changing their pkg-config check from foo-4 to foo-5 (and doing any necessary API porting).

## Configuration files ¶

From a user standpoint, the best approach to configuration files is to keep the
format both forward and backward compatible (both library versions understand
exactly the same configuration file syntax and semantics). Then the same
configuration file can be used for all versions of the library, and no
versioning is needed on the configuration file itself.

If you can’t do that, the configuration files should simply be renamed, and
users will have to configure each version of the library separately.

## Translations ¶

If you use gettext for translations, the message catalogs are installed under /usr/share/locale/lang/LC_MESSAGES/package , where package is a unique
identifier for your project. When changing the version, you will need to change package .

Typically, the package name is mapped to the GETTEXT_PACKAGE value and
defined in your build system. This value is also used in conjunction with the
localization API, such as bindtextdomain() , textdomain() , and dgettext() .

## D-Bus interfaces ¶

A D-Bus interface is another form of API, similar to a C API except that
resolution of the version is done at runtime rather than compile time.
Versioning D-Bus interfaces is otherwise no different to C APIs: version
numbers must be included in interface names, service names and object paths.

For example, for a service org.example.Foo exposing interfaces A and B on
objects Controller and Client, versions 4 and 5 of the D-Bus API would look like
this:

### Service names ¶

- Version 4: com.example.Foo4
- Version 5: com.example.Foo5

### Interface names ¶

- Version 4: com.example.Foo4.InterfaceA com.example.Foo4.InterfaceB
- Version 5: com.example.Foo5.InterfaceA com.example.Foo5.InterfaceB

### Object paths ¶

- Version 4: /com/example/Foo4/Controller /com/example/Foo4/Client
- Version 5: /com/example/Foo5/Controller /com/example/Foo5/Client

## Programs, daemons, and utilities ¶

Desktop applications generally do not need to be versioned, as they are not
depended on by any other modules. Daemons and utility programs, however,
interact with other parts of the system and hence need versioning.

Given a daemon and utility program:

- /usr/libexec/foo-daemon
- /usr/bin/foo-lookup-utility

these should be versioned as:

- /usr/libexec/foo-daemon-4
- /usr/bin/foo-lookup-utility-4

Tip

You may want to install a symbolic link from /usr/bin/foo-lookup-utility to the recommended versioned copy of the utility, to make it more convenient
for users to use.
