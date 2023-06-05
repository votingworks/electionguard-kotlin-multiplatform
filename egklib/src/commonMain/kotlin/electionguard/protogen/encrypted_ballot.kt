@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class EncryptedBallot(
    val ballotId: String = "",
    val ballotStyleId: String = "",
    val confirmationCode: electionguard.protogen.UInt256? = null,
    val contests: List<electionguard.protogen.EncryptedBallotContest> = emptyList(),
    val timestamp: Long = 0L,
    val state: electionguard.protogen.EncryptedBallot.BallotState = electionguard.protogen.EncryptedBallot.BallotState.fromValue(0),
    val isPreencrypt: Boolean = false,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallot = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallot> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallot> {
        public val defaultInstance: electionguard.protogen.EncryptedBallot by lazy { electionguard.protogen.EncryptedBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallot = electionguard.protogen.EncryptedBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallot> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallot, *>>(7)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ballot_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "ballotId",
                        value = electionguard.protogen.EncryptedBallot::ballotId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ballot_style_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "ballotStyleId",
                        value = electionguard.protogen.EncryptedBallot::ballotStyleId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "confirmation_code",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "confirmationCode",
                        value = electionguard.protogen.EncryptedBallot::confirmationCode
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptedBallotContest>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedBallotContest.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.EncryptedBallot::contests
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "timestamp",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int64(),
                        jsonName = "timestamp",
                        value = electionguard.protogen.EncryptedBallot::timestamp
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "state",
                        number = 9,
                        type = pbandk.FieldDescriptor.Type.Enum(enumCompanion = electionguard.protogen.EncryptedBallot.BallotState.Companion),
                        jsonName = "state",
                        value = electionguard.protogen.EncryptedBallot::state
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "is_preencrypt",
                        number = 10,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bool(),
                        jsonName = "isPreencrypt",
                        value = electionguard.protogen.EncryptedBallot::isPreencrypt
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedBallot",
                messageClass = electionguard.protogen.EncryptedBallot::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public sealed class BallotState(override val value: Int, override val name: String? = null) : pbandk.Message.Enum {
        override fun equals(other: kotlin.Any?): Boolean = other is EncryptedBallot.BallotState && other.value == value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = "EncryptedBallot.BallotState.${name ?: "UNRECOGNIZED"}(value=$value)"

        public object UNKNOWN : BallotState(0, "UNKNOWN")
        public object CAST : BallotState(1, "CAST")
        public object SPOILED : BallotState(2, "SPOILED")
        public class UNRECOGNIZED(value: Int) : BallotState(value)

        public companion object : pbandk.Message.Enum.Companion<EncryptedBallot.BallotState> {
            public val values: List<EncryptedBallot.BallotState> by lazy { listOf(UNKNOWN, CAST, SPOILED) }
            override fun fromValue(value: Int): EncryptedBallot.BallotState = values.firstOrNull { it.value == value } ?: UNRECOGNIZED(value)
            override fun fromName(name: String): EncryptedBallot.BallotState = values.firstOrNull { it.name == name } ?: throw IllegalArgumentException("No BallotState with name: $name")
        }
    }
}

@pbandk.Export
public data class EncryptedBallotContest(
    val contestId: String = "",
    val sequenceOrder: Int = 0,
    val contestHash: electionguard.protogen.UInt256? = null,
    val selections: List<electionguard.protogen.EncryptedBallotSelection> = emptyList(),
    val proof: electionguard.protogen.RangeChaumPedersenProofKnownNonce? = null,
    val encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null,
    val preEncryption: electionguard.protogen.PreEncryption? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallotContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallotContest> {
        public val defaultInstance: electionguard.protogen.EncryptedBallotContest by lazy { electionguard.protogen.EncryptedBallotContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallotContest = electionguard.protogen.EncryptedBallotContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallotContest, *>>(7)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "contestId",
                        value = electionguard.protogen.EncryptedBallotContest::contestId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.EncryptedBallotContest::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "contestHash",
                        value = electionguard.protogen.EncryptedBallotContest::contestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptedBallotSelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedBallotSelection.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.EncryptedBallotContest::selections
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.RangeChaumPedersenProofKnownNonce.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.EncryptedBallotContest::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_contest_data",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.HashedElGamalCiphertext.Companion),
                        jsonName = "encryptedContestData",
                        value = electionguard.protogen.EncryptedBallotContest::encryptedContestData
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "pre_encryption",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PreEncryption.Companion),
                        jsonName = "preEncryption",
                        value = electionguard.protogen.EncryptedBallotContest::preEncryption
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedBallotContest",
                messageClass = electionguard.protogen.EncryptedBallotContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class PreEncryption(
    val contestHash: electionguard.protogen.UInt256? = null,
    val selectedVectors: List<electionguard.protogen.PreEncryptionVector> = emptyList(),
    val allHashes: List<electionguard.protogen.PreEncryptionVector> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PreEncryption = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PreEncryption> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PreEncryption> {
        public val defaultInstance: electionguard.protogen.PreEncryption by lazy { electionguard.protogen.PreEncryption() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PreEncryption = electionguard.protogen.PreEncryption.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PreEncryption> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PreEncryption, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_hash",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "contestHash",
                        value = electionguard.protogen.PreEncryption::contestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selected_vectors",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PreEncryptionVector>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PreEncryptionVector.Companion)),
                        jsonName = "selectedVectors",
                        value = electionguard.protogen.PreEncryption::selectedVectors
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "all_hashes",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PreEncryptionVector>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PreEncryptionVector.Companion)),
                        jsonName = "allHashes",
                        value = electionguard.protogen.PreEncryption::allHashes
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PreEncryption",
                messageClass = electionguard.protogen.PreEncryption::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class PreEncryptionVector(
    val selectionHash: electionguard.protogen.UInt256? = null,
    val code: String = "",
    val selectedVector: List<electionguard.protogen.ElGamalCiphertext> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PreEncryptionVector = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PreEncryptionVector> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PreEncryptionVector> {
        public val defaultInstance: electionguard.protogen.PreEncryptionVector by lazy { electionguard.protogen.PreEncryptionVector() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PreEncryptionVector = electionguard.protogen.PreEncryptionVector.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PreEncryptionVector> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PreEncryptionVector, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_hash",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "selectionHash",
                        value = electionguard.protogen.PreEncryptionVector::selectionHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "code",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "code",
                        value = electionguard.protogen.PreEncryptionVector::code
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selected_vector",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElGamalCiphertext>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion)),
                        jsonName = "selectedVector",
                        value = electionguard.protogen.PreEncryptionVector::selectedVector
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PreEncryptionVector",
                messageClass = electionguard.protogen.PreEncryptionVector::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class EncryptedBallotSelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val ciphertext: electionguard.protogen.ElGamalCiphertext? = null,
    val proof: electionguard.protogen.RangeChaumPedersenProofKnownNonce? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallotSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallotSelection> {
        public val defaultInstance: electionguard.protogen.EncryptedBallotSelection by lazy { electionguard.protogen.EncryptedBallotSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallotSelection = electionguard.protogen.EncryptedBallotSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallotSelection, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.EncryptedBallotSelection::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.EncryptedBallotSelection::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "ciphertext",
                        value = electionguard.protogen.EncryptedBallotSelection::ciphertext
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.RangeChaumPedersenProofKnownNonce.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.EncryptedBallotSelection::proof
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedBallotSelection",
                messageClass = electionguard.protogen.EncryptedBallotSelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class RangeChaumPedersenProofKnownNonce(
    val proofs: List<electionguard.protogen.GenericChaumPedersenProof> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RangeChaumPedersenProofKnownNonce = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RangeChaumPedersenProofKnownNonce> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RangeChaumPedersenProofKnownNonce> {
        public val defaultInstance: electionguard.protogen.RangeChaumPedersenProofKnownNonce by lazy { electionguard.protogen.RangeChaumPedersenProofKnownNonce() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RangeChaumPedersenProofKnownNonce = electionguard.protogen.RangeChaumPedersenProofKnownNonce.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RangeChaumPedersenProofKnownNonce> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RangeChaumPedersenProofKnownNonce, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proofs",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.GenericChaumPedersenProof>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion)),
                        jsonName = "proofs",
                        value = electionguard.protogen.RangeChaumPedersenProofKnownNonce::proofs
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "RangeChaumPedersenProofKnownNonce",
                messageClass = electionguard.protogen.RangeChaumPedersenProofKnownNonce::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedBallot")
public fun EncryptedBallot?.orDefault(): electionguard.protogen.EncryptedBallot = this ?: EncryptedBallot.defaultInstance

private fun EncryptedBallot.protoMergeImpl(plus: pbandk.Message?): EncryptedBallot = (plus as? EncryptedBallot)?.let {
    it.copy(
        confirmationCode = confirmationCode?.plus(plus.confirmationCode) ?: plus.confirmationCode,
        contests = contests + plus.contests,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallot {
    var ballotId = ""
    var ballotStyleId = ""
    var confirmationCode: electionguard.protogen.UInt256? = null
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedBallotContest>? = null
    var timestamp = 0L
    var state: electionguard.protogen.EncryptedBallot.BallotState = electionguard.protogen.EncryptedBallot.BallotState.fromValue(0)
    var isPreencrypt = false

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> ballotId = _fieldValue as String
            2 -> ballotStyleId = _fieldValue as String
            5 -> confirmationCode = _fieldValue as electionguard.protogen.UInt256
            6 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.EncryptedBallotContest> }
            7 -> timestamp = _fieldValue as Long
            9 -> state = _fieldValue as electionguard.protogen.EncryptedBallot.BallotState
            10 -> isPreencrypt = _fieldValue as Boolean
        }
    }

    return EncryptedBallot(ballotId, ballotStyleId, confirmationCode, pbandk.ListWithSize.Builder.fixed(contests),
        timestamp, state, isPreencrypt, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedBallotContest")
public fun EncryptedBallotContest?.orDefault(): electionguard.protogen.EncryptedBallotContest = this ?: EncryptedBallotContest.defaultInstance

private fun EncryptedBallotContest.protoMergeImpl(plus: pbandk.Message?): EncryptedBallotContest = (plus as? EncryptedBallotContest)?.let {
    it.copy(
        contestHash = contestHash?.plus(plus.contestHash) ?: plus.contestHash,
        selections = selections + plus.selections,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        encryptedContestData = encryptedContestData?.plus(plus.encryptedContestData) ?: plus.encryptedContestData,
        preEncryption = preEncryption?.plus(plus.preEncryption) ?: plus.preEncryption,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedBallotContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallotContest {
    var contestId = ""
    var sequenceOrder = 0
    var contestHash: electionguard.protogen.UInt256? = null
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedBallotSelection>? = null
    var proof: electionguard.protogen.RangeChaumPedersenProofKnownNonce? = null
    var encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null
    var preEncryption: electionguard.protogen.PreEncryption? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> contestHash = _fieldValue as electionguard.protogen.UInt256
            4 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.EncryptedBallotSelection> }
            6 -> proof = _fieldValue as electionguard.protogen.RangeChaumPedersenProofKnownNonce
            7 -> encryptedContestData = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
            8 -> preEncryption = _fieldValue as electionguard.protogen.PreEncryption
        }
    }

    return EncryptedBallotContest(contestId, sequenceOrder, contestHash, pbandk.ListWithSize.Builder.fixed(selections),
        proof, encryptedContestData, preEncryption, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPreEncryption")
public fun PreEncryption?.orDefault(): electionguard.protogen.PreEncryption = this ?: PreEncryption.defaultInstance

private fun PreEncryption.protoMergeImpl(plus: pbandk.Message?): PreEncryption = (plus as? PreEncryption)?.let {
    it.copy(
        contestHash = contestHash?.plus(plus.contestHash) ?: plus.contestHash,
        selectedVectors = selectedVectors + plus.selectedVectors,
        allHashes = allHashes + plus.allHashes,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PreEncryption.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PreEncryption {
    var contestHash: electionguard.protogen.UInt256? = null
    var selectedVectors: pbandk.ListWithSize.Builder<electionguard.protogen.PreEncryptionVector>? = null
    var allHashes: pbandk.ListWithSize.Builder<electionguard.protogen.PreEncryptionVector>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestHash = _fieldValue as electionguard.protogen.UInt256
            2 -> selectedVectors = (selectedVectors ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.PreEncryptionVector> }
            3 -> allHashes = (allHashes ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.PreEncryptionVector> }
        }
    }

    return PreEncryption(contestHash, pbandk.ListWithSize.Builder.fixed(selectedVectors), pbandk.ListWithSize.Builder.fixed(allHashes), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPreEncryptionVector")
public fun PreEncryptionVector?.orDefault(): electionguard.protogen.PreEncryptionVector = this ?: PreEncryptionVector.defaultInstance

private fun PreEncryptionVector.protoMergeImpl(plus: pbandk.Message?): PreEncryptionVector = (plus as? PreEncryptionVector)?.let {
    it.copy(
        selectionHash = selectionHash?.plus(plus.selectionHash) ?: plus.selectionHash,
        selectedVector = selectedVector + plus.selectedVector,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PreEncryptionVector.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PreEncryptionVector {
    var selectionHash: electionguard.protogen.UInt256? = null
    var code = ""
    var selectedVector: pbandk.ListWithSize.Builder<electionguard.protogen.ElGamalCiphertext>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionHash = _fieldValue as electionguard.protogen.UInt256
            2 -> code = _fieldValue as String
            3 -> selectedVector = (selectedVector ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.ElGamalCiphertext> }
        }
    }

    return PreEncryptionVector(selectionHash, code, pbandk.ListWithSize.Builder.fixed(selectedVector), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedBallotSelection")
public fun EncryptedBallotSelection?.orDefault(): electionguard.protogen.EncryptedBallotSelection = this ?: EncryptedBallotSelection.defaultInstance

private fun EncryptedBallotSelection.protoMergeImpl(plus: pbandk.Message?): EncryptedBallotSelection = (plus as? EncryptedBallotSelection)?.let {
    it.copy(
        ciphertext = ciphertext?.plus(plus.ciphertext) ?: plus.ciphertext,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedBallotSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallotSelection {
    var selectionId = ""
    var sequenceOrder = 0
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null
    var proof: electionguard.protogen.RangeChaumPedersenProofKnownNonce? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
            6 -> proof = _fieldValue as electionguard.protogen.RangeChaumPedersenProofKnownNonce
        }
    }

    return EncryptedBallotSelection(selectionId, sequenceOrder, ciphertext, proof, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForRangeChaumPedersenProofKnownNonce")
public fun RangeChaumPedersenProofKnownNonce?.orDefault(): electionguard.protogen.RangeChaumPedersenProofKnownNonce = this ?: RangeChaumPedersenProofKnownNonce.defaultInstance

private fun RangeChaumPedersenProofKnownNonce.protoMergeImpl(plus: pbandk.Message?): RangeChaumPedersenProofKnownNonce = (plus as? RangeChaumPedersenProofKnownNonce)?.let {
    it.copy(
        proofs = proofs + plus.proofs,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun RangeChaumPedersenProofKnownNonce.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RangeChaumPedersenProofKnownNonce {
    var proofs: pbandk.ListWithSize.Builder<electionguard.protogen.GenericChaumPedersenProof>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> proofs = (proofs ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.GenericChaumPedersenProof> }
        }
    }

    return RangeChaumPedersenProofKnownNonce(pbandk.ListWithSize.Builder.fixed(proofs), unknownFields)
}
