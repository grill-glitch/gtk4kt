# Valgrind - GNOME Developer Documentation

# Valgrind ¶

Valgrind is a programmer tool that allows to track
memory related errors in C and C++ programs.

Hint

You can read a full introduction on the Valgrind website.

This page includes some tips on how to proficiently use valgrind on GTK/GNOME
programs. Feel free to add your own tricks or expand the page with more detailed
explanations.

## Memcheck ¶

Memcheck is the main valgrind tool, it allows to detect memory leaks and other
memory management errors. To run a gnome program under memcheck run:

```
valgrind
 \
  
--
leak
-
check
=
full
 \
  
--
num
-
callers
=
20
 \
  
--
log
-
file
=
vgdump
.
txt
\
  
your
-
program
```

If the program you are debugging uses dynamically loaded modules with GModule (for instance it has a GModule based plugin system) you should
use G_DEBUG=resident-modules to make sure that the modules do not get
unloaded and valgrind can retrieve the function names when writing its log:

```
G_DEBUG
=
resident
-
modules
 
valgrind
 \
  
--
leak
-
check
=
full
 \
  
--
num
-
callers
=
20
 \
  
--
log
-
file
=
vgdump
.
txt
 \
  
your
-
program
```

Note

If you are still using Autotools as your build system, your binary is really
a libtool-generated temporary wrapper, the above command line will run
valgrind on your shell which is probably not what you want. You will need to
use libtool --mode=execute valgrind ... instead.

After running the program you can inspect the log in the vgdump file. The log
contains a list of memory related issues and in particular memory leaks. Memory
leaks are marked in three ways: definitely lost, possibly lost and still
reachable. For a start, concentrate on the definitely lost ones, which are bits
of memory leaked for sure—you can filter for them by passing --show-leak-kinds=definite . For each leak valgrind provides a backtrace which
lets you pinpoint exactly where the leaks happens, in particular if your program
was compiled with debugging symbols, valgrind will tell you the exact line and
file of the leak.

### Suppression files ¶

GLib and other libraries often make one-off allocations that are meant to exist
until the end of the lifetime of your application. These are not leaks, but
Valgrind will mark them as such. In order to avoid cluttering your reports, you
can use “suppression files”, to tell Valgrind to eliminate known one-off
allocations. GLib and GTK provide suppression files that you can use.

Assuming your copy of GLib and GTK are installed under /usr , the Valgrind
suppression files are located here:

- /usr/share/glib-2.0/valgrind/glib.supp
- /usr/share/gtk-4.0/valgrind/gtk.supp

You can use them with the --suppressions argument for the memcheck tool, e.g.:

```
valgrind
 \
  
--
suppressions
=/
usr
/
share
/
glib
-
2.0
/
valgrind
/
glib
.
supp
 \
  
--
suppressions
=/
usr
/
share
/
gtk
-
4.0
/
valgrind
/
gtk
.
supp
 \
  
your
-
gtk
-
app
```

Hint

You can use multiple --suppressions arguments, one for each suppression
file.

If your project has its own one-off allocations you wish to suppress, you can
write your own suppression file by following the instructions in the Valgrind
documentation .
