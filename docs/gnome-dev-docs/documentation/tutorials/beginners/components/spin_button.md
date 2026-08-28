# Spin Buttons - GNOME Developer Documentation

# Spin Buttons ¶

A spin button is a text field that accepts a range of values, with buttons
that allow the value to be increased or decreased by a fixed amount.

- Interface guidelines

```
GtkAdjustment
 
*
spin_adjustment
 
=

  
gtk_adjustment_new
 
(
0.0
,
 
0.0
,
 
100.0
,
 
1.0
,
 
5.0
,
 
0.0
);

GtkWidget
 
*
spin_button
 
=

  
gtk_spin_button_new
 
(
spin_adjustment
,
 
1.0
,
 
0
);
```

```
spin_adjustment
 
=
 
Gtk
.
Adjustment
(
value
=
0
,

                                 
lower
=
0
,

                                 
upper
=
100
,

                                 
step_increment
=
1
,

                                 
page_increment
=
5
,

                                 
page_size
=
0
)

spin_button
 
=
 
Gtk
.
SpinButton
(
adjustment
=
spin_adjustment
,

                             
climb_rate
=
1
,

                             
digits
=
0
)
```

```
var
 
spin_adjustment
 
=
 
new
 
Gtk
.
Adjustment
 
(
0
,
 
0
,
 
100
,
 
1
,
 
5
,
 
0
);

var
 
spin_button
 
=
 
new
 
Gtk
.
SpinButton
 
(
spin_adjustment
,
 
1
,
 
0
);
```

```
const
 
spin_adjustment
 
=
 
new
 
Gtk
.
Adjustment
({

  
value
:
 
0
,

  
lower
:
 
0
,

  
upper
:
 
100
,

  
step_increment
:
 
1
,

  
page_increment
:
 
5
,

  
page_size
:
 
0
,

});

const
 
spin_button
 
=
 
new
 
Gtk
.
SpinButton
({

  
adjustment
:
 
spin_adjustment
,

  
climb_rate
:
 
1
,

  
digits
:
 
0
,

});
```

```
var
 
spinAdjustment
 
=
 
new
 
Adjustment
(
0
,
 
0
,
 
100
,
 
1
,
 
5
,
 
0
);

var
 
spinButton
 
=
 
new
 
SpinButton
(
spinAdjustment
,
 
1
,
 
0
);
```

```
<object
 
class=
"GtkSpinButton"
 
id=
"spin_button"
>

  
<property
 
name=
"adjustment"
>

    
<object
 
class=
"GtkAdjustment"
 
id=
"spin_adjustment"
>

      
<property
 
name=
"value"
>
0
</property>

      
<property
 
name=
"lower"
>
0
</property>

      
<property
 
name=
"upper"
>
100
</property>

      
<property
 
name=
"step-increment"
>
1
</property>

      
<property
 
name=
"page-increment"
>
5
</property>

    
</object>

  
</property>

  
<property
 
name=
"digits"
>
0
</property>

</object>
```

Spin buttons use GtkAdjustment objects as models to determine the range of values they can display. The current value of the
spin button will update the GtkAdjustment:value property, and vice versa.

## Changes in value ¶

You can monitor changes in the spin button’s value by using one of these
signals:

- GtkSpinButton::value-changed
- GtkAdjustment::value-changed
- GObject::notify on the GtkAdjustment:value property

```
// "adj" is defined elsewhere

GtkWidget
 
*
spin_button
 
=
 
gtk_spin_button_new
 
(
adj
,
 
1.0
,
 
0
);

// "on_value_changed" is defined elsewhere

g_signal_connect
 
(
spin_button
,
 
"value-changed"
,

                  
G_CALLBACK
 
(
on_value_changed
),

                  
NULL
);
```

```
# "adj" is defined elsewhere

spin_button
 
=
 
Gtk
.
SpinButton
(
adjustment
=
adj
,
 
climb_rate
=
1.0
,
 
digits
=
0
)

# "on_value_changed" is defined elsewhere

spin_button
.
connect
(
"value-changed"
,
 
on_value_changed
)
```

