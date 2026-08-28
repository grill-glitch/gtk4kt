# Radio Buttons - GNOME Human Interface Guidelines

# Radio Buttons #

![../../_images/radio-buttons.png](https://developer.gnome.org/hig/_images/radio-buttons.png)![../../_images/radio-buttons-dark.png](https://developer.gnome.org/hig/_images/radio-buttons-dark.png)

Radio buttons allow a selection to be made from a set of options.

## When to Use #

Radio buttons allow each option to be individually labelled, which is necesssary when they are not mutually exclusive. (For example, an option to sort by author or by date.) This makes them more appropriate than switches for this type of option.

Since radio buttons display all options without the need for disclosure, they are most appropriate for small sets of options. For longer sets of options, a drop-down list is often a better choice.

## Guidelines #

One button in the set should be selected at all times, except if the radio button represents a property of multiple items, and that property is present for some items and non-present for others. In this case, show the radio button in its mixed state.

Use sentence capitalization for radio button labels. For example, Single click to open .

## API Reference #

- GTK 4: GtkCheckButton
