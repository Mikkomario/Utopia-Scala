# Reach in Reflection
The purpose of this module is to simply enable the use of **Reach** layout system inside the **Reflection** layout system. 
This is intended to be used for existing projects only. New projects should use **Reach** only.

## Parent Modules
- Flow
- Paradigm
- Inception
- Genesis
- Firmament
- Reflection
- Reach

## Implementation Hints
This module currently contains only a single class: **ReflectionReachCanvas**. 
Use this class to construct your **ReachCanvas** instance so that it may be used in **Reflection** context. 
In cases where you build a whole window using only **Reach** components, please use **ReachWindow** instead.