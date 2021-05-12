# Utopia Reach - List of Changes

## v0.2 (an alpha release) - 12.5.2021
An update in the middle of developing layered view implementations. 
Improves paint efficiency but breaks existing package structure 
(which is normal at this stage of development).
### Breaking Changes
- Updated package structuring in utopia.reach.container
- Components must now specify `.transparent: Boolean`, which is used in drawing optimizations
### Other Changes
- Optimized component painting to ignore background elements under opaque components

## v0.1 (an alpha release) - 17.4.2021
See README.md for a list of main features.  
This library can be used as is, but is lacking some key features and will 
experience major changes in the near development.