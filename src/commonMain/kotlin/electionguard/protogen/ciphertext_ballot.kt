@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class SubmittedBallot(
    val ballotId: String = "",
    val ballotStyleId: String = "",
    val manifestHash: electionguard.protogen.UInt256? = null,
    val codeSeed: electionguard.protogen.UInt256? = null,
    val code: electionguard.protogen.UInt256? = null,
    val contests: List<electionguard.protogen.CiphertextBallotContest> = emptyList(),
    val timestamp: Long = 0L,
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val state: electionguard.protogen.SubmittedBallot.BallotState = electionguard.protogen.SubmittedBallot.BallotState.fromValue(0),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.SubmittedBallot = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SubmittedBallot> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.SubmittedBallot> {
        public val defaultInstance: electionguard.protogen.SubmittedBallot by lazy { electionguard.protogen.SubmittedBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.SubmittedBallot = electionguard.protogen.SubmittedBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SubmittedBallot> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.SubmittedBallot, *>>(9)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ballot_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "ballotId",
                        value = electionguard.protogen.SubmittedBallot::ballotId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ballot_style_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "ballotStyleId",
                        value = electionguard.protogen.SubmittedBallot::ballotStyleId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifest_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "manifestHash",
                        value = electionguard.protogen.SubmittedBallot::manifestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "code_seed",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "codeSeed",
                        value = electionguard.protogen.SubmittedBallot::codeSeed
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "code",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "code",
                        value = electionguard.protogen.SubmittedBallot::code
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextBallotContest>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextBallotContest.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.SubmittedBallot::contests
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "timestamp",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int64(),
                        jsonName = "timestamp",
                        value = electionguard.protogen.SubmittedBallot::timestamp
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_hash",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.SubmittedBallot::cryptoHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "state",
                        number = 9,
                        type = pbandk.FieldDescriptor.Type.Enum(enumCompanion = electionguard.protogen.SubmittedBallot.BallotState.Companion),
                        jsonName = "state",
                        value = electionguard.protogen.SubmittedBallot::state
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "SubmittedBallot",
                messageClass = electionguard.protogen.SubmittedBallot::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public sealed class BallotState(override val value: Int, override val name: String? = null) : pbandk.Message.Enum {
        override fun equals(other: kotlin.Any?): Boolean = other is SubmittedBallot.BallotState && other.value == value
        override fun hashCode(): Int = value.hashCode()
        override fun toString(): String = "SubmittedBallot.BallotState.${name ?: "UNRECOGNIZED"}(value=$value)"

        public object UNKNOWN : BallotState(0, "UNKNOWN")
        public object CAST : BallotState(1, "CAST")
        public object SPOILED : BallotState(2, "SPOILED")
        public class UNRECOGNIZED(value: Int) : BallotState(value)

        public companion object : pbandk.Message.Enum.Companion<SubmittedBallot.BallotState> {
            public val values: List<SubmittedBallot.BallotState> by lazy { listOf(UNKNOWN, CAST, SPOILED) }
            override fun fromValue(value: Int): SubmittedBallot.BallotState = values.firstOrNull { it.value == value } ?: UNRECOGNIZED(value)
            override fun fromName(name: String): SubmittedBallot.BallotState = values.firstOrNull { it.name == name } ?: throw IllegalArgumentException("No BallotState with name: $name")
        }
    }
}

@pbandk.Export
public data class CiphertextBallotContest(
    val contestId: String = "",
    val sequenceOrder: Int = 0,
    val contestHash: electionguard.protogen.UInt256? = null,
    val selections: List<electionguard.protogen.CiphertextBallotSelection> = emptyList(),
    val ciphertextAccumulation: electionguard.protogen.ElGamalCiphertext? = null,
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val proof: electionguard.protogen.ConstantChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextBallotContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextBallotContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextBallotContest> {
        public val defaultInstance: electionguard.protogen.CiphertextBallotContest by lazy { electionguard.protogen.CiphertextBallotContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextBallotContest = electionguard.protogen.CiphertextBallotContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextBallotContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextBallotContest, *>>(7)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "contestId",
                        value = electionguard.protogen.CiphertextBallotContest::contestId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextBallotContest::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "contestHash",
                        value = electionguard.protogen.CiphertextBallotContest::contestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextBallotSelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextBallotSelection.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.CiphertextBallotContest::selections
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext_accumulation",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "ciphertextAccumulation",
                        value = electionguard.protogen.CiphertextBallotContest::ciphertextAccumulation
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_hash",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.CiphertextBallotContest::cryptoHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ConstantChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.CiphertextBallotContest::proof
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextBallotContest",
                messageClass = electionguard.protogen.CiphertextBallotContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class CiphertextBallotSelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val selectionHash: electionguard.protogen.UInt256? = null,
    val ciphertext: electionguard.protogen.ElGamalCiphertext? = null,
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val isPlaceholderSelection: Boolean = false,
    val proof: electionguard.protogen.DisjunctiveChaumPedersenProof? = null,
    val extendedData: electionguard.protogen.ElGamalCiphertext? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextBallotSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextBallotSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextBallotSelection> {
        public val defaultInstance: electionguard.protogen.CiphertextBallotSelection by lazy { electionguard.protogen.CiphertextBallotSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextBallotSelection = electionguard.protogen.CiphertextBallotSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextBallotSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextBallotSelection, *>>(8)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.CiphertextBallotSelection::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextBallotSelection::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "selectionHash",
                        value = electionguard.protogen.CiphertextBallotSelection::selectionHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "ciphertext",
                        value = electionguard.protogen.CiphertextBallotSelection::ciphertext
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_hash",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.CiphertextBallotSelection::cryptoHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "is_placeholder_selection",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bool(),
                        jsonName = "isPlaceholderSelection",
                        value = electionguard.protogen.CiphertextBallotSelection::isPlaceholderSelection
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DisjunctiveChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.CiphertextBallotSelection::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "extended_data",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "extendedData",
                        value = electionguard.protogen.CiphertextBallotSelection::extendedData
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextBallotSelection",
                messageClass = electionguard.protogen.CiphertextBallotSelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ConstantChaumPedersenProof(
    val constant: Int = 0,
    val proof: electionguard.protogen.GenericChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ConstantChaumPedersenProof = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ConstantChaumPedersenProof> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ConstantChaumPedersenProof> {
        public val defaultInstance: electionguard.protogen.ConstantChaumPedersenProof by lazy { electionguard.protogen.ConstantChaumPedersenProof() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ConstantChaumPedersenProof = electionguard.protogen.ConstantChaumPedersenProof.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ConstantChaumPedersenProof> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ConstantChaumPedersenProof, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "constant",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "constant",
                        value = electionguard.protogen.ConstantChaumPedersenProof::constant
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.ConstantChaumPedersenProof::proof
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ConstantChaumPedersenProof",
                messageClass = electionguard.protogen.ConstantChaumPedersenProof::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class DisjunctiveChaumPedersenProof(
    val challenge: electionguard.protogen.ElementModQ? = null,
    val proof0: electionguard.protogen.GenericChaumPedersenProof? = null,
    val proof1: electionguard.protogen.GenericChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DisjunctiveChaumPedersenProof = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DisjunctiveChaumPedersenProof> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DisjunctiveChaumPedersenProof> {
        public val defaultInstance: electionguard.protogen.DisjunctiveChaumPedersenProof by lazy { electionguard.protogen.DisjunctiveChaumPedersenProof() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DisjunctiveChaumPedersenProof = electionguard.protogen.DisjunctiveChaumPedersenProof.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DisjunctiveChaumPedersenProof> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DisjunctiveChaumPedersenProof, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "challenge",
                        number = 9,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "challenge",
                        value = electionguard.protogen.DisjunctiveChaumPedersenProof::challenge
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof0",
                        number = 10,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion),
                        jsonName = "proof0",
                        value = electionguard.protogen.DisjunctiveChaumPedersenProof::proof0
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof1",
                        number = 11,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion),
                        jsonName = "proof1",
                        value = electionguard.protogen.DisjunctiveChaumPedersenProof::proof1
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DisjunctiveChaumPedersenProof",
                messageClass = electionguard.protogen.DisjunctiveChaumPedersenProof::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForSubmittedBallot")
public fun SubmittedBallot?.orDefault(): electionguard.protogen.SubmittedBallot = this ?: SubmittedBallot.defaultInstance

private fun SubmittedBallot.protoMergeImpl(plus: pbandk.Message?): SubmittedBallot = (plus as? SubmittedBallot)?.let {
    it.copy(
        manifestHash = manifestHash?.plus(plus.manifestHash) ?: plus.manifestHash,
        codeSeed = codeSeed?.plus(plus.codeSeed) ?: plus.codeSeed,
        code = code?.plus(plus.code) ?: plus.code,
        contests = contests + plus.contests,
        cryptoHash = cryptoHash?.plus(plus.cryptoHash) ?: plus.cryptoHash,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun SubmittedBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): SubmittedBallot {
    var ballotId = ""
    var ballotStyleId = ""
    var manifestHash: electionguard.protogen.UInt256? = null
    var codeSeed: electionguard.protogen.UInt256? = null
    var code: electionguard.protogen.UInt256? = null
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextBallotContest>? = null
    var timestamp = 0L
    var cryptoHash: electionguard.protogen.UInt256? = null
    var state: electionguard.protogen.SubmittedBallot.BallotState = electionguard.protogen.SubmittedBallot.BallotState.fromValue(0)

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> ballotId = _fieldValue as String
            2 -> ballotStyleId = _fieldValue as String
            3 -> manifestHash = _fieldValue as electionguard.protogen.UInt256
            4 -> codeSeed = _fieldValue as electionguard.protogen.UInt256
            5 -> code = _fieldValue as electionguard.protogen.UInt256
            6 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextBallotContest> }
            7 -> timestamp = _fieldValue as Long
            8 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            9 -> state = _fieldValue as electionguard.protogen.SubmittedBallot.BallotState
        }
    }
    return SubmittedBallot(ballotId, ballotStyleId, manifestHash, codeSeed,
        code, pbandk.ListWithSize.Builder.fixed(contests), timestamp, cryptoHash,
        state, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextBallotContest")
public fun CiphertextBallotContest?.orDefault(): electionguard.protogen.CiphertextBallotContest = this ?: CiphertextBallotContest.defaultInstance

private fun CiphertextBallotContest.protoMergeImpl(plus: pbandk.Message?): CiphertextBallotContest = (plus as? CiphertextBallotContest)?.let {
    it.copy(
        contestHash = contestHash?.plus(plus.contestHash) ?: plus.contestHash,
        selections = selections + plus.selections,
        ciphertextAccumulation = ciphertextAccumulation?.plus(plus.ciphertextAccumulation) ?: plus.ciphertextAccumulation,
        cryptoHash = cryptoHash?.plus(plus.cryptoHash) ?: plus.cryptoHash,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextBallotContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextBallotContest {
    var contestId = ""
    var sequenceOrder = 0
    var contestHash: electionguard.protogen.UInt256? = null
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextBallotSelection>? = null
    var ciphertextAccumulation: electionguard.protogen.ElGamalCiphertext? = null
    var cryptoHash: electionguard.protogen.UInt256? = null
    var proof: electionguard.protogen.ConstantChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> contestHash = _fieldValue as electionguard.protogen.UInt256
            4 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextBallotSelection> }
            5 -> ciphertextAccumulation = _fieldValue as electionguard.protogen.ElGamalCiphertext
            6 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            7 -> proof = _fieldValue as electionguard.protogen.ConstantChaumPedersenProof
        }
    }
    return CiphertextBallotContest(contestId, sequenceOrder, contestHash, pbandk.ListWithSize.Builder.fixed(selections),
        ciphertextAccumulation, cryptoHash, proof, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextBallotSelection")
public fun CiphertextBallotSelection?.orDefault(): electionguard.protogen.CiphertextBallotSelection = this ?: CiphertextBallotSelection.defaultInstance

private fun CiphertextBallotSelection.protoMergeImpl(plus: pbandk.Message?): CiphertextBallotSelection = (plus as? CiphertextBallotSelection)?.let {
    it.copy(
        selectionHash = selectionHash?.plus(plus.selectionHash) ?: plus.selectionHash,
        ciphertext = ciphertext?.plus(plus.ciphertext) ?: plus.ciphertext,
        cryptoHash = cryptoHash?.plus(plus.cryptoHash) ?: plus.cryptoHash,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        extendedData = extendedData?.plus(plus.extendedData) ?: plus.extendedData,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextBallotSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextBallotSelection {
    var selectionId = ""
    var sequenceOrder = 0
    var selectionHash: electionguard.protogen.UInt256? = null
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null
    var cryptoHash: electionguard.protogen.UInt256? = null
    var isPlaceholderSelection = false
    var proof: electionguard.protogen.DisjunctiveChaumPedersenProof? = null
    var extendedData: electionguard.protogen.ElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> selectionHash = _fieldValue as electionguard.protogen.UInt256
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
            5 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            6 -> isPlaceholderSelection = _fieldValue as Boolean
            7 -> proof = _fieldValue as electionguard.protogen.DisjunctiveChaumPedersenProof
            8 -> extendedData = _fieldValue as electionguard.protogen.ElGamalCiphertext
        }
    }
    return CiphertextBallotSelection(selectionId, sequenceOrder, selectionHash, ciphertext,
        cryptoHash, isPlaceholderSelection, proof, extendedData, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForConstantChaumPedersenProof")
public fun ConstantChaumPedersenProof?.orDefault(): electionguard.protogen.ConstantChaumPedersenProof = this ?: ConstantChaumPedersenProof.defaultInstance

private fun ConstantChaumPedersenProof.protoMergeImpl(plus: pbandk.Message?): ConstantChaumPedersenProof = (plus as? ConstantChaumPedersenProof)?.let {
    it.copy(
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ConstantChaumPedersenProof.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ConstantChaumPedersenProof {
    var constant = 0
    var proof: electionguard.protogen.GenericChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            5 -> constant = _fieldValue as Int
            6 -> proof = _fieldValue as electionguard.protogen.GenericChaumPedersenProof
        }
    }
    return ConstantChaumPedersenProof(constant, proof, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDisjunctiveChaumPedersenProof")
public fun DisjunctiveChaumPedersenProof?.orDefault(): electionguard.protogen.DisjunctiveChaumPedersenProof = this ?: DisjunctiveChaumPedersenProof.defaultInstance

private fun DisjunctiveChaumPedersenProof.protoMergeImpl(plus: pbandk.Message?): DisjunctiveChaumPedersenProof = (plus as? DisjunctiveChaumPedersenProof)?.let {
    it.copy(
        challenge = challenge?.plus(plus.challenge) ?: plus.challenge,
        proof0 = proof0?.plus(plus.proof0) ?: plus.proof0,
        proof1 = proof1?.plus(plus.proof1) ?: plus.proof1,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DisjunctiveChaumPedersenProof.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DisjunctiveChaumPedersenProof {
    var challenge: electionguard.protogen.ElementModQ? = null
    var proof0: electionguard.protogen.GenericChaumPedersenProof? = null
    var proof1: electionguard.protogen.GenericChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            9 -> challenge = _fieldValue as electionguard.protogen.ElementModQ
            10 -> proof0 = _fieldValue as electionguard.protogen.GenericChaumPedersenProof
            11 -> proof1 = _fieldValue as electionguard.protogen.GenericChaumPedersenProof
        }
    }
    return DisjunctiveChaumPedersenProof(challenge, proof0, proof1, unknownFields)
}
