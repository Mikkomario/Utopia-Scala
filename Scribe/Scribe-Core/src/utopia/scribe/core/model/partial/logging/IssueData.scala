package utopia.scribe.core.model.partial.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.CombinedOrdering
import utopia.flow.time.Now
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable

import java.time.Instant

object IssueData extends FromModelFactoryWithSchema[IssueData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that presents the least severe issues first
	  */
	implicit val ordering: Ordering[IssueData] = CombinedOrdering(
		Ordering.by { i: IssueData => i.severity },
		Ordering.by { i: IssueData => i.context },
		Ordering.by { i: IssueData => i.created }
	)
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("context", StringType), PropertyDeclaration("severity", 
			IntType, Vector(), Unrecoverable.level), PropertyDeclaration("created", InstantType, 
			isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueData(valid("context").getString, Severity.fromValue(valid("severity")), 
			valid("created").getInstant)
}

/**
  * Represents a type of problem or issue that may occur during a program's run
  * @param context Program context where this issue occurred or was logged. Should be unique.
  * @param severity The estimated severity of this issue
  * @param created Time when this issue first occurred or was first recorded
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueData(context: String, severity: Severity = Unrecoverable, created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("context" -> context, "severity" -> severity.level, 
		"created" -> created))
}

