package electionguard.json

import electionguard.ballot.Guardian
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

/* guardian_1.public_key.json
{
  "i": 1,
  "name": "Guardian 1",
  "coefficient_commitments": [
    "base64:yNAIiN4d6qWyTwRYKsXEVTCMQ0mdsMLZI5xCoHqjEDiB9IIFEe9kuBEWKBvPefRrTXe1y2qfNyIZc3Zl3pdkLZ+MtvMhnU9hJ6OAzSyyIIkEVK1V6StzsLUMsOOcnsc1/0aMNhMADtm9VfAKNNQ7TsgX7+spgb3WWHzJI8p2gxQhUPoh0p8y8rGOH6ukeLuYrRlINPuoIZMGpdOBlcI5RItt7kpEnBGC3RyUq2kx3soibSFtrItYJg1g3UJc1uQhXiF3NWuL7xbtsAqAkMxd6eiwDQzaQy16FeI7WgpDc1GOQVELYY17awdJ6nGlC+e+ZmV4BpAyR3lUX8raD8bJ8WzfxXQ8DjhS+k4Lb2vSYATaGW/0AqgJ83B9lc1NMwFxLuqIBYa/1ZexbDJmn/u5AanwfRbvyu0QR3q8mGCf9nVqESXWtUJsLrpM/GLNZr7UH/tbzqOmTlFyx9KWmfEbAUYUnC4Fz8b4HaRfLUiG1fISUCv4tJBny65l3JZwSWtVkLLcH1lyoHhFzEJsqk1gS957p2bJqKMHHacXw3QI0rkmi5iLUbqUDb4WYczcQh3kIgZLod4ZA5RYkWIrRD0N4wgYctA/ybk2j3TDP2otRHUglduoI5M2MyY8QYCz01IHRQJiQAvNfdrCIdXx24o1pl7r8GT4lu6NeYuf4KrkBVI=",
    "base64:k0/Dnl+24OgMQTaw5iPuOvXYt++pelIKGjFsxdQGFrso2dYp9jfR/tXMyPTy0fcU1jAelND7CnUluZ3jnf8I9PEnQE+Nmg4pQOK4e5sGE+dQEG9TthrJWoxhchBG7x/Go4M3X5hCP5OAbKPqFv1EmRow56DcyLuyd7vI57+gsd9u4pGA1Vr7uzUDlAzQ0+ZJLiUi/CrkemHaIWoSghLv8O6us6bZZuEwDjGgKR19K9xo/lS2TQJ5bBaFPCLB+b45HUWNCBykCpq9yPiMqIVgzi9fRVI2nYUjxyZNuN9XsU8FISV1lY/epsVFO4s7SHG2TV9L6zRT5IwA+ACLgWCExsxTgobFy7MGtycuKIcMPCdcw2H/glLzzlwW0mYhvZO7mTpq9jv1chhPsOy6AqgDODo1nDjnAOP0psp0n0NhJIDXhBIx+BtU9pQ2VmzbMnlJ53jfwQaNUCJFFL59CvWxj5wapji3GRcfRe3d6rKyNN2hi8zV8hrTSbT9WSG07U7z0NSZsBBHlm2ZpN89E8v68brPGnFMYD8BWPbuL2yM84gOEeXOcJVatD38G05HCPGp6q7+zRg+suHmMtQCqJmxkuLFHZg/CYA0kCSXxHK6GxpbyDPiTmo4BQYsHWy/J9VPlevKKZfwhW2dROzSmlRcfva1yHxi58XmrNtaYzosvb4=",
    "base64:6bb5Ye0vK6N2XcbJ2UmOdaSw1Sg7C9g4lsk25SorRfDNTKbdxM0cvQa//SMSLUqJLfSf0pr5tfq5PTMryn14GLeR9CNOULe/CdzQvykF/g8RTWREhzzq+aSPmcCIAQ+PrhcpAvAGn7EI/+udbJ32T4qZN3/kVrDJC/BQkTcEf8S74Zwt5h0xYWRitjS4gAVgovaq80TBfjn7KX86XEDFWYy4V8eYnmkkTC9/94T/DBbMhwBBuPtfGMFFnLEDeO79ha3hCbGfo0gnUs4rznBzf6+NOCkNejLeFx5mskcblvzfw+fqwwNzgY5ZwiaxR6W3uIlO6jSifTtK+BJpR1+pL9vhnZ+T4mOjnHMIwyyWHt4GZ0Tk+YG0yCmxsDwtghxQZeGCGmqW2E6rRxtcUasQSz/GJvrkrmQ59Dfa6JTEf1DBcwOk+GKL3v/NKhYRuFQdWGVpdgSsJpdSL2nVvF3HuL8LnyMK8z2pQz2scDo73bVH43EgS3hU5wQmk/gv7sZmXlstibbv9SQEuJmqewXVS64NkRGqtoxpzVFyQLMODyxxUQV2vOnBF89F+iDU5d9C7XYot2l9f/T3FPfAA18Y10jsZNoy8c9R/Jr0LxBmIKV028TsQ941nfLyPMccVwFjIhUjPjLlrZATCotsY/hBJRYd8bwPqtj/DiYKezRdrGk="
  ]
}
 */

