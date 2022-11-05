package electionguard.input

class ValidationMessages(val id: String, private val level: Int) {
    private val messages = ArrayList<String>()
    private val nested = ArrayList<ValidationMessages>()
    private val indent = Indent(level)

    fun add(mess: String) {
        messages.add(mess)
    }

    fun nested(id: String): ValidationMessages {
        val mess = ValidationMessages(id, level + 1)
        nested.add(mess)
        return mess
    }

    override fun toString(): String {
        if (!hasErrors()) {
            return "$id all OK%n"
        }
        val builder = StringBuilder(2000)
        builder.append("$id hasProblems\n")
            for (mess in messages) {
                builder.append("$indent$mess\n")
            }
            for (nest in nested) {
                if (nest.hasErrors()) {
                    builder.append("$indent$nest\n")
                }
            }
        return builder.toString()
    }

    fun hasErrors(): Boolean {
        if (!messages.isEmpty()) {
            return true
        }
        if (nested.isEmpty()) {
            return false
        }
        return nested.map { it.hasErrors()}.reduce {a, b -> a or b}
    }
}

private const val nspaces : Int = 2
class Indent(level: Int) {
    private val indent = makeBlanks(level * nspaces)

    override fun toString(): String {
        return indent
    }

    private fun makeBlanks(len: Int) : String {
        val blanks = StringBuilder(len)
        for (i in 0 until len) {
            blanks.append(" ")
        }
        return blanks.toString()
    }
}
