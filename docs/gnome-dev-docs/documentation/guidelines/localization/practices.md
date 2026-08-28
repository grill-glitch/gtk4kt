# Best Practices for Localization - GNOME Developer Documentation

# Best Practices for Localization ¶

These are best practices for application developers to ensure that their
projects can be localized more easily and more efficiently.

## Use clear, simple and consistent language ¶

Using clear, simple and consistent language and terminology is very important.
Not only does it benefit the users of the American English version of the
software, it also significantly helps the translation process and in the end the
users of the localized versions. Please remember that the majority of the
translators are not native English speakers. Any questions regarding the
interpretation of the messages in the software may thus result in accidental
mistranslations and problems for the end user of the localized version. Even if
the translator does interpret the message correctly, an ambiguous original
message will often become even more ambiguous when translated.

Specifically, never use slang, and avoid using abbreviations. These are usually
exceptionally hard to translate correctly. As an example of these guidelines,
write “IP number” instead of just “IP” when possible, “character set” instead of
“charset”, “application” instead of “app”, “the folder /foo” instead of just
“/foo”, “proxy server” instead of just “proxy”, “information” instead of just
“info”, “database” instead of just “db”, and “application launcher” instead of
just “launcher” if that’s what you’re referring to.

Also try to be consistent. Avoid using different terminology or different
spellings in different places in your software. As an example, avoid to use
several variants of “e-mail”, “E-Mail”, “email”, and “Email” simultaneously.
Also try to be consistent with the terminology of other software and especially
the ones in the same software project, such as other GNOME software if your
application is a GNOME application. The GNOME Word List is an useful resource
when choosing terminology, and its terminology and spelling should be used
whenever possible. This helps translators a lot, since they can keep translation
databases small and still have a useful result when translating other
applications, if a magnitude of different terminology and different spellings of
the same terminology can be avoided.

Messages should be written using American English. Please set (potential)
personal feelings aside and avoid other spellings than the commonly accepted
American English ones. Specifically, avoid using British English ones, like
“colour” and “centre”. Use “color” and “center” instead in the original software
messages. British English spellings can be, and are, provided by British English
translations instead, and standardizing on one way of spelling words in original
messages helps the translation effort for all translators.

## Use a consistent typographical style ¶

Keep in mind that being consistent when designing messages is not only about
using consistent writing. It’s also about having a consistent use of white space
and newlines. Every change in white space or the number of trailing newlines in
otherwise identical messages means additional messages for the translators to
translate. For this reason, try to avoid trailing spaces and newlines if
possible. Also, try to be consistent and don’t use white space before colons,
question marks, exclamation marks and other punctuation marks.

Also avoid using tabs inside messages. Tabs are often used for alignment inside
console text messages, but the amount of spacing the tab character ( \t )
represents is not easily clear from a visual inspection, and it’s difficult to
get the correct amount of tabs to use in the translated message in order for the
translated message to align properly, since the translated words or sentence
often is of a different length than the original. Please replace tabs with
spaces inside messages (if you have to use spacing inside the message).

Standardize capitalization in your messages. The GNOME Human Interface
Guidelines (HIG) has a chapter on typography , with guidelines for when to use
capitalization and when not to.

## Avoid expanding acronyms ¶

For better or for worse, the use of Internet and computers today involves the
use of many acronyms, such as Tag Image File Format (TIFF), Portable Network
Graphics (PNG), Internet Relay Chat (IRC), Rich Text Format (RTF), and Plain Old
Documentation (POD). Naturally, applications also have to deal with these in
their interface messages.

These things are often widely known by their acronym, and much less so by their
fully expanded names. Thus, avoid using the fully expanded names in your
application’s messages. Use the well-known acronym instead. As an example, if
your application saves images in the PNG format, then say so, instead of saying
that it saves images in Portable Network Graphics format.

This problem becomes even more prominent when dealing with translations. The
acronyms are used across language barriers. A “PNG” image in English or Spanish
is still referred to as a “PNG” image in Hindi or Japanese. A Japanese user will
know the file format by its well-known original “PNG” acronym.

## Use comments ¶

Gettext has a nice and very useful feature where any comments in the source code
that are immediately preceding a message marked for translation, are being
automatically picked up and displayed as comment in the .po file next to the
message in question.

To distinguish developer-related comments from translator-related comments,
prefix the comment with “Translators”, for instance in a C source file:

```
/* Translators: This is a verb, not a noun */

gtk_label_set_label
 
(
label
,
 
_
(
"Profile"
));
```

In a GtkBuilder UI definition file:

```
<property
 
name=
"label"
 
translatable=
"yes"
 
comments=
"Translators: This is a verb, not a noun"
>
Profile
</property>
```

In a GSettings schema:

```
<!-- Translators: This is a verb, not a noun -->

<summary>
Profile
</summary>
```

In a Mallard documentation file:

```
<p
 
xmlns:its=
"http://www.w3.org/2005/11/its"
 
its:locNote=
"Translators: Comment about text to translate."
>
Text
 
to
 
translate
</p>
```

## Only use valid UTF-8 in messages ¶

Try to always keep messages marked for translation in the plain 7-bit ASCII or
in the UTF-8 character sets. Avoid using any other character sets.

The reason for this is technical. Since the original strings and their
translations are stored in the same po files, they need to use a common encoding
for gettext to be able to function when accessing the translation of a
particular string. Gettext doesn’t do any character set conversion itself when
accessing a translation. Plain 7-bit ASCII is the only common subset between
most encodings, and hence it was traditionally the only choice when writing
translatable application strings.

Alternatively, you can as of lately also use UTF-8 in the translatable strings
of your application. Since all GNOME translations are supposed to be encoded in
UTF-8, this will also solve the need of using a common encoding.

## Do not mark empty strings for translation ¶

In the po format, the empty string (“”) is reserved and has a special use. It is
always used as the msgid and key for the po file header, and has the po file
header as its translation in a po file. As such, marking empty strings for
translation will not work as expected, as the result returned by the gettext ()
call will be the entire po file header. The solution is to simply not mark
empty strings for translation.

## Do not hard-code line breaks ¶

The reasons for this is that making the lines have the appropriate width with
some variable-width font that is different from the one used when editing is not
only a difficult task for the developer, it’s also a very difficult task for all
translators. Also, the danger of line breaks “moving around” when the developer
changes the hard-coded wrapping (and thus all translations needing updates) is
eliminated when line breaks are removed.
