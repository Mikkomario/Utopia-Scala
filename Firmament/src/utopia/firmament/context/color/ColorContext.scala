package utopia.firmament.context.color

import utopia.firmament.context.base.BaseContext
import utopia.firmament.context.text.TextContext

/**
  * Common trait for context instances which specify a background color (view),
  * but may be either variable (i.e. pointer-based) or static.
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait ColorContext extends BaseContext with ColorContextCopyable[ColorContext, TextContext]