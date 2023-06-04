@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class ElectionConfig(
    val constants: electionguard.protogen.ElectionConstants? = null,
    val manifestFile: pbandk.ByteArr = pbandk.ByteArr.empty,
    val manifest: electionguard.protogen.Manifest? = null,
    val numberOfGuardians: Int = 0,
    val quorum: Int = 0,
    val electionDate: String = "",
    val jurisdictionInfo: String = "",
    val parameterBaseHash: electionguard.protogen.UInt256? = null,
    val manifestHash: electionguard.protogen.UInt256? = null,
    val electionBaseHash: electionguard.protogen.UInt256? = null,
    val metadata: List<electionguard.protogen.ElectionConfig.MetadataEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionConfig = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConfig> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionConfig> {
        public val defaultInstance: electionguard.protogen.ElectionConfig by lazy { electionguard.protogen.ElectionConfig() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionConfig = electionguard.protogen.ElectionConfig.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConfig> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionConfig, *>>(11)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "constants",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionConstants.Companion),
                        jsonName = "constants",
                        value = electionguard.protogen.ElectionConfig::constants
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifest",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.Manifest.Companion),
                        jsonName = "manifest",
                        value = electionguard.protogen.ElectionConfig::manifest
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "number_of_guardians",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "numberOfGuardians",
                        value = electionguard.protogen.ElectionConfig::numberOfGuardians
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "quorum",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "quorum",
                        value = electionguard.protogen.ElectionConfig::quorum
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "metadata",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElectionConfig.MetadataEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionConfig.MetadataEntry.Companion)),
                        jsonName = "metadata",
                        value = electionguard.protogen.ElectionConfig::metadata
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifestFile",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "manifestFile",
                        value = electionguard.protogen.ElectionConfig::manifestFile
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "electionDate",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "electionDate",
                        value = electionguard.protogen.ElectionConfig::electionDate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "jurisdictionInfo",
                        number = 9,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "jurisdictionInfo",
                        value = electionguard.protogen.ElectionConfig::jurisdictionInfo
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "parameter_base_hash",
                        number = 10,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "parameterBaseHash",
                        value = electionguard.protogen.ElectionConfig::parameterBaseHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifest_hash",
                        number = 11,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "manifestHash",
                        value = electionguard.protogen.ElectionConfig::manifestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "election_base_hash",
                        number = 12,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "electionBaseHash",
                        value = electionguard.protogen.ElectionConfig::electionBaseHash
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElectionConfig",
                messageClass = electionguard.protogen.ElectionConfig::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class MetadataEntry(
        override val key: String = "",
        override val value: String = "",
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, String> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionConfig.MetadataEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConfig.MetadataEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionConfig.MetadataEntry> {
            public val defaultInstance: electionguard.protogen.ElectionConfig.MetadataEntry by lazy { electionguard.protogen.ElectionConfig.MetadataEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionConfig.MetadataEntry = electionguard.protogen.ElectionConfig.MetadataEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConfig.MetadataEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionConfig.MetadataEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.ElectionConfig.MetadataEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.ElectionConfig.MetadataEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "ElectionConfig.MetadataEntry",
                    messageClass = electionguard.protogen.ElectionConfig.MetadataEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class ElectionConstants(
    val name: String = "",
    val largePrime: pbandk.ByteArr = pbandk.ByteArr.empty,
    val smallPrime: pbandk.ByteArr = pbandk.ByteArr.empty,
    val cofactor: pbandk.ByteArr = pbandk.ByteArr.empty,
    val generator: pbandk.ByteArr = pbandk.ByteArr.empty,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionConstants = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConstants> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionConstants> {
        public val defaultInstance: electionguard.protogen.ElectionConstants by lazy { electionguard.protogen.ElectionConstants() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionConstants = electionguard.protogen.ElectionConstants.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionConstants> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionConstants, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "large_prime",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "largePrime",
                        value = electionguard.protogen.ElectionConstants::largePrime
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "small_prime",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "smallPrime",
                        value = electionguard.protogen.ElectionConstants::smallPrime
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "cofactor",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "cofactor",
                        value = electionguard.protogen.ElectionConstants::cofactor
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "generator",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Primitive.Bytes(),
                        jsonName = "generator",
                        value = electionguard.protogen.ElectionConstants::generator
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "name",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "name",
                        value = electionguard.protogen.ElectionConstants::name
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElectionConstants",
                messageClass = electionguard.protogen.ElectionConstants::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class ElectionInitialized(
    val config: electionguard.protogen.ElectionConfig? = null,
    val jointPublicKey: electionguard.protogen.ElementModP? = null,
    val cryptoExtendedBaseHash: electionguard.protogen.UInt256? = null,
    val guardians: List<electionguard.protogen.Guardian> = emptyList(),
    val metadata: List<electionguard.protogen.ElectionInitialized.MetadataEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionInitialized = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionInitialized> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionInitialized> {
        public val defaultInstance: electionguard.protogen.ElectionInitialized by lazy { electionguard.protogen.ElectionInitialized() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionInitialized = electionguard.protogen.ElectionInitialized.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionInitialized> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionInitialized, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "config",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionConfig.Companion),
                        jsonName = "config",
                        value = electionguard.protogen.ElectionInitialized::config
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "joint_public_key",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "jointPublicKey",
                        value = electionguard.protogen.ElectionInitialized::jointPublicKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_extended_base_hash",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoExtendedBaseHash",
                        value = electionguard.protogen.ElectionInitialized::cryptoExtendedBaseHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardians",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.Guardian>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.Guardian.Companion)),
                        jsonName = "guardians",
                        value = electionguard.protogen.ElectionInitialized::guardians
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "metadata",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElectionInitialized.MetadataEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionInitialized.MetadataEntry.Companion)),
                        jsonName = "metadata",
                        value = electionguard.protogen.ElectionInitialized::metadata
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElectionInitialized",
                messageClass = electionguard.protogen.ElectionInitialized::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class MetadataEntry(
        override val key: String = "",
        override val value: String = "",
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, String> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionInitialized.MetadataEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionInitialized.MetadataEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionInitialized.MetadataEntry> {
            public val defaultInstance: electionguard.protogen.ElectionInitialized.MetadataEntry by lazy { electionguard.protogen.ElectionInitialized.MetadataEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionInitialized.MetadataEntry = electionguard.protogen.ElectionInitialized.MetadataEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionInitialized.MetadataEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionInitialized.MetadataEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.ElectionInitialized.MetadataEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.ElectionInitialized.MetadataEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "ElectionInitialized.MetadataEntry",
                    messageClass = electionguard.protogen.ElectionInitialized.MetadataEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class Guardian(
    val guardianId: String = "",
    val xCoordinate: Int = 0,
    val coefficientProofs: List<electionguard.protogen.SchnorrProof> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.Guardian = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.Guardian> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.Guardian> {
        public val defaultInstance: electionguard.protogen.Guardian by lazy { electionguard.protogen.Guardian() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.Guardian = electionguard.protogen.Guardian.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.Guardian> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.Guardian, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.Guardian::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "x_coordinate",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "xCoordinate",
                        value = electionguard.protogen.Guardian::xCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "coefficient_proofs",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.SchnorrProof>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.SchnorrProof.Companion)),
                        jsonName = "coefficientProofs",
                        value = electionguard.protogen.Guardian::coefficientProofs
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "Guardian",
                messageClass = electionguard.protogen.Guardian::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class TallyResult(
    val electionInit: electionguard.protogen.ElectionInitialized? = null,
    val encryptedTally: electionguard.protogen.EncryptedTally? = null,
    val ballotIds: List<String> = emptyList(),
    val tallyIds: List<String> = emptyList(),
    val metadata: List<electionguard.protogen.TallyResult.MetadataEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.TallyResult = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.TallyResult> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.TallyResult> {
        public val defaultInstance: electionguard.protogen.TallyResult by lazy { electionguard.protogen.TallyResult() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.TallyResult = electionguard.protogen.TallyResult.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.TallyResult> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.TallyResult, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "election_init",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionInitialized.Companion),
                        jsonName = "electionInit",
                        value = electionguard.protogen.TallyResult::electionInit
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "encrypted_tally",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptedTally.Companion),
                        jsonName = "encryptedTally",
                        value = electionguard.protogen.TallyResult::encryptedTally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ballot_ids",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<String>(valueType = pbandk.FieldDescriptor.Type.Primitive.String()),
                        jsonName = "ballotIds",
                        value = electionguard.protogen.TallyResult::ballotIds
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_ids",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<String>(valueType = pbandk.FieldDescriptor.Type.Primitive.String()),
                        jsonName = "tallyIds",
                        value = electionguard.protogen.TallyResult::tallyIds
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "metadata",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.TallyResult.MetadataEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.TallyResult.MetadataEntry.Companion)),
                        jsonName = "metadata",
                        value = electionguard.protogen.TallyResult::metadata
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "TallyResult",
                messageClass = electionguard.protogen.TallyResult::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class MetadataEntry(
        override val key: String = "",
        override val value: String = "",
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, String> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.TallyResult.MetadataEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.TallyResult.MetadataEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.TallyResult.MetadataEntry> {
            public val defaultInstance: electionguard.protogen.TallyResult.MetadataEntry by lazy { electionguard.protogen.TallyResult.MetadataEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.TallyResult.MetadataEntry = electionguard.protogen.TallyResult.MetadataEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.TallyResult.MetadataEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.TallyResult.MetadataEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.TallyResult.MetadataEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.TallyResult.MetadataEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "TallyResult.MetadataEntry",
                    messageClass = electionguard.protogen.TallyResult.MetadataEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class DecryptionResult(
    val tallyResult: electionguard.protogen.TallyResult? = null,
    val decryptedTally: electionguard.protogen.DecryptedTallyOrBallot? = null,
    val lagrangeCoordinates: List<electionguard.protogen.LagrangeCoordinate> = emptyList(),
    val metadata: List<electionguard.protogen.DecryptionResult.MetadataEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptionResult = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptionResult> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptionResult> {
        public val defaultInstance: electionguard.protogen.DecryptionResult by lazy { electionguard.protogen.DecryptionResult() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptionResult = electionguard.protogen.DecryptionResult.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptionResult> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptionResult, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "tally_result",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.TallyResult.Companion),
                        jsonName = "tallyResult",
                        value = electionguard.protogen.DecryptionResult::tallyResult
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "decrypted_tally",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DecryptedTallyOrBallot.Companion),
                        jsonName = "decryptedTally",
                        value = electionguard.protogen.DecryptionResult::decryptedTally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "lagrange_coordinates",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.LagrangeCoordinate>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.LagrangeCoordinate.Companion)),
                        jsonName = "lagrangeCoordinates",
                        value = electionguard.protogen.DecryptionResult::lagrangeCoordinates
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "metadata",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.DecryptionResult.MetadataEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.DecryptionResult.MetadataEntry.Companion)),
                        jsonName = "metadata",
                        value = electionguard.protogen.DecryptionResult::metadata
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "DecryptionResult",
                messageClass = electionguard.protogen.DecryptionResult::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class MetadataEntry(
        override val key: String = "",
        override val value: String = "",
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, String> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptionResult.MetadataEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptionResult.MetadataEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptionResult.MetadataEntry> {
            public val defaultInstance: electionguard.protogen.DecryptionResult.MetadataEntry by lazy { electionguard.protogen.DecryptionResult.MetadataEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.DecryptionResult.MetadataEntry = electionguard.protogen.DecryptionResult.MetadataEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptionResult.MetadataEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptionResult.MetadataEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.DecryptionResult.MetadataEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.DecryptionResult.MetadataEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "DecryptionResult.MetadataEntry",
                    messageClass = electionguard.protogen.DecryptionResult.MetadataEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class LagrangeCoordinate(
    val guardianId: String = "",
    val xCoordinate: Int = 0,
    val lagrangeCoefficient: electionguard.protogen.ElementModQ? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.LagrangeCoordinate = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.LagrangeCoordinate> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.LagrangeCoordinate> {
        public val defaultInstance: electionguard.protogen.LagrangeCoordinate by lazy { electionguard.protogen.LagrangeCoordinate() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.LagrangeCoordinate = electionguard.protogen.LagrangeCoordinate.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.LagrangeCoordinate> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.LagrangeCoordinate, *>>(3)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.LagrangeCoordinate::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "x_coordinate",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "xCoordinate",
                        value = electionguard.protogen.LagrangeCoordinate::xCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "lagrange_coefficient",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "lagrangeCoefficient",
                        value = electionguard.protogen.LagrangeCoordinate::lagrangeCoefficient
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "LagrangeCoordinate",
                messageClass = electionguard.protogen.LagrangeCoordinate::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionConfig")
public fun ElectionConfig?.orDefault(): electionguard.protogen.ElectionConfig = this ?: ElectionConfig.defaultInstance

private fun ElectionConfig.protoMergeImpl(plus: pbandk.Message?): ElectionConfig = (plus as? ElectionConfig)?.let {
    it.copy(
        constants = constants?.plus(plus.constants) ?: plus.constants,
        manifest = manifest?.plus(plus.manifest) ?: plus.manifest,
        parameterBaseHash = parameterBaseHash?.plus(plus.parameterBaseHash) ?: plus.parameterBaseHash,
        manifestHash = manifestHash?.plus(plus.manifestHash) ?: plus.manifestHash,
        electionBaseHash = electionBaseHash?.plus(plus.electionBaseHash) ?: plus.electionBaseHash,
        metadata = metadata + plus.metadata,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionConfig.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionConfig {
    var constants: electionguard.protogen.ElectionConstants? = null
    var manifestFile: pbandk.ByteArr = pbandk.ByteArr.empty
    var manifest: electionguard.protogen.Manifest? = null
    var numberOfGuardians = 0
    var quorum = 0
    var electionDate = ""
    var jurisdictionInfo = ""
    var parameterBaseHash: electionguard.protogen.UInt256? = null
    var manifestHash: electionguard.protogen.UInt256? = null
    var electionBaseHash: electionguard.protogen.UInt256? = null
    var metadata: pbandk.ListWithSize.Builder<electionguard.protogen.ElectionConfig.MetadataEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            2 -> constants = _fieldValue as electionguard.protogen.ElectionConstants
            3 -> manifest = _fieldValue as electionguard.protogen.Manifest
            4 -> numberOfGuardians = _fieldValue as Int
            5 -> quorum = _fieldValue as Int
            6 -> metadata = (metadata ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.ElectionConfig.MetadataEntry> }
            7 -> manifestFile = _fieldValue as pbandk.ByteArr
            8 -> electionDate = _fieldValue as String
            9 -> jurisdictionInfo = _fieldValue as String
            10 -> parameterBaseHash = _fieldValue as electionguard.protogen.UInt256
            11 -> manifestHash = _fieldValue as electionguard.protogen.UInt256
            12 -> electionBaseHash = _fieldValue as electionguard.protogen.UInt256
        }
    }

    return ElectionConfig(constants, manifestFile, manifest, numberOfGuardians,
        quorum, electionDate, jurisdictionInfo, parameterBaseHash,
        manifestHash, electionBaseHash, pbandk.ListWithSize.Builder.fixed(metadata), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionConfigMetadataEntry")
public fun ElectionConfig.MetadataEntry?.orDefault(): electionguard.protogen.ElectionConfig.MetadataEntry = this ?: ElectionConfig.MetadataEntry.defaultInstance

private fun ElectionConfig.MetadataEntry.protoMergeImpl(plus: pbandk.Message?): ElectionConfig.MetadataEntry = (plus as? ElectionConfig.MetadataEntry)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionConfig.MetadataEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionConfig.MetadataEntry {
    var key = ""
    var value = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as String
        }
    }

    return ElectionConfig.MetadataEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionConstants")
public fun ElectionConstants?.orDefault(): electionguard.protogen.ElectionConstants = this ?: ElectionConstants.defaultInstance

private fun ElectionConstants.protoMergeImpl(plus: pbandk.Message?): ElectionConstants = (plus as? ElectionConstants)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionConstants.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionConstants {
    var name = ""
    var largePrime: pbandk.ByteArr = pbandk.ByteArr.empty
    var smallPrime: pbandk.ByteArr = pbandk.ByteArr.empty
    var cofactor: pbandk.ByteArr = pbandk.ByteArr.empty
    var generator: pbandk.ByteArr = pbandk.ByteArr.empty

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> largePrime = _fieldValue as pbandk.ByteArr
            2 -> smallPrime = _fieldValue as pbandk.ByteArr
            3 -> cofactor = _fieldValue as pbandk.ByteArr
            4 -> generator = _fieldValue as pbandk.ByteArr
            5 -> name = _fieldValue as String
        }
    }

    return ElectionConstants(name, largePrime, smallPrime, cofactor,
        generator, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionInitialized")
public fun ElectionInitialized?.orDefault(): electionguard.protogen.ElectionInitialized = this ?: ElectionInitialized.defaultInstance

private fun ElectionInitialized.protoMergeImpl(plus: pbandk.Message?): ElectionInitialized = (plus as? ElectionInitialized)?.let {
    it.copy(
        config = config?.plus(plus.config) ?: plus.config,
        jointPublicKey = jointPublicKey?.plus(plus.jointPublicKey) ?: plus.jointPublicKey,
        cryptoExtendedBaseHash = cryptoExtendedBaseHash?.plus(plus.cryptoExtendedBaseHash) ?: plus.cryptoExtendedBaseHash,
        guardians = guardians + plus.guardians,
        metadata = metadata + plus.metadata,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionInitialized.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionInitialized {
    var config: electionguard.protogen.ElectionConfig? = null
    var jointPublicKey: electionguard.protogen.ElementModP? = null
    var cryptoExtendedBaseHash: electionguard.protogen.UInt256? = null
    var guardians: pbandk.ListWithSize.Builder<electionguard.protogen.Guardian>? = null
    var metadata: pbandk.ListWithSize.Builder<electionguard.protogen.ElectionInitialized.MetadataEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> config = _fieldValue as electionguard.protogen.ElectionConfig
            2 -> jointPublicKey = _fieldValue as electionguard.protogen.ElementModP
            4 -> cryptoExtendedBaseHash = _fieldValue as electionguard.protogen.UInt256
            5 -> guardians = (guardians ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.Guardian> }
            6 -> metadata = (metadata ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.ElectionInitialized.MetadataEntry> }
        }
    }

    return ElectionInitialized(config, jointPublicKey, cryptoExtendedBaseHash, pbandk.ListWithSize.Builder.fixed(guardians),
        pbandk.ListWithSize.Builder.fixed(metadata), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionInitializedMetadataEntry")
public fun ElectionInitialized.MetadataEntry?.orDefault(): electionguard.protogen.ElectionInitialized.MetadataEntry = this ?: ElectionInitialized.MetadataEntry.defaultInstance

private fun ElectionInitialized.MetadataEntry.protoMergeImpl(plus: pbandk.Message?): ElectionInitialized.MetadataEntry = (plus as? ElectionInitialized.MetadataEntry)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionInitialized.MetadataEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionInitialized.MetadataEntry {
    var key = ""
    var value = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as String
        }
    }

    return ElectionInitialized.MetadataEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForGuardian")
public fun Guardian?.orDefault(): electionguard.protogen.Guardian = this ?: Guardian.defaultInstance

private fun Guardian.protoMergeImpl(plus: pbandk.Message?): Guardian = (plus as? Guardian)?.let {
    it.copy(
        coefficientProofs = coefficientProofs + plus.coefficientProofs,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun Guardian.Companion.decodeWithImpl(u: pbandk.MessageDecoder): Guardian {
    var guardianId = ""
    var xCoordinate = 0
    var coefficientProofs: pbandk.ListWithSize.Builder<electionguard.protogen.SchnorrProof>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> xCoordinate = _fieldValue as Int
            4 -> coefficientProofs = (coefficientProofs ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.SchnorrProof> }
        }
    }

    return Guardian(guardianId, xCoordinate, pbandk.ListWithSize.Builder.fixed(coefficientProofs), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForTallyResult")
public fun TallyResult?.orDefault(): electionguard.protogen.TallyResult = this ?: TallyResult.defaultInstance

private fun TallyResult.protoMergeImpl(plus: pbandk.Message?): TallyResult = (plus as? TallyResult)?.let {
    it.copy(
        electionInit = electionInit?.plus(plus.electionInit) ?: plus.electionInit,
        encryptedTally = encryptedTally?.plus(plus.encryptedTally) ?: plus.encryptedTally,
        ballotIds = ballotIds + plus.ballotIds,
        tallyIds = tallyIds + plus.tallyIds,
        metadata = metadata + plus.metadata,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun TallyResult.Companion.decodeWithImpl(u: pbandk.MessageDecoder): TallyResult {
    var electionInit: electionguard.protogen.ElectionInitialized? = null
    var encryptedTally: electionguard.protogen.EncryptedTally? = null
    var ballotIds: pbandk.ListWithSize.Builder<String>? = null
    var tallyIds: pbandk.ListWithSize.Builder<String>? = null
    var metadata: pbandk.ListWithSize.Builder<electionguard.protogen.TallyResult.MetadataEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> electionInit = _fieldValue as electionguard.protogen.ElectionInitialized
            2 -> encryptedTally = _fieldValue as electionguard.protogen.EncryptedTally
            3 -> ballotIds = (ballotIds ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<String> }
            4 -> tallyIds = (tallyIds ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<String> }
            5 -> metadata = (metadata ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.TallyResult.MetadataEntry> }
        }
    }

    return TallyResult(electionInit, encryptedTally, pbandk.ListWithSize.Builder.fixed(ballotIds), pbandk.ListWithSize.Builder.fixed(tallyIds),
        pbandk.ListWithSize.Builder.fixed(metadata), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForTallyResultMetadataEntry")
public fun TallyResult.MetadataEntry?.orDefault(): electionguard.protogen.TallyResult.MetadataEntry = this ?: TallyResult.MetadataEntry.defaultInstance

private fun TallyResult.MetadataEntry.protoMergeImpl(plus: pbandk.Message?): TallyResult.MetadataEntry = (plus as? TallyResult.MetadataEntry)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun TallyResult.MetadataEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): TallyResult.MetadataEntry {
    var key = ""
    var value = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as String
        }
    }

    return TallyResult.MetadataEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptionResult")
public fun DecryptionResult?.orDefault(): electionguard.protogen.DecryptionResult = this ?: DecryptionResult.defaultInstance

private fun DecryptionResult.protoMergeImpl(plus: pbandk.Message?): DecryptionResult = (plus as? DecryptionResult)?.let {
    it.copy(
        tallyResult = tallyResult?.plus(plus.tallyResult) ?: plus.tallyResult,
        decryptedTally = decryptedTally?.plus(plus.decryptedTally) ?: plus.decryptedTally,
        lagrangeCoordinates = lagrangeCoordinates + plus.lagrangeCoordinates,
        metadata = metadata + plus.metadata,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptionResult.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptionResult {
    var tallyResult: electionguard.protogen.TallyResult? = null
    var decryptedTally: electionguard.protogen.DecryptedTallyOrBallot? = null
    var lagrangeCoordinates: pbandk.ListWithSize.Builder<electionguard.protogen.LagrangeCoordinate>? = null
    var metadata: pbandk.ListWithSize.Builder<electionguard.protogen.DecryptionResult.MetadataEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> tallyResult = _fieldValue as electionguard.protogen.TallyResult
            2 -> decryptedTally = _fieldValue as electionguard.protogen.DecryptedTallyOrBallot
            3 -> lagrangeCoordinates = (lagrangeCoordinates ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.LagrangeCoordinate> }
            4 -> metadata = (metadata ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as kotlin.sequences.Sequence<electionguard.protogen.DecryptionResult.MetadataEntry> }
        }
    }

    return DecryptionResult(tallyResult, decryptedTally, pbandk.ListWithSize.Builder.fixed(lagrangeCoordinates), pbandk.ListWithSize.Builder.fixed(metadata), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptionResultMetadataEntry")
public fun DecryptionResult.MetadataEntry?.orDefault(): electionguard.protogen.DecryptionResult.MetadataEntry = this ?: DecryptionResult.MetadataEntry.defaultInstance

private fun DecryptionResult.MetadataEntry.protoMergeImpl(plus: pbandk.Message?): DecryptionResult.MetadataEntry = (plus as? DecryptionResult.MetadataEntry)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptionResult.MetadataEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): DecryptionResult.MetadataEntry {
    var key = ""
    var value = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as String
        }
    }

    return DecryptionResult.MetadataEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForLagrangeCoordinate")
public fun LagrangeCoordinate?.orDefault(): electionguard.protogen.LagrangeCoordinate = this ?: LagrangeCoordinate.defaultInstance

private fun LagrangeCoordinate.protoMergeImpl(plus: pbandk.Message?): LagrangeCoordinate = (plus as? LagrangeCoordinate)?.let {
    it.copy(
        lagrangeCoefficient = lagrangeCoefficient?.plus(plus.lagrangeCoefficient) ?: plus.lagrangeCoefficient,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun LagrangeCoordinate.Companion.decodeWithImpl(u: pbandk.MessageDecoder): LagrangeCoordinate {
    var guardianId = ""
    var xCoordinate = 0
    var lagrangeCoefficient: electionguard.protogen.ElementModQ? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> xCoordinate = _fieldValue as Int
            3 -> lagrangeCoefficient = _fieldValue as electionguard.protogen.ElementModQ
        }
    }

    return LagrangeCoordinate(guardianId, xCoordinate, lagrangeCoefficient, unknownFields)
}
