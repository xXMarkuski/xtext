/*
* generated by Xtext
*/
grammar DebugInternalCommentAssociationTestLanguage ;

// Rule Model
ruleModel :
	ruleElement*
;

// Rule Element
ruleElement :
	'element' RULE_ID (
		'parent' RULE_ID
	)?
;

RULE_ID :
	'^'? (
		'a' .. 'z' |
		'A' .. 'Z' |
		'_'
	) (
		'a' .. 'z' |
		'A' .. 'Z' |
		'_' |
		'0' .. '9'
	)*
;

RULE_INT :
	'0' .. '9'+
;

RULE_STRING :
	'"' (
		'\\' (
			'b' |
			't' |
			'n' |
			'f' |
			'r' |
			'u' |
			'"' |
			'\'' |
			'\\'
		) |
		~ (
			'\\' |
			'"'
		)
	)* '"' |
	'\'' (
		'\\' (
			'b' |
			't' |
			'n' |
			'f' |
			'r' |
			'u' |
			'"' |
			'\'' |
			'\\'
		) |
		~ (
			'\\' |
			'\''
		)
	)* '\''
;

RULE_ML_COMMENT :
	'/*' (
		options { greedy = false ; } : .
	)* '*/' {skip();}
;

RULE_SL_COMMENT :
	'//' ~ (
		'\n' |
		'\r'
	)* (
		'\r'? '\n'
	)? {skip();}
;

RULE_WS :
	(
		' ' |
		'\t' |
		'\r' |
		'\n'
	)+ {skip();}
;

RULE_ANY_OTHER :
	.
;