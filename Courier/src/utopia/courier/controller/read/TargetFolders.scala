package utopia.courier.controller.read

import utopia.courier.model.read.FolderPath
import utopia.flow.collection.immutable.caching.LazyTree

import scala.language.implicitConversions

object TargetFolders
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * An implementation that targets the inbox folder (if available)
	 */
	lazy val inbox = apply("INBOX")
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return An implementation that targets all folders
	 */
	def all = TargetAllFolders
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts from a function
	implicit def apply(f: LazyTree[FolderPath] => Iterator[FolderPath]): TargetFolders = new TargetFoldersFunction(f)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param folderPath Targeted folder path
	 * @return An implementation that targets that folder only
	 */
	def apply(folderPath: FolderPath): TargetFolders = new TargetSpecificFolders(Some(folderPath))
	/**
	 * @param folderPath Targeted folder path as a string
	 * @return An implementation that targets that folder only
	 */
	def apply(folderPath: String): TargetFolders = apply(FolderPath(folderPath))
	
	/**
	 * @param folderPaths Targeted folder paths as strings
	 * @return An implementation that targets all specified folders
	 */
	def apply(folderPaths: Iterable[String]): TargetFolders =
		new TargetSpecificFolders(folderPaths.map { FolderPath(_) })
	/**
	 * @param firstFolder First folder to target (as a string)
	 * @param secondFolder Second folder to target (as a string)
	 * @param moreFolders More folders to target, as strings
	 * @return An implementation that targets the specified folders
	 */
	def apply(firstFolder: String, secondFolder: String, moreFolders: String*): TargetFolders =
		apply(Vector(firstFolder, secondFolder) ++ moreFolders)
	
	
	// NESTED   -------------------------
	
	/**
	 * A TargetFolders implementation that targets all available folders
	 */
	object TargetAllFolders extends TargetFolders
	{
		override def apply(folderStructure: LazyTree[FolderPath]): Iterator[FolderPath] = {
			// If the root folder contains sub-folders, returns those
			if (folderStructure.hasChildren)
				folderStructure.navsBelowIterator
			// If sub-folders are not enabled, returns the root folder instead
			else
				Iterator.single(folderStructure.nav)
		}
	}
	
	private class TargetSpecificFolders(folders: Iterable[FolderPath]) extends TargetFolders
	{
		override def apply(folderStructure: LazyTree[FolderPath]): Iterator[FolderPath] = folders.iterator
	}
	
	private class TargetFoldersFunction(f: LazyTree[FolderPath] => Iterator[FolderPath]) extends TargetFolders
	{
		override def apply(folderStructure: LazyTree[FolderPath]): Iterator[FolderPath] = f(folderStructure)
	}
}

/**
 * Common trait for logic implementations that specify the targeted email folder or folders
 * @author Mikko Hilpinen
 * @since 13.10.2023, v1.1
 */
trait TargetFolders
{
	/**
	 * @param folderStructure Available folder structure (lazily initialized)
	 * @return An iterator that yields the targeted folders
	 */
	def apply(folderStructure: LazyTree[FolderPath]): Iterator[FolderPath]
}
