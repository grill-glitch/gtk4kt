# Flatpak - GNOME Developer Documentation

# Flatpak ¶

Flatpak is a framework for building, distributing and running apps on Linux. It allows developers to easily make apps available to users for download and installation. It also makes it easier to develop apps, by providing a stable development environment, and by making it easy to build and run apps locally.

Flatpak is GNOME’s preferred and recommended distribution framework, and Flatpak support is integrated into GNOME’s development tooling, infrastructure and documentation.

The Flatpak docs provide detailed information on how to use Flatpak, including guidance on getting started as well as more advanced topics. This short guide is intended as an introduction to Flatpak from a GNOME platform perspective.

## Features ¶

Flatpak has a number of advantages that are relevant to application developers. These include:

- Cross-distro app distribution : apps built with Flatpak can be run on any Linux distribution.
- Forward-compatibility : the same app can be run on different distro versions.
- Control over dependencies : apps can rely on stable sets of dependencies called runtimes; other dependencies can be bundled directly into the app.
- Predictable development environments : Flatpak makes it possible to develop in an identical environment to where the app will run.
- Easy testing : Flatpak makes it easy to build and run apps as they are developed, using Builder .
- Simple path to audience : the Flathub hosting service provides a single distribution point for all Linux distros.

## Key concepts ¶

Inevitably, Flatpak has some differences from other software distribution frameworks. However, it can be understood through a small number of key concepts.

### Runtimes ¶

A runtime is a collection of dependencies which acts as a platform on which apps can run. When an app is built using Flatpak, it is built against a runtime, and this runtime must be present on a system for the app to be run.

Each runtime is maintained in the same way as an operating system, with updates that are released for a predefined period of time. GNOME maintains its own runtime, which contains the components which make up the GNOME platform.

Flatpak runtimes can be used on different Linux distros, which means that they provide a stable, cross-distribution base for applications. They are also independent of different distro versions, which provides additional flexibility for app developers.

### Software Development Kits (SDKs) ¶

Each runtime is accompanied by an SDK. This is a version of the runtime which has been extended to act as a development environment. Developers develop their apps using the SDK and build them against the corresponding runtime, which is then used to run the app.

For example, when developing an app which will be built against the GNOME 40 runtime, development would happen using the GNOME 40 SDK.

### Bundled libraries ¶

If an application requires any dependencies that aren’t in its runtime, they can be bundled as part of the application. This gives application developers flexibility regarding dependencies, by allowing them to use libraries that aren’t available in a runtime, different versions of libraries from the ones that are in a runtime, and patched versions of libraries.

### Manifests ¶

A Flatpak manifest is a JSON or YML file that describes how an app is to be built. It specifies which runtime is to be used, which additional libraries are to be bundled, and any configuration steps that should be carried out as part of the build process.

## Example ¶

The Builder introduction includes a tutorial on how to create and run an app using Builder’s templating feature. Let’s pick up that example and see what the Flatpak configuration looks like.

First, open the Build Preferences , using the menu on the top-left of the header bar. On the left, you should see a sidebar listing the available manifests. The one that is selected - org.gnome.Demo.json - is the manifest file that is being used to build and run your app.

Now let’s open the manifest file to see what it looks like. Go back to the editor view, and open the org.gnome.Hello.json file in Builder. You should see something like the following:

```
{

    
"app-id"
 
:
 
"org.gnome.Hello"
,

    
"runtime"
 
:
 
"org.gnome.Platform"
,

    
"runtime-version"
 
:
 
"40"
,

    
"sdk"
 
:
 
"org.gnome.Sdk"
,

    
"command"
 
:
 
"hello"
,

    
"finish-args"
 
:
 
[

        
"--share=network"
,

        
"--share=ipc"
,

        
"--socket=fallback-x11"
,

        
"--socket=wayland"

    
],

    
"cleanup"
 
:
 
[

        
"/include"
,

        
"/lib/pkgconfig"
,

        
"/man"
,

        
"/share/doc"
,

        
"/share/gtk-doc"
,

        
"/share/man"
,

        
"/share/pkgconfig"
,

        
"*.la"
,

        
"*.a"

    
],

    
"modules"
 
:
 
[

        
{

            
"name"
 
:
 
"hello"
,

            
"builddir"
 
:
 
true
,

            
"buildsystem"
 
:
 
"meson"
,

            
"sources"
 
:
 
[

                
{

                    
"type"
 
:
 
"git"
,

                    
"url"
 
:
 
"file:///home/user/Projects/hello"

                
}

            
]

        
}

    
]

}
```

This is the app manifest. As can be seen, it specifies the ID of the app, its runtime and runtime version, and the command that used to run it.

finish-args is used to configure which services the app has access to, and cleanup is literally that - post-build clean-up.

The final modules section is a list of the software modules that are to be built and bundled inside the Flatpak. In this case, there is just one module - the app itself. However, other libraries and dependencies can be included here.

See the Flatpak docs for more information on manifests and building apps .

## Flathub hosting ¶

Flatpak uses a decentralized model, which means that anyone can host their own repository from which users can download, install and update apps. However, often it is most convenient to use Flathub , Flatpak’s primary hosting service.

Flathub is enabled by default by some Linux distros. In other cases, it is easy to manually enable.

Hosting an app on Flathub is simple: you create a Git repository under the Flathub GitHub project, which contains your application’s manifest. Flathub then builds and hosts the app based on that manifest. For more information, see the Flathub app submission guide .

## Attribution ¶

Parts of this page are copied from the Flatpak documentation .
