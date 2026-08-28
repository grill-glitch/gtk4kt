# Integrating with GNOME - GNOME Developer Documentation

# Integrating with GNOME ¶

GNOME is a project to build a complete desktop and development platform based
entirely on free software. Many companies, governments, schools, institutions,
and individuals have deployed the GNOME desktop on their systems. If you are a
developer of third-party software (“Independent Software Vendor” or ISV, or
“Independent Software Developer” (ISD) if you don’t do it commercially), you
may want to ensure that your existing software runs properly under GNOME. This
guide explains how to integrate existing software with GNOME, without actually
rewriting that software to explicitly use the GNOME platform libraries and
development tools.

This guide will be useful in the following situations:

- You are a software developer or distributor who has an application that is not
explicitly designed to work with GNOME, but you want to ensure that it runs
comfortably within a GNOME desktop.
- You are a system administrator for an institution that has deployed GNOME
desktops to its users. You also have legacy or in-house applications, and you
want your users of GNOME to be able to access those applications comfortably.
- You are writing a GNOME application proper and you need a checklist of basic
things to do to ensure that your application integrates well with the rest of
the GNOME desktop.

In general, this guide is about integrating existing software into a GNOME
desktop. On the other hand, if you are considering writing new software, we
encourage you to develop it completely with GNOME as your target platform;
please refer to the GNOME Developer’s Site for more information.

One of the main concerns of GNOME is the user experience. Users should have a
comfortable computing environment: this means having a complete desktop and a
set of applications which operate together in a consistent way. With relatively
little work, applications which are not written explicitly with GNOME in mind
can be made to run comfortably within a GNOME desktop.

## Basic integration ¶

### Desktop files ¶

To run applications from GNOME, users click on icons on their desktops or they
select the applications which they want to run from the applications grid.
Therefore, the first step in integrating an existing program with GNOME is to
register it with the set of applications that users can run.

The applications grid is automatically constructed from the list of registered
applications.

In GNOME and other freedesktop.org-compliant desktops, an application gets
registered into the desktop’s menus through a desktop entry, which is a text
file with .desktop extension. This desktop file contains a listing of the
configurations for your application. The desktop takes the information in this
file and uses it to:

- associate the name, description, and icon of the application
- associate each window created by the application with the same name and icon
- recognize the MIME types it supports for opening files

To register your application, create a desktop file using the same
application identifier you chose for your application and the .desktop file
extension.

Note

For more information about the application identifier, please see the Application ID tutorial.

A typical desktop file will have the following structure:

```
[
Desktop
 
Entry
]

Name
=
Your
 
Application

Comment
=
An
 
amazing
 
application

Exec
=
your
-
application

Icon
=
com
.
example
.
YourApplication

Keywords
=
various
;
keywords
;
describing
;
your
;
application
;

StartupNotify
=
true

Terminal
=
false
```

Some keys in your desktop files can be localized in different languages, like
the Name , Comment , GenericName , and Keywords keys. By localizing
your desktop file you allow users from different parts of the world to find your
application more easily.

Note

For more information about the format of the desktop files, please visit
the Freedesktop.org Desktop Entry specification .

Important

You should ensure that your desktop file is valid as part of your project’s
test suite. You can use the desktop-file-validate utility provided by the desktop-file-utils project

Your desktop file should be installed under one of the applications directories of the XDG_DATA_DIRS or XDG_DATA_HOME environment variables,
depending on the installation prefix. For system installations, the former
typically means /usr/share/applications ; for user installations, the latter
is typically $HOME/.local/share/applications .

Note

For more information about the XDG directories, please visit the Freedesktop.org Base Directories specification

### Icons ¶

Application icons should be installed following the Freedesktop.org Icon Theme
specification .
For GNOME applications, you should provide

- a full color scalable icon using the SVG format

OR

- a full color, 256×256 pixels raster icon using the PNG format

You should also, optionally, provide a symbolic icon using the SVG format.

You should install your icon under the hicolor icon theme namespace, using
the application identifier as the base name for the icon; for instance:

