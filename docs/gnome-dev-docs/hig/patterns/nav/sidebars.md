# Sidebars - GNOME Human Interface Guidelines

# Sidebars #

![../../_images/adaptive-sidebar-wide.png](https://developer.gnome.org/hig/_images/adaptive-sidebar-wide.png)![../../_images/adaptive-sidebar-wide-dark.png](https://developer.gnome.org/hig/_images/adaptive-sidebar-wide-dark.png)

A sidebar is a vertical panel which contains a list of different locations. Clicking each location navigates to it. Sidebars are similar to utility panes , but they play a different role and have different behavior.

## When to Use #

Sidebars can be used when it is necessary to expose a larger number of views than can be accommodated in a standard view switcher .

Sidebars can also be appropriate when it is necessary to navigate between dynamic locations, such as in a messaging app. They can also be suited to contexts where frequent switching back and forth between locations is common.

Sidebars should be avoided for apps which provide rich or immersive content. In this situation, the sidebar would be a distraction from app content.

## Guidelines #

- Order the list according to what is most useful for the users of your app. Sidebars which contain a large number of dynamic items will often need to be ordered so that recently updated items are at the top of the list.
- Header bar controls which affect the sidebar list should be placed above the list.
- Each list row can include multiple lines of text, as well as images. However, be careful to ensure that the most important information is not lost, and work to ensure a clean and attractive appearance.

## API Reference #

- Libadwaita: AdwNavigationSplitView
