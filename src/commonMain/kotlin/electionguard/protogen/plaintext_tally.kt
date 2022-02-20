@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class PlaintextTally(
    val objectId: String = "",
    val contests: List<electionguard.protogen.PlaintextTally.ContestsEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTally = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTally> {
        public val defaultInstance: electionguard.protogen.PlaintextTally by lazy { electionguard.protogen.PlaintextTally() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTally = electionguard.protogen.PlaintextTally.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTally, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.PlaintextTally::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PlaintextTally.ContestsEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PlaintextTally.ContestsEntry.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.PlaintextTally::contests
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PlaintextTally",
                messageClass = electionguard.protogen.PlaintextTally::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class ContestsEntry(
        override val key: String = "",
        override val value: electionguard.protogen.PlaintextTallyContest? = null,
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, electionguard.protogen.PlaintextTallyContest?> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTally.ContestsEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally.ContestsEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTally.ContestsEntry> {
            public val defaultInstance: electionguard.protogen.PlaintextTally.ContestsEntry by lazy { electionguard.protogen.PlaintextTally.ContestsEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTally.ContestsEntry = electionguard.protogen.PlaintextTally.ContestsEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTally.ContestsEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTally.ContestsEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.PlaintextTally.ContestsEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PlaintextTallyContest.Companion),
                            jsonName = "value",
                            value = electionguard.protogen.PlaintextTally.ContestsEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextTally.ContestsEntry",
                    messageClass = electionguard.protogen.PlaintextTally.ContestsEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class PlaintextTallyContest(
    val objectId: String = "",
    val selections: List<electionguard.protogen.PlaintextTallyContest.SelectionsEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTallyContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTallyContest> {
        public val defaultInstance: electionguard.protogen.PlaintextTallyContest by lazy { electionguard.protogen.PlaintextTallyContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTallyContest = electionguard.protogen.PlaintextTallyContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTallyContest, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.PlaintextTallyContest::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PlaintextTallyContest.SelectionsEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PlaintextTallyContest.SelectionsEntry.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.PlaintextTallyContest::selections
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PlaintextTallyContest",
                messageClass = electionguard.protogen.PlaintextTallyContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class SelectionsEntry(
        override val key: String = "",
        override val value: electionguard.protogen.PlaintextTallySelection? = null,
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, electionguard.protogen.PlaintextTallySelection?> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTallyContest.SelectionsEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest.SelectionsEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTallyContest.SelectionsEntry> {
            public val defaultInstance: electionguard.protogen.PlaintextTallyContest.SelectionsEntry by lazy { electionguard.protogen.PlaintextTallyContest.SelectionsEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTallyContest.SelectionsEntry = electionguard.protogen.PlaintextTallyContest.SelectionsEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallyContest.SelectionsEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTallyContest.SelectionsEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.PlaintextTallyContest.SelectionsEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PlaintextTallySelection.Companion),
                            jsonName = "value",
                            value = electionguard.protogen.PlaintextTallyContest.SelectionsEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextTallyContest.SelectionsEntry",
                    messageClass = electionguard.protogen.PlaintextTallyContest.SelectionsEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class PlaintextTallySelection(
    val objectId: String = "",
    val tally: Int = 0,
    val value: electionguard.protogen.ElementModP? = null,
    val message: electionguard.protogen.ElGamalCiphertext? = null,
    val shares: List<electionguard.protogen.CiphertextDecryptionSelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextTallySelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallySelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextTallySelection> {
        public val defaultInstance: electionguard.protogen.PlaintextTallySelection by lazy { electionguard.protogen.PlaintextTallySelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextTallySelection = electionguard.protogen.PlaintextTallySelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextTallySelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextTallySelection, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.PlaintextTallySelection::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "tally",
                        value = electionguard.protogen.PlaintextTallySelection::tally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "value",
                        value = electionguard.protogen.PlaintextTallySelection::value
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "message",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "message",
                        value = electionguard.protogen.PlaintextTallySelection::message
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "shares",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextDecryptionSelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextDecryptionSelection.Companion)),
                        jsonName = "shares",
                        value = electionguard.protogen.PlaintextTallySelection::shares
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PlaintextTallySelection",
                messageClass = electionguard.protogen.PlaintextTallySelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class CiphertextDecryptionSelection(
    val objectId: String = "",
    val guardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val proof: electionguard.protogen.ChaumPedersenProof? = null,
    val recoveredParts: List<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextDecryptionSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextDecryptionSelection> {
        public val defaultInstance: electionguard.protogen.CiphertextDecryptionSelection by lazy { electionguard.protogen.CiphertextDecryptionSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextDecryptionSelection = electionguard.protogen.CiphertextDecryptionSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextDecryptionSelection, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.CiphertextDecryptionSelection::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.CiphertextDecryptionSelection::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "share",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "share",
                        value = electionguard.protogen.CiphertextDecryptionSelection::share
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.CiphertextDecryptionSelection::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "recovered_parts",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry.Companion)),
                        jsonName = "recoveredParts",
                        value = electionguard.protogen.CiphertextDecryptionSelection::recoveredParts
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextDecryptionSelection",
                messageClass = electionguard.protogen.CiphertextDecryptionSelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class RecoveredPartsEntry(
        override val key: String = "",
        override val value: electionguard.protogen.CiphertextCompensatedDecryptionSelection? = null,
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, electionguard.protogen.CiphertextCompensatedDecryptionSelection?> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> {
            public val defaultInstance: electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry by lazy { electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry = electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextCompensatedDecryptionSelection.Companion),
                            jsonName = "value",
                            value = electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "CiphertextDecryptionSelection.RecoveredPartsEntry",
                    messageClass = electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class CiphertextCompensatedDecryptionSelection(
    val objectId: String = "",
    val guardianId: String = "",
    val missingGuardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val recoveryKey: electionguard.protogen.ElementModP? = null,
    val proof: electionguard.protogen.ChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CiphertextCompensatedDecryptionSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextCompensatedDecryptionSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CiphertextCompensatedDecryptionSelection> {
        public val defaultInstance: electionguard.protogen.CiphertextCompensatedDecryptionSelection by lazy { electionguard.protogen.CiphertextCompensatedDecryptionSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CiphertextCompensatedDecryptionSelection = electionguard.protogen.CiphertextCompensatedDecryptionSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CiphertextCompensatedDecryptionSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CiphertextCompensatedDecryptionSelection, *>>(6)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "object_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "objectId",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::objectId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "missing_guardian_id",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "missingGuardianId",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::missingGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "share",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "share",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::share
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "recovery_key",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "recoveryKey",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::recoveryKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.CiphertextCompensatedDecryptionSelection::proof
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CiphertextCompensatedDecryptionSelection",
                messageClass = electionguard.protogen.CiphertextCompensatedDecryptionSelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTally")
public fun PlaintextTally?.orDefault(): electionguard.protogen.PlaintextTally = this ?: PlaintextTally.defaultInstance

private fun PlaintextTally.protoMergeImpl(plus: pbandk.Message?): PlaintextTally = (plus as? PlaintextTally)?.let {
    it.copy(
        contests = contests + plus.contests,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTally.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTally {
    var objectId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextTally.ContestsEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.PlaintextTally.ContestsEntry> }
        }
    }
    return PlaintextTally(objectId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallyContestsEntry")
public fun PlaintextTally.ContestsEntry?.orDefault(): electionguard.protogen.PlaintextTally.ContestsEntry = this ?: PlaintextTally.ContestsEntry.defaultInstance

private fun PlaintextTally.ContestsEntry.protoMergeImpl(plus: pbandk.Message?): PlaintextTally.ContestsEntry = (plus as? PlaintextTally.ContestsEntry)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTally.ContestsEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTally.ContestsEntry {
    var key = ""
    var value: electionguard.protogen.PlaintextTallyContest? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as electionguard.protogen.PlaintextTallyContest
        }
    }
    return PlaintextTally.ContestsEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallyContest")
public fun PlaintextTallyContest?.orDefault(): electionguard.protogen.PlaintextTallyContest = this ?: PlaintextTallyContest.defaultInstance

private fun PlaintextTallyContest.protoMergeImpl(plus: pbandk.Message?): PlaintextTallyContest = (plus as? PlaintextTallyContest)?.let {
    it.copy(
        selections = selections + plus.selections,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTallyContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTallyContest {
    var objectId = ""
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextTallyContest.SelectionsEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.PlaintextTallyContest.SelectionsEntry> }
        }
    }
    return PlaintextTallyContest(objectId, pbandk.ListWithSize.Builder.fixed(selections), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallyContestSelectionsEntry")
public fun PlaintextTallyContest.SelectionsEntry?.orDefault(): electionguard.protogen.PlaintextTallyContest.SelectionsEntry = this ?: PlaintextTallyContest.SelectionsEntry.defaultInstance

private fun PlaintextTallyContest.SelectionsEntry.protoMergeImpl(plus: pbandk.Message?): PlaintextTallyContest.SelectionsEntry = (plus as? PlaintextTallyContest.SelectionsEntry)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTallyContest.SelectionsEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTallyContest.SelectionsEntry {
    var key = ""
    var value: electionguard.protogen.PlaintextTallySelection? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as electionguard.protogen.PlaintextTallySelection
        }
    }
    return PlaintextTallyContest.SelectionsEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextTallySelection")
public fun PlaintextTallySelection?.orDefault(): electionguard.protogen.PlaintextTallySelection = this ?: PlaintextTallySelection.defaultInstance

private fun PlaintextTallySelection.protoMergeImpl(plus: pbandk.Message?): PlaintextTallySelection = (plus as? PlaintextTallySelection)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        message = message?.plus(plus.message) ?: plus.message,
        shares = shares + plus.shares,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextTallySelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextTallySelection {
    var objectId = ""
    var tally = 0
    var value: electionguard.protogen.ElementModP? = null
    var message: electionguard.protogen.ElGamalCiphertext? = null
    var shares: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextDecryptionSelection>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> tally = _fieldValue as Int
            3 -> value = _fieldValue as electionguard.protogen.ElementModP
            4 -> message = _fieldValue as electionguard.protogen.ElGamalCiphertext
            5 -> shares = (shares ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextDecryptionSelection> }
        }
    }
    return PlaintextTallySelection(objectId, tally, value, message,
        pbandk.ListWithSize.Builder.fixed(shares), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextDecryptionSelection")
public fun CiphertextDecryptionSelection?.orDefault(): electionguard.protogen.CiphertextDecryptionSelection = this ?: CiphertextDecryptionSelection.defaultInstance

private fun CiphertextDecryptionSelection.protoMergeImpl(plus: pbandk.Message?): CiphertextDecryptionSelection = (plus as? CiphertextDecryptionSelection)?.let {
    it.copy(
        share = share?.plus(plus.share) ?: plus.share,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        recoveredParts = recoveredParts + plus.recoveredParts,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextDecryptionSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextDecryptionSelection {
    var objectId = ""
    var guardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var proof: electionguard.protogen.ChaumPedersenProof? = null
    var recoveredParts: pbandk.ListWithSize.Builder<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> guardianId = _fieldValue as String
            3 -> share = _fieldValue as electionguard.protogen.ElementModP
            4 -> proof = _fieldValue as electionguard.protogen.ChaumPedersenProof
            5 -> recoveredParts = (recoveredParts ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry> }
        }
    }
    return CiphertextDecryptionSelection(objectId, guardianId, share, proof,
        pbandk.ListWithSize.Builder.fixed(recoveredParts), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextDecryptionSelectionRecoveredPartsEntry")
public fun CiphertextDecryptionSelection.RecoveredPartsEntry?.orDefault(): electionguard.protogen.CiphertextDecryptionSelection.RecoveredPartsEntry = this ?: CiphertextDecryptionSelection.RecoveredPartsEntry.defaultInstance

private fun CiphertextDecryptionSelection.RecoveredPartsEntry.protoMergeImpl(plus: pbandk.Message?): CiphertextDecryptionSelection.RecoveredPartsEntry = (plus as? CiphertextDecryptionSelection.RecoveredPartsEntry)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextDecryptionSelection.RecoveredPartsEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextDecryptionSelection.RecoveredPartsEntry {
    var key = ""
    var value: electionguard.protogen.CiphertextCompensatedDecryptionSelection? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as electionguard.protogen.CiphertextCompensatedDecryptionSelection
        }
    }
    return CiphertextDecryptionSelection.RecoveredPartsEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCiphertextCompensatedDecryptionSelection")
public fun CiphertextCompensatedDecryptionSelection?.orDefault(): electionguard.protogen.CiphertextCompensatedDecryptionSelection = this ?: CiphertextCompensatedDecryptionSelection.defaultInstance

private fun CiphertextCompensatedDecryptionSelection.protoMergeImpl(plus: pbandk.Message?): CiphertextCompensatedDecryptionSelection = (plus as? CiphertextCompensatedDecryptionSelection)?.let {
    it.copy(
        share = share?.plus(plus.share) ?: plus.share,
        recoveryKey = recoveryKey?.plus(plus.recoveryKey) ?: plus.recoveryKey,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CiphertextCompensatedDecryptionSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CiphertextCompensatedDecryptionSelection {
    var objectId = ""
    var guardianId = ""
    var missingGuardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var recoveryKey: electionguard.protogen.ElementModP? = null
    var proof: electionguard.protogen.ChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> objectId = _fieldValue as String
            2 -> guardianId = _fieldValue as String
            3 -> missingGuardianId = _fieldValue as String
            4 -> share = _fieldValue as electionguard.protogen.ElementModP
            5 -> recoveryKey = _fieldValue as electionguard.protogen.ElementModP
            6 -> proof = _fieldValue as electionguard.protogen.ChaumPedersenProof
        }
    }
    return CiphertextCompensatedDecryptionSelection(objectId, guardianId, missingGuardianId, share,
        recoveryKey, proof, unknownFields)
}
