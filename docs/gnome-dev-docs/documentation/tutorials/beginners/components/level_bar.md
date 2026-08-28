# Level Bars - GNOME Developer Documentation

# Level Bars ¶

Level bars are used as level indicators, for instance:

- signal strength
- password strength
- battery charge level

## Offsets ¶

Level bars have separate states at different offsets. Each offset has a name
and a threshold.

```
GtkWidget
 
*
level
 
=
 
gtk_level_bar_new
 
();

// 0%-50%: Poor

gtk_level_bar_add_offset_value
 
(
GTK_LEVEL_BAR
 
(
level
),

                                
"poor"
,

                                
0.0
);

// 50%-75%: Average

gtk_level_bar_add_offset_value
 
(
GTK_LEVEL_BAR
 
(
level
),

                                
"average"
,

                                
0.50
);

// 75%-100%: Good

gtk_level_bar_add_offset_value
 
(
GTK_LEVEL_BAR
 
(
level
),

                                
"good"
,

                                
0.75
);
```

```
level
 
=
 
Gtk
.
LevelBar
()

# 0%-50%: Poor

level
.
add_offset_value
(
"poor"
,
 
0.0
)

# 50%-75%: Average

level
.
add_offset_value
(
"average"
,
 
0.5
)

# 75%-100%: Good

level
.
add_offset_value
(
"good"
,
 
0.75
)
```

```
var
 
level
 
=
 
new
 
Gtk
.
LevelBar
 
();

// 0%-50%: Poor

level
.
add_offset_value
 
(
"poor"
,
 
0.0
);

// 50%-75%: Average

level
.
add_offset_value
 
(
"average"
,
 
0.5
);

// 75%-100%: Good

level
.
add_offset_value
 
(
"good"
,
 
0.75
);
```

```
const
 
level
 
=
 
new
 
Gtk
.
LevelBar
();

// 0%-50%: Poor

level
.
add_offset_value
(
"poor"
,
 
0.0
);

// 50%-75%: Average

level
.
add_offset_value
(
"average"
,
 
0.5
);

// 75%-100%: Good

level
.
add_offset_value
(
"good"
,
 
0.75
);
```

```
var
 
level
 
=
 
new
 
LevelBar
();

// 0%-50%: Poor

level
.
addOffsetValue
(
"poor"
,
 
0.0
);

// 50%-75%: Average

level
.
addOffsetValue
(
"average"
,
 
0.5
);

// 75%-100%: Good

level
.
addOffsetValue
(
"good"
,
 
0.75
);
```

## Useful methods for the component ¶

- You can use the set_inverted() method to have the level bar grow from
right to left.

## API references ¶

In the examples we used the following classes:

- GtkLevelBar
