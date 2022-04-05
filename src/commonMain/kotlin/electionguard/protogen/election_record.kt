@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class ElectionRecord(
    val protoVersion: String = "",
    val constants: electionguard.protogen.ElectionConstants? = null,
    val manifest: electionguard.protogen.Manifest? = null,
    val context: electionguard.protogen.ElectionContext? = null,
    val guardianRecords: List<electionguard.protogen.GuardianRecord> = emptyList(),
    val devices: List<electionguard.protogen.EncryptionDevice> = emptyList(),
    val ciphertextTally: electionguard.protogen.CiphertextTally? = null,
    val decryptedTally: electionguard.protogen.PlaintextTally? = null,
    val availableGuardians: List<electionguard.protogen.AvailableGuardian> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionRecord = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionRecord> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionRecord> {
        public val defaultInstance: electionguard.protogen.ElectionRecord by lazy { electionguard.protogen.ElectionRecord() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionRecord = electionguard.protogen.ElectionRecord.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionRecord> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionRecord, *>>(9)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "proto_version",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "protoVersion",
                        value = electionguard.protogen.ElectionRecord::protoVersion
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "constants",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionConstants.Companion),
                        jsonName = "constants",
                        value = electionguard.protogen.ElectionRecord::constants
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifest",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.Manifest.Companion),
                        jsonName = "manifest",
                        value = electionguard.protogen.ElectionRecord::manifest
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "context",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionContext.Companion),
                        jsonName = "context",
                        value = electionguard.protogen.ElectionRecord::context
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_records",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.GuardianRecord>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.GuardianRecord.Companion)),
                        jsonName = "guardianRecords",
                        value = electionguard.protogen.ElectionRecord::guardianRecords
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "devices",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.EncryptionDevice>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.EncryptionDevice.Companion)),
                        jsonName = "devices",
                        value = electionguard.protogen.ElectionRecord::devices
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "ciphertext_tally",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.CiphertextTally.Companion),
                        jsonName = "ciphertextTally",
                        value = electionguard.protogen.ElectionRecord::ciphertextTally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "decrypted_tally",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.PlaintextTally.Companion),
                        jsonName = "decryptedTally",
                        value = electionguard.protogen.ElectionRecord::decryptedTally
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "available_guardians",
                        number = 9,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.AvailableGuardian>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.AvailableGuardian.Companion)),
                        jsonName = "availableGuardians",
                        value = electionguard.protogen.ElectionRecord::availableGuardians
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElectionRecord",
                messageClass = electionguard.protogen.ElectionRecord::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class AvailableGuardian(
    val guardianId: String = "",
    val xCoordinate: Int = 0,
    val lagrangeCoordinate: electionguard.protogen.ElementModQ? = null,
    val lagrangeCoordinateInt: Int = 0,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.AvailableGuardian = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.AvailableGuardian> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.AvailableGuardian> {
        public val defaultInstance: electionguard.protogen.AvailableGuardian by lazy { electionguard.protogen.AvailableGuardian() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.AvailableGuardian = electionguard.protogen.AvailableGuardian.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.AvailableGuardian> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.AvailableGuardian, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.AvailableGuardian::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "x_coordinate",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "xCoordinate",
                        value = electionguard.protogen.AvailableGuardian::xCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "lagrange_coordinate",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModQ.Companion),
                        jsonName = "lagrangeCoordinate",
                        value = electionguard.protogen.AvailableGuardian::lagrangeCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "lagrange_coordinate_int",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Primitive.SInt32(),
                        jsonName = "lagrangeCoordinateInt",
                        value = electionguard.protogen.AvailableGuardian::lagrangeCoordinateInt
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "AvailableGuardian",
                messageClass = electionguard.protogen.AvailableGuardian::class,
                messageCompanion = this,
                fields = fieldsList
            )
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
public data class ElectionContext(
    val numberOfGuardians: Int = 0,
    val quorum: Int = 0,
    val jointPublicKey: electionguard.protogen.ElementModP? = null,
    val manifestHash: electionguard.protogen.UInt256? = null,
    val cryptoBaseHash: electionguard.protogen.UInt256? = null,
    val cryptoExtendedBaseHash: electionguard.protogen.UInt256? = null,
    val commitmentHash: electionguard.protogen.UInt256? = null,
    val extendedData: List<electionguard.protogen.ElectionContext.ExtendedDataEntry> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionContext = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionContext> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionContext> {
        public val defaultInstance: electionguard.protogen.ElectionContext by lazy { electionguard.protogen.ElectionContext() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionContext = electionguard.protogen.ElectionContext.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionContext> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionContext, *>>(8)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "number_of_guardians",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "numberOfGuardians",
                        value = electionguard.protogen.ElectionContext::numberOfGuardians
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "quorum",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "quorum",
                        value = electionguard.protogen.ElectionContext::quorum
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "joint_public_key",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "jointPublicKey",
                        value = electionguard.protogen.ElectionContext::jointPublicKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "manifest_hash",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "manifestHash",
                        value = electionguard.protogen.ElectionContext::manifestHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_base_hash",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoBaseHash",
                        value = electionguard.protogen.ElectionContext::cryptoBaseHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "crypto_extended_base_hash",
                        number = 6,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "cryptoExtendedBaseHash",
                        value = electionguard.protogen.ElectionContext::cryptoExtendedBaseHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "commitment_hash",
                        number = 7,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.UInt256.Companion),
                        jsonName = "commitmentHash",
                        value = electionguard.protogen.ElectionContext::commitmentHash
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "extended_data",
                        number = 8,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElectionContext.ExtendedDataEntry>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElectionContext.ExtendedDataEntry.Companion)),
                        jsonName = "extendedData",
                        value = electionguard.protogen.ElectionContext::extendedData
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "ElectionContext",
                messageClass = electionguard.protogen.ElectionContext::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }

    public data class ExtendedDataEntry(
        override val key: String = "",
        override val value: String = "",
        override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
    ) : pbandk.Message, Map.Entry<String, String> {
        override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElectionContext.ExtendedDataEntry = protoMergeImpl(other)
        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionContext.ExtendedDataEntry> get() = Companion.descriptor
        override val protoSize: Int by lazy { super.protoSize }
        public companion object : pbandk.Message.Companion<electionguard.protogen.ElectionContext.ExtendedDataEntry> {
            public val defaultInstance: electionguard.protogen.ElectionContext.ExtendedDataEntry by lazy { electionguard.protogen.ElectionContext.ExtendedDataEntry() }
            override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElectionContext.ExtendedDataEntry = electionguard.protogen.ElectionContext.ExtendedDataEntry.decodeWithImpl(u)

            override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElectionContext.ExtendedDataEntry> by lazy {
                val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElectionContext.ExtendedDataEntry, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "key",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "key",
                            value = electionguard.protogen.ElectionContext.ExtendedDataEntry::key
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.ElectionContext.ExtendedDataEntry::value
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "ElectionContext.ExtendedDataEntry",
                    messageClass = electionguard.protogen.ElectionContext.ExtendedDataEntry::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
        }
    }
}

