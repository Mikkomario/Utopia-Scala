# Utopia Genesis
Utopia Genesis provides a foundation for interactive GUI applications, focusing on GUI events and drawing.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)

## Main Features

A generic interface for delivering various kinds of events to multiple listeners at once
- Supports grouping of these event-distributors (**Handler**s), so that a listener can be attached to all 
  applicable **Handler**s with a single method call.

Advanced 2D graphics with **Drawable** trait
- Easy painting of all 2D shapes
- Real-time asynchronous drawing events dispatched through standardized handling system
- Multi-perspective drawing using affine transformations and the **Repositioner** class

Advanced mouse and keyboard events
- **Handleable** and **Handler** design style
- Listeners can be configured to receive only certain subset of events by utilizing **Filter**
- Support for a wide range of event types, including:
  - Mouse button events
  - Mouse move events
  - Mouse wheel events
  - Keyboard state change -events
  - Key held down -events
  - Mouse drag -events
  - Mouse over -events

Real-time asynchronous action events
- Non-locked frame-rate while still keeping standard program "speed"
- Won't break even when frame rate gets low (program logic prioritized over slowdown, customizable)
- Simple to use with **Actor** and **ActorHandler** traits
- Easy to set up with **ActionLoop**

Images and image-processing
- New **Image** class for immutable image representation
  - A mutable implementation is also available (see **MutableImage**)
- Image transformations for various image-altering operations such as blurring, sharpening, color transformations, etc.
- Full support for image scaling and resizing simply by using affine transformations at draw-time
- **Strip** class for handling animated images

## Implementation Hints

### What you should know before using Genesis
Most of **Genesis** features are best available by utilizing the **Utopia Reach** GUI library.

### You should get familiar with these classes
- **Image** - When you need to draw images
- **Handlers** - Use this interface to wrap multiple **Handler** implementations and to centralize event-delivery
- **Drawable** - Implement this when you need an object to be drawn in real time
  - Use this in conjunction with **DrawableHandler**
    - This is most easily accessible in **DrawableCanvas** in the **Reach** module
- **KeyStateListener** - Implement this when you wish to receive keyboard events
- **MouseButtonStateListener** - Implement this when you wish to listen to mouse button events
- **MouseMoveListener** - Implement this when you wish to listen to mouse move events
- **Actor** - Implement this when you need an object to receive real-time action or 'tick' events
  - The **Handler** counterparts of **Actor** are **ActorHandler** and **ActionLoop**
- **KeyboardEvents** & **CommonMouseEvents** - When you want to access keyboard- and 
  mouse events outside a Java Window class.
- **Screen** - For accessing display resolution