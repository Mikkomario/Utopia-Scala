package utopia.vault.test

import utopia.flow.generic.DataType

import scala.concurrent.ExecutionContext

/**
 * Tests using special characters in queries
 * @author Mikko Hilpinen
 * @since 18.2.2020, v0.1
 */
object SpecialCharactersTest extends App
{
	DataType.setup()
	
	implicit val exc: ExecutionContext = TestThreadPool.executionContext
	TestConnectionPool { implicit connection =>
		val newPerson = Person("ÄmpäriÖykkäri")
		val id = newPerson.insert()
		val readPerson = Person.get(id)
		
		println(readPerson.get.name)
		assert(readPerson.get.name == "ÄmpäriÖykkäri")
		
		println("Done!")
	}
}
