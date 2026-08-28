# Optimizing GNOME Applications - GNOME Developer Documentation

# Optimizing GNOME Applications ¶

## What are we optimizing? ¶

When we optimize for GNOME the first thing to remember is this: we are not
trying to make the program better, we are trying to make the person using the
computer happier.

Better programs make people happier, but there are some improvements that will
make them a lot happier than others: Responsiveness, start-up time, easy to
access commands and not having the computer go into swap the moment more than
two programs are open.

Traditional optimization tackles concepts like CPU use, code size, the number of
mouse clicks and the memory use of the program. This second list has been chosen
to correlate with the first list, however there is an important difference: the
person using GNOME doesn’t care about the second list, but they care a lot about
the first list. When optimizing GNOME programs we will reduce CPU use, memory
use and all those things, but these are the means to the end, not the final
goal. We are optimizing for people.

## Optimization ¶

The previous section omitted one important qualifier: to optimize something it
has to be measurable. You can’t measure happiness. However, you can measure
start-up time so you can tell if you have improved it. Happiness will then,
hopefully, follow.

Optimization is the process of measurement, refinement and re-measurement. So
the first thing you must do is find a way to measure what you are optimizing.
Ideally this measurement is a single number, for example: the time taken to
perform a task. This is your benchmark, it is the only way to tell if you are
winning or losing. There is a big difference between a program that should be
fast and a program that is fast.

Once you have a basic benchmark you need to find out why your code is not doing
as well as it should. It is tempting to do this by inspection: just looking at
the code and trying to spot something that looks like it needs improvement. You
will invariably be wrong. Using a profiler to get a detailed break-down of what
your program really does is the only way to be sure.

Usually the problem is isolated to small sections of code. Pick the worst place
and concentrate on that first. Once that is done, rerun the profiler and repeat.
As you proceed the gains made at each step will get less and less, at some point
you will have to decide that the results are good enough. If your efforts are
only extracting 10% improvements then you are well past the point where you
should have stopped.

Don’t forget the big picture. For example, rather than just trying to speed up a
piece of code, ask yourself if it needs to be run at all. Could it be combined
with another piece of code? Can the results of previous calculations be saved
and reused? It won’t even need to be optimized if it is in a place where the
user is never going to notice it. Worse still, the code may already be optimized
and is doing the heavy calculations now to avoid doing them later. Code does not
run in isolation and neither does the optimization process.

## Hints ¶

### The fundamentals ¶

1. Re-run your benchmark after every change you make to the code and keep a log
of everything you change and how it affects the benchmark. This lets you undo
mistakes and also helps you not to repeat mistakes.

2. Make sure your code is correct and bug-free before optimizing it. Check that
it remains correct and bug-free after optimization.

- Optimize at the high level before optimizing the details.

4. Use the right algorithm. The classic text-book example is using quick-sort
instead of bubble-sort. There are many others, some save memory, some save CPU.
Also, see what shortcuts you can make: you can do quicker than quick-sort if you
are prepared to make some compromises.

5. Optimization is a trade-off. Caching results speeds up calculations, but
increases memory use. Saving data to disk saves memory, but costs time when it
is loaded back from disk.

6. Make sure you choose a wide variety of inputs to optimize against. If you
don’t it is easy to end up with a piece of code carefully optimized for one file
and no others.

7. Avoid expensive operations: Multiple small disk reads. Using up lots of
memory so disk swapping becomes necessary. Avoid anything that writes or reads
from the hard disk unnecessarily. The network is slow too. Also avoid graphics
operations that need a response from the X server.

### Traps for the unwary ¶

1. Beware of side effects. There can often be strange interactions between
different sections of code, a speed-up in one part can slow another part down.

2. When timing code, even on a quiet system, events outside the program add
noise to the timing results. Average over multiple runs. If the code is very
short, timer resolution is also a problem. In this case measure the time the
computer takes to run the code 100 or 1000 times. If the times you are recording
are longer than a few seconds, you should be OK.

