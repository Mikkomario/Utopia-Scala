package utopia.flow.parse.file

import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

import java.nio.file.Paths

/**
 * Utility functions related to files / paths
 * @author Mikko Hilpinen
 * @since 18.10.2023, v2.3
 */
object FileUtils
{
	// ATTRIBUTES   --------------------
	
	private lazy val dashRegex = Regex.escape('-')
	private lazy val multiDashRegex = dashRegex + dashRegex.oneOrMoreTimes
	private lazy val validFileNamePartRegex = (Regex.letterOrDigit || dashRegex).withinParenthesis.oneOrMoreTimes
	
	/**
	 * The current working directory
	 */
	lazy val workingDirectory = Paths.get("")
	
	
	// OTHER    ------------------------
	
	/**
	 * Removes any special characters from a file name.
	 * Replaces all separators with '-'
	 * @param fileName Original file name
	 * @return Normalized file name
	 */
	def normalizeFileName(fileName: String) = {
		val (namePart, extensionPart) = fileName.splitAtLast(".").toTuple
		val normalizedName = validFileNamePartRegex.matchesIteratorFrom(namePart)
			.filter { _.nonEmpty }.mkString("-")
			.replaceEachMatchOf(multiDashRegex, "-")
			.notStartingWith("-").notEndingWith("-")
		s"$normalizedName${extensionPart.mapIfNotEmpty { ext => s".$ext" }}"
	}
}
