# Coding Guidelines for Supporting Accessibility - GNOME Developer Documentation

# Coding Guidelines for Supporting Accessibility ¶

Here are some things you can do in your code to make your program work as well
as possible with assistive technologies.

- For components that don’t display a short string (such as an image button),
specify a name for it by setting the GTK_ACCESSIBLE_PROPERTY_LABEL . You
might want to do this for image-only buttons, panels that provide logical
groupings, text areas, and so on.
- If you can’t provide a tooltip for an UI element, use GTK_ACCESSIBLE_PROPERTY_DESCRIPTION instead to provide a description that assistive technologies can give the
user. For example, to provide an accessible description for a Close button: gtk_accessible_update_property ( GTK_ACCESSIBLE ( button ), GTK_ACCESSIBLE_PROPERTY_DESCRIPTION , _ ( "Closes the window" ), -1 );
- If several components form a logical group, try to put them in one container.
- Whenever you have a label that describes another component, use the GTK_ACCESSIBLE_RELATION_LABELLED_BY relation: gtk_accessible_update_relation ( GTK_ACCESSIBLE ( text_entry ), GTK_ACCESSIBLE_RELATION_LABELLED_BY , label , NULL , -1 );
- If you create a custom widget, make sure it supports accessibility. Custom
components that are descendants of other GTK widgets should override
inherited accessibility information as necessary.
- Don’t break what you get for free! If your application has an inaccessible
container, any components inside that container may become inaccessible.
