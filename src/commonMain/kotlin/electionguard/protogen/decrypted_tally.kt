@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptedTallyOrBallot(
    val tallyId: String = "",
    val contests: List<electionguard.protogen.DecryptedContest> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedTallyOrBallot = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedTallyOrBallot> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedTallyOrBallot> {
        public val defaultInstance: electionguard.protogen.DecryptedTallyOrBallot by lazy { electionguard.protogen.DecryptedTallyOrBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedTallyOrBallot = electionguard.protogen.DecryptedTallyOrBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedTallyOrBallot> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedTallyOrBallot, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "tallyId",
                        value = electionguard.protogen.DecryptedTallyOrBallot::tallyId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contests",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.DecryptedContest>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DecryptedContest.Companion)),
                        jsonName = "contests",
                        value = electionguard.protogen.DecryptedTallyOrBallot::contests
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptedTallyOrBallot",
                messageClass = electionguard.protogen.DecryptedTallyOrBallot::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class DecryptedContest(
    val contestId: String = "",
    val selections: List<electionguard.protogen.DecryptedSelection> = emptyList(),
    val decryptedContestData: electionguard.protogen.DecryptedContestData? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedContest> {
        public val defaultInstance: electionguard.protogen.DecryptedContest by lazy { electionguard.protogen.DecryptedContest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedContest = electionguard.protogen.DecryptedContest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedContest, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "contestId",
                        value = electionguard.protogen.DecryptedContest::contestId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selections",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.DecryptedSelection>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DecryptedSelection.Companion)),
                        jsonName = "selections",
                        value = electionguard.protogen.DecryptedContest::selections
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "decrypted_contest_data",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DecryptedContestData.Companion),
                        jsonName = "decryptedContestData",
                        value = electionguard.protogen.DecryptedContest::decryptedContestData
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptedContest",
                messageClass = electionguard.protogen.DecryptedContest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class DecryptedContestData(
    val contestData: electionguard.protogen.ContestData? = null,
    val encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null,
    val partialDecryptions: List<electionguard.protogen.PartialDecryption> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedContestData = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContestData> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedContestData> {
        public val defaultInstance: electionguard.protogen.DecryptedContestData by lazy { electionguard.protogen.DecryptedContestData() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedContestData = electionguard.protogen.DecryptedContestData.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContestData> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedContestData, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "contest_data",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ContestData.Companion),
                        jsonName = "contestData",
                        value = electionguard.protogen.DecryptedContestData::contestData
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_contest_data",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.HashedElGamalCiphertext.Companion),
                        jsonName = "encryptedContestData",
                        value = electionguard.protogen.DecryptedContestData::encryptedContestData
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "partial_decryptions",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PartialDecryption>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PartialDecryption.Companion)),
                        jsonName = "partialDecryptions",
                        value = electionguard.protogen.DecryptedContestData::partialDecryptions
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptedContestData",
                messageClass = electionguard.protogen.DecryptedContestData::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class DecryptedSelection(
    val selectionId: String = "",
    val tally: Int = 0,
    val value: electionguard.protogen.ElementModP? = null,
    val message: electionguard.protogen.ElGamalCiphertext? = null,
    val partialDecryptions: List<electionguard.protogen.PartialDecryption> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedSelection = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedSelection> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedSelection> {
        public val defaultInstance: electionguard.protogen.DecryptedSelection by lazy { electionguard.protogen.DecryptedSelection() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedSelection = electionguard.protogen.DecryptedSelection.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedSelection> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedSelection, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.DecryptedSelection::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "tally",
                        value = electionguard.protogen.DecryptedSelection::tally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "value",
                        value = electionguard.protogen.DecryptedSelection::value
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "message",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "message",
                        value = electionguard.protogen.DecryptedSelection::message
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "partial_decryptions",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.PartialDecryption>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PartialDecryption.Companion)),
                        jsonName = "partialDecryptions",
                        value = electionguard.protogen.DecryptedSelection::partialDecryptions
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptedSelection",
                messageClass = electionguard.protogen.DecryptedSelection::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class PartialDecryption(
    val selectionId: String = "",
    val guardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val proofOrParts: ProofOrParts<*>? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    public sealed class ProofOrParts<V>(value: V) : pbandk.Message.OneOf<V>(value) {
        public class Proof(proof: electionguard.protogen.GenericChaumPedersenProof) : ProofOrParts<electionguard.protogen.GenericChaumPedersenProof>(proof)
        public class RecoveredParts(recoveredParts: electionguard.protogen.RecoveredParts) : ProofOrParts<electionguard.protogen.RecoveredParts>(recoveredParts)
    }

    val proof: electionguard.protogen.GenericChaumPedersenProof?
        get() = (proofOrParts as? ProofOrParts.Proof)?.value
    val recoveredParts: electionguard.protogen.RecoveredParts?
        get() = (proofOrParts as? ProofOrParts.RecoveredParts)?.value

    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PartialDecryption = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PartialDecryption> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PartialDecryption> {
        public val defaultInstance: electionguard.protogen.PartialDecryption by lazy { electionguard.protogen.PartialDecryption() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PartialDecryption = electionguard.protogen.PartialDecryption.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PartialDecryption> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PartialDecryption, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "selection_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "selectionId",
                        value = electionguard.protogen.PartialDecryption::selectionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.PartialDecryption::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "share",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "share",
                        value = electionguard.protogen.PartialDecryption::share
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion),
                        oneofMember = true,
                        jsonName = "proof",
                        value = electionguard.protogen.PartialDecryption::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "recovered_parts",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.RecoveredParts.Companion),
                        oneofMember = true,
                        jsonName = "recoveredParts",
                        value = electionguard.protogen.PartialDecryption::recoveredParts
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "PartialDecryption",
                messageClass = electionguard.protogen.PartialDecryption::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class RecoveredParts(
    val fragments: List<electionguard.protogen.RecoveredPartialDecryption> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RecoveredParts = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredParts> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RecoveredParts> {
        public val defaultInstance: electionguard.protogen.RecoveredParts by lazy { electionguard.protogen.RecoveredParts() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RecoveredParts = electionguard.protogen.RecoveredParts.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredParts> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RecoveredParts, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "fragments",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.RecoveredPartialDecryption>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.RecoveredPartialDecryption.Companion)),
                        jsonName = "fragments",
                        value = electionguard.protogen.RecoveredParts::fragments
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "RecoveredParts",
                messageClass = electionguard.protogen.RecoveredParts::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class RecoveredPartialDecryption(
    val decryptingGuardianId: String = "",
    val missingGuardianId: String = "",
    val share: electionguard.protogen.ElementModP? = null,
    val recoveryKey: electionguard.protogen.ElementModP? = null,
    val proof: electionguard.protogen.GenericChaumPedersenProof? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RecoveredPartialDecryption = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredPartialDecryption> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RecoveredPartialDecryption> {
        public val defaultInstance: electionguard.protogen.RecoveredPartialDecryption by lazy { electionguard.protogen.RecoveredPartialDecryption() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RecoveredPartialDecryption = electionguard.protogen.RecoveredPartialDecryption.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RecoveredPartialDecryption> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RecoveredPartialDecryption, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "decrypting_guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "decryptingGuardianId",
                        value = electionguard.protogen.RecoveredPartialDecryption::decryptingGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "missing_guardian_id",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "missingGuardianId",
                        value = electionguard.protogen.RecoveredPartialDecryption::missingGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "share",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "share",
                        value = electionguard.protogen.RecoveredPartialDecryption::share
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "recovery_key",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "recoveryKey",
                        value = electionguard.protogen.RecoveredPartialDecryption::recoveryKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GenericChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.RecoveredPartialDecryption::proof
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "RecoveredPartialDecryption",
                messageClass = electionguard.protogen.RecoveredPartialDecryption::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedTallyOrBallot")
public fun DecryptedTallyOrBallot?.orDefault(): electionguard.protogen.DecryptedTallyOrBallot = this ?: DecryptedTallyOrBallot.defaultInstance

private fun DecryptedTallyOrBallot.protoMergeImpl(plus: pbandk.Message?): DecryptedTallyOrBallot = (plus as? DecryptedTallyOrBallot)?.let {
    it.copy(
        contests = contests + plus.contests,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedTallyOrBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedTallyOrBallot {
    var tallyId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.DecryptedContest>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> tallyId = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.DecryptedContest> }
        }
    }
    return DecryptedTallyOrBallot(tallyId, pbandk.ListWithSize.Builder.fixed(contests), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedContest")
public fun DecryptedContest?.orDefault(): electionguard.protogen.DecryptedContest = this ?: DecryptedContest.defaultInstance

private fun DecryptedContest.protoMergeImpl(plus: pbandk.Message?): DecryptedContest = (plus as? DecryptedContest)?.let {
    it.copy(
        selections = selections + plus.selections,
        decryptedContestData = decryptedContestData?.plus(plus.decryptedContestData) ?: plus.decryptedContestData,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedContest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedContest {
    var contestId = ""
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.DecryptedSelection>? = null
    var decryptedContestData: electionguard.protogen.DecryptedContestData? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestId = _fieldValue as String
            2 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.DecryptedSelection> }
            3 -> decryptedContestData = _fieldValue as electionguard.protogen.DecryptedContestData
        }
    }
    return DecryptedContest(contestId, pbandk.ListWithSize.Builder.fixed(selections), decryptedContestData, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedContestData")
public fun DecryptedContestData?.orDefault(): electionguard.protogen.DecryptedContestData = this ?: DecryptedContestData.defaultInstance

private fun DecryptedContestData.protoMergeImpl(plus: pbandk.Message?): DecryptedContestData = (plus as? DecryptedContestData)?.let {
    it.copy(
        contestData = contestData?.plus(plus.contestData) ?: plus.contestData,
        encryptedContestData = encryptedContestData?.plus(plus.encryptedContestData) ?: plus.encryptedContestData,
        partialDecryptions = partialDecryptions + plus.partialDecryptions,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedContestData.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedContestData {
    var contestData: electionguard.protogen.ContestData? = null
    var encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null
    var partialDecryptions: pbandk.ListWithSize.Builder<electionguard.protogen.PartialDecryption>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestData = _fieldValue as electionguard.protogen.ContestData
            2 -> encryptedContestData = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
            3 -> partialDecryptions = (partialDecryptions ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.PartialDecryption> }
        }
    }
    return DecryptedContestData(contestData, encryptedContestData, pbandk.ListWithSize.Builder.fixed(partialDecryptions), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedSelection")
public fun DecryptedSelection?.orDefault(): electionguard.protogen.DecryptedSelection = this ?: DecryptedSelection.defaultInstance

private fun DecryptedSelection.protoMergeImpl(plus: pbandk.Message?): DecryptedSelection = (plus as? DecryptedSelection)?.let {
    it.copy(
        value = value?.plus(plus.value) ?: plus.value,
        message = message?.plus(plus.message) ?: plus.message,
        partialDecryptions = partialDecryptions + plus.partialDecryptions,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedSelection {
    var selectionId = ""
    var tally = 0
    var value: electionguard.protogen.ElementModP? = null
    var message: electionguard.protogen.ElGamalCiphertext? = null
    var partialDecryptions: pbandk.ListWithSize.Builder<electionguard.protogen.PartialDecryption>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> tally = _fieldValue as Int
            3 -> value = _fieldValue as electionguard.protogen.ElementModP
            4 -> message = _fieldValue as electionguard.protogen.ElGamalCiphertext
            5 -> partialDecryptions = (partialDecryptions ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.PartialDecryption> }
        }
    }
    return DecryptedSelection(selectionId, tally, value, message,
        pbandk.ListWithSize.Builder.fixed(partialDecryptions), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForPartialDecryption")
public fun PartialDecryption?.orDefault(): electionguard.protogen.PartialDecryption = this ?: PartialDecryption.defaultInstance

private fun PartialDecryption.protoMergeImpl(plus: pbandk.Message?): PartialDecryption = (plus as? PartialDecryption)?.let {
    it.copy(
        share = share?.plus(plus.share) ?: plus.share,
        proofOrParts = when {
            proofOrParts is PartialDecryption.ProofOrParts.Proof && plus.proofOrParts is PartialDecryption.ProofOrParts.Proof ->
                PartialDecryption.ProofOrParts.Proof(proofOrParts.value + plus.proofOrParts.value)
            proofOrParts is PartialDecryption.ProofOrParts.RecoveredParts && plus.proofOrParts is PartialDecryption.ProofOrParts.RecoveredParts ->
                PartialDecryption.ProofOrParts.RecoveredParts(proofOrParts.value + plus.proofOrParts.value)
            else ->
                plus.proofOrParts ?: proofOrParts
        },
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun PartialDecryption.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PartialDecryption {
    var selectionId = ""
    var guardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var proofOrParts: PartialDecryption.ProofOrParts<*>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> guardianId = _fieldValue as String
            3 -> share = _fieldValue as electionguard.protogen.ElementModP
            4 -> proofOrParts = PartialDecryption.ProofOrParts.Proof(_fieldValue as electionguard.protogen.GenericChaumPedersenProof)
            5 -> proofOrParts = PartialDecryption.ProofOrParts.RecoveredParts(_fieldValue as electionguard.protogen.RecoveredParts)
        }
    }
    return PartialDecryption(selectionId, guardianId, share, proofOrParts, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForRecoveredParts")
public fun RecoveredParts?.orDefault(): electionguard.protogen.RecoveredParts = this ?: RecoveredParts.defaultInstance

private fun RecoveredParts.protoMergeImpl(plus: pbandk.Message?): RecoveredParts = (plus as? RecoveredParts)?.let {
    it.copy(
        fragments = fragments + plus.fragments,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun RecoveredParts.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RecoveredParts {
    var fragments: pbandk.ListWithSize.Builder<electionguard.protogen.RecoveredPartialDecryption>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> fragments = (fragments ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.RecoveredPartialDecryption> }
        }
    }
    return RecoveredParts(pbandk.ListWithSize.Builder.fixed(fragments), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForRecoveredPartialDecryption")
public fun RecoveredPartialDecryption?.orDefault(): electionguard.protogen.RecoveredPartialDecryption = this ?: RecoveredPartialDecryption.defaultInstance

private fun RecoveredPartialDecryption.protoMergeImpl(plus: pbandk.Message?): RecoveredPartialDecryption = (plus as? RecoveredPartialDecryption)?.let {
    it.copy(
        share = share?.plus(plus.share) ?: plus.share,
        recoveryKey = recoveryKey?.plus(plus.recoveryKey) ?: plus.recoveryKey,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun RecoveredPartialDecryption.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RecoveredPartialDecryption {
    var decryptingGuardianId = ""
    var missingGuardianId = ""
    var share: electionguard.protogen.ElementModP? = null
    var recoveryKey: electionguard.protogen.ElementModP? = null
    var proof: electionguard.protogen.GenericChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            2 -> decryptingGuardianId = _fieldValue as String
            3 -> missingGuardianId = _fieldValue as String
            4 -> share = _fieldValue as electionguard.protogen.ElementModP
            5 -> recoveryKey = _fieldValue as electionguard.protogen.ElementModP
            6 -> proof = _fieldValue as electionguard.protogen.GenericChaumPedersenProof
        }
    }
    return RecoveredPartialDecryption(decryptingGuardianId, missingGuardianId, share, recoveryKey,
        proof, unknownFields)
}
