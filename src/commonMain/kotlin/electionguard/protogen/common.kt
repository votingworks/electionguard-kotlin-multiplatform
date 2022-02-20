@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class ElementModP(
    val value: pbandk.ByteArr = pbandk.ByteArr.empty,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElementModP = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElementModP> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElementModP> {
        public val defaultInstance: electionguard.protogen.ElementModP by lazy { electionguard.protogen.ElementModP() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElementModP = electionguard.protogen.ElementModP.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElementModP> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElementModP, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "value",
                        value = electionguard.protogen.ElementModP::value
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElementModP",
                messageClass = electionguard.protogen.ElementModP::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ElementModQ(
    val value: pbandk.ByteArr = pbandk.ByteArr.empty,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElementModQ = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElementModQ> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElementModQ> {
        public val defaultInstance: electionguard.protogen.ElementModQ by lazy { electionguard.protogen.ElementModQ() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElementModQ = electionguard.protogen.ElementModQ.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElementModQ> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElementModQ, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "value",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "value",
                        value = electionguard.protogen.ElementModQ::value
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElementModQ",
                messageClass = electionguard.protogen.ElementModQ::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ElGamalCiphertext(
    val pad: electionguard.protogen.ElementModP? = null,
    val data: electionguard.protogen.ElementModP? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElGamalCiphertext = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalCiphertext> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElGamalCiphertext> {
        public val defaultInstance: electionguard.protogen.ElGamalCiphertext by lazy { electionguard.protogen.ElGamalCiphertext() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElGamalCiphertext = electionguard.protogen.ElGamalCiphertext.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalCiphertext> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElGamalCiphertext, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "pad",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "pad",
                        value = electionguard.protogen.ElGamalCiphertext::pad
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "data",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "data",
                        value = electionguard.protogen.ElGamalCiphertext::data
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElGamalCiphertext",
                messageClass = electionguard.protogen.ElGamalCiphertext::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ElGamalKeyPair(
    val secretKey: electionguard.protogen.ElementModQ? = null,
    val publicKey: electionguard.protogen.ElementModP? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElGamalKeyPair = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeyPair> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElGamalKeyPair> {
        public val defaultInstance: electionguard.protogen.ElGamalKeyPair by lazy { electionguard.protogen.ElGamalKeyPair() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElGamalKeyPair = electionguard.protogen.ElGamalKeyPair.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeyPair> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElGamalKeyPair, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "secret_key",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "secretKey",
                        value = electionguard.protogen.ElGamalKeyPair::secretKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "public_key",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "publicKey",
                        value = electionguard.protogen.ElGamalKeyPair::publicKey
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElGamalKeyPair",
                messageClass = electionguard.protogen.ElGamalKeyPair::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class RSAPublicKey(
    val modulus: pbandk.ByteArr = pbandk.ByteArr.empty,
    val publicExponent: pbandk.ByteArr = pbandk.ByteArr.empty,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RSAPublicKey = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RSAPublicKey> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RSAPublicKey> {
        public val defaultInstance: electionguard.protogen.RSAPublicKey by lazy { electionguard.protogen.RSAPublicKey() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RSAPublicKey = electionguard.protogen.RSAPublicKey.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RSAPublicKey> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RSAPublicKey, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "modulus",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "modulus",
                        value = electionguard.protogen.RSAPublicKey::modulus
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "public_exponent",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "publicExponent",
                        value = electionguard.protogen.RSAPublicKey::publicExponent
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "RSAPublicKey",
                messageClass = electionguard.protogen.RSAPublicKey::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class RSAPrivateKey(
    val modulus: pbandk.ByteArr = pbandk.ByteArr.empty,
    val privateExponent: pbandk.ByteArr = pbandk.ByteArr.empty,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.RSAPrivateKey = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RSAPrivateKey> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.RSAPrivateKey> {
        public val defaultInstance: electionguard.protogen.RSAPrivateKey by lazy { electionguard.protogen.RSAPrivateKey() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.RSAPrivateKey = electionguard.protogen.RSAPrivateKey.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.RSAPrivateKey> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.RSAPrivateKey, *>>(2)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "modulus",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "modulus",
                        value = electionguard.protogen.RSAPrivateKey::modulus
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "private_exponent",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "privateExponent",
                        value = electionguard.protogen.RSAPrivateKey::privateExponent
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "RSAPrivateKey",
                messageClass = electionguard.protogen.RSAPrivateKey::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ChaumPedersenProof(
    val pad: electionguard.protogen.ElementModP? = null,
    val data: electionguard.protogen.ElementModP? = null,
    val challenge: electionguard.protogen.ElementModQ? = null,
    val response: electionguard.protogen.ElementModQ? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ChaumPedersenProof = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ChaumPedersenProof> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ChaumPedersenProof> {
        public val defaultInstance: electionguard.protogen.ChaumPedersenProof by lazy { electionguard.protogen.ChaumPedersenProof() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ChaumPedersenProof = electionguard.protogen.ChaumPedersenProof.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ChaumPedersenProof> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ChaumPedersenProof, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "pad",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "pad",
                        value = electionguard.protogen.ChaumPedersenProof::pad
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "data",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "data",
                        value = electionguard.protogen.ChaumPedersenProof::data
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "challenge",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "challenge",
                        value = electionguard.protogen.ChaumPedersenProof::challenge
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "response",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "response",
                        value = electionguard.protogen.ChaumPedersenProof::response
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ChaumPedersenProof",
                messageClass = electionguard.protogen.ChaumPedersenProof::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class SchnorrProof(
    val publicKey: electionguard.protogen.ElementModP? = null,
    val commitment: electionguard.protogen.ElementModP? = null,
    val challenge: electionguard.protogen.ElementModQ? = null,
    val response: electionguard.protogen.ElementModQ? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.SchnorrProof = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SchnorrProof> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.SchnorrProof> {
        public val defaultInstance: electionguard.protogen.SchnorrProof by lazy { electionguard.protogen.SchnorrProof() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.SchnorrProof = electionguard.protogen.SchnorrProof.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.SchnorrProof> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.SchnorrProof, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "public_key",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "publicKey",
                        value = electionguard.protogen.SchnorrProof::publicKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "commitment",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "commitment",
                        value = electionguard.protogen.SchnorrProof::commitment
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "challenge",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "challenge",
                        value = electionguard.protogen.SchnorrProof::challenge
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "response",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "response",
                        value = electionguard.protogen.SchnorrProof::response
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "SchnorrProof",
                messageClass = electionguard.protogen.SchnorrProof::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class FinishRequest(
    val allOk: Boolean = false,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.FinishRequest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.FinishRequest> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.FinishRequest> {
        public val defaultInstance: electionguard.protogen.FinishRequest by lazy { electionguard.protogen.FinishRequest() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.FinishRequest = electionguard.protogen.FinishRequest.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.FinishRequest> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.FinishRequest, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "all_ok",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bool(),
                        jsonName = "allOk",
                        value = electionguard.protogen.FinishRequest::allOk
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "FinishRequest",
                messageClass = electionguard.protogen.FinishRequest::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ErrorResponse(
    val error: String = "",
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ErrorResponse = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ErrorResponse> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ErrorResponse> {
        public val defaultInstance: electionguard.protogen.ErrorResponse by lazy { electionguard.protogen.ErrorResponse() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ErrorResponse = electionguard.protogen.ErrorResponse.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ErrorResponse> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ErrorResponse, *>>(1)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "error",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "error",
                        value = electionguard.protogen.ErrorResponse::error
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ErrorResponse",
                messageClass = electionguard.protogen.ErrorResponse::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForElementModP")
public fun ElementModP?.orDefault(): electionguard.protogen.ElementModP = this ?: ElementModP.defaultInstance

private fun ElementModP.protoMergeImpl(plus: pbandk.Message?): ElementModP = (plus as? ElementModP)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElementModP.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElementModP {
    var value: pbandk.ByteArr = pbandk.ByteArr.empty

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> value = _fieldValue as pbandk.ByteArr
        }
    }
    return ElementModP(value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElementModQ")
public fun ElementModQ?.orDefault(): electionguard.protogen.ElementModQ = this ?: ElementModQ.defaultInstance

private fun ElementModQ.protoMergeImpl(plus: pbandk.Message?): ElementModQ = (plus as? ElementModQ)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElementModQ.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElementModQ {
    var value: pbandk.ByteArr = pbandk.ByteArr.empty

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> value = _fieldValue as pbandk.ByteArr
        }
    }
    return ElementModQ(value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElGamalCiphertext")
public fun ElGamalCiphertext?.orDefault(): electionguard.protogen.ElGamalCiphertext = this ?: ElGamalCiphertext.defaultInstance

private fun ElGamalCiphertext.protoMergeImpl(plus: pbandk.Message?): ElGamalCiphertext = (plus as? ElGamalCiphertext)?.let {
    it.copy(
        pad = pad?.plus(plus.pad) ?: plus.pad,
        data = data?.plus(plus.data) ?: plus.data,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElGamalCiphertext.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElGamalCiphertext {
    var pad: electionguard.protogen.ElementModP? = null
    var data: electionguard.protogen.ElementModP? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> pad = _fieldValue as electionguard.protogen.ElementModP
            2 -> data = _fieldValue as electionguard.protogen.ElementModP
        }
    }
    return ElGamalCiphertext(pad, data, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElGamalKeyPair")
public fun ElGamalKeyPair?.orDefault(): electionguard.protogen.ElGamalKeyPair = this ?: ElGamalKeyPair.defaultInstance

private fun ElGamalKeyPair.protoMergeImpl(plus: pbandk.Message?): ElGamalKeyPair = (plus as? ElGamalKeyPair)?.let {
    it.copy(
        secretKey = secretKey?.plus(plus.secretKey) ?: plus.secretKey,
        publicKey = publicKey?.plus(plus.publicKey) ?: plus.publicKey,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElGamalKeyPair.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElGamalKeyPair {
    var secretKey: electionguard.protogen.ElementModQ? = null
    var publicKey: electionguard.protogen.ElementModP? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> secretKey = _fieldValue as electionguard.protogen.ElementModQ
            2 -> publicKey = _fieldValue as electionguard.protogen.ElementModP
        }
    }
    return ElGamalKeyPair(secretKey, publicKey, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForRSAPublicKey")
public fun RSAPublicKey?.orDefault(): electionguard.protogen.RSAPublicKey = this ?: RSAPublicKey.defaultInstance

private fun RSAPublicKey.protoMergeImpl(plus: pbandk.Message?): RSAPublicKey = (plus as? RSAPublicKey)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun RSAPublicKey.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RSAPublicKey {
    var modulus: pbandk.ByteArr = pbandk.ByteArr.empty
    var publicExponent: pbandk.ByteArr = pbandk.ByteArr.empty

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> modulus = _fieldValue as pbandk.ByteArr
            2 -> publicExponent = _fieldValue as pbandk.ByteArr
        }
    }
    return RSAPublicKey(modulus, publicExponent, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForRSAPrivateKey")
public fun RSAPrivateKey?.orDefault(): electionguard.protogen.RSAPrivateKey = this ?: RSAPrivateKey.defaultInstance

private fun RSAPrivateKey.protoMergeImpl(plus: pbandk.Message?): RSAPrivateKey = (plus as? RSAPrivateKey)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun RSAPrivateKey.Companion.decodeWithImpl(u: pbandk.MessageDecoder): RSAPrivateKey {
    var modulus: pbandk.ByteArr = pbandk.ByteArr.empty
    var privateExponent: pbandk.ByteArr = pbandk.ByteArr.empty

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> modulus = _fieldValue as pbandk.ByteArr
            2 -> privateExponent = _fieldValue as pbandk.ByteArr
        }
    }
    return RSAPrivateKey(modulus, privateExponent, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForChaumPedersenProof")
public fun ChaumPedersenProof?.orDefault(): electionguard.protogen.ChaumPedersenProof = this ?: ChaumPedersenProof.defaultInstance

private fun ChaumPedersenProof.protoMergeImpl(plus: pbandk.Message?): ChaumPedersenProof = (plus as? ChaumPedersenProof)?.let {
    it.copy(
        pad = pad?.plus(plus.pad) ?: plus.pad,
        data = data?.plus(plus.data) ?: plus.data,
        challenge = challenge?.plus(plus.challenge) ?: plus.challenge,
        response = response?.plus(plus.response) ?: plus.response,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ChaumPedersenProof.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ChaumPedersenProof {
    var pad: electionguard.protogen.ElementModP? = null
    var data: electionguard.protogen.ElementModP? = null
    var challenge: electionguard.protogen.ElementModQ? = null
    var response: electionguard.protogen.ElementModQ? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> pad = _fieldValue as electionguard.protogen.ElementModP
            2 -> data = _fieldValue as electionguard.protogen.ElementModP
            3 -> challenge = _fieldValue as electionguard.protogen.ElementModQ
            4 -> response = _fieldValue as electionguard.protogen.ElementModQ
        }
    }
    return ChaumPedersenProof(pad, data, challenge, response, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForSchnorrProof")
public fun SchnorrProof?.orDefault(): electionguard.protogen.SchnorrProof = this ?: SchnorrProof.defaultInstance

private fun SchnorrProof.protoMergeImpl(plus: pbandk.Message?): SchnorrProof = (plus as? SchnorrProof)?.let {
    it.copy(
        publicKey = publicKey?.plus(plus.publicKey) ?: plus.publicKey,
        commitment = commitment?.plus(plus.commitment) ?: plus.commitment,
        challenge = challenge?.plus(plus.challenge) ?: plus.challenge,
        response = response?.plus(plus.response) ?: plus.response,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun SchnorrProof.Companion.decodeWithImpl(u: pbandk.MessageDecoder): SchnorrProof {
    var publicKey: electionguard.protogen.ElementModP? = null
    var commitment: electionguard.protogen.ElementModP? = null
    var challenge: electionguard.protogen.ElementModQ? = null
    var response: electionguard.protogen.ElementModQ? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> publicKey = _fieldValue as electionguard.protogen.ElementModP
            2 -> commitment = _fieldValue as electionguard.protogen.ElementModP
            3 -> challenge = _fieldValue as electionguard.protogen.ElementModQ
            4 -> response = _fieldValue as electionguard.protogen.ElementModQ
        }
    }
    return SchnorrProof(publicKey, commitment, challenge, response, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForFinishRequest")
public fun FinishRequest?.orDefault(): electionguard.protogen.FinishRequest = this ?: FinishRequest.defaultInstance

private fun FinishRequest.protoMergeImpl(plus: pbandk.Message?): FinishRequest = (plus as? FinishRequest)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun FinishRequest.Companion.decodeWithImpl(u: pbandk.MessageDecoder): FinishRequest {
    var allOk = false

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> allOk = _fieldValue as Boolean
        }
    }
    return FinishRequest(allOk, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForErrorResponse")
public fun ErrorResponse?.orDefault(): electionguard.protogen.ErrorResponse = this ?: ErrorResponse.defaultInstance

private fun ErrorResponse.protoMergeImpl(plus: pbandk.Message?): ErrorResponse = (plus as? ErrorResponse)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ErrorResponse.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ErrorResponse {
    var error = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> error = _fieldValue as String
        }
    }
    return ErrorResponse(error, unknownFields)
}
