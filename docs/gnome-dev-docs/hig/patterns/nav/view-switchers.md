# View Switchers - GNOME Human Interface Guidelines

# View Switchers #

![../../_images/view-switcher-title.png](https://developer.gnome.org/hig/_images/view-switcher-title.png)![../../_images/view-switcher-title-dark.png](https://developer.gnome.org/hig/_images/view-switcher-title-dark.png)

A view switcher is a control that allows switching between a small number of predefined views. For example, a music app could show different views for artists, albums and playlists.

## Guidelines #

- As a rule of thumb, a view switcher should contain between three and five views. If you have more views, a sidebar might be a more appropriate choice.
- Label views with header capitalization , and use nouns rather than verbs, for example Albums or Updates . Try to give view labels a similar length.
- When used for preferences, do not design views whose controls affect the controls in other views. Users are unlikely to discover such dependencies.
- View switcher buttons can indicate when there is activity in a view. For example, this could indicate if there is new content available.
- View switchers should switch to the bottom window edge if the window becomes too narrow for them to fit in the header bar. This can be done with the ViewSwitcher and ViewSwitcherBar widgets.

## API Reference #

- Libadwaita: AdwViewSwitcher
- Libadwaita: AdwViewSwitcherBar
