package utopia.scribe.api.database.access.logging.error.stack

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.StackTraceElementRecordDbModel
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which may be filtered based on stack trace element record 
  * properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
trait FilterStackTraceElementRecords[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines stack trace element record database properties
	  */
	def model = StackTraceElementRecordDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param lineNumber line number to target
	  * @return Copy of this access point that only includes stack trace element records with the specified 
	  * line number
	  */
	def atLine(lineNumber: Int) = filter(model.lineNumber.column <=> lineNumber)
	
	/**
	  * @param lineNumbers Targeted line numbers
	  * @return Copy of this access point that only includes stack trace element records where line number is 
	  * within the specified value set
	  */
	def atLines(lineNumbers: IterableOnce[Int]) = filter(model.lineNumber.column.in(IntSet.from(lineNumbers)))
	
	/**
	  * @param causeId cause id to target
	  * @return Copy of this access point that only includes stack trace element records with the specified 
	  * cause id
	  */
	def following(causeId: Int) = filter(model.causeId.column <=> causeId)
	
	/**
	  * @param causeIds Targeted cause ids
	  * @return Copy of this access point that only includes stack trace element records where cause id is 
	  * within the specified value set
	  */
	def followingStacks(causeIds: IterableOnce[Int]) = filter(model.causeId.column.in(IntSet.from(causeIds)))
	
	/**
	  * @param className class name to target
	  * @return Copy of this access point that only includes stack trace element records with the specified 
	  * class name
	  */
	def inClass(className: String) = filter(model.className.column <=> className)
	
	/**
	  * @param classNames Targeted class names
	  * @return Copy of this access point that only includes stack trace element records where class name is 
	  * within the specified value set
	  */
	def inClassses(classNames: Iterable[String]) = filter(model.className.column.in(classNames))
	
	/**
	  * @param fileName file name to target
	  * @return Copy of this access point that only includes stack trace element records with the specified 
	  * file name
	  */
	def inFile(fileName: String) = filter(model.fileName.column <=> fileName)
	
	/**
	  * @param fileNames Targeted file names
	  * @return Copy of this access point that only includes stack trace element records where file name is 
	  * within the specified value set
	  */
	def inFiles(fileNames: Iterable[String]) = filter(model.fileName.column.in(fileNames))
	
	/**
	  * @param methodName method name to target
	  * @return Copy of this access point that only includes stack trace element records with the specified 
	  * method name
	  */
	def inMethod(methodName: String) = filter(model.methodName.column <=> methodName)
	
	/**
	  * @param methodNames Targeted method names
	  * @return Copy of this access point that only includes stack trace element records where method name is 
	  * within the specified value set
	  */
	def inMethods(methodNames: Iterable[String]) = filter(model.methodName.column.in(methodNames))
}

