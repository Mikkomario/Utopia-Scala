package utopia.scribe.core.model.stored.management

import utopia.scribe.core.model.factory.management.CommentFactoryWrapper
import utopia.scribe.core.model.partial.management.CommentData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}

object Comment extends StandardStoredFactory[CommentData, Comment]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = CommentData
}

/**
  * Represents a comment that has already been stored in the database
  * @param id   id of this comment in the database
  * @param data Wrapped comment data
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class Comment(id: Int, data: CommentData) 
	extends StoredModelConvertible[CommentData] with FromIdFactory[Int, Comment] 
		with CommentFactoryWrapper[CommentData, Comment]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: CommentData) = copy(data = data)
}

