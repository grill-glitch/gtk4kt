# Using GtkApplication - GNOME Developer Documentation

# Using GtkApplication ¶

GtkApplication is the base class of a GTK Application.

The philosophy of GtkApplication is that applications are interested in
being told what needs to happen, when it needs to happen, in response to actions
from the user. The exact mechanism by which the operating system starts
applications is uninteresting.

To this end, GtkApplication exposes a set of signals (or virtual functions)
that an application should respond to:

- startup : sets up the application when it first starts
- shutdown : performs shutdown tasks
- activate : shows the default first window of the application (like a new
document). This corresponds to the application being launched by the desktop
environment.
- open : opens files and shows them in a new window. This corresponds to
someone trying to open a document (or documents) using the application from
the file browser, or similar.

When your application starts, the startup signal will be fired. This gives
you a chance to perform initialisation tasks that are not directly related to
showing a new window. After this, depending on how the application is started,
either activate or open will be called next.

GtkApplication defaults to applications being single-instance. If the user
attempts to start a second instance of a single-instance application then GtkApplication will signal the first instance and you will receive additional
activate or open signals. In this case, the second instance will exit
immediately, without calling startup or shutdown .

All startup initialisation should be done in startup. This avoids wasting work
in the second-instance case where the program just exits immediately.

The application will continue to run for as long as it needs to. This is usually
for as long as there are any open windows. You can additionally force the
application to stay alive using g_application_hold() .

On shutdown, you receive a shutdown signal where you can do any necessary
cleanup tasks (such as saving files to disk).

The main entry point for your application should only create a GtkApplication instance, set up the signal handlers, and then call g_application_run() .

## Primary vs. local instance ¶

The “primary instance” of an application is the first instance of it that was
run. A “remote instance” is another instance that is started that is not the
primary instance. The term “local instance” is used to refer to the current
process which may or may not be the primary instance.

GtkApplication only ever emits signals in the primary instance. Calls to GtkApplication API can be made in primary or remote instances (and are made
from the vantage of being the “local instance”). In the case that the local
instance is the primary instance, function calls on GtkApplication will
result in signals being emitted locally, in the primary instance. In the case
that the local instance is a remote instance, function calls result in messages
being sent to the primary instance and signals being emitted there.

For example, calling g_application_activate() on the primary instance will
emit the activate signal. Calling it on a remote instance will result in a
message being sent to the primary instance and it will emit activate .

You rarely need to know if the local instance is primary or remote. In almost
all cases, you should just call the GtkApplication method you are interested
in and either have it be forwarded or handled locally, as appropriate.

## Actions ¶

An application can register a set of actions that it supports in addition to the
default activate and open. Actions are added to the application with the GActionMap interface and invoked or queried with the GActionGroup interface.

As with activate and open , calling g_action_group_activate_action() on the primary instance will activate the named action in the current process.
Calling g_action_group_activate_action() on a remote instance will send a
message to the primary instance, causing the action to be activated there.

## Dealing with the command line ¶

Normally, GtkApplication will assume that arguments passed on the command
line are files to be opened. If no arguments are passed, then it assumes that an
application is being launched to show its main window or an empty document. In
the case that files were given, you will receive these files (in the form of GFile ) from the open signal. Otherwise you will receive an activate signal.
It is recommended that new applications make use of this default handling of
command line arguments.

If you want to deal with command line arguments in more advanced ways, there are
several (complementary) mechanisms by which you can do this.

First, the handle-local-options signal will be emitted and the signal
handler gets a dictionary with the parsed options. To make use of this, you need
to register your options with g_application_add_main_option_entries() . The
signal handler can return a non-negative value to end the process with this exit
code, or a negative value to continue with the regular handling of commandline
options. A popular use of for this signal is to implement a --version argument that works without communicating with a remote instance.

If handle-local-options is not flexible enough for your needs, you can
override the local_command_line virtual function to take over the handling
of command line arguments in the local instance entirely. If you do so, you will
be responsible for registering the application, and for handling a --help argument (the default local_command_line function does this for you).

It is also possible to invoke actions from handle-local-options or local_command_line in response to command line arguments. For example, a
mail client may choose to map the --compose command line argument to an
invocation of its compose action. This is done by calling g_action_group_activate_action() from the local_command_line implementation. In the case that the command line being processed is in the
primary instance then the compose action is invoked locally. In the case
that it is a remote instance, the action invocation is forwarded to the primary
instance.

