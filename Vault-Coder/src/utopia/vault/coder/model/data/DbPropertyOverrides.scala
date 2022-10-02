package utopia.vault.coder.model.data

import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.Uncertain

object DbPropertyOverrides
{
	/**
	  * Empty (ie. non-modifying) overrides
	  */
	val empty = apply()
}

/**
  * Contains user-specified default-setting overrides concerning an individual database-property
  * @author Mikko Hilpinen
  * @since 18.7.2022, v1.5.1
  * @param name Custom name to use for this property
  * @param columnName Custom column name to use for the described property. Empty if no custom (default)
  * @param default Custom default value of the described property. Empty if no custom value (default)
  * @param lengthRule Length rule to apply to the described column / property. Empty if no rule applied (default)
  * @param indexing Custom (overriding) indexing rules to apply to the described property
  *                       (default = undefined = use data type default)
  */
case class DbPropertyOverrides(name: Option[Name] = None, columnName: String = "", default: String = "",
                               lengthRule: String = "", indexing: UncertainBoolean = Uncertain)
