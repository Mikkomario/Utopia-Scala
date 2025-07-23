package utopia.logos.test

import utopia.logos.model.cached.{Link, Statement}

/**
 * Tests forming a statement from string data
 *
 * @author Mikko Hilpinen
 * @since 22.12.2024, v0.3
 */
object StatementParseTest extends App
{
	private val statements = Statement.allFrom("For more information, please visit www.example.com, or contact (www.example.com/company/us) us at marketing@example.com") // Statement.allFrom("Huom: Yrityksen perustiedot ovat muuttuneet. Katso uudet tiedot osoitteesta: https://test.com/uutiset [22.4.2024]. (Lisätiedot tarvittaessa). {\"prop\": \"value\" }")
	statements.foreach { s =>
		println()
		println(s"Content: ${ s.words.mkString(" + ") }")
		println(s"Links: ${ s.links.mkString(" + ") }")
		println(s"Words: ${ s.standardizedWords.map { _._1 }.mkString(" + ") }")
		println(s"Delimiter: \"${ s.delimiter }\"")
		println(s"Full: $s")
	}
	
	private val link = Link("https://topaasia.com/tapahtumat").get
	println(link)
	println(link.domain)
	println(link.path)
	
	//assert(s1.words.size == 2)
	//assert(s1.wordsAndLinks.first.size == 2)
}
