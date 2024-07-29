# Reach in Reflection
The purpose of this module is to simply enable the use of **Reach** layout system inside the **Reflection** layout system. 
This is intended to be used for existing projects only. New projects should use **Reach** only.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)
- [Utopia Genesis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Genesis)
- [Utopia Firmament](https://github.com/Mikkomario/Utopia-Scala/tree/master/Firmament)
- [Utopia Reflection](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reflection)
- [Utopia Reach](https://github.com/Mikkomario/Utopia-Scala/tree/master/Reach)

## Implementation Hints
This module currently contains only a single class: 
[ReflectionReachCanvas](https://github.com/Mikkomario/Utopia-Scala/blob/master/Reach-in-Reflection/src/utopia/reflection/reach/ReflectionReachCanvas.scala). 
Use this class to construct your **ReachCanvas** instance so that it may be used in **Reflection** context. 
In cases where you build a whole window using only **Reach** components, please use **ReachWindow** instead.