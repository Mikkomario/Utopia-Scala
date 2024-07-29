# Utopia Reach
A JVM GUI library with minimum reliance on Swing

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)
- [Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis)
- [Utopia Firmament](https://github.com/Mikkomario/Utopia-Scala/tree/master/Firmament)

## Main Features
Swing-independent component context
- The whole component hierarchy is hosted within a single empty swing component. 
  There are no other Swing dependencies (except when dealing with windows).
- Includes a new focus system and a paint system, and uses the mouse and keyboard event system from **Genesis**

Dynamic stack-based component layout
- Components are placed in dynamically sized stacks and containers that react to changes in 
  content and hierarchy structure
- This allows for bottom-to-top size changes, where container components react to changes in the lower components

Top-to-bottom and bottom-to-top component creation
- You can create the whole component hierarchy by using immutable, declarative structures (top-to-bottom)
- Also supports mutable components and assigning components to hierarchies after they have been created (bottom-to-top)
- Component creation context (settings and parameters) can be passed down to the whole hierarchy and 
  adjusted during the creation process

Dynamic custom components
- Material Design -inspired base components like 
  [TextField](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/text/TextField.scala) and 
  [Switch](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/check/Switch.scala)

Template traits and models for building form dialogs

OS drag-and-drop support
- Allows your components to react to events where the user, for example, 
  drags and drops a file into your application window
- See [DragAndDropManager](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/dnd/DragAndDropManager.scala) 
  for more details

## Implementation Hints
There are three ways in which you can use this library:
1. Reach only (recommended) - Start with 
  [ReachWindow](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/window/ReachWindow.scala) 
  and build the layout downwards from there
2. Reach in Reflection (requires a separate module) - Use this approach to migrate from **Reflection** to **Reach**. 
  See [Reach-in-Reflection](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach-in-Reflection) module for details.
3. Reach inside AWT or Swing (not recommended) - It is possibly to use **Reach** inside an AWT component system, 
  but you will lose many of the auto-layout benefits.

There are usually three types of container and component implementations:
- **Immutable** - For components and containers which are static and don't change (very safe)
- **View** - For components and containers which reflect changes in one or more pointers (flexible & controlled)
- **Mutable** - For components and containers that allow mutation of their contents (easy but dangerous)

### Classes to be aware of
You will likely use these classes very often:
- [ReachCanvas](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/container/ReachCanvas.scala) - 
  This component holds the whole component hierarchy
- **ReachWindow** - Use this interface for constructing windows that consist of a single **ReachCanvas** instances
- [ReachWindowContext](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/context/ReachWindowContext.scala) 
  and [ReachContentWindowContext](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/context/ReachContentWindowContext.scala) - 
  Use these context classes when using **ReachWindow**
- [Open](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/wrapper/OpenComponent.scala) - 
  For creating components that haven't yet been attached to the component hierarchy
- [ComponentCreationResult](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/wrapper/ComponentCreationResult.scala) 
  and [ComponentWrapResult](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/wrapper/ComponentWrapResult.scala)
  - While these are typically implicitly constructed, you may sometimes need to declare one explicitly when 
    building component layouts. Either way, you should be familiar with these classes. 
- [Mixed](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/factory/Mixed.scala) 
  - When building stacks or other containers that contain multiple components, you oftentimes want the 
    container to contain different kinds of components. In these cases you use **Mixed** instead.
    - I.e. Typically, you would write something like: 
      `stack.build(Label).apply(...) { labelFactory => labelFactory.apply("Hello") }`. 
      With **Mixed** you write instead: `stack.build(Mixed).apply(...) { factories => factories(Label).apply("Hello") }`
- [CursorSet](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/cursor/CursorSet.scala) - 
  When you want to use custom cursors, please specify one of these in **ReachWindowCreationContext**, 
  or directly to the **ReachCanvas** constructor.
- Please also review the documentation of the **Paradigm** and **Genesis** modules, 
  because you will be using classes from those modules a lot

Here are some built-in components and layout containers you may use:
- Components (for user-interaction)
  - [Labels](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach/src/utopia/reach/component/label)
    - Labels are used for displaying non-interactive text or images to the user
    - There are four main variants: **TextLabel**, **ImageLabel**, **ImageAndTextLabel** and **EmptyLabel**
    - All labels support the immutable and view -approaches, and some support the mutable approach 
    - There are also interactive variants, namely **SelectableTextLabel** and **EditableTextLabel**
  - [Buttons](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach/src/utopia/reach/component/button)
    - Buttons allow the user to perform some predefined action when pressed
    - There are Text, Image and ImageAndText - button variants available, 
      depending on what kind of content you want to display
    - Buttons support all three component principles (immutable, view and mutable)
  - **TextField**
    - This class allows the user to input various kinds of text or numeric values
    - Supports input validation, maximum input length and much more
  - [CheckBox](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/check/CheckBox.scala) 
    and **Switch**
    - These allow the user to select boolean values
  - [RadioButtonGroup](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/selection/RadioButtonGroup.scala)
    - This allows the user to select from a limited number of options
  - [SelectionList](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/selection/SelectionList.scala) 
    and [DropDown](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/selection/DropDown.scala)
    - These allow the user to select from a list of values
    - SelectionList displays the list within the component while DropDown opens a separate window
  - [DurationField](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/input/text/DurationField.scala) 
    allows for time and duration input
  - **DrawableCanvas** allows you to draw **Drawable** items (from **Genesis**) in real time in a GUI context
- [Containers](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach/src/utopia/reach/container) (for building layouts)
  - **Framing** - Places margins/insets around a single component
  - **AlignFrame** - Aligns a component to specific direction
  - **CachingViewSwapper** - Switches the displayed component based on a pointer value
  - **ScrollView** and **ScrollArea** for 1D and 2D scrolling
  - **Stack** for placing components in a column or in a row
    - Supports all three component principles: immutable, view and mutable
  - **Collection** for placing components in multiple automatically managed columns or rows
  - **Layers** for placing components behind or on top of each other

You will likely need these classes and traits when building your custom components and layout containers:
- [CustomDrawReachComponent](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/template/CustomDrawReachComponent.scala) - 
  Your most typical component class when building from ground-up
  - Oftentimes, however, you will be using 
    [ReachComponentWrapper](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/template/ReachComponentWrapper.scala) 
    instead, because most components are simply 
    combinations of other components (such as labels)
- [ButtonLike](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/template/ButtonLike.scala) - 
  Common trait for button implementations (including checkboxes and the like)
- [Focusable](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/template/focus/Focusable.scala) - 
  When you want your component to be recognized by the focus system
  - Sometimes you may wish to implement the more specific **FocusableWrapper** instead
- [CursorDefining](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/component/template/CursorDefining.scala) - 
  Implement when you want a hover-cursor over your component

When you've built your custom component, you should also build a set of factories for it.  
For this you will need:
- **BaseContextualFactory**, **ColorContextualFactory**, **TextContextualFactory** or some other **ContextualFactory**
  - Extending one of these traits allows you to build components using context instances
  - In case you're building layout containers, where you don't know or don't want to limit the exact context type 
    being used, use **GenericContextualFactory** instead
- Your component companion object should extend **ComponentFactoryFactory** (Cff), 
  **FromContextComponentFactoryFactory** (Ccff) or **FromGenericContextComponentFactoryFactory** (Gccff)
  - When extending **ComponentFactoryFactory**, the generated factory class (another custom class) should 
    extend either **FromContextFactory** or **FromGenericContextFactory**
    - This will enable implicit conversions from your companion object into Ccff or Gccff
    - This is assuming you want to use the context classes. If you don't, don't have the factory class extend anything.
- Note, it may be useful to utilize the [Reach Coder](https://github.com/Mikkomario/Utopia-Coder/tree/master/Reach-Coder) 
  utility application in order to get a head start in custom component creation.

When building form windows, you will deal with the following classes:
- [InteractionWindowFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/window/InteractionWindowFactory.scala) 
  or [InputWindowFactory](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/window/InputWindowFactory.scala)
  - Extend one of these in the class you use for specifying the windows you wish to create
  - **InteractionWindowFactory** is the more generic version, which doesn't require form layout
- [InputRowBlueprint](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/window/InputRowBlueprint.scala) - 
  Construct one of these for each form field you specify
- [DialogButtonBlueprint](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reflection/src/utopia/reflection/container/swing/window/interaction/DialogButtonBlueprint.scala) 
  (in **Firmament**) - Construct one of these for each dialog button
- [InputField](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/window/InputField.scala)
  - Your form input components will need to be wrapped into **InputFields**
  - `import utopia.reach.window.InputField._` will offer you a number of methods for constructing these wrappers
  - Alternatively you may use the functions within the **InputField** object itself
  - Oftentimes you should have an implicit conversion available
- [FocusRequestable](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/src/utopia/reach/focus/FocusRequestable.scala)
  - Sometimes the input components don't support focus out-of-the-box. In these cases, 
    wrap them using the **FocusRequestable** object.

Other tools available for you:
- **DragAndDropManager**, **DragAndDropTarget** and **DragAndDropEvent**
  - Use these when you want your program to support file drops from the OS file system

### Starting coding
There are many examples available to you under the `utopia.reach.test` package.  
Here's a list of things you should do to get started.

You should set the noddraw property to true (optional)  
`System.setProperty("sun.java2d.noddraw", true.toString)`  
Always call `ParadigmDataType.setup()` at program startup, before doing anything else.

First, define your commonly used settings. See 
[ReachTestContext](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach/test/utopia/reach/test/ReachTestContext.scala) 
for an example of this.  
You should specify at least the following:
- Implicit **Logger** implementation (e.g. **SysErrLogger**)
- Implicit **ExecutionContext** instance (typically **ThreadPool**)
- Implicit default language code (**String**) for localization
  - It is recommended that you use the 2-character ISO-standard language codes
- Implicit **Localizer** implementation (use **NoLocalization** to skip localization)
- An **ActorHandler** instance, and an **ActionLoop** to manage that instance
  - When the program starts, call `.runAsync()` for your **ActionLoop** instance
- Implicit **AnimationContext** and **ScrollingContext** instances, in case you use those
- A **ColorScheme** instance
  - Use `ColorScheme.default ++` your custom color scheme overrides, 
    or build one from ground up.
- Your **Fonts** (one of which will be placed in the **BaseContext** instance)
- A **Margins** instance
  - Hint: You may wish to use `Screen.ppi` and the **Distance** class when 
    defining margins and fonts. This way your application will have a certain 
    visual size, regardless of the screen resolution.
    - For this, import `utopia.paradigm.measurement.DistanceExtensions._`
- A **BaseContext** instance
- A **WindowContext** instance (if using **ReachWindow**)
  - Once you know the window background color, combine these two by 
    calling windowContext`.withContentContext(BaseContext)`

Next, define the data pointers you wish to use throughout or outside the component hierarchy.  
For this, use **EventfulPointer**, **Flag**, or **ResettableFlag**.

Next, build the component layout. Here's an example:
```
// Creates a window, which contains some margins on each side
val window = ReachWindow.contentContextual.using(Framing) { (canvas, framingF) => 
  // A text label and a button are placed on a column (i.e. stack) next to each other
  framingF.build(Stack).apply(margins.aroundMedium) { stackF => 
    stackF.build(Mixed).column(layout = Center) { factories => 
      val label = factories(Label).withTextExpandingToRight.apply("Hello World!")
      val button = factories(TextButton).larger.apply("Press Me!") { println("Hello World!") }
      Vector(label, button)
    }
  }
}
```

When you have a contextual component factory available to you, 
like the above `ReachWindow.contentContextual`, `framingF`, `stackF`, `factories` or `factories(Label)`, 
you may modify that context by calling `.mapContext { ... }`.  
In case of non-generic factories (which is typically all component factories and none of the container factories), 
you may call contextual functions, such as `.larger` or `.against(Color)` directly on the factory.

You may share data from the lower component hierarchy upwards by passing it as an additional component creation 
property. For example, in the above code, you could write `Vector(label, button) -> "Custom string result"`, 
after which you could have accessed that string (or other such object) by calling `window.result`.

Here's an example of using additional component creation results:
```
// framing is actually an ComponentWrapResult, but provides implicit access to the underlying Framing instance
val framing = framingF.build(TextField) { fieldF =>
  val field = fieldF.forString(300 upTo 400)
  // By using a tuple, we define a ComponentCreationResult with a custom result value
  field -> field.textPointer
}
// The custom result is now accessible in the higher scope
val fieldTextPointer = framing.result
```

When you're ready to display the **Window**, call `window.display(centerOnParent = true)`. 
Remember to call `.runAsync()` for your **ActionLoop** before or directly after you do this.  
Oftentimes you also want to modify the window, for example by calling `.setToExitOnClose()`

### Building Layouts Bottom-to-Top
Sometimes you may want to create the component before you define the container it will be placed in.  
In these cases, you need to use the **Open** interface. Here's an example:
```
val openLabel = Open.withContext(textContext).apply(Label) { labelF => 
  labelF.apply("Hello World!")
}
val openFraming = Open.using(Framing) { framingF => 
  framingF.apply(openLabel, insets = margins.aroundSmall)
}
```
When you build your layout top-to-bottom (recommended), you typically use the `.build(...)` option in containers.  
When you build bottom-to-top, you use the factory's `.apply(...)` method or some of its variants. These apply 
functions require the content to be specified in an open form.