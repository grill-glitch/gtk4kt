# Services - GNOME Developer Documentation

# Services ¶

An overview of GNOME system services.

## Mail & Calendar ¶

### Evolution-data-server (EDS) ¶

With Evolution Data Server, GNOME provides a single address book and calendar
that all applications can use to store and retrieve information. Using Evolution
Data Server means that users no longer have to maintain separate lists of
contacts in each application, or manually copy events to their calendar.

Applications can also use Evolution Data Server to store and retrieve
appointments on the user’s calendar. For example, the clock on the panel shows a
simple calendar when clicked. If the user has any appointments scheduled, they
are shown alongside the calendar. This makes it easy to see upcoming
appointments without opening a full calendar application.

- ECal API reference

## Contacts ¶

### Folks ¶

People use computers increasingly to interact with their friends and colleagues.
Applications such as email programs, instant messengers, and telephony and video
conferencing applications are used to communicate with others. These
applications often provide contact lists to help users. Folks takes care of
aggregating all these forms of contacts so that you can get all the accounts
that belong to one person. This lets software present lists of people in a more
useful fashion, instead of showing duplicated people whenever they have more
than one account associated to them.

- Folks API reference

## File Indexing ¶

### LocalSearch ¶

The LocalSearch filesystem indexer maintains an index of file names and file content
within configured search locations. The default search configuration includes
parts of the user’s home. The search index is stored in the XDG user cache
directory, which is also within the user’s home by default.

LocalSearch provides a D-Bus service that lets you query and search
indexed content. Apps within Flatpak do not have access to this service by default,
but can request access to query different types of content via a portal.

- LocalSearch documentation

## Authorization & Privilege Escalation ¶

### Polkit ¶

Polkit provides an authorization API intended to be used by privileged programs
offering service to unprivileged programs; for instance, a system service may
use polkit to allow an application to change the system configuration, with the
option of asking for user credentials in order to do so.

- Polkit API reference

## Secrets & Passwords ¶

### Libsecret ¶

Libsecret is a library for storing and retrieving passwords and other secrets.
It communicates with the GNOME keyring using D-Bus.

- Libsecret API reference

## Network Management ¶

### NetworkManager ¶

The NetworkManager daemon attempts to make networking configuration and
operation as painless and automatic as possible by managing the primary network
connection and other network interfaces, like Ethernet, Wi-Fi, and Mobile
Broadband devices. NetworkManager will connect any network device when a
connection for that device becomes available, unless that behavior is disabled.
Information about networking is exported via a D-Bus interface to any interested
application, providing a rich API with which to inspect and control network
settings and operation.

- NetworkManager homepage
- libnm API reference

### ModemManager ¶

ModemManager is a DBus-activated daemon which controls mobile broadband
(2G/3G/4G) devices and connections. Whether built-in devices, USB dongles,
bluetooth-paired telephones, or professional RS232/USB devices with external
power supplies, ModemManager is able to prepare and configure the modems and
setup connections with them.

- ModemManager homepage

## Power Management ¶

### UPower ¶

UPower is an abstraction for enumerating power devices, listening to device
events and querying history and statistics. Any application or service on the
system can access the org.freedesktop.UPower service via the system message bus.
Some operations (such as suspending the system) are restricted using polkit.

- UPower API reference

## Sandboxing ¶

### Portals ¶

Portals are interfaces that allow sandboxed applications to safely communicate to the
system outside the sandbox.

- Portals D-Bus interfaces
- libportal API reference (C)
- ashpd API reference (Rust)

## Media ¶

### PipeWire ¶

PipeWire provides a low-latency, graph based processing engine on top of audio
and video devices that can be used to support the use cases currently handled by
both pulseaudio and JACK. PipeWire was designed with a powerful security model
that makes interacting with audio and video devices from containerized
applications easy, with supporting Flatpak applications being the primary goal.

- PipeWire project page

### Media Playback Remote Interface (MPRIS) ¶

The Media Player Remote Interfacing Specification is a standard D-Bus interface
which aims to provide a common programmatic API for controlling media players.

It provides a mechanism for discovery, querying and basic playback control of
compliant media players, as well as a tracklist interface which is used to add
context to the active media item.

- MPRIS D-Bus Interface Specification
