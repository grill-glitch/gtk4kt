# API Stability - GNOME Developer Documentation

# API Stability ¶

## API and ABI ¶

At a high level, an API – Application Programming Interface – is the boundary
between two components when developing against them. It is closely related to an
ABI – Application Binary Interface – which is the boundary at runtime. It
defines the possible ways in which other components can interact with a
component. More concretely, this normally means the C headers of a library form
its API, and compiled library symbols its ABI. The difference between an API and
ABI is given by compilation of the code: there are certain things in a C header,
such as #defines, which can cause a library’s API to change without changing its
ABI. But these differences are mostly academic, and for all practical purposes,
API and ABI can be treated interchangeably.

Examples of API-incompatible changes to a C function would be to add a new
parameter, change the function’s return type, or remove a parameter.

However, many other parts of a project can form an API. If a daemon exposes
itself on D-Bus, the interfaces exported there form an API. Similarly, if a C
API is exposed in higher level languages by use of GIR, the GIR file forms
another API — if it changes, any higher level code using it must also change.

Other examples of more unusual APIs are configuration file locations and
formats, and GSettings schemas. Any changes to these could require code using
your library to change.

## Stability ¶

API stability refers to some level of guarantee from a project that its API will
only change in defined ways in the future, or will not change at all. Generally,
an API is considered ‘stable’ if it commits to backwards-compatibility (defined
below); but APIs could also commit to being unstable or even
forwards-compatible. The purpose of API stability guarantees is to allow people
to use your project from their own code without worrying about constantly
updating their code to keep up with API changes. Typical API stability
guarantees mean that code which is compiled against one version of a library
will run without problems against all future versions of that library with the
same major version number — or similarly that code which runs against a daemon
will continue to run against all future versions of that daemon with the same
major version number.

It is possible to apply different levels of API stability to components within a
project. For example, the core functions in a library could be stable, and hence
their API left unchanged in future; while the newer, less core functions could
be left unstable and allowed to change wildly until the right design is found,
at which point they could be marked as stable.

Several types of stability commonly considered:

*unstable*

The API could change or be removed in future.

*backwards compatible*

Only changes which permit code compiled against the unmodified API to continue
running against the modified API are allowed (for example, functions cannot be
removed).

*forwards compatible*

Only changes which permit code compiled against the modified API to run
against the unmodified API are allowed (for example, functions cannot be
added).

*totally stable*

No changes are allowed to the API, only to the implementation.

Typically, projects commit to backwards-compatibility when they say an API is
‘stable’. Very few projects commit to total stability because it would prevent
almost all further development of the project.

## Versioning ¶

API stability guarantees are strongly linked to project versioning; both package
versioning and libtool versioning. Libtool versioning exists entirely for the
purpose of tracking ABI stability, and is explained in detail on the Autotools
Mythbuster or Versioning.

Package versioning (major.minor.micro) is strongly linked to API stability:
typically, the major version number is incremented when backwards-incompatible
changes are made (for example, when functions are renamed, parameters are
changed, or functions are removed). The minor version number is incremented when
forwards-incompatible changes are made (for example, when new public API is
added). The micro version number is incremented when code changes are made
without modifying API. See Versioning for more information.

API versioning is just as important for D-Bus APIs and GSettings schemas (if
they are likely to change) as for C APIs. See the documentation on D-Bus API
versioning for details.

For GIR APIs, their stability typically follows the C API stability, as they are
generated from the C API.
