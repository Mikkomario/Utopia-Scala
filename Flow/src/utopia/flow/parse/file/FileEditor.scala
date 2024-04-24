package utopia.flow.parse.file

import utopia.flow.collection.mutable.iterator.{OptionsIterator, PollingIterator}

import java.io.PrintWriter

/**
 * A reader / writer which can be used for editing a text-based file. This class only provides an interface
 * for read + write operations, it doesn't handle file opening, writing & replacement itself
 * @author Mikko Hilpinen
 * @since 4.10.2021, v1.12.1
 */
class FileEditor(sourceLinesIterator: PollingIterator[String], writer: PrintWriter)
{
	// COMPUTED ----------------------
	
	/**
	 * @return Whether current line is available (end of file hasn't yet been reached)
	 */
	def hasCurrent = sourceLinesIterator.hasNext
	/**
	 * @return Whether this editor has reached the end of the file
	 */
	def isEndOfFile = !hasCurrent
	
	/**
	 * @return The currently targeted line
	 */
	@throws[NoSuchElementException]("If end of file has been reached")
	def currentLine = sourceLinesIterator.poll
	/**
	 * @return The currently targeted line. None if end of file has been reached.
	 */
	def currentLineOption = sourceLinesIterator.pollOption
	
	/**
	 * @return An iterator that continually reads and returns the next line.
	 *         Whenever next() is called on that iterator, the current line is set to that returned by the call.
	 */
	def nextLineIterator = OptionsIterator.continually { nextLineOption() }
	
	
	// OTHER    ---------------------
	
	/**
	 * Moves to the next line in the source document
	 */
	@throws[NoSuchElementException]("If end of file has been reached")
	def moveToNextLine() = writer.println(sourceLinesIterator.next())
	/**
	 * Moves to the next line in the source document. In case this editor was already at the end of the file,
	 * returns false instead of throwing
	 * @return False if trying to move to the next line after the end of the file
	 */
	def tryMoveToNextLine() = {
		if (isEndOfFile)
			false
		else {
			moveToNextLine()
			true
		}
	}
	/**
	 * Moves forward as long as the specified condition holds true for the current line.
	 * Will either end at the end of file or at the first line that didn't fulfill that condition
	 * @param skipCondition A condition for moving forward
	 * @return Whether there remains lines to target (end of file not reached)
	 */
	def moveToNextLineWhile(skipCondition: String => Boolean) = {
		while (hasCurrent && skipCondition(currentLine)) {
			moveToNextLine()
		}
		hasCurrent
	}
	/**
	 * Moves to the current or next line where the specified condition is met. Won't move if the current line meets the
	 * specified condition.
	 * @param condition A search condition
	 * @return Whether such a line was found
	 */
	def moveToNextLineWhere(condition: String => Boolean) = moveToNextLineWhile { !condition(_) }
	
	/**
	 * Moves to the next line and returns it
	 * @return Line where this editor arrived at
	 */
	@throws[NoSuchElementException]("If there was no next line available")
	def nextLine() = {
		moveToNextLine()
		currentLine
	}
	/**
	 * Moves to the next line and returns it, if possible
	 * @return Line where this editor arrived at. None if end of the file was reached.
	 */
	def nextLineOption() = {
		if (tryMoveToNextLine())
			currentLineOption
		else
			None
	}
	/**
	 * Moves to the next line that fulfills the specified condition
	 * @param condition A line search condition
	 * @return Line that fulfilled that condition (current line). None if end of file was reached.
	 */
	def nextLineWhere(condition: String => Boolean) = {
		if (moveToNextLineWhile { !condition(_) })
			Some(currentLine)
		else
			None
	}
	
	/**
	 * Overwrites the current line in the document. Moves to the next line.
	 * @param newLine Line that will overwrite the current line
	 */
	@throws[NoSuchElementException]("If at the end of file")
	def overwrite(newLine: String) = {
		sourceLinesIterator.next()
		writer.println(newLine)
	}
	/**
	 * Overwrites the current line in the document with 0-n lines. Moves to the next line.
	 * @param newLines Lines that will replace the current line
	 */
	@throws[NoSuchElementException]("If at the end of file")
	def overwriteWith(newLines: IterableOnce[String]) = {
		sourceLinesIterator.next()
		newLines.iterator.foreach(writer.println)
	}
	
	/**
	 * Removes the current line in the document
	 */
	@throws[NoSuchElementException]("If at the end of file")
	def remove(): Unit = sourceLinesIterator.next()
	
	/**
	 * Inserts the specified line before the current line in the document
	 * @param line Line to insert
	 */
	def insertAboveCurrent(line: String) = writer.println(line)
	/**
	 * Inserts the specified lines before the current line in the document
	 * @param lines Lines to insert
	 */
	def insertAboveCurrent(lines: IterableOnce[String]) = lines.iterator.foreach(writer.println)
	
