# Utopia Reflection - List of Changes
## v1.2 (beta)
### Breaking Changes
- Container add methods now target specific indices
    - All custom containers need to be adjusted accordingly
- ContainerManager now only updates non-equal rows on content update 
and will insert rows where new content is inserted in the vector
- DisplayFunction.interpolating -methods now use new string interpolation style and 
multiple parameter lists.
- LocalStringLike.interpolate renamed to .interpolated
    - Also, removed vararg-version of interpolation because of overlap  

### New Features
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

### New Methods
- AwtComponentWrapper: toImage
    - Draws a component to an image. This feature is also available via 
    ComponentToImage object.
- LocalStringLike.interpolate(Map)
    - String interpolation is now available for key value pairs and new string syntax. 