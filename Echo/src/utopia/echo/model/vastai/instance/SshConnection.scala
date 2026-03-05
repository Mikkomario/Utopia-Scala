package utopia.echo.model.vastai.instance

/**
 * Contains information for connecting to an instance via SSH
 * @param host Host (or IP) used for SSH connection
 * @param port Port used for SSH connection
 * @param machineDirectoryPort Calculated SSH port for accessing the machine directory
 * @param user Identifier for the SSH forwarder used
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
// TODO: Remove the user parameter (we always use root)
case class SshConnection(host: String, port: Int, machineDirectoryPort: Int, user: String)