- /usr/share/icons/hicolor/256x256/apps/com.example.YourApplication.png
- /usr/share/icons/hicolor/scalable/apps/com.example.YourApplication.svg
- /usr/share/icons/hicolor/symbolic/apps/com.example.YourApplication-symbolic.svg

## Advanced integration ¶

### D-Bus activation ¶

Instead of the typical UNIX-style fork() / exec() approach to process
creation, launching an application in GNOME is preferably done by sending a
D-Bus message to the well-known name of that application, causing a D-Bus
activation. In the case that the application is already running, it can respond
to the message by opening a new window or raising its existing window, instead
of launching a new instance.

Starting processes with D-Bus activation ensures that each application gets
started in its own pristine environment, as a direct descendent of the
session, not in the environment of whatever its parent happened to be. This is
important for ensuring the app ends up in the correct cgroup , for example.

Another reason is that being D-Bus activatable is a prerequisite for using
persistent notifications.

In order for D-Bus to know how to activate your service, you need to install a
D-Bus service file under /usr/share/dbus-1/services :

```
[
D
-
BUS
 
Service
]

Name
=
com
.
example
.
YourApplication

Exec
=/
usr
/
bin
/
your
-
app
 
--
gapplication
-
service
```

Note

The --gapplication-service command line argument on the Exec line is
used by GApplication to start the application instance as a service.

You will also need to add the following key to your application’s desktop file:

```
DBusActivatable
=
true
```

If DBusActivatable is true and the desktop file name looks like a valid
application ID, then the Exec line will be ignored and your application will
be started by way of D-Bus activation instead (using the name of the desktop
file minus the .desktop extension as the application ID).

### MIME types ¶

If your application can open specific MIME types, you need to let the desktop
know in the desktop file. For example, if your application can accept PNG files,
add the following line into your desktop file:

```
MimeType
=
image
/
png
;
```

Note

Additional MIME types can be added by separating the different types with
semicolons.

You will also need to add %F as argument to your Exec line if your
application supports opening multiple files at once, or %f if it only
supports opening one file at a time, which will open multiple instances of
your application when opening a selection of files that your application
supports.

The system already knows of a large number of MIME types. However, if you are
creating one of your own, you need to register your MIME type into the MIME
database, by creating an XML file in the /usr/share/mime/packages directory:

```
<?xml version="1.0" encoding="UTF-8"?>

<mime-info
 
xmlns=
"http://www.freedesktop.org/standards/shared-mime-info"
>

   
<mime-type
 
type=
"application/x-example"
>

     
<comment>
Example
 
file
 
type
 
</comment>

     
<magic
 
priority=
"50"
>

       
<match
 
value=
"search-string"
 
type=
"string"
 
offset=
"10:140"
/>

     
</magic>

     
<glob
 
pattern=
"*.newextension"
/>

   
</mime-type>

</mime-info>
```

Important

To avoid collisions with files added by other applications, you should use
your application’s ID as the basename for the file, e.g. com.example.YourApplication.xml

In this example, replace the example MIME type with the name of your MIME type.
The magic rule searches the contents of the file for the given string for
identification. The glob rule uses the suffix of file names for
identification.

Note

Because the magic rule forces the computer to open the files to search
for the string, the glob rule is preferable.

Once your new MIME type is adequately described in the file, run the following
in a shell:

```
update
-
mime
-
database
 
/
usr
/
share
/
mime
```

Tip

For more information on choosing a good MIME extension and to register your
MIME type, go to the IANA website .

### URI schemes handling ¶

If your application can open specific URI schemes, you need to let the desktop
know in the desktop file. For example, if your application can accept mailto: URIs,
you need to add the corresponding x-scheme-handler/mailto to the MIME types in the
desktop file, as done in the previous section. You can use any URI scheme you want,
not just the common ones like mailto/http/https/ftp, for example the gemini:// URI
scheme.

You also need to use %u or %U for respectively one or several URIs, the same way it
was done with %f and %F in the previous section. If your app handles MIME types in
addition to URIs, you only need to use the %u or %U version, no need to use %f and %F.
