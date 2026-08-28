# Banners - GNOME Human Interface Guidelines

# Banners #

![../../_images/banner.png](https://developer.gnome.org/hig/_images/banner.png)![../../_images/banner-dark.png](https://developer.gnome.org/hig/_images/banner-dark.png)

A banner is a strip along the top of a view, which contains a title and an optional button. They are used to communicate persistent states.

Examples include indicating that an app is offline, or that a document is read-only. They can also be used to present supplementary information about special locations or content items.

## When to Use #

Banners do not automatically hide, and are displayed for ongoing periods of time. This makes them appropriate for communicating states or location-specific information.

Don’t use a banner to communicate events, one-time messages, or short-lived states: use notifications or toasts instead.

Banners are deliberately attention-grabbing, and should therefore only be used to communicate important information.

## Guidelines #

Banners have a single title, which should be short and to the point, and should be written using an informal heading style . Try to avoid lengthy explanations, and instead stick to precise factual statements.

Examples of good titles include:

- “Metered network ‒ automatic updates paused”
- “Unlock to change settings”
- “Working offline”

## API Reference #

- Libadwaita: AdwBanner
