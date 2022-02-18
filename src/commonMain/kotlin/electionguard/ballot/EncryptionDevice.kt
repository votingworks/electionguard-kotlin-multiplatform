package electionguard.ballot

data class EncryptionDevice(
    /** Unique identifier for device.  */
    val device_id: Long,
    /** Used to identify session and protect the timestamp.  */
    val session_id: Long,
    /** Election initialization value.  */
    val launch_code: Long,
    val location: String,
)