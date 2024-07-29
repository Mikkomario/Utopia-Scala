# Utopia Reflection
This module provides a scala interface for Swing / AWT components. 
Many of the layout management features are custom-written, and the Swing-dependencies are somewhat limited.

## Notice
Please note that this module is unlikely to receive much development in the future. 
The new **Reach** layout module performs all the same standard functions in a cleaner manner. 
This module still contains more features, such as animation support, however.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)
- [Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis)
- [Utopia Firmament](https://github.com/Mikkomario/Utopia-Scala/tree/master/Firmament)

## Main Features
New UI component style
- ComponentLike and it's sub-traits are based on **Paradigm** shapes instead of their awt counterparts
- Completely overhauled and more reliable non-awt event system that relies on **Genesis** events
and **Inception** handling system
- Completely scala-friendly interface

Stacking layout framework
- Stacking layout system is built to work completely independently of the awt layout system
- Allows both top to bottom and bottom to top layout changes - your layout will dynamically adjust to 
  changes in component contents and components will automatically place themselves within their 
  parent component's bounds
- Dynamic size specifications are easy to make by utilizing simple classes like **StackLength**, 
  **StackSize** and **StackInsets**

Custom components
- Buttons and Labels are provided from the get-go
- Various easy to use containers like the **Stack**, **Framing** and **SwitchPanel** are also 
  available for component layout
- Pre-built **Frame**, **Dialog** and **PopUp** classes for various types of **Window**s
- Advanced selection components like **TabSelection**, **DropDown**, **SearchFrom** and **TextField** 
  make requesting user input simple
- **ScrollView** and **ScrollArea** allow for 1D and 2D scrolling without a lot of know-how or 
  code from your side

Implicit component build context
- Contextual constructor options in existing components allow you to skip repetitious style definitions by
  passing an implicit context instances instead
- This makes standardized layout styles easy to implement and use

## Implementation Hints

#### You should get familiar with these classes
- **SingleFrameSetup** - Lets you get started with your test App as smoothly as possible
  (replaces **DefaultSetup** from Genesis)
- **Stack** - You go-to container when presenting multiple components together
- **TextLabel** - Along with other **Label** classes, these basic components let you draw text, images, etc.
- **TextButton** & **TextAndImageButton** - When you need to create interactive buttons
- **TextField**, **DropDown** & **Switch** - These, among other components, allow user to input data to 
  your program.
- **Framing** - Very useful when you want to surround a component with margins. Often also works by calling
  .framed(...) on a component.
- **ScrollView** - When you need a scrollable view. Check **ScrollArea** when you need 2D scrolling.
- **Dialog**, **Frame** & **PopUp** - In case you need to display multiple windows during a program
- **ReflectionComponentLike** - All components extend this trait so you should at least know what it contains.
- **ReflectionStackable** & **CachingReflectionStackable** - In case you need to write your own components that support 
  stack layout system.
- **ContainerContentDisplayer** - When you need to present a changing list of items in
  a **Stack** or another container.

### Example code to get you started (deprecated)
The following code-template is an easy way to get started with your App and tests:

    // Set up typeless values
    ParadigmDataType.setup()

    // Set up localization context
    implicit val localizer: Localizer = NoLocalization // You can specify your own Localizer here

    // Creates component context
    val actorHandler = ActorHandler()
    val baseCB = ComponentContextBuilder(actorHandler, ...)

    implicit val baseContext: ComponentContext = baseCB.result

    val content = ... // Create your frame content here

    implicit val exc: ExecutionContext = new ThreadPool("<your program name>").executionContext
    new SingleFrameSetup(actorHandler, Frame.windowed(content, "<your program name>", Program)).start()