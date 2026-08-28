# Images - GNOME Developer Documentation

# Images ¶

![../../../_images/image.png](https://developer.gnome.org/documentation/_images/image.png)

GtkPicture is meant to be used with images using their natural size; for
fixed size images, like icons, you should use GtkImage instead.

```
// create an image and set the content of the image using the gnome-image.png file

GtkWidget
 
*
image
 
=
 
gtk_picture_new_for_filename
 
(
"gnome-image.png"
);
```

```
# create an image

image
 
=
 
Gtk
.
Picture
()

# set the content of the image using the gnome-image.png file

image
.
set_filename
(
"gnome-image.png"
)
```

```
// create an picture and set the content of the picture using the gnome-image.png file

var
 
picture
 
=
 
new
 
Gtk
.
Picture
.
for_filename
 
(
"gnome-image.png"
);
```

```
// create an picture and set the content of the picture using the gnome-image.png file

const
 
picture
 
=
 
Gtk
.
Picture
.
new_for_filename
(
"gnome-image.png"
);
```

```
// create an picture and set the content of the picture using the gnome-image.png file

var
 
picture
 
=
 
Picture
.
forFilename
(
"gnome-image.png"
);
```

Important

If the image file is not loaded successfully, the image will contain a
“broken image” icon. The gnome-image.png file needs to be in the
current directory for this code to work.

## Useful methods for a Picture widget ¶

- set_keep_aspect_ratio(True) will size the GtkPicture widget so that
the aspect ratio of the image will be maintained when resizing the widget.
- set_alternative_text("An image of a 1000 words") will set the string
“An image of a 1000 words” as the textual representation of the image; this
text will be used by accessibility tool.

## API references ¶

- GtkPicture
- GtkImage
