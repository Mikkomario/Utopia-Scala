UTOPIA REFLECTION
-----------------


Required Libraries
------------------
    - Utopia Flow
    - Utopia Inception
    - Utopia Genesis


Purpose
-------

    Utopia Reflection is your go-to framework for Scala program GUI. Utopia Reflection is built upon and practically
    replaces Java Swing framework. All components support modern dynamic layout and a basis is added for standardized
    localization. Various custom components are included as well.

    There is also base level support for real-time, completely non-swing implementation for games etc, although not
    that many components have yet been built for this alternative framework style.


Main Features
-------------

    New UI component style
        - ComponentLike and it's sub-traits are based on Genesis shapes instead of their awt counterparts
        - Completely overhauled and more reliable non-awt event system that relies on Genesis events
        and Inception handling system
        - Completely scala-friendly interface

    Stacking layout framework
        - Stacking layout system is built to work completely independent from the awt layout system
        - Allows both top to bottom and bottom to top layout changes - your layout will dynamically adjust to changes
        in component contents and components will automatically place themselves within their parent component's bounds
        - Dynamic size specifications are easy to make by utilizing simple classes like StackLength, StackSize and
        StackInsets

    Localization support
        - All components that display text are built around localization
        - LocalString, LocalizedString, Localizer and DisplayFunction make handling localization easy
        - Localization is done mostly implicitly behind the scenes
        - You can skip all localization simply by defining an implicit NoLocalization localizer

    Custom drawing
        - Many of the components support custom drawing over their boundaries, which allows for animation,
        advanced borders, image overlays etc.
        - Custom drawers utilize easy to use Drawer class from Genesis

    Custom components
        - Buttons and Labels are provided from the get-go
        - Various easy to use containers like the Stack, Framing and SwitchPanel are also available for component layout
        - Pre-built Frame, Dialog and Pop-Up classes for various types of Windows
        - Advanced selection components like TabSelection, DropDown, SearchFrom and TextField make requesting user
        input simple
        - ScrollView and ScrollArea allow for 1D and 2D scrolling without a lot of know-how or code from your side

    Implicit component build context
        - Contextual constructor options in existing components allow you to skip repetitious style definitions by
        passing an implicit ComponentContext -instance instead
        - This makes standardized layout styles easy to implement and use

    Automatic container content management
        - ContentManager and it's subclasses handle displaying of item lists for you


Usage Notes
-----------

    Please call GenesisDataType.setup() at the beginning of every Reflection App you create. This enables typeless
    values, which are used in many Genesis-originated shapes.

    The following code-template is an easy way to get started with your App and tests:

        // Set up typeless values
        GenesisDataType.setup()

        // Set up localization context
        implicit val localizer: Localizer = NoLocalization // You can specify your own Localizer here

        // Creates component context
        val actorHandler = ActorHandler()
        val baseCB = ComponentContextBuilder(actorHandler, ...)

        implicit val baseContext: ComponentContext = baseCB.result

        val content = ... // Create your frame content here

        implicit val exc: ExecutionContext = new ThreadPool("<your program name>").executionContext
        new SingleFrameSetup(actorHandler, Frame.windowed(content, "<your program name>", Program)).start()

    Usually, when specifying component context, margins etc. it's useful to
    import utopia.reflection.shape.LengthExtensions._ This allows you to create StackLength instances by simply
    writing "14.any", "8.downscaling", "10.downTo(2)" etc. (without quotations).

    Stack container class and .framed(StackInsets, Color (optional)) methods will be very useful when creating your
    component layout, especially Stack.withItems(...) and Stack.buildWithContext(...).

    In case you want to skip localization in some strings, please import utopia.reflection.localization.LocalString._
    and call .noLanguageLocalizationSkipped or .local(...).localizationSkipped on your string.


Available Extensions
--------------------

    utopia.reflection.shape.LengthExtensions
        - Allows one to create StackLength instances by calling .any, .downscaling, .upscaling etc. on doubles or integers.

    utopia.reflection.localization.LocalString
        - Allows one to skip localization for specific strings by calling .noLanguageLocalizationSkipped, .local etc.
        on a string.


v1  -----------------------------------------

Required Libraries
------------------
    - Utopia Flow v1.6.1+
    - Utopia Inception v2+
    - Utopia Genesis v2.1+