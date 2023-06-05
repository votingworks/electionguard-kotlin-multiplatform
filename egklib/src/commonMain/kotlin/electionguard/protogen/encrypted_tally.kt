@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class EncryptedTally(
    val tallyId: String = "",
    val contests: List<electionguard.protogen.EncryptedTallyContest> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedTally = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTally> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedTally> {
        public val defaultInstance: electionguard.protogen.EncryptedTally by lazy { electionguard.protogen.EncryptedTally() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedTally = electionguard.protogen.EncryptedTally.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTally> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedTally, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "tallyId",
                        value = electionguard.protogen.EncryptedTally::tallyId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptedTallyContest>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedTallyContest.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.EncryptedTally::contests
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedTally",
                messageClass = electionguard.protogen.EncryptedTally::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class EncryptedTallyContest(
    val contestId: String = "",
    val sequenceOrder: Int = 0,
    val selections: List<electionguard.protogen.EncryptedTallySelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedTallyContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTallyContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedTallyContest> {
        public val defaultInstance: electionguard.protogen.EncryptedTallyContest by lazy { electionguard.protogen.EncryptedTallyContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedTallyContest = electionguard.protogen.EncryptedTallyContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTallyContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedTallyContest, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "contestId",
                        value = electionguard.protogen.EncryptedTallyContest::contestId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.EncryptedTallyContest::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptedTallySelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedTallySelection.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.EncryptedTallyContest::selections
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedTallyContest",
                messageClass = electionguard.protogen.EncryptedTallyContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class EncryptedTallySelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val ciphertext: electionguard.protogen.ElGamalCiphertext? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedTallySelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTallySelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedTallySelection> {
        public val defaultInstance: electionguard.protogen.EncryptedTallySelection by lazy { electionguard.protogen.EncryptedTallySelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedTallySelection = electionguard.protogen.EncryptedTallySelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedTallySelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedTallySelection, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.EncryptedTallySelection::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.EncryptedTallySelection::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "ciphertext",
                        value = electionguard.protogen.EncryptedTallySelection::ciphertext
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedTallySelection",
                messageClass = electionguard.protogen.EncryptedTallySelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedTally")
public fun EncryptedTally?.orDefault(): electionguard.protogen.EncryptedTally = this ?: EncryptedTally.defaultInstance

private fun EncryptedTally.protoMergeImpl(plus: pbandk.Message?): EncryptedTally = (plus as? EncryptedTally)?.let {
    it.copy(
        contests = contests + plus.contests,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedTally.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedTally {
    var tallyId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedTallyContest>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> tallyId = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.EncryptedTallyContest> }
        }
    }

    return EncryptedTally(tallyId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedTallyContest")
public fun EncryptedTallyContest?.orDefault(): electionguard.protogen.EncryptedTallyContest = this ?: EncryptedTallyContest.defaultInstance

private fun EncryptedTallyContest.protoMergeImpl(plus: pbandk.Message?): EncryptedTallyContest = (plus as? EncryptedTallyContest)?.let {
    it.copy(
        selections = selections + plus.selections,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedTallyContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedTallyContest {
    var contestId = ""
    var sequenceOrder = 0
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedTallySelection>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            4 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.EncryptedTallySelection> }
        }
    }

    return EncryptedTallyContest(contestId, sequenceOrder, pbandk.ListWithSize.Builder.fixed(selections), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptedTallySelection")
public fun EncryptedTallySelection?.orDefault(): electionguard.protogen.EncryptedTallySelection = this ?: EncryptedTallySelection.defaultInstance

private fun EncryptedTallySelection.protoMergeImpl(plus: pbandk.Message?): EncryptedTallySelection = (plus as? EncryptedTallySelection)?.let {
    it.copy(
        ciphertext = ciphertext?.plus(plus.ciphertext) ?: plus.ciphertext,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedTallySelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedTallySelection {
    var selectionId = ""
    var sequenceOrder = 0
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
        }
    }

    return EncryptedTallySelection(selectionId, sequenceOrder, ciphertext, unknownFields)
}
