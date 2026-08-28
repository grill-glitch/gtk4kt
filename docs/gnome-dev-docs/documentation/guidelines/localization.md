# Localization - GNOME Developer Documentation

# Localization ¶

This section aims to help software developers getting familiar with the localization (l10n) tools and the internationalization (i18n) process for
the GNOME project and software written for GNOME. It should cover most of the
aspects of localization, from the technical details on how to enable
localization, to how to avoid common pitfalls and how to prevent making the
translation work for translators unnecessarily troublesome.

## Why localize? ¶

The process of localization is making software accessible to users of a
different language or a different culture. The last part is important. Users may
share more or less the same language but still require different settings due to
differences in culture and society.

Why should one localize? It’s a complex question with many answers, but one is
of course that this dramatically helps attract many more users. In the free
software world, a large user base is not only important because it helps getting
the software tested, but it also helps with getting potential new contributors
in the future, in addition to several other important benefits. Another aspect
is the aspect of freedom. The user has the freedom to choose whatever
localization of your software that they prefer and are most comfortable with,
thanks to the software supporting localization. Please bear in mind that you as
a developer only have to enable localization in the software. Lots of the other
hard work is done by other volunteers such as translators and developers of the
libraries and tools used. Thus it is usually a small price to pay for the user
getting a lot of freedom and an accessible application.

The most well-known aspect of localization is the localization of the language,
the translation, but localization also covers a lot of other aspects, ranging
from the decimal character used, the thousands separator used, whether the week
starts on Sunday or Monday, or whether other icons or symbols are needed, and
countless other aspects. Most of these should be considered as important as the
translation. The translation is just one of several steps needed for making the
software usable to people around the world. If the user should have difficulties
understanding for example the display of a decimal number, then the application
still isn’t accessible. Even if the user does understand it but it is presented
in a, from their perspective, very strange and inconvenient format, then the
application still isn’t accessible. Other similar applications that can present
the values correctly will most likely be preferred. Worse, the user might even
interpret the value wrongly, like for example with different and ambiguous date
formats or different decimal and thousands separators, in which case the end
result can be disastrous.

## Useful resources ¶

- GNU gettext manual

## Contents ¶

- Best Practices for Localization
