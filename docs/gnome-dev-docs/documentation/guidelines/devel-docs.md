# Developer Documentation Style Guidelines - GNOME Developer Documentation

# Developer Documentation Style Guidelines ¶

## Motivation ¶

In order to create a consistent platform, GNOME modules must have a consistent
documentation not just in terms of content, but also in terms of style. A
singular editorial voice helps in creating high-quality, readable, and
consistent documentation.

This style guide encodes the style used by the GNOME documentation for
developers; it does not claim to be objectively better than other style guides.
The rules in this guide are not meant to be absolute: you should have readable
documentation, first and foremost; if the result is not readable, you should
exercise your own judgement.

It is important to note that this style guide is a living document: context and
recommendations may change over time, and these guidelines will change as well.
You should check the What’s new section of this document for
changes.

## Voice and tone ¶

The documentation should use a friendly, conversational voice. Be respectful and
approachable. Be straightforward, and clear, without being pedantic and pushy;
like a friend helping somebody out in their time of need.

- Get to the point . Make sure to present the key information first; put it
in the most noticeable spot. Make steps and choices obvious. Give all the
necessary information up front. Do not get in the way.
- Talk like a person . Write colloquially, without being cutesy. Don’t write
exactly how you would speak, but feel free to use everyday words and
contractions. Don’t use jargon and acronyms when explaining things. Don’t use
pop culture references or inside jokes. Developer documentation is technical,
but you can still be human. Be personal and memorable, and you can be funny
when needed.
- Keep it simple . Don’t be unnecessarily verbose or pedantic. Write short
sentences that are easy to scan and read. Break up explanations, and layer
the information so that people can stop once they found out what they need.

For developer documentation you can assume that the reader is going to be
knowledgeable, but remember that there are varying levels of proficiency; your
goal is to allow developers to achieve their goals.

### Some things to avoid ¶

- Buzzwords and jargon
- Ableist language
- Placeholder phrases, like please note and at this time
- Long-winded sentences
- Starting all sentences in the same way, e.g. You can or To do
- Jokes at the expense of other projects, or people using them
- Pop culture references
- Community inside jokes
- Exclamation marks
- Mixing metaphors
- Obscure implementation details
- Phrases like simply , it is simple to , it’s easy to , just , quickly
- Internet slang, like tl;dr or ymmv
- Excessive use of please , e.g. please view this document or please use
this class

## Writing inclusively ¶

GNOME strives to be an inclusive community. Our developer documentation should
reflect that committment.

### Global audience ¶

GNOME users and contributors live and work all over the world, and you can
assume that the documentation will be read by people whose first language is not
English.

- Write short sentences . Consider breaking sentences with multiple clauses
and commas into multiple sentences.
- Use list and tables . If you have multiple paragraphs and complex
sentences, consider using list and tables instead.
- Use sentence-style capitalization . Capitalize proper nouns only, including
trademarks and name of projects and products.
- Avoid idioms, colloquial expressions, and culture-specific references .
- Avoid stacking modifiers . Long chains of modifying words are confusing,
even to native English speakers.
- Use active voice .
- Keep adjective and adverbs close to the word they modify .
- Use “that”, “who”, and “which” to clarify sentence structure .

### Gender identity and pronouns ¶

Do not use gender-specific pronouns unless the person you’re referring to is
actually that gender.

In particular, do not use he , him , she , or her as gender-neutral
pronouns, and do not use he/she , (s)he , or he or she or other similar
compound approaches. Instead, use the singular “they” .

Avoid first-person pronouns ( I , we , us , our , ours ), except:

- questions in a “frequently asked questions” document
- a document written explicitly in first person
- a sentence referring to your project or organization

Always use the second-person pronoun wherever possible.

For relative pronouns, always make use of that , which , and who to clarify
the sentence structure. That and which are not interchangeable:

- That introduces a restrictive clause, and isn’t preceded by a comma
- Which introduces a non-restrictive clause, and is preceded by a comma

When referring to a person, always use who .

When referring to groups of people, animals, or things you can use whose .

### Avoiding bias in communication ¶

- When writing examples, be mindful of stereotypes . Choose names, gender
identities, and cultural backgrounds to reflect a variety of cases.
- Do not make generalizations about people, countries, regions, and
cultures .
- Don’t use slang .
- Don’t use profane or derogatory terms .
- Don’t use terms that carry racial bias . For instance, do not use terms
like master/slave . Use primary/secondary or physical/logical , depending
on the context. Instead of replacing a word you can also rewrite to improve
the clarity of a sentence; for instance, instead of replacing whitelist with allowlist , you can explain what is allowed.
- Focus on people, not disabilities . Do not describe people without
disabilities using terms like normal or healthy ; avoid euphemisms, like special or differently able ; avoid terms that remove personhood, like quadriplegic , and instead use quadriplegic person .
- Use inclusive language .

## Formatting ¶

### Dates and times ¶

When expressing times use the 12-hour clock, except in cases where the 24-hour
clock is required:

- use exact times, in the HH:MM format
- always capitalize AM and PM
- remove the minutes for round hours

For ranges, use hyphens without spaces, for instance: 5-10 minutes ago .

Avoid specifying time zones unless absolutely necessary; use the full name of
the time zone, adding the offset from UTC as a parenthetical, for instance:

- US Pacific Standard Time (UTC-8)
- Central European Summer Time (UTC+2)

