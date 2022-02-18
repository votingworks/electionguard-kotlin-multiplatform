package electionguard.ballot

import electionguard.core.*

class EncryptionMediator(
    val groupContext : GroupContext,
    val metadata: InternalManifest,
    val context: CiphertextElectionContext,
    val encryption_device: EncryptionDevice
) {
    init {
        // LOOK does not follow validation spec 6.A, which calls for crypto_base_hash.
        //   Ok to use device hash see Issue #272. Spec should be updated.
        var previous_tracking_hash = groupContext.hashElements(
            encryption_device.device_id,
            encryption_device.session_id,
            encryption_device.launch_code,
            encryption_device.location)
    }

    /** Encrypt the plaintext ballot using the joint public key K.  */
    fun encrypt(ballot: PlaintextBallot): CiphertextBallot? {
        val encrypted_ballot: encrypt_ballot(
            ballot, metadata, context, previous_tracking_hash, Optional.empty<ElementModQ>(), true
        )
        encrypted_ballot.ifPresent(java.util.function.Consumer<CiphertextBallot> { (_, _, _, _, _, code): CiphertextBallot ->
            previous_tracking_hash = code
        })
        return encrypted_ballot
    }
}

/**
 * Construct a `BallotSelection` from a specific `SelectionDescription`.
 * This function is useful for filling selections when a voter undervotes a ballot.
 * It is also used to create placeholder representations when generating the `ConstantChaumPedersenProof`
 *
 * @param mselection:    The `SelectionDescription` which provides the relevant `object_id`
 * @param is_placeholder: Mark this selection as a placeholder value
 * @param is_affirmative: Mark this selection as `yes`
 */
fun selection_from(
    mselection: Manifest.SelectionDescription,
    is_placeholder: Boolean,
    is_affirmative: Boolean
): PlaintextBallot.Selection {
    return PlaintextBallot.Selection(
        mselection.selection_id,
        mselection.sequence_order,
        if (is_affirmative) 1 else 0,
        is_placeholder,
        null
    )
}

/**
 * Construct a `BallotContest` from a specific `ContestDescription` with all false fields.
 * This function is useful for filling contests and selections when a voter undervotes a ballot.
 *
 * @param mcontest: The `ContestDescription` used to derive the well-formed `BallotContest`
 */
fun contest_from(mcontest: Manifest.ContestDescription): PlaintextBallot.Contest? {
    val selections = mutableListOf<PlaintextBallot.Selection>()
    for (selection_description in mcontest.ballot_selections) {
        selections.add(selection_from(selection_description, false, false))
    }
    return PlaintextBallot.Contest(mcontest.contest_id, mcontest.sequence_order, selections)
}

/**
 * Encrypt a PlaintextBallotSelection in the context of a specific SelectionDescription.
 *
 * @param selection:                 the selection in the valid input form
 * @param selection_description:     the `SelectionDescription` from the `ContestDescription` which defines this selection's structure
 * @param elgamal_public_key:        the public key (K) used to encrypt the ballot
 * @param crypto_extended_base_hash: the extended base hash of the election
 * @param nonce_seed:                an `ElementModQ` used as a header to seed the `Nonce` generated for this selection.
 * this value can be (or derived from) the BallotContest nonce, but no relationship is required
 * @param is_placeholder:            specifies if this is a placeholder selection
 * @param should_verify_proofs:      specify if the proofs should be verified prior to returning (default True)
 */
