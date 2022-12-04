grammar Crater;

@header {
package org.craterlang.language;
}

program:
    body=block EOF #BlockProgram
  | body=expression EOF #ExpressionProgram;

block:
    (statements+=statement)* ret=returnStatement?;

returnStatement:
    'return' (values+=expression (',' values+=expression)*)? ';'?;

statement:
    ';' #EmptyStatement
  | lhs+=var (',' lhs+=var)* '=' rhs+=expression (',' rhs+=expression)* #AssignmentStatement
  | receiver=prefixExpression (':' method=Name)? arguments=args #FunctionCallStatement
  | '::' name=Name '::' #LabelStatement
  | 'break' #BreakStatement
  | 'goto' target=Name #GotoStatement
  | 'do' body=block 'end' #BlockStatement
  | 'while' condition=expression 'do' body=block 'end' #WhileStatement
  | 'repeat' body=block 'until' condition=expression #RepeatStatement
  | 'if' conditions+=expression 'then' consequents+=block ('elseif' conditions+=expression 'then' consequents+=block)* ('else' alternate=block)? 'end' #IfStatement
  | 'for' name=Name '=' start=expression ',' stop=expression (',' step=expression)? 'do' body=block 'end' #ForEqualsStatement
  | 'for' names+=Name (',' names+=Name)* 'in' values+=expression (',' values+=expression)* 'do' body=block 'end' #ForInStatement
  | 'function' path+=Name ('.' path+=Name)* (':' method=Name)? body=functionBody #FunctionDeclarationStatement
  | 'local' 'function' name=Name body=functionBody #LocalFunctionDeclarationStatement
  | 'local' names+=attributedName (',' names+=attributedName)* ('=' values+=expression (',' values+=expression)*)? #LocalDeclarationStatement;

attributedName:
    name=Name ('<' attribute=Name '>')?;

var:
    name=Name #NamedVar
  | receiver=prefixExpression '[' key=expression ']' #IndexedVar
  | receiver=prefixExpression '.' member=Name #MemberVar;

expression:
    token=('nil' | 'false' | 'true' | DecInteger | HexInteger | DecFloat | HexFloat | ShortString | LongString | '...') #LiteralExpression
  | 'function' body=functionBody #FunctionExpression
  | child=prefixExpression #PrefixExpressionExpression
  | child=tableConstructor #TableExpression
  | <assoc=right> lhs=expression '^' rhs=expression #PowerExpression
  | op=('not' | '#' | '-' | '~') operand=expression #PrefixOpExpression
  | lhs=expression op=('*' | '/' | '//' | '%') rhs=expression #MulDivRemExpression
  | lhs=expression op=('+' | '-') rhs=expression #AddSubExpression
  | <assoc=right> lhs=expression '..' rhs=expression #ConcatExpression
  | lhs=expression op=('<<' | '>>') rhs=expression #BitShiftExpression
  | lhs=expression '&' rhs=expression #BitwiseAndExpression
  | lhs=expression '~' rhs=expression #BitwiseXOrExpression
  | lhs=expression '|' rhs=expression #BitwiseOrExpression
  | lhs=expression op=('<' | '>' | '<=' | '>=' | '~=' | '==') rhs=expression #ComparisonExpression
  | lhs=expression 'and' rhs=expression #AndExpression
  | lhs=expression 'or' rhs=expression #OrExpression;

prefixExpression:
    Name #NameExpression
  | receiver=prefixExpression '[' key=expression ']' #IndexExpression
  | receiver=prefixExpression '.' member=Name #MemberExpression
  | receiver=prefixExpression (':' method=Name)? arguments=args #CallExpression
  | '(' child=expression ')' #ParenthesizedExpression;

args:
    '(' (values+=expression (',' values+=expression)*)? ')' #ValueArgs
  | child=tableConstructor #TableArgs
  | token=(ShortString | LongString) #StringArgs;

functionBody:
    '(' (formals+=Name (',' formals+=Name)* (',' varargs='...')? | varargs='...')? ')' body=block 'end';

tableConstructor:
    '{' (fields+=field ((',' | ';') fields+=field)* (',' | ';')?)? '}';

field:
    '[' key=expression ']' '=' value=expression #IndexedField
  | name=Name '=' value=expression #NamedField
  | value=expression #OrdinalField;

Ampersand: '&';
Caret: '^';
Colon2: '::';
Colon: ':';
Comma: ',';
Dot2: '..';
Dot3: '...';
Dot: '.';
Equals2: '==';
Equals: '=';
Hash: '#';
LAngle2: '<<';
LAngle: '<';
LAngleEquals: '<=';
LCurly: '{';
LParen: '(';
LSquare: '[';
Minus: '-';
Percent: '%';
Pipe: '|';
Plus: '+';
RAngle2: '>>';
RAngle: '>';
RAngleEquals: '>=';
RCurly: '}';
RParen: ')';
RSquare: ']';
Semicolon: ';';
Slash2: '//';
Slash: '/';
Star: '*';
Tilde: '~';
TildeEquals: '~=';

KwAnd: 'and';
KwBreak: 'break';
KwDo: 'do';
KwElse: 'else';
KwElseif: 'elseif';
KwEnd: 'end';
KwFalse: 'false';
KwFor: 'for';
KwFunction: 'function';
KwGoto: 'goto';
KwIf: 'if';
KwIn: 'in';
KwLocal: 'local';
KwNil: 'nil';
KwNot: 'not';
KwOr: 'or';
KwRepeat: 'repeat';
KwReturn: 'return';
KwThen: 'then';
KwTrue: 'true';
KwUntil: 'until';
KwWhile: 'while';

HexInteger:
    '0' [xX] HexDigit+;

DecInteger:
    DecDigit+;

DecFloat:
    (DecDigit+ ('.' DecDigit*)? | '.' DecDigit+) ([eE] [+\-]? DecDigit+)?;

HexFloat:
    '0' [xX] (HexDigit+ ('.' HexDigit*)? | '.' HexDigit+) ([pP] [+\-]? DecDigit+)?;

LongComment:
    '--' LongBlock -> skip;

LineComment:
    '--' ~[\r\n]* -> skip;

LongString:
    LongBlock;

fragment LongBlock:
    '[' LongBlockBody ']';

fragment LongBlockBody:
    '=' LongBlockBody '='
  | '[' .*? ']';

ShortString:
    '"' (ShortStringToken | '\'')* '"'
  | '\'' (ShortStringToken | '"')* '\'' ;

fragment ShortStringToken:
    '\\' ShortStringEscape
  | ~[\\'"];

fragment ShortStringEscape:
    'a'
  | 'b'
  | 'f'
  | 'n'
  | 'r'
  | 't'
  | 'v'
  | '\\'
  | '"'
  | '\''
  | '\r\n'
  | '\r'
  | '\n'
  | 'z' Whitespace*
  | 'x' HexDigit HexDigit
  | 'd' DecDigit (DecDigit DecDigit?)?
  | 'u{' HexDigit+ '}';

fragment DecDigit:
    [0-9];

fragment HexDigit:
    [0-9a-fA-F];

Name:
    NameStart NameRest*;

fragment NameStart:
    [\p{Ll}\p{Lm}\p{Lo}\p{Lt}\p{Lu}\p{Pc}];

fragment NameRest:
    NameStart | [\p{Mc}\p{Me}\p{Mn}\p{Nd}\p{Nl}\p{No}];

Whitespace:
    [ \f\n\r\t\u{000b}] -> skip;
