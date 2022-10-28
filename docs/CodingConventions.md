# Coding Conventions

draft 10/28/2022

## Proto Conversions

fun \[GroupContext\].importT( proto: proto.T? or proto.T) : T? or Result<T, String>

fun T.publish() : proto.T

no Exceptions should throw
do not use object!! without checking for object == null