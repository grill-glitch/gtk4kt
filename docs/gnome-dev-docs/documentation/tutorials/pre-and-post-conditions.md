# Pre- and Post-Conditions - GNOME Developer Documentation

# Pre- and Post-Conditions ¶

An important part of secure coding is ensuring that incorrect data does not
propagate far through code — the further some malicious input can propagate,
the more code it sees, and the greater potential there is for an exploit to be
possible.

A standard way of preventing the propagation of invalid data is to check all
inputs to, and outputs from, all publicly visible functions in a library or
module. There are two levels of checking:

Check for invalid input and return an error gracefully on failure.

Check for programmer errors and abort the program on failure.

## Validation ¶

Validation is a complex topic, and is typically handled using GError . For more information on how to use GError in your API, see the GLib reference on error reporting .

The remainder of this section discusses pre- and post-condition assertions, which are purely for catching programmer errors.
A programmer error is where a function is called in a way which is documented as disallowed.
For example, if NULL is passed to a parameter which is documented as requiring a non- NULL value to be passed; or if a negative value is passed to a function which requires a positive value.
Programmer errors can happen on output too — for example, returning NULL when it is not documented to, or not setting a GError output when it fails.

## Assertions ¶

Adding pre- and post-condition assertions to code is as much about ensuring the behavior of each function is correctly and completely documented as it is about adding the assertions themselves.
All assertions should be documented, preferably by using the relevant introspection annotations , such as ( nullable ).

Pre- and post-condition assertions are implemented using g_return_if_fail() and g_return_val_if_fail() .

The pre-conditions should check each parameter at the start of the function, before any other code is executed (even retrieving the private data structure from a GObject, for example, since the GObject pointer could be NULL ).
The post-conditions should check the return value and any output parameters at the end of the function — this requires a single return statement and use of goto to merge other control paths into it.
See Single-path cleanup for an example.
