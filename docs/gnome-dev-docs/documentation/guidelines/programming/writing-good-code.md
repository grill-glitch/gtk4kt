# The Importance of Writing Good Code - GNOME Developer Documentation

# The Importance of Writing Good Code ¶

GNOME is a very ambitious free software project, and it is composed of many
software packages that are more or less independent of each other. A lot of the
work in GNOME is done by volunteers: although there are many people working on
GNOME full-time or part-time for here, volunteers still make up a large
percentage of our contributors. Programmers may come and go at any time and they
will be able to dedicate different amounts of time to the GNOME project.
People’s “real world” responsibilities may change, and this will be reflected in
the amount of time that they can devote to GNOME.

Software development takes long amounts of time and painstaking effort. This is
why most part-time volunteers cannot start big projects by themselves; it is
much easier and more rewarding to contribute to existing projects, as this
yields results that are immediately visible and usable.

Thus, we conclude that it is very important for existing projects to make it as
easy as possible for people to contribute to them. One way of doing this is by
making sure that programs are easy to read, understand, modify, and maintain.

Messy code is hard to read, and people may lose interest if they cannot decipher
what the code tries to do. Also, it is important that programmers be able to
understand the code quickly so that they can start contributing with bug fixes
and enhancements in a short amount of time. Source code is a form of
communication, and it is more for people than for computers. Just as someone
would not like to read a novel with spelling errors, bad grammar, and sloppy
punctuation, programmers should strive to write good code that is easy to
understand and modify by others.

The following are some important qualities of good code:

**cleanliness**

Clean code is easy to read with minimum effort. This lets people start to
understand it easily. This includes the coding style itself (brace placement,
indentation, variable names), and the actual control flow of the code.

**consistency**

Consistent code makes it easy for people to understand how a program works.
When reading consistent code, one subconsciously forms a number of
assumptions and expectations about how the code works, so it is easier and
safer to make modifications to it. Code that looks the same in two places
should work the same, too.

**extensibility**

General-purpose code is easier to reuse and modify than very specific code
with lots of hardcoded assumptions. When someone wants to add a new feature to
a program, it will obviously be easier to do so if the code was designed to be
extensible from the beginning. Code that was not written this way may lead
people into having to implement ugly hacks to add features.

**correctness**

Finally, code that is designed to be correct lets people spend less time
worrying about bugs, and more time enhancing the features of a program. Users
also appreciate correct code, since nobody likes software that crashes. Code
that is written for correctness and safety (i.e. code that explicitly tries to
ensure that the program remains in a consistent state) prevents many kinds of
silly bugs.

## Book references ¶

- Code Complete , by Steve McConnell
- Refactoring: Improving the Design of Existing Code , by Martin Fowler.
- Design Patterns: Elements of Reusable Object-Oriented Software , by Erich Gamma, Richard Helm, Ralph Johnson and John Vlissides.
- Object-Oriented Design Heuristics , by Arthur Riel.