@Serializable
data class GuardianJsonR(
    val i : Int,
    val name : String,
    val coefficient_commitments : List<ElementModPJsonR>,
)

data class GuardianR(
    val name : String,
    val i : Int,
    val coefficient_commitments : List<ElementModP>,
)

fun GuardianJsonR.import(group: GroupContext, errs : ErrorMessages) : GuardianR? {
    val coefficient_commitments = this.coefficient_commitments.map {
        it.import(group) ?: errs.addNull("wtf") as ElementModP?
    }

    return if (errs.hasErrors()) null
    else GuardianR(this.name, this.i, coefficient_commitments.filterNotNull())
}

fun GuardianR.convert(group: GroupContext) = Guardian(
    this.name,
    this.i,
    //     Schnorr proof is missing - fake it TODO WTF?
    this.coefficient_commitments.map { SchnorrProof(it, group.ONE_MOD_Q, group.TWO_MOD_Q) }
)

/* guardian_1.SECRET_key.json
{
  "i": 1,
  "name": "Guardian 1",
  "secret_coefficients": [
    "base64:tG6O+Bu6GOQJusisrWEYSj4nPhSLRkDtBoA7Gs3zrb4=",
    "base64:Cx9kBv/oJaGz7onZMhq0MivEKP4DJEnOP5n8L7Hm65A=",
    "base64:N+wkretXJ7zv60tWdEwpjdRk6qpitOS9msl4beNe2G4="
  ],
  "coefficient_commitments": [
    "base64:yNAIiN4d6qWyTwRYKsXEVTCMQ0mdsMLZI5xCoHqjEDiB9IIFEe9kuBEWKBvPefRrTXe1y2qfNyIZc3Zl3pdkLZ+MtvMhnU9hJ6OAzSyyIIkEVK1V6StzsLUMsOOcnsc1/0aMNhMADtm9VfAKNNQ7TsgX7+spgb3WWHzJI8p2gxQhUPoh0p8y8rGOH6ukeLuYrRlINPuoIZMGpdOBlcI5RItt7kpEnBGC3RyUq2kx3soibSFtrItYJg1g3UJc1uQhXiF3NWuL7xbtsAqAkMxd6eiwDQzaQy16FeI7WgpDc1GOQVELYY17awdJ6nGlC+e+ZmV4BpAyR3lUX8raD8bJ8WzfxXQ8DjhS+k4Lb2vSYATaGW/0AqgJ83B9lc1NMwFxLuqIBYa/1ZexbDJmn/u5AanwfRbvyu0QR3q8mGCf9nVqESXWtUJsLrpM/GLNZr7UH/tbzqOmTlFyx9KWmfEbAUYUnC4Fz8b4HaRfLUiG1fISUCv4tJBny65l3JZwSWtVkLLcH1lyoHhFzEJsqk1gS957p2bJqKMHHacXw3QI0rkmi5iLUbqUDb4WYczcQh3kIgZLod4ZA5RYkWIrRD0N4wgYctA/ybk2j3TDP2otRHUglduoI5M2MyY8QYCz01IHRQJiQAvNfdrCIdXx24o1pl7r8GT4lu6NeYuf4KrkBVI=",
    "base64:k0/Dnl+24OgMQTaw5iPuOvXYt++pelIKGjFsxdQGFrso2dYp9jfR/tXMyPTy0fcU1jAelND7CnUluZ3jnf8I9PEnQE+Nmg4pQOK4e5sGE+dQEG9TthrJWoxhchBG7x/Go4M3X5hCP5OAbKPqFv1EmRow56DcyLuyd7vI57+gsd9u4pGA1Vr7uzUDlAzQ0+ZJLiUi/CrkemHaIWoSghLv8O6us6bZZuEwDjGgKR19K9xo/lS2TQJ5bBaFPCLB+b45HUWNCBykCpq9yPiMqIVgzi9fRVI2nYUjxyZNuN9XsU8FISV1lY/epsVFO4s7SHG2TV9L6zRT5IwA+ACLgWCExsxTgobFy7MGtycuKIcMPCdcw2H/glLzzlwW0mYhvZO7mTpq9jv1chhPsOy6AqgDODo1nDjnAOP0psp0n0NhJIDXhBIx+BtU9pQ2VmzbMnlJ53jfwQaNUCJFFL59CvWxj5wapji3GRcfRe3d6rKyNN2hi8zV8hrTSbT9WSG07U7z0NSZsBBHlm2ZpN89E8v68brPGnFMYD8BWPbuL2yM84gOEeXOcJVatD38G05HCPGp6q7+zRg+suHmMtQCqJmxkuLFHZg/CYA0kCSXxHK6GxpbyDPiTmo4BQYsHWy/J9VPlevKKZfwhW2dROzSmlRcfva1yHxi58XmrNtaYzosvb4=",
    "base64:6bb5Ye0vK6N2XcbJ2UmOdaSw1Sg7C9g4lsk25SorRfDNTKbdxM0cvQa//SMSLUqJLfSf0pr5tfq5PTMryn14GLeR9CNOULe/CdzQvykF/g8RTWREhzzq+aSPmcCIAQ+PrhcpAvAGn7EI/+udbJ32T4qZN3/kVrDJC/BQkTcEf8S74Zwt5h0xYWRitjS4gAVgovaq80TBfjn7KX86XEDFWYy4V8eYnmkkTC9/94T/DBbMhwBBuPtfGMFFnLEDeO79ha3hCbGfo0gnUs4rznBzf6+NOCkNejLeFx5mskcblvzfw+fqwwNzgY5ZwiaxR6W3uIlO6jSifTtK+BJpR1+pL9vhnZ+T4mOjnHMIwyyWHt4GZ0Tk+YG0yCmxsDwtghxQZeGCGmqW2E6rRxtcUasQSz/GJvrkrmQ59Dfa6JTEf1DBcwOk+GKL3v/NKhYRuFQdWGVpdgSsJpdSL2nVvF3HuL8LnyMK8z2pQz2scDo73bVH43EgS3hU5wQmk/gv7sZmXlstibbv9SQEuJmqewXVS64NkRGqtoxpzVFyQLMODyxxUQV2vOnBF89F+iDU5d9C7XYot2l9f/T3FPfAA18Y10jsZNoy8c9R/Jr0LxBmIKV028TsQ941nfLyPMccVwFjIhUjPjLlrZATCotsY/hBJRYd8bwPqtj/DiYKezRdrGk="
  ]
}
 */

