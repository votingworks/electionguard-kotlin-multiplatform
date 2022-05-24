@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class EncryptedBallot(
    val ballotId: String = "",
    val ballotStyleId: String = "",
    val manifestHash: electionguard.protogen.UInt256? = null,
    val codeSeed: electionguard.protogen.UInt256? = null,
    val code: electionguard.protogen.UInt256? = null,
    val contests: List<electionguard.protogen.EncryptedBallotContest> = emptyList(),
    val timestamp: Long = 0L,
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val state: electionguard.protogen.EncryptedBallot.BallotState = electionguard.protogen.EncryptedBallot.BallotState.fromValue(0),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallot = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallot> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallot> {
        public val defaultInstance: electionguard.protogen.EncryptedBallot by lazy { electionguard.protogen.EncryptedBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallot = electionguard.protogen.EncryptedBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallot> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallot, *>>(9)
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
                        name = "manifest_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "manifestHash",
                        value = electionguard.protogen.EncryptedBallot::manifestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "code_seed",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "codeSeed",
                        value = electionguard.protogen.EncryptedBallot::codeSeed
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "code",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "code",
                        value = electionguard.protogen.EncryptedBallot::code
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
                        name = "crypto_hash",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.EncryptedBallot::cryptoHash
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
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val proof: electionguard.protogen.ConstantChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallotContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallotContest> {
        public val defaultInstance: electionguard.protogen.EncryptedBallotContest by lazy { electionguard.protogen.EncryptedBallotContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallotContest = electionguard.protogen.EncryptedBallotContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallotContest, *>>(6)
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
                        name = "crypto_hash",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.EncryptedBallotContest::cryptoHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ConstantChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.EncryptedBallotContest::proof
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
public data class EncryptedBallotSelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val selectionHash: electionguard.protogen.UInt256? = null,
    val ciphertext: electionguard.protogen.ElGamalCiphertext? = null,
    val cryptoHash: electionguard.protogen.UInt256? = null,
    val isPlaceholderSelection: Boolean = false,
    val proof: electionguard.protogen.DisjunctiveChaumPedersenProof? = null,
    val extendedData: electionguard.protogen.HashedElGamalCiphertext? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedBallotSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedBallotSelection> {
        public val defaultInstance: electionguard.protogen.EncryptedBallotSelection by lazy { electionguard.protogen.EncryptedBallotSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedBallotSelection = electionguard.protogen.EncryptedBallotSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedBallotSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedBallotSelection, *>>(8)
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
                        name = "selection_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "selectionHash",
                        value = electionguard.protogen.EncryptedBallotSelection::selectionHash
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
                        name = "crypto_hash",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoHash",
                        value = electionguard.protogen.EncryptedBallotSelection::cryptoHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "is_placeholder_selection",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bool(),
                        jsonName = "isPlaceholderSelection",
                        value = electionguard.protogen.EncryptedBallotSelection::isPlaceholderSelection
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DisjunctiveChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.EncryptedBallotSelection::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "extended_data",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.HashedElGamalCiphertext.Companion),
                        jsonName = "extendedData",
                        value = electionguard.protogen.EncryptedBallotSelection::extendedData
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
@pbandk.JsName("orDefaultForEncryptedBallot")
public fun EncryptedBallot?.orDefault(): electionguard.protogen.EncryptedBallot = this ?: EncryptedBallot.defaultInstance

private fun EncryptedBallot.protoMergeImpl(plus: pbandk.Message?): EncryptedBallot = (plus as? EncryptedBallot)?.let {
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
private fun EncryptedBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallot {
    var ballotId = ""
    var ballotStyleId = ""
    var manifestHash: electionguard.protogen.UInt256? = null
    var codeSeed: electionguard.protogen.UInt256? = null
    var code: electionguard.protogen.UInt256? = null
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedBallotContest>? = null
    var timestamp = 0L
    var cryptoHash: electionguard.protogen.UInt256? = null
    var state: electionguard.protogen.EncryptedBallot.BallotState = electionguard.protogen.EncryptedBallot.BallotState.fromValue(0)

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> ballotId = _fieldValue as String
            2 -> ballotStyleId = _fieldValue as String
            3 -> manifestHash = _fieldValue as electionguard.protogen.UInt256
            4 -> codeSeed = _fieldValue as electionguard.protogen.UInt256
            5 -> code = _fieldValue as electionguard.protogen.UInt256
            6 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.EncryptedBallotContest> }
            7 -> timestamp = _fieldValue as Long
            8 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            9 -> state = _fieldValue as electionguard.protogen.EncryptedBallot.BallotState
        }
    }
    return EncryptedBallot(ballotId, ballotStyleId, manifestHash, codeSeed,
        code, pbandk.ListWithSize.Builder.fixed(contests), timestamp, cryptoHash,
        state, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedBallotContest")
public fun EncryptedBallotContest?.orDefault(): electionguard.protogen.EncryptedBallotContest = this ?: EncryptedBallotContest.defaultInstance

private fun EncryptedBallotContest.protoMergeImpl(plus: pbandk.Message?): EncryptedBallotContest = (plus as? EncryptedBallotContest)?.let {
    it.copy(
        contestHash = contestHash?.plus(plus.contestHash) ?: plus.contestHash,
        selections = selections + plus.selections,
        cryptoHash = cryptoHash?.plus(plus.cryptoHash) ?: plus.cryptoHash,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedBallotContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallotContest {
    var contestId = ""
    var sequenceOrder = 0
    var contestHash: electionguard.protogen.UInt256? = null
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedBallotSelection>? = null
    var cryptoHash: electionguard.protogen.UInt256? = null
    var proof: electionguard.protogen.ConstantChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> contestHash = _fieldValue as electionguard.protogen.UInt256
            4 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.EncryptedBallotSelection> }
            6 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            7 -> proof = _fieldValue as electionguard.protogen.ConstantChaumPedersenProof
        }
    }
    return EncryptedBallotContest(contestId, sequenceOrder, contestHash, pbandk.ListWithSize.Builder.fixed(selections),
        cryptoHash, proof, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedBallotSelection")
public fun EncryptedBallotSelection?.orDefault(): electionguard.protogen.EncryptedBallotSelection = this ?: EncryptedBallotSelection.defaultInstance

private fun EncryptedBallotSelection.protoMergeImpl(plus: pbandk.Message?): EncryptedBallotSelection = (plus as? EncryptedBallotSelection)?.let {
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
private fun EncryptedBallotSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedBallotSelection {
    var selectionId = ""
    var sequenceOrder = 0
    var selectionHash: electionguard.protogen.UInt256? = null
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null
    var cryptoHash: electionguard.protogen.UInt256? = null
    var isPlaceholderSelection = false
    var proof: electionguard.protogen.DisjunctiveChaumPedersenProof? = null
    var extendedData: electionguard.protogen.HashedElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> selectionHash = _fieldValue as electionguard.protogen.UInt256
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
            5 -> cryptoHash = _fieldValue as electionguard.protogen.UInt256
            6 -> isPlaceholderSelection = _fieldValue as Boolean
            7 -> proof = _fieldValue as electionguard.protogen.DisjunctiveChaumPedersenProof
            8 -> extendedData = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
        }
    }
    return EncryptedBallotSelection(selectionId, sequenceOrder, selectionHash, ciphertext,
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