Note

It is possible to use action activations instead of activate or open .
It is perfectly reasonable that an application could start without an activate signal ever being emitted. The activate signal is only
supposed to be the default “started with no options” signal. Actions are
meant to be used for anything else.

Some applications may wish to perform even more advanced handling of command
lines, including controlling the life cycle of the remote instance and its exit
status once it quits as well as forwarding the entire contents of the command
line arguments, the environment and even forwarding standard input, output, and
error streams. This can be accomplished using G_APPLICATION_HANDLES_COMMAND_LINE and the command-line signal.

### Adding custom commandline options ¶

GApplication supports parsing of additional commandline options if they are
specified using g_application_add_main_option_entries() . The ideal place to
call this is from the instance initialization function of your application class
or, if you are not defining your own class, after g_application_new() has
returned.

g_application_add_main_option_entries() takes a pointer to an array of GOptionEntry structures. When a particular commandline option is
encountered, the arg_data field of the corresponding GOptionEntry is set
to the result of parsing this option. If arg_data is NULL then the
option will be stored in the options dictionary that is passed to the handle-local-options signal handler (or virtual function).

You can also specify additional commandline options with g_application_add_main_option() or g_application_add_option_group() .

The handle-local-options handler is expected to handle the commandline
options. There are a number of things that can be done from this handler:

- handle an option locally and exit (either with success or error status); the
typical example for this is a --version option
- treat an option as a request to perform an action on the primary instance
- treat an option as a request to open one or more files on the primary instance
- inspect the options, find them uninteresting, and resume normal processing

The return value of the handle-local-options signal handler will determine
what GApplication does next. If the return value is -1 then the default
processing proceeds (see above). If a non-negative value is returned then this
is taken to mean that the options have been handled locally and the process
should exit (with the returned value as the exit status).

### Using G_APPLICATION_HANDLES_COMMAND_LINE ¶

Normally, when the application is launched for the second time, the
communication of the local instance and the primary instance is short and
simple—usually just a request to show a new window or open some files. The local
instance typically exits immediately.

G_APPLICATION_HANDLES_COMMAND_LINE allows for a more complex interaction
between the two sides. If in doubt, you should not use G_APPLICATION_HANDLES_COMMAND_LINE . There are a number of situations under
which its use may be necessary:

- your application needs to print data from the primary instance to the
stdout/stderr of the terminal of the remote instance
- the primary instance of your application needs to control the duration of the
remote instance
- the primary instance of your application needs to return a particular exit
status from the remote instance
- the primary instance of your application needs access to the environment
variables or file descriptors from the remote instance (such as stdin)
- you may find it more convenient to pass pre-parsed commandline options to the
primary instance in this way (although action parameters should provide a
sufficiently convenient method of accomplishing the same thing with less
overhead)
- when porting your application to GApplication you find that it is easier to
proceed in this way temporarily

A good example of this type of application is a text editor that might be used
from the EDITOR environment variable. If invoked from git commit, the remote
instance must not exit until after the user is done editing the file but it
still needs to exit even if the user has opened other windows and still has them
open.

If G_APPLICATION_HANDLES_COMMAND_LINE is set then after the handle-local-options handler returns then instead of interpreting the
remaining commandline arguments as a list of files, the arguments are passed to
the primary instance via the command-line signal. The options array, as
constructed during parsing of the commandline options and possibly modified from handle-local-options , is passed along to the primary instance, where it can
be accessed using g_application_command_line_get_options_dict() .

It is possible to have all processing done from the primary instance (by using GOptionContext inside of the command-line handler) but this is strongly
discouraged. GOptionContext is very much designed around the assumption that
it will only ever be run once, on argc and argv , and this is not well
matched with the fact that command-line could be invoked multiple times.
Additionally, it is more elegant to report errors in the commandline parsing
directly from the local instance, without communication with the primary
instance. Finally, it is better to have the options registered with the local
instance is that the --help output will list them. All of that said, if you
do not use g_application_add_main_option_entries() and you have set G_APPLICATION_HANDLES_COMMAND_LINE then any unknown options will be ignored
and forwarded to the command-line signal on the primary instance.

Warning

While GOptionEntry allows specifying a callback function to be invoked
in case an argument is found when used with GOptionContext to manually
parse command line arguments, this type of option entries is not allowed
when using GApplication to parse the command line arguments for an
application.
