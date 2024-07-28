# Utopia Firmament
Utopia module for building GUI layouts. Basis for both **Reach** and **Reflection**.

## Parent Modules
- Flow
- Paradigm
- Genesis

## Main Features
Abstract traits for UI components
- **Component** and it's sub-traits are based on **Paradigm** shapes instead of their awt counterparts
- Supports building swing-based (**Reflection**) and custom (**Reach**) component hierarchies

Stacking layout templates
- Stacking layout system is built to work completely independently of the awt layout system
- Dynamic size specifications are easy to make by utilizing simple classes like **StackLength**,
  **StackSize**, **StackInsets** and **LengthExtensions**

Localization support
- All components that display text are built to support localization
- **LocalString**, **LocalizedString**, **Localizer** and **DisplayFunction** make handling localization easy
- Localization is performed mostly implicitly
- You can skip all localization simply by defining an implicit **NoLocalization** localizer
- Note: No actual localization implementation exists in this module at this time.

Custom drawing
- Contains traits for components to support custom drawing over their boundaries, which allows for animation,
  advanced borders, image overlays etc.
- Custom drawers utilize the easy-to-use **Drawer** class from **Genesis**

Custom component templates
- Contains generic implementations for certain standard layout concepts, 
  such as placing margins around a component (**Framing**), 
  or placing components next to each other (**Stack**)
- See `utopia.firmament.component.container` for details

New window system
- Contains a wrapper (**Window**) for the **JDialog** and **JFrame** classes, which provides a scala-friendly and 
  customizable interface with pointers, futures and computed properties.

Context classes for defining standard component behavior
- **Firmament** provides a number of so-called context classes for defining commonly used values for 
  component constructors
- This makes standardized layout styles easy to implement and use, 
  as context instances are automatically passed to all child component constructors.

Automatic container content management
- **ContentDisplayer** and it's subclasses handle displaying of item lists on your behalf

Interfaces for dealing with the AWT
- **AwtEventThread** object provides an easy way to perform certain operations on the AWT event thread, 
  which is required when dealing with AWT and Swing components
- **AwtComponentExtensions** provides a set of utility functions for AWT components

## Implementation Hints
You will most likely be dealing with **Firmament** via either **Reach** or **Reflection**. 
However, here are some details you should be aware of, whichever approach you choose.

### Available Extensions
- utopia.firmament.model.stack.**LengthExtensions**
  - This set of extensions allows you to easily construct **StackLengths**, **StackSizes** and **StackInsets**, 
    it also adds some **StackSize** -based functions to **Alignment**. 
- utopia.firmament.localization.**LocalString**
  - Importing `LocalString._` will enable automatic localization and localization-related functions for strings
- utopia.firmament.awt.**AwtComponentExtensions**
  - This set of extensions adds a number of utility functions to **java.awt.Component**

### Classes you should be aware of
- **StackLength**, **StackSize** and **StackInsets**
  - You will need these when defining size limits (minimum, optimum and maximum)
- **BaseContext**, **ColorContext**, **TextContext** and **WindowContext**
  - These classes allow you to define 
    repeating settings in a single place, which you can pass along using a single (implicit) parameter
    - Also check the other classes in package `utopia.firmament.context`
- **AwtEventThread**
  - Use this whenever you need to perform and action in the AWT event thread, or to modify any AWT component
- **Window**
  - This will be your main interface with Windows when using **Reach**
    - At this time, **Reflection** still uses an old version of this class
- **LocalString**, **LocalizedString**, **Localizer** and **DisplayFunction**
  - These are your main classes when dealing with localization
  - If you don't want to use localization, define `implicit val localizer: Localizer = NoLocalization`
- **Input**, **Interaction**, **Pool**, **Refreshable**, **Selection** and **Selectable**
  - Inherit these traits in your custom components when you aim to provide either a read-only or a read-write access 
  to either a value, a set of displayed values, or both
- **TextDrawContext**
  - This class defines the basic settings for drawing text
- **SingleColorIcon**
  - This class represents an icon that consists of a single color
  - Many **Reach** and **Reflection** components support icons
- **ContainerContentDisplayer** and **ContainerSingleSelectionManager**
  - These classes handle pointer-based data- and selection management for you
  - I recommend also checking the other classes and traits in package `utopia.firmament.controller.data`
- The drawer classes in `utopia.firmament.drawing`
  - There are a number of drawer classes here to select from. These may be used in components which support 
    custom drawing.
  - These come in 3 categories:
    - Immutable - Static drawers which don't change (e.g. drawing a static background color)
    - View - Drawers which reflect pointers (e.g. drawing the image contained within a pointer)
    - Mutable - Drawers which provide a read-and-write interface
- **StackSizeModifier** and **StackLengthModifier**
  - If you want to override component stack size logic, use these classes 
    (requires the component to implement **Constrainable**)
  - See also the built-in modifiers in` utopia.firmament.model.stack.modifier`
- You will also likely need the following model classes:
  - **Margins** (for **BaseContext**)
  - **StackLayout** (for **Stacks**)
  - **HotKey** (for keyboard support)
  - **RowGroup**, **RowGroups** and **WindowButtonBlueprint** for **Reach** **InputWindow** creation