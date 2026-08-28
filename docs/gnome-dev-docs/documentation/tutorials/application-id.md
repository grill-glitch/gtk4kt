# Application ID - GNOME Developer Documentation

# Application ID ¶

GNOME technologies make extensive use of “reverse DNS” style identifiers for
applications. One of the first things that you will need to do when developing
an application is to choose an appropriate identifier.

Note

An example of such an identifier is “org.gnome.TextEditor”

Application identifiers are widely used and changing them later can cause
problems. For this reason, you should choose your identifier carefully, with an
eye to the future of your application.

## Things that use application IDs ¶

Application IDs are used in the following places:

- by GtkApplication (or GApplication ) as a method of identifying your
application to the system, for ensuring that only one instance of your
application is running at a given time, and as a way of passing messages to your
application (such as an instruction to open a file)
- by D-Bus, to name your application on the message bus. This is the primary
means of communicating between applications and is visible via the gdbus
commandline tool or the d-feet graphical D-Bus browser.
- as the name of the .desktop file for your application. This file is how
you describe your application to the system (so that it can be displayed in and
launched by GNOME).
- as the base name of any GSettings schemas that your application may install.
These names are visible via the gsettings commandline tool or the dconf-editor
graphical editor.
- as a way for the system to remember state information about your applications
(for example, which notifications it has requested to be shown to the user) and
as a way for it to control settings about your application (for example, if its
notifications have been blocked by the user)
- as a way for the system to use your application to extend itself (for example,
by way of search providers)
- as the bundle name for application bundles, like Flatpak

## Rules for application IDs ¶

The precise rules for what makes a valid application ID are as follows:

- the application ID must be composed of two or more elements separated by a
period (‘.’) character
- each element must contain one or more of the alphanumeric characters (A-Z,
a-z, 0-9) plus underscore (‘_’) and hyphen (‘-’) and must not start with a
digit.
- the empty string is not a valid element (ie: your application ID may not start
or end with a period and it is not valid to have two periods in a row)
- the entire ID must be less than 255 characters in length

Warning

While hyphens are allowed, you should not use them in your application ID, as
they are not supported by all components that might use the ID, like D-Bus.
If you are using a real DNS name as the basis for your application ID, and
the name contains an hyphen, you should replace it with an underscore. For
instance: from 7-zip.org to org._7_zip .

See also

For more information on what constitutes a valid application id, you should
read the documentation for g_application_id_is_valid()

## Guidelines for choosing an application ID ¶

The most important thing in choosing a name is that it must be globally unique.
Bad things will happen if two unrelated applications try to use the same
application ID.

For this reason, it is very strongly recommended to choose a name based on the
global public DNS system. For example, if you owned the domain yorba.org, you
would probably want to name your application “MyApp” like “org.yorba.MyApp”.

If your application is a member of or strongly affiliated with a given Free
Software project then it is appropriate to use the public DNS name of that
project, provided you follow their guidelines and policies. “Strongly
affiliated” in this case generally means “using the version control,
bugtracking, etc. of the project in question”.

Important

In the case of the GNOME project, applications which are hosted in the GNOME
group on gitlab.gnome.org should use names like “org.gnome.MyApp”.

Sometimes it is not possible to choose an ID based on a domain that you own. In
this case, it is usually possible to fall back to something reasonable, such as
an account name on a public provider. Names such as “com.github.username.MyApp”
or “com.gmail.myemailaddr.MyApp” are examples of those.

The Flatpak documentation also provides guidelines.

## Application ID for Flatpak development ¶

For development builds, it is recommended to use a different application ID
so that it can be installed and used alongside the stable build.

The recommended way is to suffix your application ID with .Devel ,
for example org.gnome.TextEditor.Devel .