When expressing dates:

- use the name of months and days of the week in full, with the month followed
by the day of the month, a comma, and the full four-digits year
- for numeric only dates, use the YYYY-MM-DD format conforming to the
ISO-8601 standard

### Numbers ¶

- When using ordinal numbers, spell them out in text: first, second, tenth,
twenty-first.
- Spell out numbers between zero and nine.
- Spell out numbers at the beginning of a sentence.
- Use numerals for negative numbers, fractions, percentages, dimensions, and
decimals.

### Capitalization ¶

- Use title-style capitalization only for top level titles, and sentence-style
capitalization for everything else: Capitalize the first word of a sentence Use lowercase for every other word When words are joined by a slash, capitalize the second word if the first is
also capitalized
- Never use all uppercase for emphasis.
- Do not use all lowercase as a style choice.
- Do not use internal capitalization, unless it’s a brand name.
- Do not capitalize a spelled-out acronym, unless it’s a brand name or a proper
noun.

## Writing API references ¶

When you are documenting an API, you should provide a complete reference,
typically by generating it from the source code through documentation comments.

### Basics ¶

An API reference must provide a description of:

- every type: classes, structures, unions, interfaces, and enumerations
- every public field inside classes, unions, and structures
- every virtual function inside classes and interfaces
- every member of an enumeration
- every constant
- every function and method
- every signal and property defined by a class or an interface

For every type there should be a short (25-50 lines of code) example on how to
use the type.

### Typography ¶

- All type, function, signal, and property names should use code style .
- String literals, like XML elements and attributes, should use code style .
- Parameters for functions, methods, and signals should be in italics .
- Type names in the documentation should match the type name in the code Make sure not to pluralize the type name. For instance, do not use GtkButtons , and use “ GtkButton instances” instead

### Classes, structures, interfaces, enumerations ¶

Begin the description of a type with a short, unique sentence, briefly stating
the purpose of the type, especially if it cannot be immediately deduced by the
name of the type itself.

Note

The first sentence will typically be used as a summary inside the indices, so
it should be in the range of 10-12 words.

- Do not repeat the name of the type in the first sentence.
- Do not say this class does… or this type will… .

For classes and structures that have public fields, keep the description of each
field as brief as possible.

### Callable symbols ¶

The documentation for callable symbols like methods, functions, and signals
should briefly state the action performed; state the prerequisites and side
effects; detail what kind of errors may be raised; and specify any related API.

You should use the present tense for all descriptions.

- Description . The description should start with a verb describing the
operation, using the name of the symbol as the unstated subject: Adds a
label widget to the notebook page . If the symbol is a getter function and it returns a boolean value,
start with Checks whether… If the symbol is a getter function and returns something other than a
boolean value, start with Gets the… or Retrieves the… If the symbol is a setter function, start with Sets the… If the symbol updates some state, start with Updates the… If the symbol deletes something, start with Deletes the… or Removes the… For constructors, start with Creates a new… For callbacks, start with Called by…
- Parameters . Be as brief as possible in the description. Put any detailed
information in the callable’s description. Start the parameter description with a lower case word Do not end the description with a period Start the parameter description with “a” or “the” Do not repeat the type of the parameter For boolean parameters that describe an action, start the sentence
with if true… or if false… For boolean parameters that describe a state, use the format true if…; false otherwise * In these contexts, do not capitalize “true” and “false”, and do not use code style with constants Do not include or NULL in the description of nullable parameters For parameters using variadic arguments, use the format a list of…
- Return values . Be as brief as possible in the description. Put any
detailed information in the callable’s description. For boolean return values, use the format true if…; false otherwise For any other type, start with The… Do not include or NULL in the description of nullable return values
- Errors . If the function sets a GError make sure to include the
domains and codes that are going to be set in case of error in the
callable’s description.

### Properties ¶

When describing properties include the same information as the setter and getter
functions:

- preconditions , like range of valid values
- side effects , for instance updating a property causes another read-only
property to change state and emit a notification
- default value , in case an object determines the initial state depending
on whether the property was set at construction time or not

### Code examples ¶

Code examples should illustrate how to use a specific API in the most idiomatic
way possible. You might use:

- one-line examples interspersed with text
- short, self-contained examples presenting a single feature
- long examples, presenting multiple features

Provide an introduction about what the example does, and what requirements and
preconditions it has.

Many developers will copy the examples you provide, and use them as a basis for
their own needs. Design code for reuse, and leave comments on what to modify.

Do not include complicated or convoluted code; make examples easy to scan and
follow. Complex examples can be part of tutorials or deep dive articles.

Do not state the obvious inside the comments.

If you have elided a portion of an example for the sake of brevity, make sure to
provide a comment that explains what you removed.

Show the expected output, using images if needed.

Always compile and test the example code.

Make sure the example code follows the best practices for accessibility and
security.

### Deprecations ¶

Whenever some symbol or type is deprecated, specify the replacement and the
version in which the deprecation occurred. If there is no direct replacement,
describe how the reader can achieve a similar result.

## What’s new ¶

In this section we will list the changes in the style guidelines, so you can
easily follow along.

## Related resources ¶

- Microsoft Writing Style Guide
- Google developer documentation style guide
- Apple Style Guide
- Red Hat Technical Writing Style Guide
- Gov.UK Writing API reference documentation
