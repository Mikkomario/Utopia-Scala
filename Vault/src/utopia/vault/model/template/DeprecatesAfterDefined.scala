package utopia.vault.model.template

/**
  * A version of [[DeprecatesAfter]], where the deprecation (timestamp) column contains NULL until the row is
  * (manually) deprecated.
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait DeprecatesAfterDefined extends DeprecatesAfter with DeprecatesIfDefined
{
	override def activeCondition = deprecationColumn.isNull
	override def deprecatedCondition = deprecationColumn.isNotNull
}
