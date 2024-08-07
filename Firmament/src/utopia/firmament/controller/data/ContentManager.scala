package utopia.firmament.controller.data

import utopia.firmament.component.display.{Refreshable, RefreshableWithPointer}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer

/**
  * ContentManagers update content on a component. Please note that when using ContentManagers, you shouldn't modify
  * the underlying displays through other means. Implementing classes should call <i>setup()</i> on initialization
  * @author Mikko Hilpinen
  * @since 22.5.2019, Reflection v1+
  * @tparam A Type of item displayed in a single display component
 *  @tparam C Type of display component
  */
trait ContentManager[A, C <: Refreshable[A]] extends ContentDisplayer[A, C, EventfulPointer[Seq[A]]]
	with RefreshableWithPointer[Seq[A]]
{
	/**
	  * Replaces a single item in content with a new version. This is only useful when content has mutable states.
	  * @param oldItem Item to be replaced
	  * @param newItem The item that will replace the old item
	  */
	def replace(oldItem: A, newItem: A) = {
		content.findIndexWhere { representSameItem(oldItem, _) }
			.foreach { targetIndex => content = content.updated(targetIndex, newItem) }
	}
	
	/**
	  * Updates a single item in this display's content (only useful when managed content has mutable state)
	  * @param item Item to be updated
	  */
	def updateSingle(item: A) = {
		content.findIndexWhere { representSameItem(item, _) } match {
			case Some(index) =>
				// val targetedDisplay = displays(index)
				// targetedDisplay.content = item
				content = content.updated(index, item)
			case None => content :+= item
		}
	}
}
