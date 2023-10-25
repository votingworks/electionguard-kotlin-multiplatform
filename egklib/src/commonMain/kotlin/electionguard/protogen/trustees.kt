@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptingTrustee(
    val guardianId: String = "",
    val guardianXCoordinate: Int = 0,
    val publicKey: electionguard.protogen.ElementModP? = null,
    val keyShare: electionguard.protogen.ElementModQ? = null,
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
                        name = "public_key",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "publicKey",
                        value = electionguard.protogen.DecryptingTrustee::publicKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "key_share",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "keyShare",
                        value = electionguard.protogen.DecryptingTrustee::keyShare
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
public data class EncryptedKeyShare(
    val ownerXcoord: Int = 0,
    val polynomialOwner: String = "",
    val secretShareFor: String = "",
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
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptedKeyShare, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "owner_xcoord",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "ownerXcoord",
                        value = electionguard.protogen.EncryptedKeyShare::ownerXcoord
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "polynomial_owner",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "polynomialOwner",
                        value = electionguard.protogen.EncryptedKeyShare::polynomialOwner
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "secret_share_for",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "secretShareFor",
                        value = electionguard.protogen.EncryptedKeyShare::secretShareFor
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_coordinate",
                        number = 4,
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
        publicKey = publicKey?.plus(plus.publicKey) ?: plus.publicKey,
        keyShare = keyShare?.plus(plus.keyShare) ?: plus.keyShare,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptingTrustee.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptingTrustee {
    var guardianId = ""
    var guardianXCoordinate = 0
    var publicKey: electionguard.protogen.ElementModP? = null
    var keyShare: electionguard.protogen.ElementModQ? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> guardianXCoordinate = _fieldValue as Int
            3 -> publicKey = _fieldValue as electionguard.protogen.ElementModP
            4 -> keyShare = _fieldValue as electionguard.protogen.ElementModQ
        }
    }

    return DecryptingTrustee(guardianId, guardianXCoordinate, publicKey, keyShare, unknownFields)
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
    var ownerXcoord = 0
    var polynomialOwner = ""
    var secretShareFor = ""
    var encryptedCoordinate: electionguard.protogen.HashedElGamalCiphertext? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> ownerXcoord = _fieldValue as Int
            2 -> polynomialOwner = _fieldValue as String
            3 -> secretShareFor = _fieldValue as String
            4 -> encryptedCoordinate = _fieldValue as electionguard.protogen.HashedElGamalCiphertext
        }
    }

    return EncryptedKeyShare(ownerXcoord, polynomialOwner, secretShareFor, encryptedCoordinate, unknownFields)
}
