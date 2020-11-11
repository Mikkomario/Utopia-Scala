package utopia.reflection.cursor

import utopia.genesis.image.Image

/**
  * A set of cursors to use in an application
  * @author Mikko Hilpinen
  * @since 11.11.2020, v2
  */
case class CursorSet(cursors: Map[CursorType, Image], default: Image = Image.empty)
{
	/**
	  * @param cursorType Targeted cursor type
	  * @return Cursor image that best represents the specified cursor type
	  */
	def apply(cursorType: CursorType) = cursors.get(cursorType) match
	{
		case Some(cursor) => cursor
		case None => cursorType.backup.flatMap { _apply(_, Set(cursorType)) }.getOrElse(default)
	}
	
	private def _apply(cursorType: CursorType, testedTypes: Set[CursorType]): Option[Image] =
	{
		if (testedTypes.contains(cursorType))
			None
		else if (cursors.contains(cursorType))
			cursors.get(cursorType)
		else
			cursorType.backup.flatMap { c => _apply(c, testedTypes + cursorType) }
	}
}
