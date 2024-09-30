package utopia.firmament.context.color

import utopia.firmament.context.base.BaseContext2

/**
  * Common trait for context instances which specify a background color (view),
  * but may be either variable (i.e. pointer-based) or static.
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
// TODO: Change second param to TextContext2
trait ColorContext2 extends BaseContext2 with ColorContextCopyable[ColorContext2, ColorContext2]