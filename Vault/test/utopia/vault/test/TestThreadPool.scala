package utopia.vault.test

import utopia.flow.async.ThreadPool

/**
 * A thread pool used in vault tests
 * @author Mikko Hilpinen
 * @since 28.1.2020, v1.4
 */
object TestThreadPool extends ThreadPool("Vault-Test")