fun encrypt_selection(
    selection: PlaintextBallot.Selection,
    selection_description: Manifest.SelectionDescription,
    elgamal_public_key: ElementModP,
    crypto_extended_base_hash: ElementModQ,
    nonce_seed: ElementModQ,
    is_placeholder: Boolean,  // default false
    should_verify_proofs: Boolean /* default true */
): CiphertextBallot.Selection? {

    // Validate Input
    if (!selection.is_valid(selection_description.object_id)) {
        com.sunya.electionguard.Encrypt.logger.atWarning().log("invalid input selection_id: %s", selection.selection_id)
        return Optional.empty<CiphertextBallot.Selection>()
    }
    val selection_description_hash: ElementModQ = selection_description.cryptoHash()
    val nonce_sequence = Nonces(selection_description_hash, nonce_seed)
    val selection_nonce: ElementModQ = nonce_sequence.get(selection_description.sequence_order)
    // logger.atFine().log(
    //    "encrypt_selection %n  %s%n  %s%n  %d%n%s%n",
    //    selection_description.cryptoHash(), nonce_seed, selection_description.sequence_order, selection_nonce
    // )
    val disjunctive_chaum_pedersen_nonce: ElementModQ = nonce_sequence.get(0)

    // Generate the encryption
    val elgamal_encryption =
        ElGamal.elgamal_encrypt(selection.vote, selection_nonce, elgamal_public_key)
    if (elgamal_encryption.isEmpty()) {
        // will have logged about the failure earlier, so no need to log anything here
        return null
    }

    // TODO: ISSUE #47: encrypt/decrypt: encrypt the extended_data field
    val encrypted_selection: CiphertextBallot.Selection = CiphertextBallot.Selection(
        selection.selection_id,
        selection.sequence_order,
        selection_description_hash,
        elgamal_encryption.get(),
        elgamal_public_key,
        crypto_extended_base_hash,
        disjunctive_chaum_pedersen_nonce,
        selection.vote,
        is_placeholder,
        selection_nonce,
        null,
        null,
        null,
    )
    if (encrypted_selection.proof.isEmpty()) {
        return Optional.empty<CiphertextBallot.Selection>()
    }

    // optionally, skip the verification step
    if (!should_verify_proofs) {
        return Optional.of<CiphertextBallot.Selection>(encrypted_selection)
    }

    // verify the selection.
    return if (encrypted_selection.is_valid_encryption(
            selection_description_hash,
            elgamal_public_key,
            crypto_extended_base_hash
        )
    ) {
        Optional.of<CiphertextBallot.Selection>(encrypted_selection)
    } else {
        com.sunya.electionguard.Encrypt.logger.atWarning()
            .log("Failed selection proof for selection: %s", encrypted_selection.object_id)
        Optional.empty<CiphertextBallot.Selection>()
    }
}

/**
 * Encrypt a PlaintextBallotContest into CiphertextBallot.Contest.
 *
 *
 * This method accepts a contest representation that only includes `True` selections.
 * It will fill missing selections for a contest with `False` values, and generate `placeholder`
 * selections to represent the number of seats available for a given contest.  By adding `placeholder`
 * votes
 *
 * @param contest:                   the contest in the valid input form
 * @param contest_description:       the `ContestDescriptionWithPlaceholders` from the `ContestDescription` which defines this contest's structure
 * @param elgamal_public_key:        the public key (k) used to encrypt the ballot
 * @param crypto_extended_base_hash: the extended base hash of the election
 * @param nonce_seed:                an `ElementModQ` used as a header to seed the `Nonce` generated for this contest.
 * this value can be (or derived from) the Ballot nonce, but no relationship is required
 * @param should_verify_proofs:      specify if the proofs should be verified prior to returning (default True)
 */
