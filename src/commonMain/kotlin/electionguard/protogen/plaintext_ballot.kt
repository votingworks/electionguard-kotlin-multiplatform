@file:OptIn(pbandk.PublicForGeneratedCode::class)

package electionguard.protogen

@pbandk.Export
public data class PlaintextBallot(
    val ballotId: String = "",
    val ballotStyleId: String = "",
    val contests: List<electionguard.protogen.PlaintextBallotContest> = emptyList(),
    val errors: String = "",
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.PlaintextBallot =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallot>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.PlaintextBallot> {
        public val defaultInstance: electionguard.protogen.PlaintextBallot by
            lazy { electionguard.protogen.PlaintextBallot() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.PlaintextBallot =
            electionguard.protogen.PlaintextBallot.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallot> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.PlaintextBallot, *>>(4)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "ballot_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "ballotId",
                            value = electionguard.protogen.PlaintextBallot::ballotId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "ballot_style_id",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "ballotStyleId",
                            value = electionguard.protogen.PlaintextBallot::ballotStyleId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "contests",
                            number = 3,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.PlaintextBallotContest>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .PlaintextBallotContest
                                                            .Companion
                                                )
                                    ),
                            jsonName = "contests",
                            value = electionguard.protogen.PlaintextBallot::contests
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "errors",
                            number = 4,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "errors",
                            value = electionguard.protogen.PlaintextBallot::errors
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextBallot",
                    messageClass = electionguard.protogen.PlaintextBallot::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class PlaintextBallotContest(
    val contestId: String = "",
    val sequenceOrder: Int = 0,
    val selections: List<electionguard.protogen.PlaintextBallotSelection> = emptyList(),
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.PlaintextBallotContest = protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallotContest>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.PlaintextBallotContest> {

        public val defaultInstance: electionguard.protogen.PlaintextBallotContest by
            lazy { electionguard.protogen.PlaintextBallotContest() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.PlaintextBallotContest =
            electionguard.protogen.PlaintextBallotContest.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallotContest> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<electionguard.protogen.PlaintextBallotContest, *>
                    >(3)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "contest_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "contestId",
                            value = electionguard.protogen.PlaintextBallotContest::contestId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "sequence_order",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "sequenceOrder",
                            value = electionguard.protogen.PlaintextBallotContest::sequenceOrder
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selections",
                            number = 3,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Repeated<electionguard.protogen.PlaintextBallotSelection>(
                                        valueType =
                                            pbandk.FieldDescriptor
                                                .Type
                                                .Message(
                                                    messageCompanion =
                                                        electionguard.protogen
                                                            .PlaintextBallotSelection
                                                            .Companion
                                                )
                                    ),
                            jsonName = "selections",
                            value = electionguard.protogen.PlaintextBallotContest::selections
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextBallotContest",
                    messageClass = electionguard.protogen.PlaintextBallotContest::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class PlaintextBallotSelection(
    val selectionId: String = "",
    val sequenceOrder: Int = 0,
    val vote: Int = 0,
    val isPlaceholderSelection: Boolean = false,
    val extendedData: electionguard.protogen.ExtendedData? = null,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(
        other: pbandk.Message?
    ): electionguard.protogen.PlaintextBallotSelection = protoMergeImpl(other)
    override val descriptor:
        pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallotSelection>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object :
        pbandk.Message.Companion<electionguard.protogen.PlaintextBallotSelection> {

        public val defaultInstance: electionguard.protogen.PlaintextBallotSelection by
            lazy { electionguard.protogen.PlaintextBallotSelection() }
        override fun decodeWith(
            u: pbandk.MessageDecoder
        ): electionguard.protogen.PlaintextBallotSelection =
            electionguard.protogen.PlaintextBallotSelection.decodeWithImpl(u)

        override val descriptor:
            pbandk.MessageDescriptor<electionguard.protogen.PlaintextBallotSelection> by
            lazy {
                val fieldsList =
                    ArrayList<
                        pbandk.FieldDescriptor<electionguard.protogen.PlaintextBallotSelection, *>
                    >(5)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "selection_id",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "selectionId",
                            value = electionguard.protogen.PlaintextBallotSelection::selectionId
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "sequence_order",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "sequenceOrder",
                            value = electionguard.protogen.PlaintextBallotSelection::sequenceOrder
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "vote",
                            number = 3,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "vote",
                            value = electionguard.protogen.PlaintextBallotSelection::vote
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "is_placeholder_selection",
                            number = 4,
                            type = pbandk.FieldDescriptor.Type.Primitive.Bool(),
                            jsonName = "isPlaceholderSelection",
                            value =
                                electionguard.protogen
                                    .PlaintextBallotSelection::isPlaceholderSelection
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "extended_data",
                            number = 5,
                            type =
                                pbandk.FieldDescriptor
                                    .Type
                                    .Message(
                                        messageCompanion =
                                            electionguard.protogen.ExtendedData.Companion
                                    ),
                            jsonName = "extendedData",
                            value = electionguard.protogen.PlaintextBallotSelection::extendedData
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "PlaintextBallotSelection",
                    messageClass = electionguard.protogen.PlaintextBallotSelection::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
public data class ExtendedData(
    val value: String = "",
    val length: Int = 0,
    override val unknownFields: Map<Int, pbandk.UnknownField> = emptyMap()
) : pbandk.Message {
    override operator fun plus(other: pbandk.Message?): electionguard.protogen.ExtendedData =
        protoMergeImpl(other)
    override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ExtendedData>
        get() = Companion.descriptor
    override val protoSize: Int by lazy { super.protoSize }
    public companion object : pbandk.Message.Companion<electionguard.protogen.ExtendedData> {
        public val defaultInstance: electionguard.protogen.ExtendedData by
            lazy { electionguard.protogen.ExtendedData() }
        override fun decodeWith(u: pbandk.MessageDecoder): electionguard.protogen.ExtendedData =
            electionguard.protogen.ExtendedData.decodeWithImpl(u)

        override val descriptor: pbandk.MessageDescriptor<electionguard.protogen.ExtendedData> by
            lazy {
                val fieldsList =
                    ArrayList<pbandk.FieldDescriptor<electionguard.protogen.ExtendedData, *>>(2)
                fieldsList.apply {
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "value",
                            number = 1,
                            type = pbandk.FieldDescriptor.Type.Primitive.String(),
                            jsonName = "value",
                            value = electionguard.protogen.ExtendedData::value
                        )
                    )
                    add(
                        pbandk.FieldDescriptor(
                            messageDescriptor = this@Companion::descriptor,
                            name = "length",
                            number = 2,
                            type = pbandk.FieldDescriptor.Type.Primitive.UInt32(),
                            jsonName = "length",
                            value = electionguard.protogen.ExtendedData::length
                        )
                    )
                }
                pbandk.MessageDescriptor(
                    fullName = "ExtendedData",
                    messageClass = electionguard.protogen.ExtendedData::class,
                    messageCompanion = this,
                    fields = fieldsList
                )
            }
    }
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextBallot")
public fun PlaintextBallot?.orDefault(): electionguard.protogen.PlaintextBallot =
    this ?: PlaintextBallot.defaultInstance

private fun PlaintextBallot.protoMergeImpl(plus: pbandk.Message?): PlaintextBallot =
    (plus as? PlaintextBallot)
        ?.let {
            it.copy(
                contests = contests + plus.contests,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextBallot.Companion.decodeWithImpl(u: pbandk.MessageDecoder): PlaintextBallot {
    var ballotId = ""
    var ballotStyleId = ""
    var contests: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextBallotContest>? = null
    var errors = ""

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> ballotId = _fieldValue as String
                2 -> ballotStyleId = _fieldValue as String
                3 ->
                    contests =
                        (contests ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.PlaintextBallotContest>
                            }
                4 -> errors = _fieldValue as String
            }
        }
    return PlaintextBallot(
        ballotId,
        ballotStyleId,
        pbandk.ListWithSize.Builder.fixed(contests),
        errors,
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextBallotContest")
public fun PlaintextBallotContest?.orDefault(): electionguard.protogen.PlaintextBallotContest =
    this ?: PlaintextBallotContest.defaultInstance

private fun PlaintextBallotContest.protoMergeImpl(plus: pbandk.Message?): PlaintextBallotContest =
    (plus as? PlaintextBallotContest)
        ?.let {
            it.copy(
                selections = selections + plus.selections,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextBallotContest.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): PlaintextBallotContest {
    var contestId = ""
    var sequenceOrder = 0
    var selections: pbandk.ListWithSize.Builder<electionguard.protogen.PlaintextBallotSelection>? =
        null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> contestId = _fieldValue as String
                2 -> sequenceOrder = _fieldValue as Int
                3 ->
                    selections =
                        (selections ?: pbandk.ListWithSize.Builder())
                            .apply {
                                this +=
                                    _fieldValue as
                                        Sequence<electionguard.protogen.PlaintextBallotSelection>
                            }
            }
        }
    return PlaintextBallotContest(
        contestId,
        sequenceOrder,
        pbandk.ListWithSize.Builder.fixed(selections),
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForPlaintextBallotSelection")
public fun PlaintextBallotSelection?.orDefault(): electionguard.protogen.PlaintextBallotSelection =
    this ?: PlaintextBallotSelection.defaultInstance

private fun PlaintextBallotSelection.protoMergeImpl(
    plus: pbandk.Message?
): PlaintextBallotSelection =
    (plus as? PlaintextBallotSelection)
        ?.let {
            it.copy(
                extendedData = extendedData?.plus(plus.extendedData) ?: plus.extendedData,
                unknownFields = unknownFields + plus.unknownFields
            )
        } ?: this

@Suppress("UNCHECKED_CAST")
private fun PlaintextBallotSelection.Companion.decodeWithImpl(
    u: pbandk.MessageDecoder
): PlaintextBallotSelection {
    var selectionId = ""
    var sequenceOrder = 0
    var vote = 0
    var isPlaceholderSelection = false
    var extendedData: electionguard.protogen.ExtendedData? = null

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> selectionId = _fieldValue as String
                2 -> sequenceOrder = _fieldValue as Int
                3 -> vote = _fieldValue as Int
                4 -> isPlaceholderSelection = _fieldValue as Boolean
                5 -> extendedData = _fieldValue as electionguard.protogen.ExtendedData
            }
        }
    return PlaintextBallotSelection(
        selectionId,
        sequenceOrder,
        vote,
        isPlaceholderSelection,
        extendedData,
        unknownFields
    )
}

@pbandk.Export
@pbandk.JsName("orDefaultForExtendedData")
public fun ExtendedData?.orDefault(): electionguard.protogen.ExtendedData =
    this ?: ExtendedData.defaultInstance

private fun ExtendedData.protoMergeImpl(plus: pbandk.Message?): ExtendedData =
    (plus as? ExtendedData)?.let { it.copy(unknownFields = unknownFields + plus.unknownFields) }
        ?: this

@Suppress("UNCHECKED_CAST")
private fun ExtendedData.Companion.decodeWithImpl(u: pbandk.MessageDecoder): ExtendedData {
    var value = ""
    var length = 0

    val unknownFields =
        u.readMessage(this) { _fieldNumber, _fieldValue ->
            when (_fieldNumber) {
                1 -> value = _fieldValue as String
                2 -> length = _fieldValue as Int
            }
        }
    return ExtendedData(value, length, unknownFields)
}
