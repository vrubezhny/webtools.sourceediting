/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*nlsXXX*/
package org.eclipse.wst.css.core.internal.contenttype;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.wst.xml.core.internal.contenttype.EncodingParserConstants;
import org.eclipse.wst.xml.core.internal.contenttype.XMLHeadTokenizerConstants;



%%

%{


	private boolean hasMore = true;
	private final static int MAX_TO_SCAN = 8000;
	StringBuffer string = new StringBuffer();
	// state stack for easier state handling
	private IntStack fStateStack = new IntStack();
	private String valueText = null;



	public CSSHeadTokenizer() {
		super();
	}

	  public void reset (Reader in) {
	  	/* the input device */
	  	yy_reader = in;

  		/* the current state of the DFA */
  		yy_state = 0;

  		/* the current lexical state */
  		yy_lexical_state = YYINITIAL;

  		/* this buffer contains the current text to be matched and is
  		 the source of the yytext() string */
  		java.util.Arrays.fill(yy_buffer, (char)0);

  		/* the textposition at the last accepting state */
  		yy_markedPos = 0;

  		/* the textposition at the last state to be included in yytext */
  		yy_pushbackPos = 0;

  		/* the current text position in the buffer */
  		yy_currentPos = 0;

  		/* startRead marks the beginning of the yytext() string in the buffer */
  		yy_startRead = 0;

  		/** 
  		 * endRead marks the last character in the buffer, that has been read
  		 * from input 
  		 */
  		yy_endRead = 0;

  		/* number of newlines encountered up to the start of the matched text */
  		yyline = 0;

  		/* the number of characters up to the start of the matched text */
  		yychar = 0;

  		/**
  		 * the number of characters from the last newline up to the start
  		 * of the matched text
  		 */
  		yycolumn = 0; 

  		/** 
  		 * yy_atBOL == true <=> the scanner is currently at the beginning 
  		 * of a line
  		 */
  		yy_atBOL = false;

  		/* yy_atEOF == true <=> the scanner has returned a value for EOF */
  		yy_atEOF = false;

  		/* denotes if the user-EOF-code has already been executed */
  		yy_eof_done = false;


  		fStateStack.clear();
  		
  		hasMore = true;
  		
		// its a little wasteful to "throw away" first char array generated
		// by class init (via auto generated code), but we really do want
		// a small buffer for our head parsers.
		if (yy_buffer.length != MAX_TO_SCAN) {
			yy_buffer = new char[MAX_TO_SCAN];
		}
  		

  	}


	public final HeadParserToken getNextToken() throws IOException {
		String context = null;
		context = primGetNextToken();
		HeadParserToken result = null;
		if (valueText != null) {
			result = createToken(context, yychar, valueText);
			valueText = null;
		} else {
			result = createToken(context, yychar, yytext());
		}
		return result;
	}

	public final boolean hasMoreTokens() {
		return hasMore && yychar < MAX_TO_SCAN;
	}
	private void pushCurrentState() {
		fStateStack.push(yystate());

	}

	private void popState() {
		yybegin(fStateStack.pop());
	}
	private HeadParserToken createToken(String context, int start, String text) {
		return new HeadParserToken(context, start, text);
	}
	

%}

%eof{
	hasMore=false;
%eof}

%public
%class CSSHeadTokenizer
%function primGetNextToken
%type String
%char
%unicode
%ignorecase 
%debug
%switch


UTF16BE = \xFE\xFF
UTF16LE = \xFF\xFE
UTF83ByteBOM = \xEF\xBB\xBF

//SpaceChar = [\x20\x09]



// [3] S ::= (0x20 | 0x9 | 0xD | 0xA)+
S = [\x20\x09\x0D\x0A]

//BeginAttribeValue = {S}* \= {S}*

LineTerminator = \r|\n

// Z is the single-byte zero character to be used in parsing double-byte files
Z = (\x00)?
S_UTF = {Z}{S}{Z}
BeginAttributeValueUTF = {S_UTF}* \= {S_UTF}*

%state ST_XMLDecl
%state CHARSET_RULE
%state QuotedAttributeValue
%state DQ_STRING
%state SQ_STRING
%state UnDelimitedString

%%


