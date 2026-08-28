# Making Custom Components Accessible - GNOME Developer Documentation

# Making Custom Components Accessible ¶

Adding accessibility support to your custom widget will assure its cooperation
with the accessibility infrastructure. These are the general steps that are
required:

- assess a custom widget according to the applicable human interface guidelines
- determine which accessible role a custom widget should provide, according to
the widget’s feature set and function
- update the appropriate accessible properties when the widget’s content change
- update the appropriate accessible states when the widget’s state changes
- update the appropriate accessible relations between the custom widget and its
children
