package utopia.logos.model.factory.url

import utopia.logos.model.factory.text.TextPlacementFactoryWrapper

/**
  * Common trait for classes that implement LinkPlacementFactory by wrapping a LinkPlacementFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait LinkPlacementFactoryWrapper[A <: LinkPlacementFactory[A], +Repr] 
	extends LinkPlacementFactory[Repr] with TextPlacementFactoryWrapper[A, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def withLinkId(linkId: Int) = withPlacedId(linkId)
	
	override def withStatementId(statementId: Int) = withParentId(statementId)
}

