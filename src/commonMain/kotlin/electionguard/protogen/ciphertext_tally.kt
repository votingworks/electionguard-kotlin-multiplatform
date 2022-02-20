@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class CiphertextTally(
    val objectId: String = "",
    val contests: List<electionguard.protogen.CiphertextTally.ContestsEntry> = emptyList(),
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
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.CiphertextTally::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextTally.ContestsEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTally.ContestsEntry.Companion)),
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

    public data class ContestsEntry(
        override val key: String = "",
        override val value: electionguard.protogen.CiphertextTallyContest? = null,
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, electionguard.protogen.CiphertextTallyContest?> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextTally.ContestsEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTally.ContestsEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextTally.ContestsEntry> {
            public val defaultInstance: electionguard.protogen.CiphertextTally.ContestsEntry by lazy { electionguard.protogen.CiphertextTally.ContestsEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextTally.ContestsEntry = electionguard.protogen.CiphertextTally.ContestsEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTally.ContestsEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextTally.ContestsEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.CiphertextTally.ContestsEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTallyContest.Companion),
                            jsonName = "value",
                            value = electionguard.protogen.CiphertextTally.ContestsEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "CiphertextTally.ContestsEntry",
                    messageClass = electionguard.protogen.CiphertextTally.ContestsEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class CiphertextTallyContest(
    val objectId: String = "",
    val sequenceOrder: Int = 0,
    val descriptionHash: electionguard.protogen.ElementModQ? = null,
    val tallySelections: List<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry> = emptyList(),
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
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.CiphertextTallyContest::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextTallyContest::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "description_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "descriptionHash",
                        value = electionguard.protogen.CiphertextTallyContest::descriptionHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_selections",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry.Companion)),
                        jsonName = "tallySelections",
                        value = electionguard.protogen.CiphertextTallyContest::tallySelections
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

    public data class TallySelectionsEntry(
        override val key: String = "",
        override val value: electionguard.protogen.CiphertextTallySelection? = null,
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, electionguard.protogen.CiphertextTallySelection?> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry> {
            public val defaultInstance: electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry by lazy { electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry = electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTallySelection.Companion),
                            jsonName = "value",
                            value = electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "CiphertextTallyContest.TallySelectionsEntry",
                    messageClass = electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class CiphertextTallySelection(
    val objectId: String = "",
    val sequenceOrder: Int = 0,
    val descriptionHash: electionguard.protogen.ElementModQ? = null,
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
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.CiphertextTallySelection::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "sequence_order",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int32(),
                        jsonName = "sequenceOrder",
                        value = electionguard.protogen.CiphertextTallySelection::sequenceOrder
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "description_hash",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "descriptionHash",
                        value = electionguard.protogen.CiphertextTallySelection::descriptionHash
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
    var objectId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextTally.ContestsEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextTally.ContestsEntry> }
        }
    }
    return CiphertextTally(objectId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallyContestsEntry")
public fun CiphertextTally.ContestsEntry?.orDefault(): electionguard.protogen.CiphertextTally.ContestsEntry = this ?: CiphertextTally.ContestsEntry.defaultInstance

private fun CiphertextTally.ContestsEntry.protoMergeImpl(plus: pbandk.Message?): CiphertextTally.ContestsEntry = (plus as? CiphertextTally.ContestsEntry)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTally.ContestsEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTally.ContestsEntry {
    var key = ""
    var value: electionguard.protogen.CiphertextTallyContest? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as electionguard.protogen.CiphertextTallyContest
        }
    }
    return CiphertextTally.ContestsEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallyContest")
public fun CiphertextTallyContest?.orDefault(): electionguard.protogen.CiphertextTallyContest = this ?: CiphertextTallyContest.defaultInstance

private fun CiphertextTallyContest.protoMergeImpl(plus: pbandk.Message?): CiphertextTallyContest = (plus as? CiphertextTallyContest)?.let {
    it.copy(
        descriptionHash = descriptionHash?.plus(plus.descriptionHash) ?: plus.descriptionHash,
        tallySelections = tallySelections + plus.tallySelections,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTallyContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTallyContest {
    var objectId = ""
    var sequenceOrder = 0
    var descriptionHash: electionguard.protogen.ElementModQ? = null
    var tallySelections: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> descriptionHash = _fieldValue as electionguard.protogen.ElementModQ
            4 -> tallySelections = (tallySelections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry> }
        }
    }
    return CiphertextTallyContest(objectId, sequenceOrder, descriptionHash, pbandk.ListWithSize.Builder.fixed(tallySelections), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallyContestTallySelectionsEntry")
public fun CiphertextTallyContest.TallySelectionsEntry?.orDefault(): electionguard.protogen.CiphertextTallyContest.TallySelectionsEntry = this ?: CiphertextTallyContest.TallySelectionsEntry.defaultInstance

private fun CiphertextTallyContest.TallySelectionsEntry.protoMergeImpl(plus: pbandk.Message?): CiphertextTallyContest.TallySelectionsEntry = (plus as? CiphertextTallyContest.TallySelectionsEntry)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTallyContest.TallySelectionsEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTallyContest.TallySelectionsEntry {
    var key = ""
    var value: electionguard.protogen.CiphertextTallySelection? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as electionguard.protogen.CiphertextTallySelection
        }
    }
    return CiphertextTallyContest.TallySelectionsEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextTallySelection")
public fun CiphertextTallySelection?.orDefault(): electionguard.protogen.CiphertextTallySelection = this ?: CiphertextTallySelection.defaultInstance

private fun CiphertextTallySelection.protoMergeImpl(plus: pbandk.Message?): CiphertextTallySelection = (plus as? CiphertextTallySelection)?.let {
    it.copy(
        descriptionHash = descriptionHash?.plus(plus.descriptionHash) ?: plus.descriptionHash,
        ciphertext = ciphertext?.plus(plus.ciphertext) ?: plus.ciphertext,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextTallySelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextTallySelection {
    var objectId = ""
    var sequenceOrder = 0
    var descriptionHash: electionguard.protogen.ElementModQ? = null
    var ciphertext: electionguard.protogen.ElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> sequenceOrder = _fieldValue as Int
            3 -> descriptionHash = _fieldValue as electionguard.protogen.ElementModQ
            4 -> ciphertext = _fieldValue as electionguard.protogen.ElGamalCiphertext
        }
    }
    return CiphertextTallySelection(objectId, sequenceOrder, descriptionHash, ciphertext, unknownFields)
}
