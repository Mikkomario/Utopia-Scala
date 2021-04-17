package utopia.reach.cursor

import utopia.genesis.shape.shape2D.Bounds

/**
  * A set of cursors to use in an application
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
  */
case class CursorSet(cursors: Map[CursorType, Cursor], default: Cursor)
{
	// ATTRIBUTES	--------------------------
	
	/**
	  * Bounds that should (approximately) contain all of the cursor bound variations
	  */
	lazy val expectedMaxBounds = Bounds.around(all.map { _.defaultBounds })
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return All available cursors
	  */
	def all = cursors.values.toSet + default
	
	
	// OTHER	------------------------------
	
	/**
	  * @param cursorType Targeted cursor type
	  * @return Cursor image that best represents the specified cursor type
	  */
	def apply(cursorType: CursorType) = cursors.get(cursorType) match
	{
		case Some(cursor) => cursor
		case None => cursorType.backup.flatMap { _apply(_, Set(cursorType)) }.getOrElse(default)
	}
	
	private def _apply(cursorType: CursorType, testedTypes: Set[CursorType]): Option[Cursor] =
	{
		if (testedTypes.contains(cursorType))
			None
		else if (cursors.contains(cursorType))
			cursors.get(cursorType)
		else
			cursorType.backup.flatMap { c => _apply(c, testedTypes + cursorType) }
	}
}
