grammar Crater;

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

ShortLiteralString:
    '"' (ShortStringToken | '\'')* '"'
  | '\'' (ShortStringToken | '"')* '\''
;

fragment ShortStringToken:
    '\\' ShortStringEscape
  | [^\\'"]
;

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
  | '\r'? '\n'
  | 'z' Whitespace*
  | 'x' HexDigit HexDigit
  | 'd' DecDigit (DecDigit DecDigit?)?;


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