@pbandk.Export
public data class EncryptionDevice(
    val deviceId: Long = 0L,
    val sessionId: Long = 0L,
    val launchCode: Long = 0L,
    val location: String = "",
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.EncryptionDevice = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptionDevice> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.EncryptionDevice> {
        public val defaultInstance: electionguard.protogen.EncryptionDevice by lazy { electionguard.protogen.EncryptionDevice() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.EncryptionDevice = electionguard.protogen.EncryptionDevice.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.EncryptionDevice> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.EncryptionDevice, *>>(4)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "device_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int64(),
                        jsonName = "deviceId",
                        value = electionguard.protogen.EncryptionDevice::deviceId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "session_id",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int64(),
                        jsonName = "sessionId",
                        value = electionguard.protogen.EncryptionDevice::sessionId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "launch_code",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Primitive.Int64(),
                        jsonName = "launchCode",
                        value = electionguard.protogen.EncryptionDevice::launchCode
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "location",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "location",
                        value = electionguard.protogen.EncryptionDevice::location
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "EncryptionDevice",
                messageClass = electionguard.protogen.EncryptionDevice::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
public data class GuardianRecord(
    val guardianId: String = "",
    val xCoordinate: Int = 0,
    val guardianPublicKey: electionguard.protogen.ElementModP? = null,
    val coefficientCommitments: List<electionguard.protogen.ElementModP> = emptyList(),
    val coefficientProofs: List<electionguard.protogen.SchnorrProof> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.GuardianRecord = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.GuardianRecord> get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.GuardianRecord> {
        public val defaultInstance: electionguard.protogen.GuardianRecord by lazy { electionguard.protogen.GuardianRecord() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.GuardianRecord = electionguard.protogen.GuardianRecord.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.GuardianRecord> by lazy {
            val fieldsList = ArrayList<pbandk.FieldDescriptor<electionguard.protogen.GuardianRecord, *>>(5)
            fieldsList.apply {
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_id",
                        number = 1,
                        type = pbandk.FieldDescriptor.Type.Primitive.String(),
                        jsonName = "guardianId",
                        value = electionguard.protogen.GuardianRecord::guardianId
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "x_coordinate",
                        number = 2,
                        type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                        jsonName = "xCoordinate",
                        value = electionguard.protogen.GuardianRecord::xCoordinate
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "guardian_public_key",
                        number = 3,
                        type = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion),
                        jsonName = "guardianPublicKey",
                        value = electionguard.protogen.GuardianRecord::guardianPublicKey
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "coefficient_commitments",
                        number = 4,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.ElementModP>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.ElementModP.Companion)),
                        jsonName = "coefficientCommitments",
                        value = electionguard.protogen.GuardianRecord::coefficientCommitments
                    )
                )
                add(
                    pbandk.FieldDescriptor(
                        messageDescriptor = this@Companion::descriptor,
                        name = "coefficient_proofs",
                        number = 5,
                        type = pbandk.FieldDescriptor.Type.Repeated<electionguard.protogen.SchnorrProof>(valueType = pbandk.FieldDescriptor.Type.Message(messageCompanion = electionguard.protogen.SchnorrProof.Companion)),
                        jsonName = "coefficientProofs",
                        value = electionguard.protogen.GuardianRecord::coefficientProofs
                    )
                )
            }
            pbandk.MessageDescriptor(
                fullName = "GuardianRecord",
                messageClass = electionguard.protogen.GuardianRecord::class,
                messageCompanion = this,
                fields = fieldsList
            )
        }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionRecord")
public fun ElectionRecord?.orDefault(): electionguard.protogen.ElectionRecord = this ?: ElectionRecord.defaultInstance

private fun ElectionRecord.protoMergeImpl(plus: pbandk.Message?): ElectionRecord = (plus as? ElectionRecord)?.let {
    it.copy(
        constants = constants?.plus(plus.constants) ?: plus.constants,
        manifest = manifest?.plus(plus.manifest) ?: plus.manifest,
        context = context?.plus(plus.context) ?: plus.context,
        guardianRecords = guardianRecords + plus.guardianRecords,
        devices = devices + plus.devices,
        ciphertextTally = ciphertextTally?.plus(plus.ciphertextTally) ?: plus.ciphertextTally,
        decryptedTally = decryptedTally?.plus(plus.decryptedTally) ?: plus.decryptedTally,
        availableGuardians = availableGuardians + plus.availableGuardians,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionRecord.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionRecord {
    var protoVersion = ""
    var constants: electionguard.protogen.ElectionConstants? = null
    var manifest: electionguard.protogen.Manifest? = null
    var context: electionguard.protogen.ElectionContext? = null
    var guardianRecords: pbandk.ListWithSize.Builder<electionguard.protogen.GuardianRecord>? = null
    var devices: pbandk.ListWithSize.Builder<electionguard.protogen.EncryptionDevice>? = null
    var ciphertextTally: electionguard.protogen.CiphertextTally? = null
    var decryptedTally: electionguard.protogen.PlaintextTally? = null
    var availableGuardians: pbandk.ListWithSize.Builder<electionguard.protogen.AvailableGuardian>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> protoVersion = _fieldValue as String
            2 -> constants = _fieldValue as electionguard.protogen.ElectionConstants
            3 -> manifest = _fieldValue as electionguard.protogen.Manifest
            4 -> context = _fieldValue as electionguard.protogen.ElectionContext
            5 -> guardianRecords = (guardianRecords ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.GuardianRecord> }
            6 -> devices = (devices ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.EncryptionDevice> }
            7 -> ciphertextTally = _fieldValue as electionguard.protogen.CiphertextTally
            8 -> decryptedTally = _fieldValue as electionguard.protogen.PlaintextTally
            9 -> availableGuardians = (availableGuardians ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.AvailableGuardian> }
        }
    }
    return ElectionRecord(protoVersion, constants, manifest, context,
        pbandk.ListWithSize.Builder.fixed(guardianRecords), pbandk.ListWithSize.Builder.fixed(devices), ciphertextTally, decryptedTally,
        pbandk.ListWithSize.Builder.fixed(availableGuardians), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForAvailableGuardian")
public fun AvailableGuardian?.orDefault(): electionguard.protogen.AvailableGuardian = this ?: AvailableGuardian.defaultInstance

private fun AvailableGuardian.protoMergeImpl(plus: pbandk.Message?): AvailableGuardian = (plus as? AvailableGuardian)?.let {
    it.copy(
        lagrangeCoordinate = lagrangeCoordinate?.plus(plus.lagrangeCoordinate) ?: plus.lagrangeCoordinate,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun AvailableGuardian.Companion.decodeWithImpl(u: pbandk.MessageDecoder): AvailableGuardian {
    var guardianId = ""
    var xCoordinate = 0
    var lagrangeCoordinate: electionguard.protogen.ElementModQ? = null
    var lagrangeCoordinateInt = 0

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> xCoordinate = _fieldValue as Int
            3 -> lagrangeCoordinate = _fieldValue as electionguard.protogen.ElementModQ
            4 -> lagrangeCoordinateInt = _fieldValue as Int
        }
    }
    return AvailableGuardian(guardianId, xCoordinate, lagrangeCoordinate, lagrangeCoordinateInt, unknownFields)
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
@pbandk.JsName("orDefaultForElectionContext")
public fun ElectionContext?.orDefault(): electionguard.protogen.ElectionContext = this ?: ElectionContext.defaultInstance

private fun ElectionContext.protoMergeImpl(plus: pbandk.Message?): ElectionContext = (plus as? ElectionContext)?.let {
    it.copy(
        jointPublicKey = jointPublicKey?.plus(plus.jointPublicKey) ?: plus.jointPublicKey,
        manifestHash = manifestHash?.plus(plus.manifestHash) ?: plus.manifestHash,
        cryptoBaseHash = cryptoBaseHash?.plus(plus.cryptoBaseHash) ?: plus.cryptoBaseHash,
        cryptoExtendedBaseHash = cryptoExtendedBaseHash?.plus(plus.cryptoExtendedBaseHash) ?: plus.cryptoExtendedBaseHash,
        commitmentHash = commitmentHash?.plus(plus.commitmentHash) ?: plus.commitmentHash,
        extendedData = extendedData + plus.extendedData,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionContext.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionContext {
    var numberOfGuardians = 0
    var quorum = 0
    var jointPublicKey: electionguard.protogen.ElementModP? = null
    var manifestHash: electionguard.protogen.UInt256? = null
    var cryptoBaseHash: electionguard.protogen.UInt256? = null
    var cryptoExtendedBaseHash: electionguard.protogen.UInt256? = null
    var commitmentHash: electionguard.protogen.UInt256? = null
    var extendedData: pbandk.ListWithSize.Builder<electionguard.protogen.ElectionContext.ExtendedDataEntry>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> numberOfGuardians = _fieldValue as Int
            2 -> quorum = _fieldValue as Int
            3 -> jointPublicKey = _fieldValue as electionguard.protogen.ElementModP
            4 -> manifestHash = _fieldValue as electionguard.protogen.UInt256
            5 -> cryptoBaseHash = _fieldValue as electionguard.protogen.UInt256
            6 -> cryptoExtendedBaseHash = _fieldValue as electionguard.protogen.UInt256
            7 -> commitmentHash = _fieldValue as electionguard.protogen.UInt256
            8 -> extendedData = (extendedData ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.ElectionContext.ExtendedDataEntry> }
        }
    }
    return ElectionContext(numberOfGuardians, quorum, jointPublicKey, manifestHash,
        cryptoBaseHash, cryptoExtendedBaseHash, commitmentHash, pbandk.ListWithSize.Builder.fixed(extendedData), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionContextExtendedDataEntry")
public fun ElectionContext.ExtendedDataEntry?.orDefault(): electionguard.protogen.ElectionContext.ExtendedDataEntry = this ?: ElectionContext.ExtendedDataEntry.defaultInstance

private fun ElectionContext.ExtendedDataEntry.protoMergeImpl(plus: pbandk.Message?): ElectionContext.ExtendedDataEntry = (plus as? ElectionContext.ExtendedDataEntry)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionContext.ExtendedDataEntry.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ElectionContext.ExtendedDataEntry {
    var key = ""
    var value = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> key = _fieldValue as String
            2 -> value = _fieldValue as String
        }
    }
    return ElectionContext.ExtendedDataEntry(key, value, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForEncryptionDevice")
public fun EncryptionDevice?.orDefault(): electionguard.protogen.EncryptionDevice = this ?: EncryptionDevice.defaultInstance

private fun EncryptionDevice.protoMergeImpl(plus: pbandk.Message?): EncryptionDevice = (plus as? EncryptionDevice)?.let {
    it.copy(
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun EncryptionDevice.Companion.decodeWithImpl(u: pbandk.MessageDecoder): EncryptionDevice {
    var deviceId = 0L
    var sessionId = 0L
    var launchCode = 0L
    var location = ""

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> deviceId = _fieldValue as Long
            2 -> sessionId = _fieldValue as Long
            3 -> launchCode = _fieldValue as Long
            4 -> location = _fieldValue as String
        }
    }
    return EncryptionDevice(deviceId, sessionId, launchCode, location, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForGuardianRecord")
public fun GuardianRecord?.orDefault(): electionguard.protogen.GuardianRecord = this ?: GuardianRecord.defaultInstance

private fun GuardianRecord.protoMergeImpl(plus: pbandk.Message?): GuardianRecord = (plus as? GuardianRecord)?.let {
    it.copy(
        guardianPublicKey = guardianPublicKey?.plus(plus.guardianPublicKey) ?: plus.guardianPublicKey,
        coefficientCommitments = coefficientCommitments + plus.coefficientCommitments,
        coefficientProofs = coefficientProofs + plus.coefficientProofs,
        unknownFields = unknownFields + plus.unknownFields
    )
} ?: this

@Suppress("UNCHECKED_CAST")
private fun GuardianRecord.Companion.decodeWithImpl(u: pbandk.MessageDecoder): GuardianRecord {
    var guardianId = ""
    var xCoordinate = 0
    var guardianPublicKey: electionguard.protogen.ElementModP? = null
    var coefficientCommitments: pbandk.ListWithSize.Builder<electionguard.protogen.ElementModP>? = null
    var coefficientProofs: pbandk.ListWithSize.Builder<electionguard.protogen.SchnorrProof>? = null

    val unknownFields = u.readMessage(this) { _fieldNumber, _fieldValue ->
        when (_fieldNumber) {
            1 -> guardianId = _fieldValue as String
            2 -> xCoordinate = _fieldValue as Int
            3 -> guardianPublicKey = _fieldValue as electionguard.protogen.ElementModP
            4 -> coefficientCommitments = (coefficientCommitments ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.ElementModP> }
            5 -> coefficientProofs = (coefficientProofs ?: pbandk.ListWithSize.Builder()).apply { this += _fieldValue as Sequence<electionguard.protogen.SchnorrProof> }
        }
    }
    return GuardianRecord(guardianId, xCoordinate, guardianPublicKey, pbandk.ListWithSize.Builder.fixed(coefficientCommitments),
        pbandk.ListWithSize.Builder.fixed(coefficientProofs), unknownFields)
}
