package utopia.firmament.context.base

import utopia.firmament.context.color.ColorContext2

/**
  * Common trait for component contexts which specify general information.
  * This is the common trait between the static and the variable base context variant.
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait BaseContext2 extends BaseContextCopyable[BaseContext2, ColorContext2]