3. It is very easy to be misled by the profiler. There are stories of people
optimizing the operating system idle-loop because that is where it spent all its
time! Don’t optimize code that does nothing the user cares about.

4. Remember the resources on the X server. Your program’s memory usage doesn’t
include the pixmaps that are stored in the X server’s process, but they are
still using up memory. Use xrestop to see what resources your program is using.

### Low level hints ¶

1. When optimizing memory use, be wary of the difference between peak usage and
average memory usage. Some memory is almost always allocated, this is usually
bad. Some is only briefly allocated, this may be quite acceptable. Tools like
massif use the concept of space-time, the product of memory used and the
duration it was allocated for, instead.

2. Time simplified bits of code that do only the things you know are essential,
this gives an absolute lower limit on the time your code will take. For example,
when optimizing a loop time the empty loop. If that is still too long no amount
of micro-optimization will help and you will have to change your design. Make
sure the compiler doesn’t optimize away your empty loop.

3. Move code out from inside loops. A slightly more complicated piece of code
that is executed once is far quicker than a simple piece of code executed a
thousand times. Avoid calling slow code often.

4. Give the compiler as many hints as possible. Use the const keyword. Use G_INLINE_FUNC for short, frequently called, functions. Look up G_GNUC_PURE , G_LIKELY and the other GLib miscellaneous macros. Use the
macros instead of compiler-specific keywords to ensure portability.

5. Don’t use assembly language. It is not portable and, while it may be fast on
one processor, it is not even guaranteed to be fast on every processor that
supports that architecture.

6. Don’t rewrite an existing library routine unless you are sure it is
unnecessarily slow. Many CPU-intensive library routines have already been
optimized. Conversely, some library routines are slow, especially ones that make
system calls to the operating system.

7. Minimize the number of libraries you link to. The fewer libraries to link in,
the faster the program starts. This can be a difficult thing to do with GNOME.

### High level tricks ¶

1. Take advantage of concurrency. This doesn’t just mean using multiple
processors, it also means taking advantage of the time the user spends thinking
about what they are going to do next to perform some calculations in
anticipation. Do calculations while waiting for data to be loaded off disk. Take
advantage of multiple resources, use them all at once.

2. Cheat. The user only has to think that the computer is fast, it doesn’t
matter whether it actually is or not. It is the time between the command and the
answer that is important, it doesn’t matter if the response is pre-calculated,
cached, or will in fact be worked out later at a more convenient time, as long
as the user gets what they expect.

3. Do things in the idle loop. It is easier to program than using full
multi-threading but still gets things done out of the users eye. Be careful
though, if you spend too long in the idle loop your program will become
sluggish. So regularly give control back to the main loop.

4. If all else fails, tell the user that the code is going to be slow and put up
a progress bar. They won’t be as happy as if you had just presented the results,
but they will at least know the program hasn’t crashed and they can go get a cup
of coffee.

## Disk seeks considered harmful ¶

Disk seeks are one of the most expensive operations you can possibly perform.
You might not know this from looking at how many of them we perform, but trust
me, they are. Consequently, please refrain from the following suboptimal
behavior:

- Placing lots of small files all over the disk.
- Opening, stating, and reading lots of files all over the disk
- Doing the above on files that are laid out at different times, so as to
ensure that they are fragmented and cause even more seeking.
- Doing the above on files that are in different directories, so as to ensure
that they are in different cylinder groups and cause even more seeking.
- Repeatedly doing the above when it only needs to be done once.

Ways in which you can optimize your code to be seek-friendly:

- Consolidate data into a single file.
- Keep data together in the same directory.
- Cache data so as to not need to reread constantly.
- Share data so as not to have to reread it from disk when each application
loads.
- Consider caching all of the data in a single binary file that is properly
aligned and can be mmapped.

The trouble with disk seeks are compounded for reads, which is unfortunately
what we are doing. Remember, reads are generally synchronous while writes are
asynchronous. This only compounds the problem, serializing each read, and
contributing to program latency.
