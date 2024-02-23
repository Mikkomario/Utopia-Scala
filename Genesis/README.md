# Utopia Genesis

## Parent Modules
- Utopia Flow
- Utopia Paradigm

## Main Features

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

Images and image processing
- New **Image** class for immutable image representation
- Image transformations for various image-altering operations
- Full support for image scaling and resizing simply by using affine transformations at draw time
- **Strip** class for handling animated images

## Implementation Hints

### What you should know before using Genesis
You can get started quickly by utilizing **DefaultSetup** class. You can also create your own implementation of
**Setup**, in which case I would still recommend you to refer to **DefaultSetup** source code.

### You should get familiar with these classes
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