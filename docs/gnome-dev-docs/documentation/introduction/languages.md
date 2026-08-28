# Programming Languages - GNOME Developer Documentation

# Programming Languages ¶

GNOME applications can be written in a range of languages, including C++, Javascript, Python, Rust and Vala. This page provides an overview of the languages that are available, with links to the relevant developer documentation.

## Overview ¶

GNOME platform libraries are primarily written in C, and provide a machine-readable description of their API and ABI through GObject-Introspection . This means that C is the “default” platform language, and that the upstream documentation for each library typically references C, but it is possible to use multiple programming languages to write applications for GNOME.

GObject-Introspection allows GNOME to easily provide support for a range of high level languages. In each case, this support is provided by a separate project, which provides its own documentation and support.

Language support typically needs to be installed as part of your development environment. Packages are available for most Linux distributions. In some cases, Flatpak runtime extensions are also available.

## Available languages ¶

Here are the most commonly used programming languages available for writing GNOME applications.

Tip

The GNOME project recommends using the C programming language for libraries, as it allows the maximum support across multiple programming languages. Applications, on the other hand, can be written in C or in any programming languages that provides access to the GNOME platform libraries through language bindings.

| Language | Project | Documentation | Notes |
|---|---|---|---|
| C++ | gtkmm | Documentation overview | Applications that use gtkmm include Gnote , GParted , and Inkscape . |
| Java | Java-GI | API references | Works with OpenJDK 25 or later. |
| JavaScript | GJS | API references | Built on Mozilla’s SpiderMonkey, featuring ES6 (ECMAScript 2015). Applications which use GJS include Polari , Maps and Sound Recorder . |
| Perl | Glib::Object::Introspection | Documentation overview |  |
| Python | PyGObject | API references | Works with Python 3 and PyPy3. Applications which use PyGObject include Music , Lollypop and Pitivi . |
| Rust | gtk-rs | Book | Applications which use gtk-rs include Authenticator , Shortwave and Video Trimmer . |
| Vala | Vala | API References | Vala is a programming language which wraps GNOME libraries and outputs C code. Applications which use Vala include Calculator , Boxes , Clocks and Gitg . |
| C# | gir.core | Get started | Applications which us Gir.core include Denaro , Parabolic , and Tagger . |

Note

For more information about applications written in these languages, go to the Welcome to GNOME website .

See the Libraries overview for a list of libraries in the GNOME platform, and their documentation.
