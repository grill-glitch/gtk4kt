# Leaflets - GNOME Developer Documentation

# Leaflets ¶

A leaflet is a responsive container that behaves like a box when
there is enough space, or as a stack if the width available is
not enough.

```
<object
 
class=
"AdwLeaflet"
 
id=
"leaflet"
>

  
<child>

    
<object
 
class=
"AdwLeafletPage"
>

      
<property
 
name=
"name"
>
beginning
</property>

      
<property
 
name=
"child"
>

        
<!-- ... -->

      
</property>

    
</object>

  
</child>

  
<child>

    
<object
 
class=
"AdwLeafletPage"
>

      
<property
 
name=
"name"
>
end
</property>

      
<property
 
name=
"child"
>

        
<!-- ... -->

      
</property>

    
</object>

  
</child>

</object>
```

## API references ¶

In the examples we used the following classes:

- AdwLeaflet
- AdwLeafletPage
