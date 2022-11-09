@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptingTrustee(
    val guardianId: String = "",
    val guardianXCoordinate: Int = 0,
    val electionKeypair: electionguard.protogen.ElGamalKeypair? = null,
    val secretKeyShares: List<electionguard.protogen.EncryptedKeyShare> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptingTrustee = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptingTrustee> {
        public val defaultInstance: electionguard.protogen.DecryptingTrustee by lazy { electionguard.protogen.DecryptingTrustee() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptingTrustee = electionguard.protogen.DecryptingTrustee.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptingTrustee, *>>(4)
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
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptedKeyShare>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedKeyShare.Companion)),
                        jsonName = "secretKeyShares",
                        value = electionguard.protogen.DecryptingTrustee::secretKeyShares
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
public data class EncryptedKeyShare(
    val generatingGuardianId: String = "",
    val designatedGuardianId: String = "",
    val encryptedCoordinate: electionguard.protogen.HashedElGamalCiphertext? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptedKeyShare = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedKeyShare> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptedKeyShare> {
        public val defaultInstance: electionguard.protogen.EncryptedKeyShare by lazy { electionguard.protogen.EncryptedKeyShare() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptedKeyShare = electionguard.protogen.EncryptedKeyShare.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptedKeyShare> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedKeyShare, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "generating_guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "generatingGuardianId",
                        value = electionguard.protogen.EncryptedKeyShare::generatingGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "designated_guardian_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "designatedGuardianId",
                        value = electionguard.protogen.EncryptedKeyShare::designatedGuardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_coordinate",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.HashedElGamalCiphertext.Companion),
                        jsonName = "encryptedCoordinate",
                        value = electionguard.protogen.EncryptedKeyShare::encryptedCoordinate
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptedKeyShare",
                messageClass = electionguard.protogen.EncryptedKeyShare::class,
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
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptingTrustee.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptingTrustee {
    var guardianId = ""
    var guardianXCoordinate = 0
    var electionKeypair: electionguard.protogen.ElGamalKeypair? = null
    var secretKeyShares: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptedKeyShare>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> guardianXCoordinate = _fieldValue as Int
            3 -> electionKeypair = _fieldValue as electionguard.protogen.ElGamalKeypair
            4 -> secretKeyShares = (secretKeyShares ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.EncryptedKeyShare> }
        }
    }
    return DecryptingTrustee(guardianId, guardianXCoordinate, electionKeypair, pbandk.ListWithSize.Builder.fixed(secretKeyShares), unknownFields)
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
@pbandk.JsName("orDefaultForEncryptedKeyShare")
public fun EncryptedKeyShare?.orDefault(): electionguard.protogen.EncryptedKeyShare = this ?: EncryptedKeyShare.defaultInstance

private fun EncryptedKeyShare.protoMergeImpl(plus: pbandk.Message?): EncryptedKeyShare = (plus as? EncryptedKeyShare)?.let {
    it.copy(
        encryptedCoordinate = encryptedCoordinate?.plus(plus.encryptedCoordinate) ?: plus.encryptedCoordinate,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptedKeyShare.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptedKeyShare {
    var generatingGuardianId = ""
    var designatedGuardianId = ""
    var encryptedCoordinate: electionguard.protogen.HashedElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> generatingGuardianId = _fieldValue as String
            2 -> designatedGuardianId = _fieldValue as String
            5 -> encryptedCoordinate = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
        }
    }
    return EncryptedKeyShare(generatingGuardianId, designatedGuardianId, encryptedCoordinate, unknownFields)
}