	/**
	 * Edits the current line, moving to the next one
	 * @param f A function that returns the edited copy of the line
	 */
	def mapCurrent(f: String => String) = if (hasCurrent) overwrite(f(currentLine))
	/**
	 * Edits the current line, moving to the next one. May increase or decrease line count.
	 * @param f A function that returns the edited lines based on the original
	 */
	def flatMapCurrent(f: String => IterableOnce[String]) = if (hasCurrent) overwriteWith(f(currentLine))
	
	/**
	 * Edits the current or next line that fulfills the specified condition, moves to the line after the edited line.
	 * @param find A function for finding the targeted line
	 * @param map A function that returns the edited copy of that line
	 */
	def mapNextWhere(find: String => Boolean)(map: String => String) =
		nextLineWhere(find).foreach { line => overwrite(map(line)) }
	/**
	 * Edits the current or next line that fulfills the specified condition, moves to the line after the edited line.
	 * @param find A function for finding the targeted line
	 * @param map A function that returns the edited lines based on that line
	 */
	def flatMapNextWhere(find: String => Boolean)(map: String => IterableOnce[String]) =
		nextLineWhere(find).foreach { line => overwriteWith(map(line)) }
	
	/**
	 * Edits all the remaining lines in this document, including the current line
	 * @param f A function that returns the edited copy of that line
	 */
	def mapRemaining(f: String => String) = sourceLinesIterator.foreach { line => writer.println(f(line)) }
	/**
	 * Edits all the remaining lines in this document, including the current line. May increase or decrease line count.
	 * @param f A function that returns the edited lines based on the original
	 */
	def flatMapRemaining(f: String => IterableOnce[String]) =
		sourceLinesIterator.foreach { line => f(line).iterator.foreach(writer.println) }
	
	/**
	 * Edits the current and the following lines if / as long as they fulfill the specified condition. Moves to the
	 * first line that didn't fulfill that condition, which may be the current line.
	 * @param condition A condition that must be met for the edit to happen
	 * @param map A function that returns the edited copy of that line
	 */
	def mapWhile(condition: String => Boolean)(map: String => String) =
		sourceLinesIterator.foreachWhile(condition) { line => writer.println(map(line)) }
	/**
	 * Edits the current and the following lines until the specified condition is met. Moves to the
	 * line that fulfilled that condition, which may be the current line.
	 * @param terminator A condition that ends the edits
	 * @param map A function that returns the edited copy of that line
	 */
	def mapUntil(terminator: String => Boolean)(map: String => String) =
		mapWhile { !terminator(_) }(map)
	
	/**
	 * Edits the current and the following lines if / as long as they fulfill the specified condition. Moves to the
	 * first line that didn't fulfill that condition, which may be the current line.
	 * @param condition A condition that must be met for the edit to happen
	 * @param map A function that returns the edited copy of that line
	 */
	def flatMapWhile(condition: String => Boolean)(map: String => IterableOnce[String]) =
		sourceLinesIterator.foreachWhile(condition) { map(_).iterator.foreach(writer.println) }
	/**
	 * Edits the current and the following lines until the specified condition is met. Moves to the
	 * line that fulfilled that condition, which may be the current line.
	 * @param terminator A condition that ends the edits
	 * @param map A function that returns the edited copy of that line
	 */
	def flatMapUntil(terminator: String => Boolean)(map: String => IterableOnce[String]) =
		flatMapWhile { !terminator(_) }(map)
	
	/**
	 * Finds and edits the line or lines that fulfill the specified condition. Moves to the first line after
	 * the edited lines.
	 * @param find A condition that returns true for the lines to edit
	 * @param map A function that returns the edited copy of a line
	 */
	def mapNextConsecutiveWhere(find: String => Boolean)(map: String => String) =
		nextLineWhere(find).foreach { firstLine =>
			overwrite(map(firstLine))
			mapWhile(find)(map)
		}
	/**
	 * Finds and edits the line or lines that fulfill the specified condition. Moves to the first line after
	 * the edited lines.
	 * @param find A condition that returns true for the lines to edit
	 * @param map A function that returns the edited copies of a line
	 */
	def flatMapNextConsecutiveWhere(find: String => Boolean)(map: String => IterableOnce[String]) =
		nextLineWhere(find).foreach { firstLine =>
			map(firstLine).iterator.foreach(writer.println)
			flatMapWhile(find)(map)
		}
	
	/**
	 * Moves to the end of the file, copying the rest of the document as is
	 */
	def flush() = sourceLinesIterator.foreach(writer.println)
	/**
	 * Removes all the remaining lines from the document, including the current line
	 */
	def removeRemaining() = while (sourceLinesIterator.hasNext) { sourceLinesIterator.next() }
}
