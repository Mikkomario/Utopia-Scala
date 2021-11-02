package utopia.vault.coder.test

import utopia.flow.parse.Regex
import utopia.vault.coder.model.scala.declaration.{DeclarationPrefix, DeclarationType}

/**
  * Testing regular expressions used in scala parsing
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  */
object ReadRegexTest extends App
{
	private val visibilityRegex = (Regex("protected ") || Regex("private ")).withinParenthesis
	private val declarationPrefixRegex = DeclarationPrefix.values.map { p => Regex(p.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationModifierRegex = (visibilityRegex || declarationPrefixRegex).withinParenthesis
	private val declarationKeywordRegex = DeclarationType.values.map { d => Regex(d.keyword + " ") }
		.reduceLeft { _ || _ }.withinParenthesis
	private val declarationStartRegex = declarationModifierRegex.zeroOrMoreTimes + declarationKeywordRegex
	private val namedDeclarationStartRegex = declarationStartRegex + Regex.word + Regex("(\\_\\=)?")
	val testRegex = Regex("protected |private ")
	
	val testData = "object DescriptionData extends FromModelFactoryWithSchema[DescriptionData]"
	assert(!visibilityRegex.existsIn(testData))
	assert(!declarationPrefixRegex.existsIn(testData))
	assert(!declarationModifierRegex.existsIn(testData))
	assert(declarationKeywordRegex.existsIn(testData))
	assert(declarationStartRegex.existsIn(testData))
	assert(Regex.word.existsIn(testData))
	Regex.word.matchesIteratorFrom(testData).foreach(println)
	assert(Regex.word.matchesIteratorFrom(testData).contains("DescriptionData"))
	
	assert(testRegex("protected "))
	assert(testRegex("private "))
	assert(!testRegex("p"))
	
	println(namedDeclarationStartRegex.string)
	assert(namedDeclarationStartRegex.existsIn(testData))
	assert(namedDeclarationStartRegex.existsIn("case class DescriptionData(roleId: Int, languageId: Int, text: String, authorId: Option[Int] = None, "))
	
	println("Success!")
}
