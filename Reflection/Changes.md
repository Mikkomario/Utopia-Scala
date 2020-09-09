# Utopia Reflection - List of Changes
## v1.3 (beta)
### Breaking Changes
- Moved some of the clases in utopia.reflection.shape -package to separate sub-packages
### New Features
- Added LoadingView and LoadingWindow
    - Includes a new ProgressState class for representing background process progress
- Added ExpandingLengthModifier and NoShrinkingConstraint
### New Methods
- Added && and map functions to StackLengthModifier and StackSizeModifier
### Fixes
- Added missing focus gain to InputWindow when a component's value needs to be fixed
### Other Changes
- Frame.title and Dialog.title now default to empty strings in constructors
- MultiLineTextView now extends AwtContainerRelated

## v1.2
### Scala
- Module is now based on Scala v2.13.3
### Breaking Changes
- Major package restructuring
- ComponentContext and ComponentContextBuilder were replaced with new implementations. 
Existing components don't support old context types anymore.
- Container add methods now target specific indices
    - All custom containers need to be adjusted accordingly
- ContainerManager now only updates non-equal rows on content update 
and will insert rows where new content is inserted in the vector
    - All subclasses of ContainerManager need to be inspected since there are multiple 
    changes and new methods (most notably two different equality check levels).
    - All usages of ContainerManager, SelectionManager, and DropDownLike components 
    should also be re-examined in case equality checks or other features need to be adjusted.
        - Many classes received contentIsStateless -parameter which determines one or two 
        equality checks should be made (defaults to 1)
- DisplayFunction.interpolating -methods now use new string interpolation style and 
multiple parameter lists.
- LocalStringLike.interpolate renamed to .interpolated
    - Also, removed vararg-version of interpolation because of overlap 
- Rewrote CollectionView. The new implementation doesn't support negative directions and takes an 
Axis2D as a parameter instead of Direction2D.
- Removed useLowPriority parameter from AlignFrame. Now all frames expand the content when necessary.
- Moved Insets and Screen classes from Reflection to Genesis
- Changed constructor parameter ordering in Switch
- Refactored color handling in context classes
### Deprecations
- StackSelectionManager was deprecated. You should now use ContainerSelectionManager instead.
    - ContainerSelectionManager supports a wider range of containers.
- SegmentedRow and SegmentedGroup were deprecated since new implementations (SegmentGroup) were added
### Major Changes
- DropDownLike now uses animated stack instead of normal stack in its pop-up view.
### Fixes
- TextLabel now calls repaint upon layout update 
- Made StackHierarchyManager more thread-safe, although there are still probably non-throwing issues
### New Features
- SegmentGroup
    - Replaces old SegmentedRow and SegmentedGroup implementations by wrapping components instead of 
    imitating a stack. Wrapped components may then be placed within stacks or other containers.
    - Used by creating a new SegmentGroup and by calling .wrap(...) for each generated row of components
    - Segment dependency management is now automatic and you don't need to manually register or unregister 
    components from the segments.
- AnimatedStack
    - Animates new component additions and component removals
- AnimatedCollectionView
    - Animates new component additions and component removals
    - Row addition animations don't yet work as they should since the animated image is not updated.
- AnimatedSizeContainer
    - By wrapping components with this container, you will be able to 
    use animated content resizes.
- AnimatedVisibility
    - By wrapping a component in AnimatedVisibility, you can animate component 
    show & hide events.
- AnimatedTransition
    - An appearance / disappearance transition for a component. 
    Used in AnimatedVisibility, which is oftentimes easier to use. 
        - AnimatedTransition is better, however, when you only need a singular transition.
- MappingContainer, WrappingContainer & AnimatedChangesContainer for wrapping other containers and adding 
animations.
- CollectionViewLike trait for adding custom collection views for both Swing and non-swing approaches
- ContentDisplayer and ContainerContentDisplayer for features similar to ContentManager, except read-only.
- AnimationLabel
    - Can be used for animated drawing (Sprites, rotating images etc.)
- InteractionWindow traits and classes for window-based user interaction
- Added smoother animations to Switch and included same animations to ProgressBar
- Added AnimationLabel
- Added AnimatedSwitchPanel
### New Methods
- AwtComponentWrapper: toImage
    - Draws a component to an image. This feature is also available via 
    ComponentToImage object.
- LocalStringLike.interpolate(Map)
    - String interpolation is now available for key value pairs and new string syntax. 
### Other Changes
- ContainerContentManager now accepts any MultiContainer with Stackable and not just MultiStackContainers
- TextFields now downscale their prompts if they would not fit into bounds.
- Tweaks to component coloring (Eg. Button hover color)
- Component creation and context class default values are now defined in ComponentCreationDefaults