package utopia.reach.context

import utopia.firmament.context.text.TextContext

import scala.language.implicitConversions

/**
  * Common trait for context items that are used for creating Reach Popup windows
  * (that contain or are related with textual elements)
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
trait ReachContentWindowContext
	extends ReachWindowContext with TextContext
		with ReachContentWindowContextCopyable[ReachContentWindowContext, StaticReachContentWindowContext]
