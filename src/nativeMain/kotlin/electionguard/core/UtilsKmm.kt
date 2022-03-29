package electionguard.core

import kotlin.system.getTimeMillis

actual fun getSystemTimeInMillis() : Long = getTimeMillis()
