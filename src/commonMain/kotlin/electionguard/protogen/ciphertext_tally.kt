@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class CiphertextTally(
    val tallyId: String = "",
    val contests: List<electionguard.protogen.CiphertextTallyContest> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextTally = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTally> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextTally> {
        public val defaultInstance: electionguard.protogen.CiphertextTally by lazy { electionguard.protogen.CiphertextTally() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextTally = electionguard.protogen.CiphertextTally.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTally> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextTally, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "tallyId",
                        value = electionguard.protogen.CiphertextTally::tallyId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextTallyContest>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTallyContest.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.CiphertextTally::contests
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextTally",
                messageClass = electionguard.protogen.CiphertextTally::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class CiphertextTallyContest(
    val contestId: String = "",
    val sequenceOrder: Int = 0,
    val contestDescriptionHash: electionguard.protogen.ElementModQ? = null,
    val selections: List<electionguard.protogen.CiphertextTallySelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextTallyContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallyContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextTallyContest> {
        public val defaultInstance: electionguard.protogen.CiphertextTallyContest by lazy { electionguard.protogen.CiphertextTallyContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextTallyContest = electionguard.protogen.CiphertextTallyContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallyContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextTallyContest, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "contestId",
                        value = electionguard.protogen.CiphertextTallyContest::contestId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextTallyContest::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_description_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "contestDescriptionHash",
                        value = electionguard.protogen.CiphertextTallyContest::contestDescriptionHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextTallySelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTallySelection.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.CiphertextTallyContest::selections
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextTallyContest",
                messageClass = electionguard.protogen.CiphertextTallyContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class CiphertextTallySelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val selectionDescriptionHash: electionguard.protogen.ElementModQ? = null,
    val ciphertext: electionguard.protogen.ElGamalCiphertext? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextTallySelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallySelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextTallySelection> {
        public val defaultInstance: electionguard.protogen.CiphertextTallySelection by lazy { electionguard.protogen.CiphertextTallySelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextTallySelection = electionguard.protogen.CiphertextTallySelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallySelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextTallySelection, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.CiphertextTallySelection::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextTallySelection::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_description_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "selectionDescriptionHash",
                        value = electionguard.protogen.CiphertextTallySelection::selectionDescriptionHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "ciphertext",
                        value = electionguard.protogen.CiphertextTallySelection::ciphertext
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextTallySelection",
                messageClass = electionguard.protogen.CiphertextTallySelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTally")
public fun CiphertextTally?.orDefault(): electionguard.protogen.CiphertextTally = this ?: CiphertextTally.defaultInstance

private fun CiphertextTally.protoMergeImpl(plus: pbandk.Message?): CiphertextTally = (plus as? CiphertextTally)?.let {
    it.copy(
        contests = contests + plus.contests,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTally.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTally {
    var tallyId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextTallyContest>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> tallyId = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextTallyContest> }
        }
    }
    return CiphertextTally(tallyId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallyContest")
public fun CiphertextTallyContest?.orDefault(): electionguard.protogen.CiphertextTallyContest = this ?: CiphertextTallyContest.defaultInstance

private fun CiphertextTallyContest.protoMergeImpl(plus: pbandk.Message?): CiphertextTallyContest = (plus as? CiphertextTallyContest)?.let {
    it.copy(
        contestDescriptionHash = contestDescriptionHash?.plus(plus.contestDescriptionHash) ?: plus.contestDescriptionHash,
        selections = selections + plus.selections,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTallyContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTallyContest {
    var contestId = ""
    var sequenceOrder = 0
    var contestDescriptionHash: electionguard.protogen.ElementModQ? = null
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextTallySelection>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> contestDescriptionHash = _fieldValue as electionguard.protogen.ElementModQ
            4 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextTallySelection> }
        }
    }
    return CiphertextTallyContest(contestId, sequenceOrder, contestDescriptionHash, pbandk.ListWithSize.Builder.fixed(selections), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallySelection")
public fun CiphertextTallySelection?.orDefault(): electionguard.protogen.CiphertextTallySelection = this ?: CiphertextTallySelection.defaultInstance

private fun CiphertextTallySelection.protoMergeImpl(plus: pbandk.Message?): CiphertextTallySelection = (plus as? CiphertextTallySelection)?.let {
    it.copy(
        selectionDescriptionHash = selectionDescriptionHash?.plus(plus.selectionDescriptionHash) ?: plus.selectionDescriptionHash,
        ciphertext = ciphertext?.plus(plus.ciphertext) ?: plus.ciphertext,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTallySelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTallySelection {
    var selectionId = ""
    var sequenceOrder = 0
    var selectionDescriptionHash: electionguard.protogen.ElementModQ? = null
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> selectionDescriptionHash = _fieldValue as electionguard.protogen.ElementModQ
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
        }
    }
    return CiphertextTallySelection(selectionId, sequenceOrder, selectionDescriptionHash, ciphertext, unknownFields)
}
