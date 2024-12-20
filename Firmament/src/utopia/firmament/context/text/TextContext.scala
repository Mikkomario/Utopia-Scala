package utopia.firmament.context.text

import utopia.firmament.context.color.ColorContext

/**
  * Common trait for text context implementations
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
  */
trait TextContext extends ColorContext with TextContextCopyable[TextContext]
