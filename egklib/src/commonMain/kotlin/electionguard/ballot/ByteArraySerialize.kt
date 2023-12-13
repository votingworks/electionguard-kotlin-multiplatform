package electionguard.ballot

import com.github.michaelbull.result.Result

expect fun ContestData.encodeToByteArray(fill: String? = null): ByteArray

expect fun ByteArray.decodeToContestData() : Result<ContestData, String>

