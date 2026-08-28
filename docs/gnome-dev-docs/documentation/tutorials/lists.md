# Using GLib Lists - GNOME Developer Documentation

# Using GLib Lists ¶

GLib provides several container types for sets of data: GList , GSList , GPtrArray and GArray .

It has been common practice in the past to use GList in all situations where
a sequence or set of data needs to be stored. This is inadvisable: you should
always prefer a GPtrArray in most situations. GPtrArray has lower memory
overhead (a third to a half of an equivalent list), better cache locality, and
the same or lower algorithmic complexity for all common operations. The only
typical situation where a GList may be more appropriate is when dealing with
ordered data, which requires expensive insertions at arbitrary indexes in the
array. If your ordered data set is large, though, you may want to use GSequence , which is a much more efficient data store.

If linked lists are used, be careful to keep the complexity of operations on
them low, using standard CS complexity analysis. Any operation which uses g_list_nth() or g_list_nth_data() is almost certainly wrong.

For example, iteration over a GList should be implemented using the linking
pointers, rather than a incrementing index:

```
// some_list is a GList defined elsewhere

for
 
(
GList
 
*
l
 
=
 
some_list
;
 
l
 
!=
 
NULL
;
 
l
 
=
 
l
->
next
)

  
{

    
YourDataType
 
*
element_data
 
=
 
l
->
data
;

    
// Do something with @element_data

  
}
```

```
// some_list is a GList defined elsewhere

for
 
(
List
 
l
 
=
 
some_list
;
 
l
 
!=
 
null
;
 
l
 
=
 
l
.
next
)
 
{

    
YourDataType
 
element_data
 
=
 
l
.
data
;

    
// Do something with @element_data

}

// or

foreach
 
(
YourDataType
 
element_data
 
in
 
some_list
)
 
{

    
// Do something with @element_data

}
```

Using an incrementing index instead results in a quadratic decrease in
performance (O(N²) rather than O(N)):

```
// some_list is a GList defined elsewhere

// g_list_length() will iterate the list to determine its length

for
 
(
guint
 
i
 
=
 
0
;
 
i
 
<
 
g_list_length
 
(
some_list
);
 
i
++
)

  
{

    
// g_list_nth_data() will iterate the list to retrieve the data

    
YourDataType
 
*
element_data
 
=
 
g_list_nth_data
 
(
some_list
,
 
i
);

    
// Do something with @element_data

  
}
```

```
// some_list is a GList defined elsewhere

// List.length () will iterate the list to determine its length

for
 
(
int
 
i
 
=
 
0
;
 
i
 
<
 
some_list
.
length
 
();
 
i
++
)
 
{

    
// List.nth_data () will iterate the list to retrieve the data

    
YourDataType
 
element_data
 
=
 
some_list
.
nth_data
 
(
i
);

    
// Do something with @element_data

}
```

The performance penalty in the code above comes from g_list_length() and g_list_nth_data() , which both traverse the list to perform their operations.

Implementing the above with a GPtrArray has the same complexity as the first
(correct) GList example, but better cache locality and lower memory
consumption, so will perform better for large numbers of elements:

```
// some_array is a GPtrArray defined elsewhere

for
 
(
guint
 
i
 
=
 
0
;
 
i
 
<
 
some_array
->
len
;
 
i
++
)

  
{

    
YourDataType
 
*
element_data
 
=
 
g_ptr_array_index
 
(
some_array
,
 
i
);

    
// Do something with @element_data

  
}
```

```
// some_array is a GPtrArray defined elsewhere

for
 
(
int
 
i
 
=
 
0
;
 
i
 
<
 
some_array
.
length
;
 
i
++
)
 
{

    
YourDataType
 
element_data
 
=
 
some_array
.
get
 
(
i
);

    
// Do something with @element_data

}
```
