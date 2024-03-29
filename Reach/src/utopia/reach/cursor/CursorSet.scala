package utopia.reach.cursor

import utopia.firmament.image.SingleColorIcon
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.combine.Combinable
import utopia.flow.util.TryCatch
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.cursor.CursorType.Default

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
	 *         Success also contains non-critical cursor-loading failures.
	 */
	def loadIcons(paths: Map[CursorType, (Path, Point)], defaultCursorType: CursorType = Default,
	              drawEdgesFor: Set[CursorType] = Set()): TryCatch[CursorSet] =
	{
		// Reads the images from the specified files
		val readResults = paths.map { case (cType, (path, origin)) =>
			cType -> Image.readFrom(path).map { img =>
				Cursor(SingleColorIcon(img.withSourceResolutionOrigin(origin)), drawEdges = drawEdgesFor.contains(cType))
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
				apply(cursors.toMap, default) -> failures
			}
	}
}

/**
  * A set of cursors to use in an application
  * @author Mikko Hilpinen
  * @since 11.11.2020, v0.1
  */
case class CursorSet(cursors: Map[CursorType, Cursor], default: Cursor)
	extends Combinable[(CursorType, Cursor), CursorSet]
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
	
	
	// IMPLEMENTED  --------------------------
	
	override def +(other: (CursorType, Cursor)): CursorSet = copy(cursors = cursors + other)
	
	
	// OTHER	------------------------------
	
	/**
	  * @param cursorType Targeted cursor type
	  * @return Cursor image that best represents the specified cursor type
	  */
	def apply(cursorType: CursorType) = cursors.get(cursorType) match {
		case Some(cursor) => cursor
		case None => cursorType.backup.flatMap { _apply(_, Set(cursorType)) }.getOrElse(default)
	}
	
	/**
	  * @param cursor A cursor type to remove from this set
	  * @return A copy of this set without that cursor
	  */
	def -(cursor: CursorType) = copy(cursors = cursors - cursor)
	
	private def _apply(cursorType: CursorType, testedTypes: Set[CursorType]): Option[Cursor] = {
		if (testedTypes.contains(cursorType))
			None
		else if (cursors.contains(cursorType))
			cursors.get(cursorType)
		else
			cursorType.backup.flatMap { c => _apply(c, testedTypes + cursorType) }
	}
}
