package utopia.reach.test

import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.logging.SysErrLogger
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.cursor.CursorSet
import utopia.reach.cursor.CursorType.{Default, Interactive, Text}

import java.nio.file.Path

/**
  *
  * @author Mikko
  * @since 22.12.2020, v
  */
object TestCursors
{
	private val cursorsDirectory: Path = "Reach/test-images"
	
	val cursors = CursorSet.loadIcons(Map(
		Default -> (cursorsDirectory/"cursor-arrow.png", Point(7, 4)),
		Interactive -> (cursorsDirectory/"cursor-hand.png", Point(9, 1)),
		Text -> (cursorsDirectory/"cursor-text.png", Point(12, 12))
	), drawEdgesFor = Set(Default, Interactive, Text)).log(SysErrLogger)
}
