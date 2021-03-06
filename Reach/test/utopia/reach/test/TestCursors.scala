package utopia.reach.test

import utopia.genesis.image.Image
import utopia.genesis.shape.shape2D.Point
import utopia.reflection.image.SingleColorIcon
import utopia.flow.util.FileExtensions._
import utopia.reach.cursor.CursorType.{Default, Interactive, Text}
import utopia.reach.cursor.{Cursor, CursorSet, CursorType}

import java.nio.file.Path

/**
  *
  * @author Mikko
  * @since 22.12.2020, v
  */
object TestCursors
{
	private val cursorsDirectory: Path = "Reach/test-images"
	// TODO: Fix cursor setup
	lazy val cursors: Option[CursorSet] = Image.readFrom(cursorsDirectory/"cursor-arrow.png").toOption.map { arrowImage =>
		val arrowCursor = Cursor(new SingleColorIcon(arrowImage.withSourceResolutionOrigin(Point(7, 4))))
		val handImage = Image.readFrom(cursorsDirectory/"cursor-hand.png").toOption.map { i =>
			new SingleColorIcon(i.withSourceResolutionOrigin(Point(9, 1))) }
		val textImage = Image.readFrom(cursorsDirectory/"cursor-text.png").toOption.map { i =>
			new SingleColorIcon(i.withCenterOrigin) }
		
		CursorSet(Vector(Interactive -> handImage, Text -> textImage)
			.flatMap { case (cursorType, cursor) => cursor.map { cursorType -> Cursor(_) } }
			.toMap[CursorType, Cursor] + (Default -> arrowCursor), arrowCursor)
	}
}
