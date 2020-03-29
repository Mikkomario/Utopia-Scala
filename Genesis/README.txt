UTOPIA GENESIS
--------------


Required Libraries
------------------
    - Utopia Flow
    - Utopia Inception


Purpose
-------

    Utopia Genesis is the first place to look for tools for any client side GUI. Special support is added for
    game-like programs with real-time asynchronous painting and action handling. Even non-real-time programs
    benefit from various 2D shape representations.


Main Features
-------------

    2D Shapes with 3D Vectors
        - Greatly simplifies advanced vector mathematics
        - Scala-friendly Support for basic everyday tools like Point, Size and Bounds
        - More advanced shapes from Line to triangular and rectangular shapes to advanced 2D polygonic shapes
        - Affine transformations (translation, rotation, scaling, shear) that fully support all of these shapes
        - Vector projection fully supported with shape intersection and collision handling in mind
            - This feature is extended in the Utopia Conflict project
        - Typeless value support for the basic shapes
        - Supporting classes for velocity and acceleration included for movement vectors

    Advanced 2D graphics with Drawable trait
        - Easy painting of all 2D shapes
        - Real-time asynchronous drawing events dispatched through standardized handling system
        - Multi-perspective drawing with affine transformations and Camera trait
        - Easy to setup with Canvas and MainFrame

    Advanced Mouse and Keyboard events
        - Handleable and Handler design style
        - Both mutable and immutable implementations provided
        - Listeners can be configured to receive only certain subset of events
        - Separate handling of mouse movement, mouse buttons and keyboard buttons
        - Easy to setup with CanvasMouseEventGenerator and ConvertingKeyListener

    Real-time asynchronous action events
        - Non-locked frame rate while still keeping standard program "speed"
        - Won't break even when frame rate gets low (program logic prioritized over slowdown, customizable)
        - Simple to use with Actor and ActorHandler traits
        - Easy to setup with ActorLoop

    New Color representation
        - Built-in support for both RGB and HSL color styles
        - Various color transformations
        - Implicit conversions between different styles
        - Alpha support

    Images and image processing
        - New Image class for immutable image representation
        - Image transformations for various image-altering operations
        - Full support for image scaling and resizing simply by using affine transformations at draw time


Usage Notes
-----------

    When setting up a Genesis program, you can either use DefaultSetup or create your own and follow these instructions
    (observe DefaultSetup source code when necessary)

        When using typeless values, please call GenesisDataType.setup() on program startup (replaces DataType.setup() from Flow)

        ActorLoop and Canvas / RepaintLoop must be started separately in an asynchronous execution context (calling .startAsync()).
        You can use ThreadPool from Flow to create an execution context.

        CanvasMouseEventGenerator must be added to an ActorHandler before it will work.

    When handling polygons, it's often better to accept Polygonic instead of Polygon to support wider range of classes.


Available Extensions
--------------------

    utopia.genesis.util.Extensions
        - Approximately equals (~==) support for Double


v2.1 ------------------------------------

    New Features
    ------------

        Functional constructors added for all mouse listeners and key listeners

        MouseButtonStateEvent and MouseWheelEvent now contain value isConsumed that specifies whether a listener
        has consumed the event already.

        New RGB, HSL and Color classes to better handle colors

        New Image and image transformation classes

        Various path funtions, including Bezier curves

        Utility constructors added to Angle for HSL color support

        Velocity & Acceleration classes added

        Couple new utility filters in KeyStateEvent

        New methods in Drawer for text drawing


    Changes & Updates
    -----------------

        java.time.Duration replaced with scala.concurrent.FiniteDuration in Actor, MouseMoveListener and MouseEvent

        MouseButtonStateListeners and MouseWheelListeners now return a consume event if they consumed incoming events.
        Listener classes now use multiple parameter lists when cosntructing function-based listeners.

        Drawer withColor separated to withColor that works with utopia.genesis.colo.Color class and withPaint
        that works with java.awt.Paint class

        Some utility methods to Size (for determining whether size is zero or negative and for fitting into another size)

        New utility methods added to Bounds

        Unnecessary type parameter removed from ApproximatelyEquatable methods

        Fixed errors from Flow changes

        MouseMoveEvent.velocity now returns a Velocity instead of a Vector3D, also transition now returns a
        Vector3D instead of a Point

        Changed standardized cubic bezier to a more generic standaradized velocity path

        VectorLike.combineWith(...) -syntax changed to include two parameter lists

        Point from map creation altered to accept other super types of Axis2D as keys

        map(Axis, Double => Double) was replaced with mapAxis(Axis)(Double => Double). The previous implementation is
        deprecated and will be removed in a future release.

        Drawer text drawing now uses anti-alising for better draw quality


    Fixes
    -----

        Mouse event coordinates were incorrect in dialogs (windows with parent components). Now fixed.


    Required Libraries
    ------------------
        - Utopia Flow v1.6.1+
        - Utopia Inception v2+


v2  ----------------------------------------

    New Features
    ------------

        New classes for 2D shapes
            - Size
            - Point
            - Axis & Axis2D
            - Bounds
            - Rectangle
            - Parallelogram
            - Triangle
            - Polygon

        New traits for polygon shapes
            - Polygonic
            - Parallelogramic
            - Rectangular

        VectorLike trait for Vector3D, Point and Size

        Immutable Handler versions

        getX methods added to GenesisValue

        Setup class for quickly setting up Genesis environment


    Updates & Changes
    -----------------

        Major refactoring in shape classes
            - Line & Bounds refactored
            - Vector3D, Point and Size now have a common trait VectorLike
            - Some parameters and results were changed from Vector3D to Point or Size

        Major refactoring in Handler classes
            - Handler classes moved to utopia.genesis.handling
            - ActorHandler refactored & separated to mutable & immutable versions
            - DrawableHandler separated to mutable & immutable versions
            - Same with all MouseEvent and KeyEvent handlers

        Handleable classes refactored
            - Traits do not specify mutability by default
            - Mutable versions added to utopia.genesis.handling.mutable
            - Added computed accessors for handling states

        Actor act(Double) replaced with act(Duration)

        ActorThread replaced with ActorLoop

        WaitUtil removed (was already deprecated)

        KeyStatus class refactored. The constructor is now private.

        KeyStateEvent filter methods renamed

        MouseButtonStatus class refactored. The constructor is now private.

        Mouse Event class changed to trait

        WindowKeyEventGenerator replaced with ConvertingKeyListener

        Canvas asynchronous refreshing separated to a new loop class (RepaintLoop)

        Deprecated methods removed from Vector3D. Some new deprecations.

