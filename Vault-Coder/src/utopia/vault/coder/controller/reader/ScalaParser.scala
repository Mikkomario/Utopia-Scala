package utopia.vault.coder.controller.reader

import utopia.flow.collection.{MultiMapBuilder, PollingIterator}
import utopia.flow.datastructure.mutable.MutatingOnce
import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.IterateLines
import utopia.vault.coder.model.reader.ReadCodeBlock
import utopia.vault.coder.model.scala.ScalaTypeCategory.{CallByName, Standard}
import utopia.vault.coder.model.scala.{Extension, Package, Parameter, Parameters, Reference, ScalaDoc, ScalaDocKeyword, ScalaDocPart, ScalaType}
import utopia.vault.coder.model.scala.Visibility.{Private, Protected, Public}
import utopia.vault.coder.model.scala.code.{Code, CodeLine, CodePiece}
import utopia.vault.coder.model.scala.declaration.DeclarationPrefix.Override
import utopia.vault.coder.model.scala.declaration.FunctionDeclarationType.{FunctionD, ValueD, VariableD}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue, Variable}
import utopia.vault.coder.model.scala.declaration.{DeclarationPrefix, DeclarationStart, DeclarationType, FunctionDeclarationType, InstanceDeclarationType, MethodDeclaration, PropertyDeclaration}

import java.nio.file.Path
import scala.collection.immutable.VectorBuilder

/**
  * Used for reading and interpreting scala files
  * @author Mikko Hilpinen
  * @since 29.10.2021, v1.3
  */
object ScalaParser
{
	private lazy val packageRegex = Regex("package ") + Regex.any
	private lazy val importRegex = Regex("import ") + Regex.any
	