```
// "adj" is defined elsewhere

var
 
spin_button
 
=
 
new
 
Gtk
.
SpinButton
 
(
adj
,
 
1
,
 
0
);

// "on_value_changed" is defined elsewhere

spin_button
.
value_changed
.
connect
 
(
on_value_changed
);
```

```
// "adj" is defined elsewhere

const
 
spin_button
 
=
 
new
 
Gtk
.
SpinButton
(
adj
,
 
1
,
 
0
);

// "on_value_changed" is defined elsewhere

spin_button
.
connect
(
"value_changed"
,
 
on_value_changed
);
```

```
// "adj" is defined elsewhere

var
 
spinButton
 
=
 
new
 
SpinButton
 
(
adj
,
 
1
,
 
0
);

// "handleValueChanged" is defined elsewhere

spinButton
.
onValueChanged
(()
 
->
 
handleValueChanged
());
```

## Non-numerical spin buttons ¶

Adjustments can only interpolate between numerical values, but spin buttons
can show any type of alphanumerical value, as long as it can be mapped to
the adjustment’s range and current value. Spin buttons can use the GtkSpinButton::input signal to transform the current content of the
spin button into a numerical value, and the GtkSpinButton::output signal
to transform the numerical value of the adjustment into the data to be
displayed.

```
static
 
const
 
char
 
*
values
[]
 
=
 
{

  
"Live"
,

  
"Laugh"
,

  
"Love"
,

};

static
 
const
 
guint
 
n_values
 
=
 
G_N_ELEMENTS
 
(
values
);

static
 
void

transform_spin_input
 
(
GtkSpinButton
 
*
self
,

                      
double
 
*
new_value
,

                      
gpointer
 
user_data
 
G_GNUC_UNUSED
)

{

  
const
 
char
 
*
text
 
=
 
gtk_editable_get_text
 
(
GTK_EDITABLE
 
(
self
));

  
// Search for the allowed values, and map them to their

  
// numerical identifier

  
for
 
(
guint
 
i
 
=
 
0
;
 
i
 
<
 
n_values
;
 
i
++
)

    
{

      
if
 
(
g_strcmp0
 
(
text
,
 
values
[
i
])
 
==
 
0
)

        
{

          
gtk_widget_remove_css_class
 
(
GTK_WIDGET
 
(
self
),
 
"error"
);

          
*
new_value
 
=
 
i
;

          
return
;

        
}

    
}

  
*
new_value
 
=
 
0
;

  
gtk_widget_add_css_class
 
(
GTK_WIDGET
 
(
self
),
 
"error"
);

}

static
 
void

transform_spin_output
 
(
GtkSpinButton
 
*
self
,

                       
gpointer
 
user_data
 
G_GNUC_UNUSED
)

{

  
GtkAdjustment
 
*
adjustment
 
=
 
gtk_spin_button_get_adjustment
 
(
self
);

  
int
 
value
 
=
 
gtk_adjustment_get_value
 
(
adjustment
);

  
g_assert
 
(
value
 
>=
 
0
 
&&
 
value
 
<
 
n_values
);

  
gtk_editable_set_text
 
(
GTK_EDITABLE
 
(
self
),
 
values
[
value
]);

}

// ...

GtkAdjustment
 
*
adj
 
=
 
gtk_adjustment_new
 
(
0
,
 
0
,
 
2
,
 
1
,
 
1
,
 
0
);

GtkWidget
 
*
spin_button
 
=
 
gtk_spin_button_new
 
(
adj
,
 
1.0
,
 
0
);

gtk_spin_button_set_numeric
 
(
GTK_SPIN_BUTTON
 
(
spin_button
),
 
FALSE
);

g_signal_connect
 
(
spin_button
,
 
"input"
,

                  
G_CALLBACK
 
(
transform_spin_input
),

                  
NULL
);

g_signal_connect
 
(
spin_button
,
 
"output"
,

                  
G_CALLBACK
 
(
transform_spin_output
),

                  
NULL
);
```

