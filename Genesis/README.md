# Utopia Genesis

## Parent Modules
- Utopia Flow
- Utopia Inception

## Main Features

2D Shapes, Vectors and Matrices
- Greatly simplifies advanced vector mathematics
- Scala-friendly Support for basic everyday tools like **Point**, **Size** and **Bounds**
- More advanced shapes from Line to triangular and rectangular shapes to advanced 2D polygonic shapes
- Affine transformations (translation, rotation, scaling, shear) that fully support all of these shapes
- Vector projection fully supported with shape intersection and collision handling in mind
    - This feature is extended in the **Utopia Conflict** project
- Typeless value support for the basic shapes
- Includes classes for velocity and acceleration that can be used in movement vectors and physics

Advanced 2D graphics with **Drawable** trait
- Easy painting of all 2D shapes
- Real-time asynchronous drawing events dispatched through standardized handling system
- Multi-perspective drawing with affine transformations and **Camera** trait
- Quick set up with **Canvas** and **MainFrame**

Advanced Mouse and Keyboard events
- **Handleable** and **Handler** design style
- Both mutable and immutable implementations provided
- Listeners can be configured to receive only certain subset of events
- Separate handling of mouse movement, mouse buttons and keyboard buttons
- Quick set up with **CanvasMouseEventGenerator** and **ConvertingKeyListener**

Real-time asynchronous action events
- Non-locked frame rate while still keeping standard program "speed"
- Won't break even when frame rate gets low (program logic prioritized over slowdown, customizable)
- Simple to use with **Actor** and **ActorHandler** traits
- Easy to set up with **ActorLoop**

New Color representation
- Built-in support for both RGB and HSL color styles
- Various color transformations
- Implicit conversions between different styles
- Alpha (transparency) support

Images and image processing
- New **Image** class for immutable image representation
- Image transformations for various image-altering operations
- Full support for image scaling and resizing simply by using affine transformations at draw time
- **Strip** class for handling animated images
    
Animation support
- **Animation** and **TimedAnimation** traits for creating new animations
- **AnimatedAffineTransformation** and **TimedTransform** traits for animated mapping functions
- **Animator** trait for real-time animation drawers
- Concrete **SpriteDrawer** implementation for drawing animated images

## Implementation Hints

### What you should know before using Genesis
You can get started quickly by utilizing **DefaultSetup** class. You can also create your own implementation of
**Setup**, in which case I would still recommend you to refer to **DefaultSetup** source code.

### Extensions you should be aware of
- utopia.genesis.util.**Extensions**
    - Provides approximately equals -feature (~==) for doubles

### You should get familiar with these classes
- **Vector2D** & **Point** - Your standard way of representing points in 2D space.
- **Size** & **Bounds** - These size representations are used especially often when dealing with UI components
- **Axis** & **Axis2D** - Many shapes and features refer to this enumeration
- **AffineTransformation** & **LinearTransformation** - When you need to deal with affine 
  transformations (translation, rotation, scaling, etc.)
- **Color** - When you need to deal with colors (replaces java.awt.Color)
- **Image** - When you need to draw images
- **Drawable** - Implement this when you need an object to be drawn in real time
- **KeyStateListener** - Implement this when you wish to receive keyboard events
- **MouseButtonStateListener** - Implement this when you wish to listen to mouse button events
- **MouseMoveListener** - Implement this when you wish to listen to mouse move events
- **Actor** - Implement this when you need an object to receive real-time action or 'tick' events
- **GlobalKeyboardEventHandler** & **GlobalMouseEventHandler** - When you want to access keyboard- and 
  mouse events outside a java window.
- **DefaultSetup** - When you want to create a quick test program
- **Screen** - For accessing display resolution