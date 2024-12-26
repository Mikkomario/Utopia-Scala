package utopia.logos.test

import utopia.logos.model.cached.Statement

/**
 * Tests forming a statement from string data
 *
 * @author Mikko Hilpinen
 * @since 22.12.2024, v0.3
 */
object StatementParseTest extends App
{
	private val statements = Statement.allFrom("Huom: Yrityksen perustiedot ovat muuttuneet. Katso uudet tiedot osoitteesta: https://test.com/uutiset [22.4.2024]. (Lisätiedot tarvittaessa). {\"prop\": \"value\" }")
	statements.foreach { s =>
		println()
		println(s.words.mkString(" + "))
		println(s.wordsAndLinks.second.mkString(" + "))
		println(s.wordsAndLinks.first.mkString(" + "))
	}
	
	//assert(s1.words.size == 2)
	//assert(s1.wordsAndLinks.first.size == 2)
}
