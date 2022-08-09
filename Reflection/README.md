# Utopia Reflection

## Parent Modules
- Utopia Flow
- Utopia Inception
- Utopia Genesis

## Main Features

New UI component style
- ComponentLike and it's sub-traits are based on Genesis shapes instead of their awt counterparts
- Completely overhauled and more reliable non-awt event system that relies on Genesis events
and Inception handling system
- Completely scala-friendly interface

Stacking layout framework
- Stacking layout system is built to work completely independently of the awt layout system
- Allows both top to bottom and bottom to top layout changes - your layout will dynamically adjust to 
  changes in component contents and components will automatically place themselves within their 
  parent component's bounds
- Dynamic size specifications are easy to make by utilizing simple classes like **StackLength**, 
  **StackSize** and **StackInsets**

Localization support
- All components that display text are built around localization
- **LocalString**, **LocalizedString**, **Localizer** and **DisplayFunction** make handling localization easy
- Localization is done mostly implicitly behind the scenes
- You can skip all localization simply by defining an implicit **NoLocalization** localizer

Custom drawing
- Many of the components support custom drawing over their boundaries, which allows for animation,
advanced borders, image overlays etc.
- Custom drawers utilize easy to use **Drawer** class from **Genesis**

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

Automatic container content management
- **ContentDisplayer** and it's subclasses handle displaying of item lists for you

## Implementation Hints

### Extensions you should be aware of
- utopia.reflection.util.**AwtComponentExtensions**
  - Adds new utility methods to awt component classes
- utopia.reflection.shape.**LengthExtensions**
    - Allows you to generate **StackLength** instances simply by writing 4.upscaling, 
      2.downscaling, 8.upTo(10) etc.
- utopia.reflection.localization.**LocalString**
    - Allows one to skip localization for specific strings by calling .noLanguageLocalizationSkipped,
      .local etc. on a string.

#### You should get familiar with these classes
- **SingleFrameSetup** - Lets you get started with your test App as smoothly as possible
  (replaces **DefaultSetup** from Genesis)
- **BaseContext**, **ColorContext**, **TextContext** and **ButtonContext** - You will most
  likely need these to specify common component creation parameters (E.g. Font used). Seek for
  .contextual -constructors in components to utilize these.
- **Stack** - You go-to container when presenting multiple components together
- **StackLength**, **StackSize** & **StackInsets** - Basic building blocks for dynamic sizes used in 
  most components
- **StackLayout** & **Alignment** - These enumerations are used for specifying content placement in 
  **Stacks** and other components.
- **Font** - Replaces java.awt.Font
- **LocalizedString**, **Localizer** & **NoLocalization** - When you need to present localized text
- **TextLabel** - Along with other **Label** classes, these basic components let you draw text, images, etc.
- **TextButton** & **TextAndImageButton** - When you need to create interactive buttons
- **TextField**, **DropDown** & **Switch** - These, among other components, allow user to input data to 
  your program.
- **Framing** - Very useful when you want to surround a component with margins. Often also works by calling
  .framed(...) on a component.
- **ScrollView** - When you need a scrollable view. Check **ScrollArea** when you need 2D scrolling.
- **Dialog**, **Frame** & **PopUp** - In case you need to display multiple windows during a program
- **Refreshable** - One of the many input traits that allows you to display content on a UI component.
- **ComponentLike** - All components extend this trait so you should at least know what it contains.
- **Stackable** & **CachingStackable** - In case you need to write your own components that support 
  stack layout system.
- **CustomDrawer** - In case you need to implement your own custom drawer. It's useful to check the 
  sub-classes as well.
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