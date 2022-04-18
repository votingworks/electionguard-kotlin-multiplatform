@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class DecryptingTrustees(
    val trustees: List<electionguard.protogen.DecryptingTrustee> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptingTrustees =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustees>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptingTrustees> {
        public val defaultInstance: electionguard.protogen.DecryptingTrustees by
            lazy { electionguard.protogen.DecryptingTrustees() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.DecryptingTrustees =
            electionguard.protogen.DecryptingTrustees.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustees>
            by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptingTrustees, *>>(
                        1
                    )
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "trustees",
                            number = 1,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.DecryptingTrustee>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .DecryptingTrustee
                                                            .Companion
                                                )
                                    ),
                            jsonName = "trustees",
                            value = electionguard.protogen.DecryptingTrustees::trustees
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "DecryptingTrustees",
                    messageClass = electionguard.protogen.DecryptingTrustees::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class DecryptingTrustee(
    val guardianId: String = "",
    val guardianXCoordinate: Int = 0,
    val electionKeyPair: electionguard.protogen.ElGamalKeyPair? = null,
    val otherGuardianBackups: List<electionguard.protogen.ElectionPartialKeyBackup2> = emptyList(),
    val guardianCommitments: List<electionguard.protogen.CommitmentSet> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.DecryptingTrustee =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.DecryptingTrustee> {
        public val defaultInstance: electionguard.protogen.DecryptingTrustee by
            lazy { electionguard.protogen.DecryptingTrustee() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.DecryptingTrustee =
            electionguard.protogen.DecryptingTrustee.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.DecryptingTrustee>
            by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.DecryptingTrustee, *>>(
                        5
                    )
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
                            name = "election_key_pair",
                            number = 3,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElGamalKeyPair.Companion
                                    ),
                            jsonName = "electionKeyPair",
                            value = electionguard.protogen.DecryptingTrustee::electionKeyPair
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "other_guardian_backups",
                            number = 5,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.ElectionPartialKeyBackup2>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .ElectionPartialKeyBackup2
                                                            .Companion
                                                )
                                    ),
                            jsonName = "otherGuardianBackups",
                            value = electionguard.protogen.DecryptingTrustee::otherGuardianBackups
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "guardian_commitments",
                            number = 6,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.CommitmentSet>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .CommitmentSet
                                                            .Companion
                                                )
                                    ),
                            jsonName = "guardianCommitments",
                            value = electionguard.protogen.DecryptingTrustee::guardianCommitments
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
public data class ElGamalKeyPair(
    val secretKey: electionguard.protogen.ElementModQ? = null,
    val publicKey: electionguard.protogen.ElementModP? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ElGamalKeyPair =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeyPair>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ElGamalKeyPair> {
        public val defaultInstance: electionguard.protogen.ElGamalKeyPair by
            lazy { electionguard.protogen.ElGamalKeyPair() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ElGamalKeyPair =
            electionguard.protogen.ElGamalKeyPair.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ElGamalKeyPair> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ElGamalKeyPair, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "secret_key",
                            number = 1,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModQ.Companion
                                    ),
                            jsonName = "secretKey",
                            value = electionguard.protogen.ElGamalKeyPair::secretKey
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "public_key",
                            number = 2,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModP.Companion
                                    ),
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
public data class CommitmentSet(
    val guardianId: String = "",
    val commitments: List<electionguard.protogen.ElementModP> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.CommitmentSet =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CommitmentSet>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.CommitmentSet> {
        public val defaultInstance: electionguard.protogen.CommitmentSet by
            lazy { electionguard.protogen.CommitmentSet() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.CommitmentSet =
            electionguard.protogen.CommitmentSet.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.CommitmentSet> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.CommitmentSet, *>>(2)
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
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.ElementModP>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen.ElementModP.Companion
                                                )
                                    ),
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
public data class ElectionPartialKeyBackup2(
    val generatingGuardianId: String = "",
    val designatedGuardianId: String = "",
    val designatedGuardianXCoordinate: Int = 0,
    val coordinate: electionguard.protogen.ElementModQ? = null,
    val error: String = "",
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.ElectionPartialKeyBackup2 = protoMergeImpl(other)
    override val descriptor:
        pbandk.MessageDescriptor<electionguard.protogen.ElectionPartialKeyBackup2>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.ElectionPartialKeyBackup2> {

        public val defaultInstance: electionguard.protogen.ElectionPartialKeyBackup2 by
            lazy { electionguard.protogen.ElectionPartialKeyBackup2() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.ElectionPartialKeyBackup2 =
            electionguard.protogen.ElectionPartialKeyBackup2.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.ElectionPartialKeyBackup2> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<electionguard.protogen.ElectionPartialKeyBackup2, *>
                    >(5)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "generating_guardian_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "generatingGuardianId",
                            value =
                                electionguard.protogen
                                    .ElectionPartialKeyBackup2::generatingGuardianId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "designated_guardian_id",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "designatedGuardianId",
                            value =
                                electionguard.protogen
                                    .ElectionPartialKeyBackup2::designatedGuardianId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "designated_guardian_x_coordinate",
                            number = 3,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "designatedGuardianXCoordinate",
                            value =
                                electionguard.protogen
                                    .ElectionPartialKeyBackup2::designatedGuardianXCoordinate
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "coordinate",
                            number = 4,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ElementModQ.Companion
                                    ),
                            jsonName = "coordinate",
                            value = electionguard.protogen.ElectionPartialKeyBackup2::coordinate
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "error",
                            number = 5,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "error",
                            value = electionguard.protogen.ElectionPartialKeyBackup2::error
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "ElectionPartialKeyBackup2",
                    messageClass = electionguard.protogen.ElectionPartialKeyBackup2::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptingTrustees")
public fun DecryptingTrustees?.orDefault(): electionguard.protogen.DecryptingTrustees =
    this ?: DecryptingTrustees.defaultInstance

private fun DecryptingTrustees.protoMergeImpl(plus: pbandk.Message?): DecryptingTrustees =
    (plus as? DecryptingTrustees)
        ?.let {
            it.copy(
                trustees = trustees + plus.trustees,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptingTrustees.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): DecryptingTrustees {
    var trustees: pbandk.ListWithSize.Builder<electionguard.protogen.DecryptingTrustee>? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 ->
                    trustees =
                        (trustees ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.DecryptingTrustee>
                            }
            }
        }
    return DecryptingTrustees(pbandk.ListWithSize.Builder.fixed(trustees), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForDecryptingTrustee")
public fun DecryptingTrustee?.orDefault(): electionguard.protogen.DecryptingTrustee =
    this ?: DecryptingTrustee.defaultInstance

private fun DecryptingTrustee.protoMergeImpl(plus: pbandk.Message?): DecryptingTrustee =
    (plus as? DecryptingTrustee)
        ?.let {
            it.copy(
                electionKeyPair =
                    electionKeyPair?.plus(plus.electionKeyPair) ?: plus.electionKeyPair,
                otherGuardianBackups = otherGuardianBackups + plus.otherGuardianBackups,
                guardianCommitments = guardianCommitments + plus.guardianCommitments,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun DecryptingTrustee.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): DecryptingTrustee {
    var guardianId = ""
    var guardianXCoordinate = 0
    var electionKeyPair: electionguard.protogen.ElGamalKeyPair? = null
    var otherGuardianBackups:
        pbandk.ListWithSize.Builder<electionguard.protogen.ElectionPartialKeyBackup2>? = null
    var guardianCommitments: pbandk.ListWithSize.Builder<electionguard.protogen.CommitmentSet>? =
        null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> guardianId = _fieldValue as String
                2 -> guardianXCoordinate = _fieldValue as Int
                3 -> electionKeyPair = _fieldValue as electionguard.protogen.ElGamalKeyPair
                5 ->
                    otherGuardianBackups =
                        (otherGuardianBackups ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.ElectionPartialKeyBackup2>
                            }
                6 ->
                    guardianCommitments =
                        (guardianCommitments ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as Sequence<electionguard.protogen.CommitmentSet>
                            }
            }
        }
    return DecryptingTrustee(
        guardianId,
        guardianXCoordinate,
        electionKeyPair,
        pbandk.ListWithSize.Builder.fixed(otherGuardianBackups),
        pbandk.ListWithSize.Builder.fixed(guardianCommitments),
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForElGamalKeyPair")
public fun ElGamalKeyPair?.orDefault(): electionguard.protogen.ElGamalKeyPair =
    this ?: ElGamalKeyPair.defaultInstance

private fun ElGamalKeyPair.protoMergeImpl(plus: pbandk.Message?): ElGamalKeyPair =
    (plus as? ElGamalKeyPair)
        ?.let {
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

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> secretKey = _fieldValue as electionguard.protogen.ElementModQ
                2 -> publicKey = _fieldValue as electionguard.protogen.ElementModP
            }
        }
    return ElGamalKeyPair(secretKey, publicKey, unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForCommitmentSet")
public fun CommitmentSet?.orDefault(): electionguard.protogen.CommitmentSet =
    this ?: CommitmentSet.defaultInstance

private fun CommitmentSet.protoMergeImpl(plus: pbandk.Message?): CommitmentSet =
    (plus as? CommitmentSet)
        ?.let {
            it.copy(
                commitments = commitments + plus.commitments,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun CommitmentSet.Companion.decodeWithImpl(u: pbandk.MessageDecoder): CommitmentSet {
    var guardianId = ""
    var commitments: pbandk.ListWithSize.Builder<electionguard.protogen.ElementModP>? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> guardianId = _fieldValue as String
                3 ->
                    commitments =
                        (commitments ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this += _fieldValue as Sequence<electionguard.protogen.ElementModP>
                            }
            }
        }
    return CommitmentSet(guardianId, pbandk.ListWithSize.Builder.fixed(commitments), unknownFields)
}

@pbandk.Export
@pbandk.JsName("orDefaultForElectionPartialKeyBackup2")
public fun ElectionPartialKeyBackup2?.orDefault():
    electionguard.protogen.ElectionPartialKeyBackup2 =
        this ?: ElectionPartialKeyBackup2.defaultInstance

private fun ElectionPartialKeyBackup2.protoMergeImpl(
    plus: pbandk.Message?
): ElectionPartialKeyBackup2 =
    (plus as? ElectionPartialKeyBackup2)
        ?.let {
            it.copy(
                coordinate = coordinate?.plus(plus.coordinate) ?: plus.coordinate,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun ElectionPartialKeyBackup2.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): ElectionPartialKeyBackup2 {
    var generatingGuardianId = ""
    var designatedGuardianId = ""
    var designatedGuardianXCoordinate = 0
    var coordinate: electionguard.protogen.ElementModQ? = null
    var error = ""

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> generatingGuardianId = _fieldValue as String
                2 -> designatedGuardianId = _fieldValue as String
                3 -> designatedGuardianXCoordinate = _fieldValue as Int
                4 -> coordinate = _fieldValue as electionguard.protogen.ElementModQ
                5 -> error = _fieldValue as String
            }
        }
    return ElectionPartialKeyBackup2(
        generatingGuardianId,
        designatedGuardianId,
        designatedGuardianXCoordinate,
        coordinate,
        error,
        unknownFields
    )
}
