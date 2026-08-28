# Getting Started - GNOME Developer Documentation

# Getting Started ¶

In order to start writing an application for the GNOME desktop environment, you
should follow these steps:

![New project creation dialog in GNOME Builder](https://developer.gnome.org/documentation/_images/getting-started-new-project.png)![New project creation dialog in GNOME Builder](https://developer.gnome.org/documentation/_images/getting-started-new-project-dark.png)
- download and install the latest version of GNOME Builder
- in the Welcome screen, select Create new project…
- configure the project options write “text-viewer” as the project’s name write “com.example.TextViewer” as the project’s application id . select GPL-3.0-or-later as the licensing terms for your project
- select the GNOME Application template
![Initial project contents in GNOME Builder](https://developer.gnome.org/documentation/_images/getting-started-project-files.png)![Initial project contents in GNOME Builder](https://developer.gnome.org/documentation/_images/getting-started-project-files-dark.png)

Once Builder finishes creating your application’s project, you will find the
following files:

`com.example.TextViewer.json`

This is the Flatpak manifest for your application.
You can use the manifest to define your project’s dependencies. The default
manifest depends on the latest stable version of the GNOME platform. You can also
include additional dependencies not provided by the GNOME runtime.

`meson.build`

This is the main Meson build file, which
defines how and what to build in your application.

`src`

This is the directory with the sources of your application, as well as
the UI definition files for its widgets.

`src/text-viewer.gresource.xml`

The GResource manifest for assets that will be built into the project using glib-compile-resources .

`po/POTFILES`

The list of files containing translatable ,
user-visible strings.

`data/com.example.TextViewer.gschema.xml`

The schema file for the settings of the application.

`data/com.example.TextViewer.desktop.in`

The desktop entry file for the
application.

`data/com.example.TextViewer.appdata.xml.in`

The application metadata used by app stores and application distributors.

If you want to, you can now build and run the application by pressing the Run Project button, or Shift + Ctrl + Space .

Note

The code in this tutorial is available on GitLab .
