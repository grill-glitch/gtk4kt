# Sysprof - GNOME Developer Documentation

# Sysprof ¶

Sysprof is a system-wide statistical
profiler for Linux. You can use Sysprof to measure how much time is spent in
each function of your application, down to the system calls into the Linux
kernel.

![../_images/sysprof-config.png](https://developer.gnome.org/documentation/_images/sysprof-config.png)

The initial configuration and capture sources for Sysprof ¶

## Finding code run often on the system ¶

Sysprof, at it’s core, is a “whole system” profiler. That means it is not
designed to profile just your single program, but instead all the processes on
the system. This is very useful in a desktop scenario where we have lots of
interconnected components.

Important

You have to make sure you’re compiling with flags that allow us to have
enough information to unwind stack frames. Sysprof will use libunwind in
some cases, but a majority of our stack unwinding is done by the Linux
kernel which can currently only follow eh_frame (exception handling)
information. For C projects, you want to make sure that -fno-omit-frame-pointer is in your compiler flags.

![../_images/sysprof-callgraph.png](https://developer.gnome.org/documentation/_images/sysprof-callgraph.png)

The callgraph tool ¶

On the right side is a callgraph starting from “[Everything]”. It is split out
by process and then by the callstack you see in that program. On the top-left
side, is a list of all functions that were collected (and decoded). On the
bottom-left side is a list of callers for the selected function above it. This
is useful when you want to backtrack to all the places a function was called.

Note

Sysprof is a sampling-based profiler, so there is no guarantee all functions
were intercepted.

Use this information to find the relevant code within a particular project.
Tweak some things, try again, test…

## Tracking down extraneous allocations ¶

One of the things that can slow down your application is doing memory
allocations in the hot paths. Allocating memory is still pretty expensive
compared to all of the other things your application could be doing.

At this point run your application to exercise the targeted behavior. Then
press “Stop” and you’ll be presented with the recording. Usually the normal
callgraph is selected by default. Select the “Memory Allocations” row and
you’ll see the memory callgraph.

This time you’ll see memory allocation size next to the function. Explore a bit,
and look for things that seem out of place. In the following image, there are a
lot of transforms being allocated.

![../_images/sysprof-allocations.png](https://developer.gnome.org/documentation/_images/sysprof-allocations.png)

Cumulative allocations ¶

## Finding main loop slow downs ¶

The “Speedtrack” aid will give you callgraphs of various things that happened
in your main thread that you might want to avoid doing, like fsync() , read() and more. It also creates marks for the duration of these calls so
you can track down how long they ran for.

![../_images/sysprof-io.png](https://developer.gnome.org/documentation/_images/sysprof-io.png)

Various files being loaded in Pango ¶

![../_images/sysprof-mainloop.png](https://developer.gnome.org/documentation/_images/sysprof-mainloop.png)

A stall in the main loop iteration ¶

You can also see how long some operations have taken. Here we see g_main_context_iteration() took 22 milliseconds. On a 60 Hz system, that
can’t be good because we either missed a frame or took too long to do something
to be able to submit our frame in time. You can select the time range by
activating this row. In the future we want this to play better with callgraphs
so you can see what was sampled during that timespan.
