package utopia.flow.parse.file

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.view.mutable.eventful.Flag

import java.nio.file.Path
import scala.util.Try

/**
  * Iterates over a directory and its sub-directories, their sub-directories and so on.
  * @author Mikko Hilpinen
  * @since 21.9.2022, v1.17
  */
class RecursiveDirectoriesIterator(origin: Path) extends Iterator[Try[(Path, Vector[Path])]]
{
	// ATTRIBUTES   ---------------------
	
	private val originConsumedFlag = Flag()
	// Left side is regular children, right side is sub-directories
	private lazy val children = origin.iterateChildren { _.divideBy { _.isDirectory }.map { _.toVector } }
	private lazy val subDirectoriesIterator = children.toOption.iterator
		.flatMap { case Pair(_, dirs) => dirs.iterator.flatMap { new RecursiveDirectoriesIterator(_) } }
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext = originConsumedFlag.isNotSet || subDirectoriesIterator.hasNext
	
	override def next() = {
		if (originConsumedFlag.set())
			children.map { case Pair(files, _) => origin -> files }
		else
			subDirectoriesIterator.next()
	}
}
