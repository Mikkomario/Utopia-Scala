package utopia.reach.cursor

import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.cursor.CursorType.Default
import utopia.reflection.image.SingleColorIcon

import java.nio.file.Path
import scala.util.{Failure, Success}

object CursorSet
{
	/**
	 * Loads a cursor set from a set of icon files
	 * @param paths Paths to the icon files, mapped to the type of cursor they represent.
	 *              Each path is also accompanied by the cursor origin / pointer pixel coordinates.
	 * @param defaultCursorType The cursor type that is considered the default (default = Default)
	 * @return Failure if the default cursor couldn't be loaded. Success otherwise.
	 *         Success contains 2 elements:
	 *          1) Encountered cursor load failures
	 *          2) Resulting cursor set
	 */
	def loadIcons(paths: Map[CursorType, (Path, Point)], defaultCursorType: CursorType = Default) = {
		// Reads the images from the specified files
		val readResults = paths.map { case (cType, (path, origin)) =>
			cType -> Image.readFrom(path).map { img =>
				Cursor(new SingleColorIcon(img.withSourceResolutionOrigin(origin)))
			}
		}
		// The default cursor load must succeed
		readResults.getOrElse(defaultCursorType,
			Failure(new NoSuchElementException(s"No path defined for the default cursor type $defaultCursorType")))
			.map { default =>
				// Other load failures are recorded but not considered critical
				val (failures, cursors) = (readResults - defaultCursorType).divideWith { case (cType, result) =>
					result match {
						case Success(cursor) => Right(cType -> cursor)
						case Failure(error) => Left(error)
					}
				}
				failures -> apply(cursors.toMap, default)
			}
	}
}

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