@Serializable
data class GuardianSecretJsonR(
    val i : Int,
    val name : String,
    val secret_coefficients : List<ElementModQJsonR>,
    val coefficient_commitments : List<ElementModPJsonR>,
)

data class GuardianSecretR(
    val name : String,
    val i : Int,
    val secret_coefficients : List<ElementModQ>,
    val coefficient_commitments : List<ElementModP>,
)

fun GuardianSecretJsonR.import(group: GroupContext, errs : ErrorMessages) : GuardianSecretR? {
    val secret_coefficients = this.secret_coefficients.map{
        it.import(group) ?: errs.addNull("wtf") as ElementModQ?
    }
    val coefficient_commitments = this.coefficient_commitments.map {
        it.import(group) ?: errs.addNull("wtf") as ElementModP?
    }

    return if (errs.hasErrors()) null
    else
        GuardianSecretR(
            this.name,
            this.i,
            secret_coefficients.filterNotNull(),
            coefficient_commitments.filterNotNull()
        )
}

//     val id: String,
//    val xCoordinate: Int,
//    val publicKey: ElementModP, // Must match the public record
//    val keyShare: ElementModQ, // P(i) = (P1 (i) + P2 (i) + · · · + Pn (i)) eq 65
fun GuardianSecretR.convert() = DecryptingTrusteeDoerre(
    this.name,
    this.i,
    coefficient_commitments[0],
    secret_coefficients[0], // this is s_i instead of P(i). thresholding not implemented i think.
)

// DecryptingTrusteeDoerre

/* joint_election_public_key.json
{
  "joint_election_public_key": "base64:f83HoytqNClEYzNDRFti0W63k3VXT78FbF13Zr/GSxEDvbtRoW7JezIiN0gN+RPiytrlRAf2ez5Tr7nYNenG5l48pnQQZkxJIQz0X9QbgEqmqH+Ke1MSrGEFtyLx5E4Mro151ws4BZ8y6mDyVCRRwVl3eaUqH5mYQwR813AyD8FGbN9nXJk3y6Kkj4B1OBGBpa84YVSKiqaweiCWbtIJ3mmughVfxWw11cTcjTtcLbyZ1wHkHV/qG+/N1IS/6V3C6ZvhxUr5JjJtFmmEoMS+FP+sjjovo1TYD7B9NmQdZVq7GbftNv7xq8j+c/Kxx7FsJwHYHo6WcVqfyni4rY1gXKUtUBLqlTXtF/fIO8TEtP/n7rCOzneEJhjarom2tnPNouGIFokbDEzNW09xNs7TU8MmSjuel7M9PTchTfDklD387A78zsBtz6Mt8iWh0FO8RuMTyoKE6oKD65DCI/xhdniq1UB62J+g/C+cmuIG6/CTTZu2eu5a0cUiX/ykPjqXnvVc85/WYGjLtfprsPIalPyJ9MTYSWxT4yTGcHCwackc0tYgPwnJH2hZPM4SaOkXVlwWsL0niX3YAZNlm2UFuDSdxC5V6DsEPo/37hq+lb4fWR0nZi8fNXsRRGSY73dU9wcpQGdeHtfRuNOYtdLLzLwtpMuLppXVOtgAqov56NI="
}
 */

@Serializable
data class JointElectionPublicKeyJsonR(
    val joint_election_public_key : ElementModPJsonR,
)

fun JointElectionPublicKeyJsonR.import(group: GroupContext) : ElementModP? {
    return this.joint_election_public_key.import(group)
}
