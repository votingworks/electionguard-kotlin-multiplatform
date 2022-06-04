@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptingTrustee(
    val guardianId: String = "",
    val guardianXCoordinate: Int = 0,
    val electionKeypair: electionguard.protogen.ElGamalKeypair? = null,
    val secretKeyShares: List<electionguard.protogen.SecretKeyShare> = emptyList(),
    val coefficientCommitments: List<electionguard.protogen.CommitmentSet> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptingTrustee = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptingTrustee> {
        public val defaultInstance: electionguard.protogen.DecryptingTrustee by lazy { electionguard.protogen.DecryptingTrustee() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptingTrustee = electionguard.protogen.DecryptingTrustee.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptingTrustee, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.DecryptingTrustee::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_x_coordinate",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "guardianXCoordinate",
                        value = electionguard.protogen.DecryptingTrustee::guardianXCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "election_keypair",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElGamalKeypair.Companion),
                        jsonName = "electionKeypair",
                        value = electionguard.protogen.DecryptingTrustee::electionKeypair
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "secret_key_shares",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.SecretKeyShare>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.SecretKeyShare.Companion)),
                        jsonName = "secretKeyShares",
                        value = electionguard.protogen.DecryptingTrustee::secretKeyShares
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "coefficient_commitments",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.CommitmentSet>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CommitmentSet.Companion)),
                        jsonName = "coefficientCommitments",
                        value = electionguard.protogen.DecryptingTrustee::coefficientCommitments
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptingTrustee",
                messageClass = electionguard.protogen.DecryptingTrustee::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ElGamalKeypair(
    val secretKey: electionguard.protogen.ElementModQ? = null,
    val publicKey: electionguard.protogen.ElementModP? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElGamalKeypair = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeypair> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElGamalKeypair> {
        public val defaultInstance: electionguard.protogen.ElGamalKeypair by lazy { electionguard.protogen.ElGamalKeypair() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElGamalKeypair = electionguard.protogen.ElGamalKeypair.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeypair> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElGamalKeypair, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "secret_key",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "secretKey",
                        value = electionguard.protogen.ElGamalKeypair::secretKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "public_key",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "publicKey",
                        value = electionguard.protogen.ElGamalKeypair::publicKey
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElGamalKeypair",
                messageClass = electionguard.protogen.ElGamalKeypair::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class CommitmentSet(
    val guardianId: String = "",
    val commitments: List<electionguard.protogen.ElementModP> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CommitmentSet = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CommitmentSet> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CommitmentSet> {
        public val defaultInstance: electionguard.protogen.CommitmentSet by lazy { electionguard.protogen.CommitmentSet() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CommitmentSet = electionguard.protogen.CommitmentSet.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CommitmentSet> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CommitmentSet, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.CommitmentSet::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "commitments",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElementModP>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion)),
                        jsonName = "commitments",
                        value = electionguard.protogen.CommitmentSet::commitments
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "CommitmentSet",
                messageClass = electionguard.protogen.CommitmentSet::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class SecretKeyShare(
    val generatingGuardianId: String = "",
    val designatedGuardianId: String = "",
    val designatedGuardianXCoordinate: Int = 0,
    val encryptedCoordinate: electionguard.protogen.HashedElGamalCiphertext? = null,
    val error: String = "",
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.SecretKeyShare = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SecretKeyShare> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.SecretKeyShare> {
        public val defaultInstance: electionguard.protogen.SecretKeyShare by lazy { electionguard.protogen.SecretKeyShare() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.SecretKeyShare = electionguard.protogen.SecretKeyShare.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SecretKeyShare> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.SecretKeyShare, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "generating_guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "generatingGuardianId",
                        value = electionguard.protogen.SecretKeyShare::generatingGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "designated_guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "designatedGuardianId",
                        value = electionguard.protogen.SecretKeyShare::designatedGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "designated_guardian_x_coordinate",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "designatedGuardianXCoordinate",
                        value = electionguard.protogen.SecretKeyShare::designatedGuardianXCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "error",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "error",
                        value = electionguard.protogen.SecretKeyShare::error
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_coordinate",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.HashedElGamalCiphertext.Companion),
                        jsonName = "encryptedCoordinate",
                        value = electionguard.protogen.SecretKeyShare::encryptedCoordinate
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "SecretKeyShare",
                messageClass = electionguard.protogen.SecretKeyShare::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptingTrustee")
public fun DecryptingTrustee?.orDefault(): electionguard.protogen.DecryptingTrustee = this ?: DecryptingTrustee.defaultInstance

private fun DecryptingTrustee.protoMergeImpl(plus: pbandk.Message?): DecryptingTrustee = (plus as? DecryptingTrustee)?.let {
    it.copy(
        electionKeypair = electionKeypair?.plus(plus.electionKeypair) ?: plus.electionKeypair,
        secretKeyShares = secretKeyShares + plus.secretKeyShares,
        coefficientCommitments = coefficientCommitments + plus.coefficientCommitments,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptingTrustee.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptingTrustee {
    var guardianId = ""
    var guardianXCoordinate = 0
    var electionKeypair: electionguard.protogen.ElGamalKeypair? = null
    var secretKeyShares: pbandk.ListWithSize.Builder<electionguard.protogen.SecretKeyShare>? = null
    var coefficientCommitments: pbandk.ListWithSize.Builder<electionguard.protogen.CommitmentSet>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> guardianXCoordinate = _fieldValue as Int
            3 -> electionKeypair = _fieldValue as electionguard.protogen.ElGamalKeypair
            4 -> secretKeyShares = (secretKeyShares ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.SecretKeyShare> }
            5 -> coefficientCommitments = (coefficientCommitments ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.CommitmentSet> }
        }
    }
    return DecryptingTrustee(guardianId, guardianXCoordinate, electionKeypair, pbandk.ListWithSize.Builder.fixed(secretKeyShares),
        pbandk.ListWithSize.Builder.fixed(coefficientCommitments), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElGamalKeypair")
public fun ElGamalKeypair?.orDefault(): electionguard.protogen.ElGamalKeypair = this ?: ElGamalKeypair.defaultInstance

private fun ElGamalKeypair.protoMergeImpl(plus: pbandk.Message?): ElGamalKeypair = (plus as? ElGamalKeypair)?.let {
    it.copy(
        secretKey = secretKey?.plus(plus.secretKey) ?: plus.secretKey,
        publicKey = publicKey?.plus(plus.publicKey) ?: plus.publicKey,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElGamalKeypair.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElGamalKeypair {
    var secretKey: electionguard.protogen.ElementModQ? = null
    var publicKey: electionguard.protogen.ElementModP? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> secretKey = _fieldValue as electionguard.protogen.ElementModQ
            2 -> publicKey = _fieldValue as electionguard.protogen.ElementModP
        }
    }
    return ElGamalKeypair(secretKey, publicKey, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCommitmentSet")
public fun CommitmentSet?.orDefault(): electionguard.protogen.CommitmentSet = this ?: CommitmentSet.defaultInstance

private fun CommitmentSet.protoMergeImpl(plus: pbandk.Message?): CommitmentSet = (plus as? CommitmentSet)?.let {
    it.copy(
        commitments = commitments + plus.commitments,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun CommitmentSet.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CommitmentSet {
    var guardianId = ""
    var commitments: pbandk.ListWithSize.Builder<electionguard.protogen.ElementModP>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            3 -> commitments = (commitments ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.ElementModP> }
        }
    }
    return CommitmentSet(guardianId, pbandk.ListWithSize.Builder.fixed(commitments), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForSecretKeyShare")
public fun SecretKeyShare?.orDefault(): electionguard.protogen.SecretKeyShare = this ?: SecretKeyShare.defaultInstance

private fun SecretKeyShare.protoMergeImpl(plus: pbandk.Message?): SecretKeyShare = (plus as? SecretKeyShare)?.let {
    it.copy(
        encryptedCoordinate = encryptedCoordinate?.plus(plus.encryptedCoordinate) ?: plus.encryptedCoordinate,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun SecretKeyShare.Companion.decodeWithImpl(u: pbandk.MessageDecoder): SecretKeyShare {
    var generatingGuardianId = ""
    var designatedGuardianId = ""
    var designatedGuardianXCoordinate = 0
    var encryptedCoordinate: electionguard.protogen.HashedElGamalCiphertext? = null
    var error = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> generatingGuardianId = _fieldValue as String
            2 -> designatedGuardianId = _fieldValue as String
            3 -> designatedGuardianXCoordinate = _fieldValue as Int
            5 -> error = _fieldValue as String
            6 -> encryptedCoordinate = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
        }
    }
    return SecretKeyShare(generatingGuardianId, designatedGuardianId, designatedGuardianXCoordinate, encryptedCoordinate,
        error, unknownFields)
}