```
def
 
transform_spin_input
(
spin
):

    
text
 
=
 
spin
.
get_text
()

    
if
 
text
 
in
 
[
"Live"
,
 
"Laugh"
,
 
"Love"
]:

        
spin
.
remove_css_class
(
"error"
)

    
else
:

        
spin
.
add_css_class
(
"error"
)

    
if
 
text
 
==
 
"Live"
:

        
return
 
0

    
elif
 
text
 
==
 
"Laugh"
:

        
return
 
1

    
elif
 
text
 
==
 
"Love"
:

        
return
 
2

    
else
:

        
return
 
0

def
 
transform_spin_output
(
spin
):

    
adj
 
=
 
spin
.
get_adjustment
()

    
value
 
=
 
adj
.
get_value
()

    
if
 
value
 
==
 
0
:

        
spin
.
set_text
(
"Live"
)

    
elif
 
value
 
==
 
1
:

        
spin
.
set_text
(
"Laugh"
)

    
else
:

        
spin
.
set_text
(
"Love"
)

adj
 
=
 
Gtk
.
Adjustment
(
value
=
0
,
 
lower
=
0
,
 
upper
=
2
,

                     
step_increment
=
1
,

                     
page_increment
=
1
,

                     
page_size
=
0
)

spin_button
 
=
 
Gtk
.
SpinButton
(
adjustment
=
adj
,

                             
climb_rate
=
1.0
,

                             
digits
=
0
,

                             
numeric
=
False
)
```

```
var
 
adj
 
=
 
new
 
Gtk
.
Adjustment
 
(
0
,
 
0
,
 
2
,
 
1
,
 
1
,
 
0
);

var
 
spin_button
 
=
 
new
 
Gtk
.
SpinButton
 
(
adj
,
 
1
,
 
0
);

spin_button
.
numeric
 
=
 
false
;

spin_button
.
input
.
connect
 
((
new_value
)
 
=>
 
{

    
string
 
text
 
=
 
spin_button
.
text
;

    
spin_button
.
remove_css_class
 
(
"error"
);

    
if
 
(
text
 
==
 
"Live"
)

        
new_value
 
=
 
0
;

    
else
 
if
 
(
text
 
==
 
"Laugh"
)

        
new_value
 
=
 
1
;

    
else
 
if
 
(
text
 
==
 
"Love"
)

        
new_value
 
=
 
2
;

    
else

        
new_value
 
=
 
0
;

        
spin_button
.
add_css_class
 
(
"error"
);

        
new_value
 
=
 
0
;

});

spin_button
.
output
.
connect
 
(()
 
=>
 
{

    
int
 
value
 
=
 
adj
.
value
;

    
assert
 
(
value
 
>=
 
0
 
&&
 
value
 
<=
 
3
);

    
switch
 
(
value
)
 
{

        
case
 
0
:

            
spin_button
.
text
 
=
 
"Live"
;

        
case
 
1
:

            
spin_button
.
text
 
=
 
"Laugh"
;

        
case
 
2
:

            
spin_button
.
text
 
=
 
"Love"
;

    
}

});
```

```
var
 
values
 
=
 
List
.
of
(
"Live"
,
 
"Laugh"
,
 
"Love"
);

var
 
adj
 
=
 
new
 
Adjustment
(
0
,
 
0
,
 
2
,
 
1
,
 
1
,
 
0
);

var
 
spinButton
 
=
 
new
 
SpinButton
(
adj
,
 
1
,
 
0
);

spinButton
.
setNumeric
(
false
);

spinButton
.
onInput
(
newValue
 
->
 
{

    
String
 
text
 
=
 
spinButton
.
getText
();

    
if
 
(
values
.
contains
(
text
))

        
spinButton
.
removeCssClass
(
"error"
);

    
else

        
spinButton
.
addCssClass
(
"error"
);

    
newValue
.
set
((
double
)
 
values
.
indexOf
(
text
));

});

spinButton
.
onOutput
(()
 
->
 
{

    
int
 
value
 
=
 
(
int
)
 
adj
.
getValue
();

    
assert
 
(
value
 
>=
 
0
 
&&
 
value
 
<=
 
3
);

    
spinButton
.
setText
(
values
.
get
(
value
));

    
return
 
true
;

});
```

## Useful methods for the component ¶

- The set_digits() method controls the precision of the value of a spin
button; you can use the value of 0 if you only need integer values.

## API references ¶

In the examples we used the following classes:

- GtkSpinButton
- GtkAdjustment
