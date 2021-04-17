# Utopia Reach

## Parent Modules
- Utopia Flow
- Utopia Inception
- Utopia Genesis
- Utopia Reflection

## Main Features
Swing-independent component context
- The whole component hierarchy is hosted within a single empty swing component. 
  There are no other Swing dependencies (except when dealing with multiple windows).
- Includes a new focus system and a paint system

Dynamic stack-based component layout (same as **Reflection**)
- Components are placed in dynamically sized stacks and containers that react to changes in 
  content and hierarchy structure

Top-to-bottom and bottom-to-top component creation
- You can create the whole component hierarchy by using immutable, declarative structures
- Also supports mutable components and assigning components to hierarchies after they have been created
- Component creation context (settings and parameters) can be passed down to the whole hierarchy

Dynamic custom components
- Material Design -inspired base components like **TextField** and **Switch**

## Implementation Hints
It is recommended to call `System.setProperty("sun.java2d.noddraw", true.toString)` and 
`GenesisDataType.setup()` at the program startup.

You will need to create a **ReachCanvas** instance, place it in a **Frame** or other 
**Utopia Reflection** container. It is also recommended to use **SingleFrameSetup** or other similar 
setup class.

There are usually three types of container and component implementations:
- Immutable - For components and containers which are static and don't change (very safe)
- View - For components and containers which reflect changes in one or more pointers (flexible & controlled)
- Mutable - For components and containers that allow mutation of their contents (easy but dangerous)

### Classes to be aware of
- **ReachCanvas** - This component holds the whole component hierarchy
- **Open** - For creating components that haven't yet been attached to the component hierarchy
- **container** and **component** packages, which contain the pre-built components and component 
  container implementations, including:
    - Buttons
    - Labels
    - TextField, RadioButton & Switch
    - Framing & Stacks
    - ScrollArea & ScrollView