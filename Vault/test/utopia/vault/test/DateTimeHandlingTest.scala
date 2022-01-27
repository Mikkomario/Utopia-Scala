package utopia.vault.test

import utopia.flow.async.ThreadPool
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.DataType
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitUtils
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.model.enumeration.ComparisonOperator.{LargerOrEqual, SmallerOrEqual}
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.Stored
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.Delete

import java.time.Instant
import scala.concurrent.ExecutionContext

/**
  * Tests date time handling
  * @author Mikko Hilpinen
  * @since 28.12.2020, v1.6.1
  */
object DateTimeHandlingTest extends App
{
	DataType.setup()
	
	implicit val exc: ExecutionContext = new ThreadPool("Vault-Test").executionContext
	val cPool = new ConnectionPool(25, 5, 10.seconds)
	
	cPool { implicit connection =>
		val baseTime = Instant.now()
		// Inserts test data
		val hourDiffs = Vector(-7, -2, -1, 0, 1, 2, 7)
		hourDiffs.foreach { h => DTModel.insert(baseTime + h.hours) }
		// Queries data
		def test(hourDiff: Int, expectedResults: Int) =
		{
			println(s"Querying <= $hourDiff difference. Expects $expectedResults rows")
			val timestampRows = DTFactory.findMany(DTModel.withTimestamp(baseTime + hourDiff.hours)
				.toConditionWithOperator(SmallerOrEqual)).size
			val dateTimeRows = DTFactory.findMany(DTModel.withDatetime(baseTime + hourDiff.hours)
				.toConditionWithOperator(SmallerOrEqual)).size
			println(s"Timestamp: $timestampRows (${if (timestampRows == expectedResults) "ok" else "FAILURE"})")
			println(s"Datetime: $dateTimeRows (${if (dateTimeRows == expectedResults) "ok" else "FAILURE"})")
		}
		hourDiffs.zipWithIndex.foreach { case (diff, index) => test(diff, index + 1) }
		// Tests whether row update affects timestamp column
		DTFactory.find(DTModel.withTimestamp(baseTime + 7.hours - 1.minutes).toConditionWithOperator(LargerOrEqual)).foreach { target =>
			println("Waits a moment to ensure different update time...")
			WaitUtils.wait(1.1.seconds, new AnyRef)
			println(s"Updating +7 hour version (id=${target.id})")
			val wasUpdated = DTModel.withId(target.id).withDateTime(baseTime + 1.days).update()
			println(if (wasUpdated) "Updated!" else "Update FAILED")
			DTFactory.get(target.id) match
			{
				case Some(targetV2) =>
					if (target.timestamp ~== targetV2.timestamp)
						println(s"Successfully updated from ${target.datetime} to ${targetV2.datetime}")
					else
						println(s"TIMESTAMP WAS CHANGED (${target.timestamp} => ${targetV2.timestamp})")
				case None => println("TARGET LOST")
			}
		}
		// Removes test data
		connection(Delete(DTFactory.table))
	}
	
	println("Done")
}

private case class DTData(timestamp: Instant, datetime: Instant)
private case class DT(id: Int, data: DTData) extends Stored[DTData, Int]

private object DTFactory extends FromValidatedRowModelFactory[DT]
{
	override def table = TestTables.dateTimeTest
	
	override def defaultOrdering = None
	
	override protected def fromValidatedModel(model: Model) =
		DT(model("id"), DTData(model("timestamp"), model("datetime")))
}
private object DTModel
{
	def withId(id: Int) = apply(Some(id))
	
	def withTimestamp(time: Instant) = apply(timestamp = Some(time))
	
	def withDatetime(time: Instant) = apply(datetime = Some(time))
	
	def insert(data: DTData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.timestamp), Some(data.datetime)).insert().getInt
		DT(id, data)
	}
	
	def insert(time: Instant)(implicit connection: Connection): DT = insert(DTData(time, time))
}
private case class DTModel(id: Option[Int] = None, timestamp: Option[Instant] = None, datetime: Option[Instant] = None)
	extends StorableWithFactory[DT]
{
	override def factory = DTFactory
	
	override def valueProperties = Vector("id" -> id, "timestamp" -> timestamp, "datetime" -> datetime)
	
	def withDateTime(time: Instant) = copy(datetime = Some(time))
}