fun encrypt_contest(
    contest: PlaintextBallot.Contest?,
    contest_description: ContestWithPlaceholders,
    elgamal_public_key: ElementModP?,
    crypto_extended_base_hash: ElementModQ?,
    nonce_seed: ElementModQ?,
    should_verify_proofs: Boolean /* default true */
): Optional<CiphertextBallot.Contest?>? {

    // Validate Input
    if (!contest.is_valid(
            contest_description.object_id,
            contest_description.ballot_selections.size(),
            contest_description.number_elected,
            contest_description.votes_allowed
        )
    ) {
        com.sunya.electionguard.Encrypt.logger.atWarning().log("invalid input contest: %s", contest)
        return Optional.empty<CiphertextBallot.Contest>()
    }
    if (!contest_description.is_valid()) {
        com.sunya.electionguard.Encrypt.logger.atWarning()
            .log("invalid input contest_description: %s", contest_description)
        return Optional.empty<CiphertextBallot.Contest>()
    }

    // LOOK using sequence_order. Do we need to check for uniqueness?
    val contest_description_hash: ElementModQ = contest_description.cryptoHash()
    val nonce_sequence = Nonces(contest_description_hash, nonce_seed)
    val contest_nonce: ElementModQ = nonce_sequence.get(contest_description.sequence_order)
    val chaum_pedersen_nonce: ElementModQ = nonce_sequence.get(0)
    var selection_count = 0
    val encrypted_selections: MutableList<CiphertextBallot.Selection> =
        java.util.ArrayList<CiphertextBallot.Selection>()
    // LOOK this will fail if there are duplicate selection_id's
    val plaintext_selections: Map<String, PlaintextBallot.Selection> = contest!!.ballot_selections.stream().collect(
        Collectors.toMap(
            java.util.function.Function<T, K> { s: T -> s.selection_id },
            java.util.function.Function<T, U> { s: T? -> s })
    )

    // LOOK only iterate on selections that match the manifest. If there are selections contests on the ballot,
    //   they are silently ignored.
    for (description in contest_description.ballot_selections) {
        var encrypted_selection: Optional<CiphertextBallot.Selection?>

        // Find the actual selection matching the contest description.
        // If there is not one, an explicit false is entered instead and the selection_count is not incremented.
        // This allows ballots to contain only the yes votes, if so desired.
        val plaintext_selection = plaintext_selections[description.object_id]
        if (plaintext_selection != null) {
            // track the selection count so we can append the
            // appropriate number of true placeholder votes
            selection_count += plaintext_selection.vote
            encrypted_selection = encrypt_selection(
                plaintext_selection,
                description,
                elgamal_public_key,
                crypto_extended_base_hash,
                contest_nonce,
                false,
                true
            )
        } else {
            // No selection was made for this possible value so we explicitly set it to false
            encrypted_selection = encrypt_selection(
                selection_from(description, false, false),
                description,
                elgamal_public_key,
                crypto_extended_base_hash,
                contest_nonce,
                false,
                true
            )
        }
        if (encrypted_selection.isEmpty()) {
            return Optional.empty<CiphertextBallot.Contest>() // log will have happened earlier
        }
        encrypted_selections.add(encrypted_selection.get())
    }

    // Handle Placeholder selections. After we loop through all of the real selections on the ballot,
    // we loop through each placeholder value and determine if it should be filled in

    // Add a placeholder selection for each possible seat in the contest
    for (placeholder in contest_description.placeholder_selections) {
        // for undervotes, select the placeholder value as true for each available seat
        // note this pattern is used since DisjunctiveChaumPedersen expects a 0 or 1
        // so each seat can only have a maximum value of 1 in the current implementation
        var select_placeholder = false
        if (selection_count < contest_description.number_elected) {
            select_placeholder = true
            selection_count += 1
        }
        val encrypted_selection: Optional<CiphertextBallot.Selection> = encrypt_selection(
            selection_from(placeholder, true, select_placeholder),
            placeholder,
            elgamal_public_key,
            crypto_extended_base_hash,
            contest_nonce, true, true
        )
        if (encrypted_selection.isEmpty()) {
            return Optional.empty<CiphertextBallot.Contest>() // log will have happened earlier
        }
        encrypted_selections.add(encrypted_selection.get())
    }

    // TODO: ISSUE #33: support other cases such as cumulative voting (individual selections being an encryption of > 1)
    if (contest_description.votes_allowed.isPresent() && selection_count < contest_description.votes_allowed.get()) {
        com.sunya.electionguard.Encrypt.logger.atWarning()
            .log("mismatching selection count: only n-of-m style elections are currently supported")
    }
    val encrypted_contest: CiphertextBallot.Contest = CiphertextBallot.Contest.create(
        contest.contest_id,
        contest.sequence_order,
        contest_description_hash,
        encrypted_selections,
        elgamal_public_key,
        crypto_extended_base_hash,
        chaum_pedersen_nonce,
        contest_description.number_elected,
        Optional.of(contest_nonce)
    )
    if (encrypted_contest.proof.isEmpty()) {
        return Optional.empty<CiphertextBallot.Contest>() // log error will have happened earlier
    }
    if (!should_verify_proofs) {
        return Optional.of<CiphertextBallot.Contest>(encrypted_contest)
    }

    // Verify the proof
    return if (encrypted_contest.is_valid_encryption(
            contest_description_hash,
            elgamal_public_key,
            crypto_extended_base_hash
        )
    ) {
        Optional.of<CiphertextBallot.Contest>(encrypted_contest)
    } else {
        encrypted_contest.is_valid_encryption(contest_description_hash, elgamal_public_key, crypto_extended_base_hash)
        com.sunya.electionguard.Encrypt.logger.atWarning()
            .log("mismatching contest proof for contest %s", encrypted_contest.object_id)
        Optional.empty<CiphertextBallot.Contest>()
    }
}

// TODO: ISSUE #57: add the device hash to the function interface so it can be propagated with the ballot.
//  also propagate the seed hash so that the ballot tracking id's can be regenerated
//  by traversing the collection of ballots encrypted by a specific device

// TODO: ISSUE #57: add the device hash to the function interface so it can be propagated with the ballot.
//  also propagate the seed hash so that the ballot tracking id's can be regenerated
//  by traversing the collection of ballots encrypted by a specific device
/**
 * Encrypt a PlaintextBallot into a CiphertextBallot.
 *
 *
 * This method accepts a ballot representation that only includes `True` selections.
 * It will fill missing selections for a contest with `False` values, and generate `placeholder`
 * selections to represent the number of seats available for a given contest.
 *
 *
 * This method also allows for ballots to exclude passing contests for which the voter made no selections.
 * It will fill missing contests with `False` selections and generate `placeholder` selections that are marked `True`.
 *
 * @param ballot:               the ballot in the valid input form
 * @param internal_manifest:    the InternalManifest which defines this ballot's structure
 * @param context:              all the cryptographic context for the election
 * @param previous_tracking_hash Hash from previous ballot or starting hash from device. python: seed_hash
 * @param nonce:                an optional nonce used to encrypt this contest
 * if this value is not provided, a random nonce is used.
 * @param should_verify_proofs: specify if the proofs should be verified prior to returning (default True)
 */