	private lazy val visibilityRegex = (Regex("protected ") || Regex("private ")).withinParenthesis
	private lazy val declarationPrefixRegex = DeclarationPrefix.values.map { p => Regex(p.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private lazy val declarationModifierRegex = (visibilityRegex || declarationPrefixRegex).withinParenthesis
	private lazy val declarationKeywordRegex = DeclarationType.values.map { d => Regex(d.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private lazy val declarationStartRegex = declarationModifierRegex.zeroOrMoreTimes + declarationKeywordRegex
	private lazy val namedDeclarationStartRegex = declarationStartRegex + Regex.word + Regex("\\_\\=").noneOrOnce
	
	private lazy val extendsRegex = Regex(" extends ")
	private lazy val withRegex = Regex(" with ")
	
	private lazy val commaOutsideParenthesesRegex = Regex.escape(',').ignoringParentheses
	
	private lazy val scalaDocStartRegex = Regex("\\/\\*\\*")
	private lazy val commentEndRegex = Regex("\\*\\/")
	
	def apply(path: Path) =
	{
		IterateLines.fromPath(path) { linesIter =>
			val iter = linesIter.pollable
			
			// Searches for package declaration first
			val filePackageString = iter.pollToNextWhere { _.nonEmpty }.filter(packageRegex.apply)
				.map { s => s.afterFirst("package ") }.getOrElse("")
			
			// Next looks for import statements
			val importStatements = iter.collectWhile { line => line.isEmpty || importRegex(line) }.filter { _.nonEmpty }
				.map { _.afterFirst("import ") }
				.flatMap { importString =>
					if (importString.contains('{'))
					{
						val (basePart, endPart) = importString.splitAtFirst("{")
						val trimmedBase = basePart.trim
						val endItems = endPart.untilFirst("}").split(',').toVector.map { _.trim }
						endItems.map { trimmedBase + _ }
					}
					else
						Vector(importString.trim)
				}
			val separatedImportStatements = importStatements.map { importStatement =>
				val (beginning, end) = importStatement.splitAtLast(".")
				end.notEmpty match
				{
					case Some(end) =>
						if (end == "_")
						{
							val (packagePart, targetPrefix) = beginning.splitAtLast(".")
							if (targetPrefix.isEmpty)
								filePackageString -> importStatement
							else
								packagePart -> s"$targetPrefix._"
						}
						else
							beginning -> end
					case None => filePackageString -> importStatement
				}
			}
			val packagePerString = separatedImportStatements.map { case (packageString, _) => packageString }
				.toSet.map { s: String => s -> Package(s) }.toMap
			val referencesPerTarget = separatedImportStatements
				.map { case (packageString, target) => target -> Reference(packagePerString(packageString), target) }
				.toMap
			
			// Finally, finds and processes the object and/or class statements
			val builder = new FileBuilder(Package(filePackageString))
			val codeLineIterator = iter.map { line =>
				val indentation = line.dropWhile { _ == '\t' }.length
				CodeLine(indentation, line.drop(indentation))
			}
			while (iter.hasNext)
			{
				readNextItemFrom(codeLineIterator.pollable, referencesPerTarget, builder)
			}
			// Returns the parsed file
			builder.result() -> referencesPerTarget.valuesIterator.toSet
		}
	}
	
	private def readNextItemFrom(linesIter: PollingIterator[CodeLine], refMap: Map[String, Reference],
	                             parentBuilder: InstanceBuilderLike): Unit =
	{
		// Checks what the next non-empty line looks like
		linesIter.nextWhere { _.nonEmpty }.foreach { firstLine =>
			// Processes the scaladoc block if there is one
			val scalaDoc = {
				if (scalaDocStartRegex.existsIn(firstLine.code))
				{
					if (commentEndRegex.existsIn(firstLine.code))
					{
						val scalaDoc = firstLine.code.afterFirst("/**").untilFirst("*/").trim
						scalaDocFromLines(Vector(scalaDoc))
					}
					else
					{
						val trimmedFirstLine = firstLine.code.afterFirst("/**")
						val rawLines = linesIter.collectTo { line => commentEndRegex.existsIn(line.code) }
						if (rawLines.isEmpty)
							scalaDocFromLines(Vector(trimmedFirstLine))
						else
						{
							val middleLines = rawLines.dropRight(1)
								.map { _.code.dropWhile { c => c == '\t' || c == '*' || c == ' ' } }
							val lastLine = rawLines.last.code.untilFirst("*/")
								.dropWhile { c => c == '\t' || c == ' ' || c == '*' }
							scalaDocFromLines(trimmedFirstLine +: middleLines :+ lastLine)
						}
					}
				}
				else
					ScalaDoc.empty
			}
			// Collects lines before the first declaration, if necessary
			val (beforeDeclarationLines, declarationLine) = {
				if (namedDeclarationStartRegex.existsIn(firstLine.code))
					Vector() -> Some(firstLine)
				else
					// Skips leading and trailing empty lines
					(firstLine +: linesIter.collectUntil { line => namedDeclarationStartRegex.existsIn(line.code) })
						.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty } -> linesIter.nextOption()
			}
			declarationLine match
			{
				// Case: Declaration found => parses it
				case Some(declarationLine) =>
					// Processes the "free" code before the declaration
					// - comments will be attached to the parsed declaration
					val (beforeDeclarationComments, beforeDeclarationCode) = beforeDeclarationLines.dividedWith { line =>
						if (line.code.startsWith("//"))
							Left(line.code.drop(2).trim)
						else
							Right(line.copy(indentation = line.indentation - declarationLine.indentation))
					}
					parentBuilder.addFreeCode(beforeDeclarationCode)
					// Identifies the declaration in question
					val declarationStartRange = namedDeclarationStartRegex.firstRangeFrom(declarationLine.code).get
					val declarationStart = declarationLine.code.slice(declarationStartRange)
					val afterDeclarationStart = declarationLine.code.substring(declarationStartRange.exclusiveEnd)
					val declarationName = if (declarationStart.contains(" ")) declarationStart.afterLast(" ") else
						declarationStart
					val declarationType = DeclarationType.values
						.find { d => declarationStart.contains(d.keyword + " ") }.get
					// Parses prefixes and visibility
					val prefixes = declarationType.availablePrefixes
						.filter { prefix => declarationStart.contains(prefix.keyword + " ") }
					val visibility = {
						if (declarationStart.contains("private "))
							Private
						else if (declarationStart.contains("protected "))
							Protected
						else
							Public
					}
					// Parses parameter lists, if needed
					val (parameters, afterParameterLists) = {
						if (declarationType.acceptsParameterList && afterDeclarationStart.startsWith("(")) {
							val (rawLists, remaining) = readRawParameterLists(afterDeclarationStart, linesIter)
							val parameters = parametersFrom(rawLists.map { _.mkString }, refMap, scalaDoc)
							Some(parameters) -> remaining
						}
						else
							None -> afterDeclarationStart.trim.notEmpty
					}
					// Handles functions and instances differently
					declarationType match
					{
						case declarationType: FunctionDeclarationType =>
							// Checks whether explicit parameter type has been specified
							val (explicitType, declarationEnd) = explicitTypeFrom(
								afterParameterLists.getOrElse(""), refMap)
							
							// Expects an assignment operator before a body
							// - if no operator is given, assumes that the function is abstract
							val body = {
								if (declarationEnd.contains('='))
								{
									val lineAfterAssignment = declarationEnd.afterFirst("=").trim
									val firstBodyLine = {
										if (lineAfterAssignment.isEmpty)
											linesIter.nextOption() match
											{
												case Some(line) => line
												case None => CodeLine.empty
											}
										else
											CodeLine(lineAfterAssignment)
									}
									// Case: Function body is wrapped in a block => reads the block contents
									if (firstBodyLine.code.startsWith("{"))
									{
										val (block, _) = readBlock(firstBodyLine.code.drop(1), linesIter)
										block.toCodeWith(refMap)
									}
									// Case: Function is a single line function (possibly on multiple lines)
									// => Squashes the lines to a single code line
									else
									{
										// Collects the lines based on indentation
										// (hoping that they are indented correctly)
										val moreBodyLines = linesIter
											.collectWhile { _.indentation > declarationLine.indentation }
											.dropRightWhile { _.isEmpty }
										val allCodeLine = CodeLine(firstBodyLine.code +
											moreBodyLines.map { _.code }.mkString)
										Code(Vector(allCodeLine), refMap.keySet.filter { target =>
											allCodeLine.code.contains(target) }.map(refMap.apply))
									}
								}
								else
									Code.empty
							}
							// Adds the parsed function (property or method) to the parent instance
							// Case: Parsed item is a method
							if (declarationType == FunctionD && parameters.exists { _.containsExplicits })
								parentBuilder.addMethod(MethodDeclaration(visibility, declarationName, parameters.get,
									body, explicitType, scalaDoc.description, scalaDoc.returnDescription,
									beforeDeclarationComments, prefixes.contains(Override)))
							else
							{
								val propertyType = declarationType match {
									case ValueD => ImmutableValue
									case VariableD => Variable
									case _ => ComputedProperty
								}
								val implicitParameters = parameters match
								{
									case Some(parameters) => parameters.implicits
									case None => Vector()
								}
								parentBuilder.addProperty(PropertyDeclaration(propertyType, declarationName, body,
									visibility, explicitType, implicitParameters,
									scalaDoc.description.notEmpty.getOrElse(scalaDoc.returnDescription),
									beforeDeclarationComments, prefixes.contains(Override)))
							}
						case declarationType: InstanceDeclarationType =>
							// Looks for extends portion
							// The instance body is always expected to be wrapped in a block,
							// which is processed separately
							val (extensions, contentBlock) = extensionsAndBlockFrom(afterParameterLists,
								linesIter, refMap)
							val builder = new InstanceBuilder(visibility, prefixes, declarationType, declarationName,
								parameters, extensions, scalaDoc, beforeDeclarationComments)
							contentBlock.foreach { block =>
								// Reads all available items from the block
								val blockLinesIterator = block.lines.iterator.pollable
								while (blockLinesIterator.hasNext)
								{
									readNextItemFrom(blockLinesIterator, refMap, builder)
								}
							}
							parentBuilder.addNested(builder.result(refMap))
					}
				// Case: No declaration found => adds read code lines as free code
				case None => parentBuilder.addFreeCode(beforeDeclarationLines)
			}
		}
	}
	
	// Expects tabulator strikes, whitespaces etc. to be removed from line beginnings
	private def scalaDocFromLines(lines: Vector[String]) =
	{
		if (lines.isEmpty)
			ScalaDoc.empty
		else
		{
			val builder = new MultiMapBuilder[Option[ScalaDocKeyword], String]
			var lastKeyword: Option[ScalaDocKeyword] = None
			lines.foreach { line =>
				val (keyword, content) = extractScalaDocKeyword(line)
				if (keyword.nonEmpty)
					lastKeyword = keyword
				builder += lastKeyword -> content
			}
			ScalaDoc(builder.result().map { case (keyword, lines) => ScalaDocPart(lines, keyword) }
				.toVector.sortBy { _.keyword })
		}
	}
	
	// Expects leading tabulator strikes etc. to be removed at this point
	private def extractScalaDocKeyword(line: String) =
	{
		if (line.startsWith("@"))
		{
			val (keywordPart, afterKeyword) = line.splitAtFirst(" ")
			val contentPartPointer = new MutatingOnce(afterKeyword, afterKeyword.afterFirst(" "))
			
			ScalaDocKeyword.matching(keywordPart.drop(1),
				contentPartPointer.value.untilFirst(" ")) -> contentPartPointer.value
		}
		else
			None -> line
	}
	
	private def extensionsAndBlockFrom(openLine: Option[String], moreLinesIter: PollingIterator[CodeLine],
	                                   refMap: Map[String, Reference]) =
	{
		// Case: Instance block starts on the first line
		if (openLine.exists { _.contains('{') })
		{
			val (beforeBlock, afterBlockStart) = openLine.get.splitAtFirst("{")
			val (block, _) = readBlock(afterBlockStart, moreLinesIter)
			extensionsFrom(Vector(beforeBlock), refMap) -> Some(block)
		}
		// Case: Instance block may start on a later line (after extension lines) or there may not be a code block
		else
		{
			// Looks for the block start, but terminates on a named declaration
			// Collects the in-between lines as extension lines
			val extensionLinesBuilder = new VectorBuilder[String]()
			openLine.foreach { extensionLinesBuilder += _ }
			var afterBlockPart: Option[String] = None
			while (afterBlockPart.isEmpty && moreLinesIter.hasNext &&
				!namedDeclarationStartRegex.existsIn(moreLinesIter.poll.code))
			{
				// Case: Extension and/or block start line found
				val line = moreLinesIter.next().code
				line.optionIndexOf("{") match
				{
					// Case: Block starts on this line
					case Some(blockStartIndex) =>
						if (blockStartIndex > 0)
							extensionLinesBuilder += line.take(blockStartIndex)
						afterBlockPart = Some(line.drop(blockStartIndex + 1))
					// Case: This line is only extensions, still
					case None => extensionLinesBuilder += line
				}
			}
			val extensions = extensionsFrom(extensionLinesBuilder.result(), refMap)
			afterBlockPart match
			{
				// Case: Block was found
				case Some(afterBlockStart) =>
					val (block, _) = readBlock(afterBlockStart, moreLinesIter)
					extensions -> Some(block)
				// Case: Instance didn't contain a code block
				case None => extensions -> None
			}
		}
	}
	
	private def extensionsFrom(lines: Vector[String], refMap: Map[String, Reference]): Vector[Extension] =
	{
		// Finds the line that contains the extends -keyword
		lines.indexWhereOption { line => extendsRegex.existsIn(line) || line.startsWith("extends ") ||
			line.endsWith(" extends") } match
		{
			// Case: Extends -keyword found => Parses the extensions from the remaining line part + remaining lines
			case Some(firstLineIndex) =>
				val firstLineRemain = lines(firstLineIndex).afterFirst("extends")
				// Individual extensions are separated with a " with "
				val parts = withRegex.split((firstLineRemain +: lines.drop(firstLineIndex + 1)).mkString(" "))
					.map { _.trim }.toVector
				parts.map { partString =>
					// Parses the parent type
					val (parentType, afterType) = scalaTypeFrom(partString, refMap)
					// Checks whether a constructor list should be included
					if (afterType.startsWith("("))
					{
						val (constructorLists, _) = readOneLineRawParameterLists(afterType.drop(1))
						val constructorAssignments = constructorLists
							.map { commaOutsideParenthesesRegex.split(_).toVector
								.map { addReferencesToCode(_, refMap) } }
						Extension(parentType, constructorAssignments)
					}
					else
						Extension(parentType)
				}
			// Case: No extends -keyword found => no extensions
			case None => Vector()
		}
	}
	
	private def addReferencesToCode(code: String, refMap: Map[String, Reference]) =
		CodePiece(code, refMap.keySet.filter(code.contains).map(refMap.apply))
	
	private def explicitTypeFrom(string: String, refMap: Map[String, Reference]) =
	{
		if (string.startsWith(":"))
		{
			val (dataType, remaining) = scalaTypeFrom(string.drop(1).trim, refMap)
			Some(dataType) -> remaining
		}
		else
			None -> string
	}
	
	private def parametersFrom(parameterListStrings: Vector[String], refMap: Map[String, Reference],
	                           scalaDoc: ScalaDoc) =
	{
		// Handles the implicit parameter list separately, which is expected to be the last list, if present
		val (standardLists, implicitList) = {
			if (parameterListStrings.lastOption.exists { _.startsWith("implicit ") })
				parameterListStrings.dropRight(1) -> Some(parameterListStrings.last.afterFirst("implicit "))
			else
				parameterListStrings -> None
		}
		val standardParameters = standardLists.map { parameterListFrom(_, refMap, scalaDoc) }
		val implicitParameters = implicitList match
		{
			case Some(listString) => parameterListFrom(listString, refMap, scalaDoc)
			case None => Vector()
		}
		Parameters(standardParameters, implicitParameters)
	}
	
	private def parameterListFrom(parameterListString: String, refMap: Map[String, Reference], scalaDoc: ScalaDoc) =
	{
		if (parameterListString.isEmpty)
			Vector()
		else
		{
			val paramsBuilder = new VectorBuilder[Parameter]()
			val (firstParam, firstRemaining) = nextParameterFrom(parameterListString, refMap, scalaDoc)
			paramsBuilder += firstParam
			var remaining = firstRemaining
			while (remaining.startsWith(","))
			{
				val (nextParam, nextRemaining) = nextParameterFrom(remaining.drop(1), refMap, scalaDoc)
				paramsBuilder += nextParam
				remaining = nextRemaining
			}
			paramsBuilder.result()
		}
	}
	
	private def nextParameterFrom(string: String, refMap: Map[String, Reference], scalaDoc: ScalaDoc) =
	{
		// Parses name and data type
		val (namePart, remaining) = string.splitAtFirst(":")
		val (dataType, afterType) = scalaTypeFrom(remaining.trim, refMap)
		val (beforeName, name) = if (namePart.contains(' ')) namePart.splitAtLast(" ") else "" -> namePart
		// Parses parameter prefix, if there is one
		val prefix = DeclarationType.values.find { t => beforeName.contains(t.keyword) }.map { declarationType =>
			val visibility = {
				if (beforeName.contains("private ")) Private
				else if (beforeName.contains("protected ")) Protected
				else Public
			}
			val prefixes = DeclarationPrefix.values.filter { p => beforeName.contains(p.keyword) }
			DeclarationStart(declarationType, visibility, prefixes)
		}
		// Reads parameter description from the scaladoc
		val description = scalaDoc.param(name)
		
		// Case: Parameter has a default value
		if (afterType.startsWith("="))
			commaOutsideParenthesesRegex.startIndexIteratorIn(afterType).nextOption() match
			{
				// Case: There remains yet another parameter
				case Some(nextCommaIndex) =>
					Parameter(name, dataType, afterType.slice(1, nextCommaIndex).trim, prefix, description) ->
						afterType.drop(nextCommaIndex)
				// Case: This was the last parameter
				case None =>
					Parameter(name, dataType, afterType.drop(1).trim, prefix, description) -> ""
			}
		// Case: No default value provided
		else
			Parameter(name, dataType, prefix = prefix, description = description) -> afterType
	}
	
	private def scalaTypeFrom(string: String, refMap: Map[String, Reference]): (ScalaType, String) =
	{
		// Case: Starts with parentheses => Leads to either tuple or function type
		if (string.startsWith("("))
		{
			val (parenthesisPart, remainingPart) = readOneLineParentheses(string.drop(1))
			// Parses types within the parentheses
			val (parenthesesTypes, _) = scalaTypesFrom(parenthesisPart, refMap)
			
			// Case: Function type => Parses result type also
			if (remainingPart.startsWith("=>"))
			{
				val functionResultPart = remainingPart.afterFirst("=>").trim
				val (resultType, remaining) = scalaTypeFrom(functionResultPart, refMap)
				resultType.fromParameters(parenthesesTypes) -> remaining
			}
			// Case: Tuple type => wraps the types into a "basic" scala type
			// TODO: References should be handled properly
			else
				ScalaType.basic(parenthesesTypes.map { _.toScala }
					.reduceLeftOption { _.append(_, ", ") }.getOrElse(CodePiece.empty)
					.withinParenthesis.text) -> remainingPart
		}
		else
		{
			// Checks whether this is a call-by-name type
			val (mainPart, isCallByName) = if (string.startsWith("=>")) string.afterFirst("=>").trim -> true else
				string -> false
			// Parses the type name until generic types list or some interrupting character
			val mainPartEndIndex = mainPart.indexWhere { c => !c.isLetterOrDigit && c != '_' }
			val (mainTypeString, remaining) = if (mainPartEndIndex < 0) mainPart -> "" else
				mainPart.take(mainPartEndIndex) -> mainPart.drop(mainPartEndIndex)
			// Checks whether the main type is referencing a type
			val mainType = refMap.get(mainTypeString) match
			{
				case Some(ref) => Right(ref)
				case None => Left(mainTypeString)
			}
			// Parses generic types if available
			if (remaining.startsWith("["))
			{
				val (typesPart, afterTypes) = readOneLineBrackets(remaining.drop(1))
				val (types, _) = scalaTypesFrom(typesPart, refMap)
				ScalaType(mainType, types, if (isCallByName) CallByName else Standard) -> afterTypes
			}
			else
				ScalaType(mainType, category = if (isCallByName) CallByName else Standard) -> remaining
		}
	}
	
	private def scalaTypesFrom(string: String, refMap: Map[String, Reference]): (Vector[ScalaType], String) =
	{
		// Parses types as long as there are some remaining
		// Expects them to be separated by a comma
		val (firstType, firstRemain) = scalaTypeFrom(string, refMap)
		val typesBuilder = new VectorBuilder[ScalaType]()
		typesBuilder += firstType
		
		var remaining = firstRemain
		while (remaining.startsWith(","))
		{
			val (nextType, nextRemain) = scalaTypeFrom(remaining, refMap)
			typesBuilder += nextType
			remaining = nextRemain
		}
		typesBuilder.result() -> remaining
	}
	
	// Returns lists as lists of lines
	private def readRawParameterLists(listStartLine: String,
	                                  remainingLinesIter: PollingIterator[CodeLine]): (Vector[Vector[String]], Option[String]) =
	{
		val listsBuilder = new VectorBuilder[Vector[String]]()
		// Code that appears after the initial parameter lists will be stored here
		var codeAfter: Option[String] = None
		// Checks whether the first line starts a parameter list
		if (listStartLine.startsWith("("))
		{
			val (list, remainingAfter) = readBlockLike(listStartLine.drop(1), remainingLinesIter,
				'(', ')')
			listsBuilder += list.map { _.code }
			// Collects lists while the remaining part keeps initiating new lists
			codeAfter = remainingAfter
			while (codeAfter.exists { _.startsWith("(") })
			{
				val (list, remainingAfter) = readBlockLike(codeAfter.get.drop(1), remainingLinesIter,
					'(', ')')
				listsBuilder += list.map { _.code }
				codeAfter = remainingAfter
			}
		}
		// May continue to look for lists from the next lines
		if (codeAfter.nonEmpty)
			listsBuilder.result() -> codeAfter
		else
			remainingLinesIter.pollOption match
			{
				case Some(nextLine) =>
					val trimmedNextLine = nextLine.code.trim
					// Case: Next line opens a new parameter list => processes that and the potential lists after
					if (trimmedNextLine.startsWith("("))
					{
						remainingLinesIter.skip()
						val (nextLists, remaining) = readRawParameterLists(trimmedNextLine, remainingLinesIter)
						(listsBuilder.result() ++ nextLists) -> remaining
					}
					// Case: Next line doesn't open a new list => leaves it as it is
					else
						listsBuilder.result() -> None
				// Case: There are no more lines left
				case None => listsBuilder.result() -> None
			}
	}
	
	// Block start line must have the initiating { -char removed
	private def readBlock(blockStartLine: String, remainingLinesIter: Iterator[CodeLine]) =
	{
		val (blockLines, after) = readBlockLike(blockStartLine, remainingLinesIter, '{', '}')
		// Removes the unnecessary indentation and possible leading and trailing empty lines
		val actualLines = blockLines.dropWhile { _.isEmpty }.dropRightWhile { _.isEmpty }
		if (actualLines.isEmpty)
			ReadCodeBlock.empty -> after
		else
		{
			val zeroIndentLevel = actualLines.head.indentation
			ReadCodeBlock(actualLines.map { line => line.copy(indentation = line.indentation - zeroIndentLevel) }) ->
				after
		}
	}
	
	// Block start line must have the initiating opening character removed
	private def readBlockLike(blockStartLine: String, remainingLinesIter: Iterator[CodeLine],
	                          openChar: Char, closeChar: Char) =
	{
		val blockLinesBuilder = new VectorBuilder[CodeLine]()
		// Opens the first line
		var openBlockCount = 1 + blockStartLine.count { _ == openChar } - blockStartLine.count { _ == closeChar }
		var lastLine = CodeLine(blockStartLine)
		// Adds multiple lines if necessary
		while (openBlockCount > 0 && remainingLinesIter.hasNext)
		{
			blockLinesBuilder += lastLine
			lastLine = remainingLinesIter.next()
			openBlockCount = openBlockCount +
				lastLine.code.count { _ == openChar } - lastLine.code.count { _ == closeChar }
		}
		// Handles the last line, possibly splitting it to two parts
		val (lastLineBlockPart, afterPart) = separateBlockClosuresFrom(lastLine, -openBlockCount, closeChar)
		blockLinesBuilder += lastLineBlockPart
		// Returns all lines plus the part after the last closure
		blockLinesBuilder.result() -> Some(afterPart.trim).filter { _.nonEmpty }
	}
	
	private def readOneLineRawParameterLists(line: String) =
	{
		val resultsBuilder = new VectorBuilder[String]()
		val (firstList, firstRemaining) = readOneLineParentheses(line)
		resultsBuilder += firstList
		var remaining = firstRemaining
		while (remaining.startsWith("("))
		{
			val (nextList, nextRemaining) = readOneLineParentheses(remaining.drop(1))
			resultsBuilder += nextList
			remaining = nextRemaining
		}
		resultsBuilder.result() -> remaining
	}
	
	private def readOneLineBrackets(line: String) = readOneLineBlockLike(line, '[', ']')
	
	private def readOneLineParentheses(line: String) =
		readOneLineBlockLike(line, '(', ')')
	
	// Returns block part and remaining part
	private def readOneLineBlockLike(line: String, openChar: Char, closeChar: Char) =
	{
		val openOrCloseCharRegex = Regex.escape(openChar) || Regex.escape(closeChar)
		val changeIterator = openOrCloseCharRegex.startIndexIteratorIn(line)
		var currentDepth = 1
		var lastIndex = line.length
		while (currentDepth > 0 && changeIterator.hasNext)
		{
			lastIndex = changeIterator.next()
			if (line(lastIndex) == openChar)
				currentDepth += 1
			else
				currentDepth -= 1
		}
		line.take(lastIndex).trim -> line.drop(lastIndex + 1).trim
	}
	
	// Returns part before the last included closure (without closing character) +
	// part after last included closure, including 'closuresToSeparate' closing characters
	private def separateBlockClosuresFrom(line: CodeLine, closuresToSeparate: Int, closeChar: Char) =
	{
		val splitIndex = {
			if (closuresToSeparate <= 0)
				line.code.lastIndexOf(closeChar)
			else
			{
				// Closure indices from right to left
				val closureIndices = line.code.indexOfIterator(closeChar.toString).toVector.reverse
				closureIndices(closuresToSeparate)
			}
		}
		val (lineCode, remainingCode) = line.code.substring(0, splitIndex) -> line.code.substring(splitIndex + 1)
		line.copy(code = lineCode) -> remainingCode
	}
}
