package utopia.vault.model.error

import utopia.vault.model.immutable.{Row, Table}

/**
 * Thrown when a receiver expects there to be model data in row but there isn't
 * @author Mikko Hilpinen
 * @since 31.1.2020, v1.4
 */
class NoModelDataInRowException(requiredTable: Table, row: Row)
	extends RuntimeException(s"Following row didn't contain data for ${requiredTable.name}: $row")
