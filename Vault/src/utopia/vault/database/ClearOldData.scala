package utopia.vault.database

import java.time.Period
import utopia.flow.util.TimeExtensions._

import utopia.vault.model.immutable.Table
import utopia.vault.sql.Condition

/**
 * Used for clearing deprecated data from the database
 * @author Mikko Hilpinen
 * @since 2.4.2020, v1.5
 */
class ClearOldData(rules: Iterable[(Table, Period, Option[Condition])])
{
	// ATTRIBUTES	---------------------------
	
	// Forms the actual deletion rules based on those provided + existing table references
	private val finalRules =
	{
		val baseRulesByTable = rules.groupBy { _._1 }
		baseRulesByTable.map { case (table, baseRules) =>
			// Checks if there exist any rules for tables referencing the table in question
			val tree = References.referenceTree(table)
			val referencingRulesWithPaths = baseRulesByTable.flatMap { case (childTable, childRules) =>
				lazy val basePeriod = basePeriodFrom(childRules)
				tree.filterWithPaths { _.content == childTable }.map { childPath =>
					val conditionalPeriods = conditionalPeriodsFrom(childRules)
					childPath -> TableDeletionRule(childTable, basePeriod, conditionalPeriods)
				}
			}
			// Creates the deletion rule for the primary table
			val basePeriod = basePeriodFrom(baseRules)
			val conditionalPeriods = conditionalPeriodsFrom(baseRules)
			TableDeletionRule(table, basePeriod, conditionalPeriods, referencingRulesWithPaths)
		}.toVector
	}
	
	
	// OTHER	------------------------------
	
	// TODO: Implement the actual deletion process
	
	// FIXME: Change to return option and to only consider non-conditional rules
	private def basePeriodFrom(rules: Iterable[(_, Period, Option[Condition])]) =
		rules.filter { _._3.isEmpty }.map { _._2 }.maxOption.getOrElse { rules.map { _._2 }.max }
	
	private def conditionalPeriodsFrom(rules: Iterable[(_, Period, Option[Condition])]) =
		rules.filter { _._3.isDefined }.map {
			case (_, conditionalPeriod: Period, condition: Option[Condition]) => condition.get -> conditionalPeriod }.toMap
}

// FIXME: Not all tables should have unconditional deletion time. Change it to Option and fix calculations
private case class TableDeletionRule(table: Table, baseLiveDuration: Period, conditionalPeriods: Map[Condition, Period] = Map(),
								childDeletionRules: Map[Vector[Table], TableDeletionRule] = Map())