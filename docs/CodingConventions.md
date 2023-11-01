# Coding Conventions

last changed 10/30/2023

## Error Handling

* throw Exception  - only when code must have an error in it.
* fun operationSafe() - caller promises this is safe (throws no Exceptions), and its ok to throw exception (code has error).
* return null - when contract cant be fulfilled
* return Result - when theres enough context to make a useful error message
* return Result<obj, ErrorMessages> - when you have a complex object with many possible error messages

Low level methods may need to throw Exceptions without implying theres a code error. These must always be caught and
translated into one of the above.

## Proto Conversions

fun \[GroupContext\].importT( proto: proto.T? or proto.T) : T? or Result<T, String>

fun T.publish() : proto.T

no Exceptions should throw
do not use object!! without checking for object == null