<YYINITIAL>  
{
	{UTF16BE}   		{hasMore = false; return EncodingParserConstants.UTF16BE;}
	{UTF16LE}   		{hasMore = false; return EncodingParserConstants.UTF16LE;}
	{UTF83ByteBOM}   	{hasMore = false; return EncodingParserConstants.UTF83ByteBOM;}

	// force to be started on first line, but we do allow preceeding spaces
	^ {Z}({S}{Z})* ({Z}<{Z}\?{Z}x{Z}m{Z}l{Z}){S_UTF}+ {if (yychar == 0 ) {yybegin(ST_XMLDecl); return XMLHeadTokenizerConstants.XMLDeclStart;}}
	
	^ {Z}({S}{Z})*({Z}@{Z}c{Z}h{Z}a{Z}r{Z}s{Z}e{Z}t{Z})   {if (yychar == 0 )  {yybegin(CHARSET_RULE); return CSSHeadTokenizerConstants.CHARSET_RULE;}}
}	

// I don't think there's really an XML form of CSS files ... but will leave here for consistency	
<ST_XMLDecl> 
{
	//"version" {BeginAttribeValue} {pushCurrentState(); yybegin(QuotedAttributeValue); return XMLHeadTokenizerConstants.XMLDeclVersion;}
	{Z}e{Z}n{Z}c{Z}o{Z}d{Z}i{Z}n{Z}g{Z} {BeginAttributeValueUTF} {pushCurrentState(); yybegin(QuotedAttributeValue); return XMLHeadTokenizerConstants.XMLDelEncoding;}
	// note this "forced end" once end of XML Declaration found
	{Z}\?{Z}>{Z}    {yybegin(YYINITIAL);  return XMLHeadTokenizerConstants.XMLDeclEnd;}
}	

<CHARSET_RULE> 
{

	{S_UTF}*  {pushCurrentState(); yybegin(QuotedAttributeValue);}
	{Z};{Z}    { yybegin(YYINITIAL);  hasMore = false; return CSSHeadTokenizerConstants.RuleEnd;}
}
	

<QuotedAttributeValue>
{
	{Z}\"{Z}                     { yybegin(DQ_STRING); string.setLength(0); }
	{Z}\'{Z}			{ yybegin(SQ_STRING); string.setLength(0); }
	// in this state, anything other than a space character can start an undelimited string
	{S_UTF}*.           { yypushback(yylength()); yybegin(UnDelimitedString); string.setLength(0);}

}	


<DQ_STRING>
{

	{Z}\"{Z}                      { popState(); valueText = string.toString(); return EncodingParserConstants.StringValue; }
  	{Z}{LineTerminator}{Z}        { yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}\?{Z}>{Z}			{ yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}<{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	
	{Z}>{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}\/{Z}>{Z}			{ yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z};{Z}                      { yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue; }
	[^\x00]			{ string.append( yytext() ); }


}

<SQ_STRING>
{

	{Z}\'{Z}                      { popState(); valueText = string.toString(); return EncodingParserConstants.StringValue;}
  	{Z}{LineTerminator}{Z}        { yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}%{Z}>{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}<{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}>{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}\/{Z}>{Z}			{ yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z};{Z}                      { yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue; }
	[^\x00]			{ string.append( yytext() ); }

}

<UnDelimitedString>
{


	{S_UTF}                     { yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.UnDelimitedStringValue; }
  	{Z}{LineTerminator}{Z}        { yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}\?{Z}>{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}<{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	// these are a bit special, since we started an undelimit string, but found a quote ... probably indicates a missing beginning quote
	{Z}\'{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTermintatedUnDelimitedStringValue;}
	{Z}\"{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTermintatedUnDelimitedStringValue;}
	
	{Z}>{Z}			{ yypushback(yylength());popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z}\/{Z}>{Z}			{ yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.InvalidTerminatedStringValue;}
	{Z};{Z}                      { yypushback(yylength()); popState(); valueText = string.toString(); return EncodingParserConstants.UnDelimitedStringValue; }
	[^\x00]			{ string.append( yytext() ); }
}

// The "match anything" rule should always be in effect except for when looking for end of string
// (That is, remember to update state list if/when new states added)
.|\n	{if(yychar > MAX_TO_SCAN) {hasMore=false; return EncodingParserConstants.MAX_CHARS_REACHED;}}

// this rule always in effect
<<EOF>>         {hasMore = false; return EncodingParserConstants.EOF;}
