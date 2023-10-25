@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptedTallyOrBallot(
    val id: String = "",
    val contests: List<electionguard.protogen.DecryptedContest> = emptyList(),
    val electionId: electionguard.protogen.UInt256? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedTallyOrBallot = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedTallyOrBallot> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedTallyOrBallot> {
        public val defaultInstance: electionguard.protogen.DecryptedTallyOrBallot by lazy { electionguard.protogen.DecryptedTallyOrBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedTallyOrBallot = electionguard.protogen.DecryptedTallyOrBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedTallyOrBallot> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedTallyOrBallot, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "id",
                        value = electionguard.protogen.DecryptedTallyOrBallot::id
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
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "election_id",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "electionId",
                        value = electionguard.protogen.DecryptedTallyOrBallot::electionId
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
public data class DecryptedSelection(
    val selectionId: String = "",
    val tally: Int = 0,
    val bOverM: electionguard.protogen.ElementModP? = null,
    val encryptedVote: electionguard.protogen.ElGamalCiphertext? = null,
    val proof: electionguard.protogen.ChaumPedersenProof? = null,
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
                        name = "b_over_m",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "bOverM",
                        value = electionguard.protogen.DecryptedSelection::bOverM
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_vote",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalCiphertext.Companion),
                        jsonName = "encryptedVote",
                        value = electionguard.protogen.DecryptedSelection::encryptedVote
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proof",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.DecryptedSelection::proof
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
public data class DecryptedContestData(
    val contestData: electionguard.protogen.ContestData? = null,
    val encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null,
    val proof: electionguard.protogen.ChaumPedersenProof? = null,
    val beta: electionguard.protogen.ElementModP? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptedContestData = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContestData> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptedContestData> {
        public val defaultInstance: electionguard.protogen.DecryptedContestData by lazy { electionguard.protogen.DecryptedContestData() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptedContestData = electionguard.protogen.DecryptedContestData.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptedContestData> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptedContestData, *>>(4)
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
                        name = "proof",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ChaumPedersenProof.Companion),
                        jsonName = "proof",
                        value = electionguard.protogen.DecryptedContestData::proof
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "beta",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "beta",
                        value = electionguard.protogen.DecryptedContestData::beta
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
@pbandk.JsName("orDefaultForDecryptedTallyOrBallot")
public fun DecryptedTallyOrBallot?.orDefault(): electionguard.protogen.DecryptedTallyOrBallot = this ?: DecryptedTallyOrBallot.defaultInstance

private fun DecryptedTallyOrBallot.protoMergeImpl(plus: pbandk.Message?): DecryptedTallyOrBallot = (plus as? DecryptedTallyOrBallot)?.let {
    it.copy(
        contests = contests + plus.contests,
        electionId = electionId?.plus(plus.electionId) ?: plus.electionId,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedTallyOrBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedTallyOrBallot {
    var id = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.DecryptedContest>? = null
    var electionId: electionguard.protogen.UInt256? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> id = _fieldValue as String
            2 -> contests = (contests ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.DecryptedContest> }
            3 -> electionId = _fieldValue as electionguard.protogen.UInt256
        }
    }

    return DecryptedTallyOrBallot(id, pbandk.ListWithSize.Builder.fixed(contests), electionId, unknownFields)
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
            2 -> selections = (selections ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.DecryptedSelection> }
            3 -> decryptedContestData = _fieldValue as electionguard.protogen.DecryptedContestData
        }
    }

    return DecryptedContest(contestId, pbandk.ListWithSize.Builder.fixed(selections), decryptedContestData, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedSelection")
public fun DecryptedSelection?.orDefault(): electionguard.protogen.DecryptedSelection = this ?: DecryptedSelection.defaultInstance

private fun DecryptedSelection.protoMergeImpl(plus: pbandk.Message?): DecryptedSelection = (plus as? DecryptedSelection)?.let {
    it.copy(
        bOverM = bOverM?.plus(plus.bOverM) ?: plus.bOverM,
        encryptedVote = encryptedVote?.plus(plus.encryptedVote) ?: plus.encryptedVote,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedSelection.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedSelection {
    var selectionId = ""
    var tally = 0
    var bOverM: electionguard.protogen.ElementModP? = null
    var encryptedVote: electionguard.protogen.ElGamalCiphertext? = null
    var proof: electionguard.protogen.ChaumPedersenProof? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> selectionId = _fieldValue as String
            2 -> tally = _fieldValue as Int
            3 -> bOverM = _fieldValue as electionguard.protogen.ElementModP
            4 -> encryptedVote = _fieldValue as electionguard.protogen.ElGamalCiphertext
            5 -> proof = _fieldValue as electionguard.protogen.ChaumPedersenProof
        }
    }

    return DecryptedSelection(selectionId, tally, bOverM, encryptedVote,
        proof, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptedContestData")
public fun DecryptedContestData?.orDefault(): electionguard.protogen.DecryptedContestData = this ?: DecryptedContestData.defaultInstance

private fun DecryptedContestData.protoMergeImpl(plus: pbandk.Message?): DecryptedContestData = (plus as? DecryptedContestData)?.let {
    it.copy(
        contestData = contestData?.plus(plus.contestData) ?: plus.contestData,
        encryptedContestData = encryptedContestData?.plus(plus.encryptedContestData) ?: plus.encryptedContestData,
        proof = proof?.plus(plus.proof) ?: plus.proof,
        beta = beta?.plus(plus.beta) ?: plus.beta,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptedContestData.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptedContestData {
    var contestData: electionguard.protogen.ContestData? = null
    var encryptedContestData: electionguard.protogen.HashedElGamalCiphertext? = null
    var proof: electionguard.protogen.ChaumPedersenProof? = null
    var beta: electionguard.protogen.ElementModP? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> contestData = _fieldValue as electionguard.protogen.ContestData
            2 -> encryptedContestData = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
            3 -> proof = _fieldValue as electionguard.protogen.ChaumPedersenProof
            4 -> beta = _fieldValue as electionguard.protogen.ElementModP
        }
    }

    return DecryptedContestData(contestData, encryptedContestData, proof, beta, unknownFields)
}
