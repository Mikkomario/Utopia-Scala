package utopia.vault.test.database

import utopia.flow.async.context.ThreadPool
import utopia.flow.util.logging.SysErrLogger

/**
  * A thread pool used in vault tests
  * @author Mikko Hilpinen
  * @since 28.1.2020, v1.4
  */
object TestThreadPool extends ThreadPool("Vault-Test")(SysErrLogger)