fun encrypt_ballot(
    ballot: PlaintextBallot,
    internal_manifest: InternalManifest,
    context: CiphertextElectionContext,
    previous_tracking_hash: ElementModQ?,
    nonce: ElementModQ?,
    should_verify_proofs: Boolean
): CiphertextBallot? {

    // Determine the relevant range of contests for this ballot style
    val style = internal_manifest.get_ballot_style(ballot.style_id)

    // Validate Input
    if (!style) {
        // logger.atWarning().log("Ballot Style '%s' does not exist in election", ballot.style_id)
        return null
    }

    // Validate Input LOOK could just call BallotInputValidation? Or rely on it being done externally.
    if (!ballot.is_valid(style.get().object_id)) {
        return Optional.empty<CiphertextBallot>()
    }

    // Generate a random master nonce to use for the contest and selection nonce's on the ballot
    val random_master_nonce: ElementModQ = nonce.orElse(rand_q())

    // Include a representation of the election and the ballot Id in the nonce's used
    // to derive other nonce values on the ballot
    val nonce_seed: ElementModQ =
        CiphertextBallot.nonce_seed(internal_manifest.manifest.cryptoHash, ballot.object_id, random_master_nonce)
    val encrypted_contests: Optional<List<CiphertextBallot.Contest>> = encrypt_ballot_contests(
        ballot, internal_manifest, context, nonce_seed
    )
    if (encrypted_contests.isEmpty()) {
        return Optional.empty<CiphertextBallot>()
    }

    // Create the return object
    val encrypted_ballot: CiphertextBallot = CiphertextBallot.create(
        ballot.object_id,
        ballot.style_id,
        internal_manifest.manifest.cryptoHash,
        previous_tracking_hash,  // python uses Optional
        encrypted_contests.get(),
        Optional.of(random_master_nonce), Optional.empty(), Optional.empty()
    )
    if (!should_verify_proofs) {
        return Optional.of<CiphertextBallot>(encrypted_ballot)
    }

    // Verify the proofs
    return if (encrypted_ballot.is_valid_encryption(
            internal_manifest.manifest.cryptoHash,
            context.elgamal_public_key,
            context.crypto_extended_base_hash
        )
    ) {
        Optional.of<CiphertextBallot>(encrypted_ballot)
    } else {
        Optional.empty<CiphertextBallot>() // log error will have happened earlier
    }
}

/** Encrypt contests from a plaintext ballot with a specific style.  */
fun encrypt_ballot_contests(
    ballot: PlaintextBallot,
    description: InternalManifest,
    context: CiphertextElectionContext,
    nonce_seed: ElementModQ?
): List<CiphertextBallot.Contest>? {
    val encrypted_contests: MutableList<CiphertextBallot.Contest> = java.util.ArrayList<CiphertextBallot.Contest>()
    // LOOK this will fail if there are duplicate contest_id's
    val plaintext_contests: Map<String, PlaintextBallot.Contest> = ballot.contests.stream()
        .collect(
            Collectors.toMap(
                java.util.function.Function<T, K> { c: T -> c.contest_id },
                java.util.function.Function<T, U> { c: T? -> c })
        )

    // LOOK only iterate on contests that match the manifest. If there are miscoded contests on the ballot,
    //   they are silently ignored.
    for (contestDescription in description.get_contests_for_style(ballot.style_id)) {
        var use_contest = plaintext_contests[contestDescription.object_id]

        // no selections provided for the contest, so create a placeholder contest
        // LOOK says "create a placeholder contest" but selections are not placeholders.
        if (use_contest == null) {
            use_contest = contest_from(contestDescription)
        }
        val encrypted_contest: Optional<CiphertextBallot.Contest> = encrypt_contest(
            use_contest,
            contestDescription,
            context.elgamal_public_key,
            context.crypto_extended_base_hash,
            nonce_seed, true
        )
        if (encrypted_contest.isEmpty()) {
            return Optional.empty<List<CiphertextBallot.Contest>>() //log will have happened earlier
        }
        encrypted_contests.add(encrypted_contest.get())
    }
    return Optional.of<List<CiphertextBallot.Contest>>(encrypted_contests)
}