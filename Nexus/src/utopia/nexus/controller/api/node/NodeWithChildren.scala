package utopia.nexus.controller.api.node

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.{Follow, NotFound}

/**
  * A common trait for API nodes that have static child nodes
  * @author Mikko Hilpinen
  * @since 06.11.2025, v2.0, based on ResourceWithChildren written 8.3.2021 for v1.5.1
  */
trait NodeWithChildren[-C] extends ApiNode[C]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Child nodes available under this node
	  */
	def children: Iterable[ApiNode[C]]
	
	
	// IMPLEMENTED	---------------------
	
	override def follow(step: String)(implicit context: C): PathFollowResult[C] = {
		val children = this.children
		children.find { _.name ~== step } match {
			case Some(next) => Follow(next)
			case None =>
				NotFound(s"$step is not a child node of $name. The available options are: [${
					children.map {_.name}.mkString(", ")
				}]")
		}
	}
}
