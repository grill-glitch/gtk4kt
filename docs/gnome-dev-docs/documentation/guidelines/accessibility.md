# Accessibility - GNOME Developer Documentation

# Accessibility ¶

Accessibility means helping people with disabilities to participate in
substantial life activities. That includes work, and the use of services,
products, and information. GNOME includes libraries and a support framework
that allow people with disabilities to utilize all of the functionality of the
GNOME user environment.

In conjunction with assistive technologies if necessary - voice interfaces,
screen readers, alternate input devices, and so on - people with permanent or
temporary disabilities can therefore use the GNOME desktop and applications.
Assistive technologies are also useful for people using computers outside their
home or office. For example, if you’re stuck in traffic, you might use voice
input and output to check your email.

Assistive technologies receive information from applications via the AT-SPI
D-Bus protocol, which you can find in the at-spi2-core repository. GTK implements the
AT-SPI protocol for its own widgets, and exposes the GtkAccessible API to
application developers. Because support for the accessibility API is built into
the GTK toolkit, your GNOME program should function reasonably well with
assistive technologies with no extra work on your part. For example, assistive
technologies can automatically read the widget labels that you would normally
set in your program anyway (e.g. with GTK function calls such as gtk_label_set_text() or gtk_button_new_with_label() ).  They can also
find out if there is any tooltip text associated with a widget, and use that to
describe the widget to the user.

With a little extra effort, however, you can make your program function even
more smoothly with assistive technologies. Besides helping individual users,
this will also make your product more attractive to government and education
markets, many of which now require their applications to be accessible by law.

## Types of disabilities ¶

Disabilities fall into one of these categories:

these can range from low-vision (including dim or hazy vision, extreme far- or
near-sightedness, color-blindness, and tunnel vision, amongst others) to
complete blindness. Poor choice of text size and color, and tasks that involve
good hand-eye coordination (such as moving the mouse) can cause problems for
these users.

users with poor muscle control or weaknesses can find it hard to use a
standard keyboard or mouse. For example, they may be unable to hold down two
keys simultaneously, or they may be more likely to strike keys accidentally.

these can range from being able to hear some sounds but not distinguish spoken
words, to profound deafness. Applications that convey important information by
sound alone will cause problems for these users.

these can range from dyslexia to difficulties remembering things, solving
problems or comprehending and using spoken or written language. Complex or
inconsistent displays, or poor choice of words can make using computers
difficult for these users.

certain light or sound patterns can cause epileptic seizures in some
susceptible users.

## Content overview ¶

- The GNOME Human Interface Guidelines contain a section on how to design your application so that it is accessible
right from the start.
- Improving your UI’s accessibility
- Making custom UI elements accessible
- GTK Accessibility guidelines
