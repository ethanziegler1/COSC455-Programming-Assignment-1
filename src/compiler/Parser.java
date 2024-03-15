//  ************** REQUIRES JAVA 17 or later! (https://adoptium.net/) ************** //
package compiler;

import java.util.logging.Logger;

/*
 * GRAMMAR FOR PROCESSING SIMPLE SENTENCES:
 *
 * <START> ::= <SENTENCE> $$
 * <SENTENCE> ::= <NOUN_PHRASE> <VERB_PHRASE> <NOUN_PHRASE> <PREP_PHRASE> <SENTENCE_TAIL>
 * <SENTENCE_TAIL> ::= <CONJ> <SENTENCE> | <EOS>
 *
 * <NOUN_PHRASE> ::= <ART> <ADJ_LIST> <NOUN>
 * <ADJ_LIST> ::= <ADJECTIVE> <ADJ_TAIL> | ε
 * <ADJ_TAIL> ::= <COMMA> <ADJECTIVE> <ADJ_TAIL> | ε
 *
 * <VERB_PHRASE> ::= <ADVERB> <VERB> | <VERB>
 * <PREP_PHRASE> ::= <PREPOSITION> <NOUN_PHRASE> | ε
 *
 * ### Terminal Productions (Actual terminals omitted, but they are just the valid words in the language). ###
 *
 * <COMMA> ::= ','
 * <EOS> ::= '.'
 *
 * <ADJECTIVE> ::= ...any adjective...
 * <ADVERB> ::= ...any adverb...
 * <ART> ::= ...any article...
 * <CONJ> ::= ...any conjunction...
 * <NOUN> ::= ...any noun...
 * <PREPOSITION> ::= ...any preposition...
 * <VERB> ::= ...any verb....
 * 
 * 
 * 
 * 
 * <UNTIL_R> ::= <UNTIL> <id> <REPEAT>
 * <ITE> ::= <IF> <id> <THEN> <id> <ELSE> <id> <ENDIF>
 * 
 * <PROGRAM> ::= <STMT_LIST> $$
 * <STMT_LIST> ::= <STMT> <STMT_LIST> | ε
 * 
 * <STMT> ::= <READ_STMT> | <WRITE_STMT> |  <VAR_DECL> | <SUBR_CALL> | let id <ASGN_STMT>
 * 
 * <READ_STMT> ::= read id
 * <WRITE_STMT> ::= write expr 
 * 
 * <VAR_DECL> ::= <VARIABLE> id
 * <SUBR_CALL> ::= id (<ARG_LIST>)
 * 
 * <ASGN_STMT> ::= = <EXPR> | <B_ARROW> <SUBR_CALL>
 * 
 * <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
 * <ARGS_TAIL> ::= , <ARG_LIST> | ε
 * 
 * <EXPR> ::= <TERM> <TERM_TAIL>
 * 
 * <TERM> ::= <FACTOR> <FACTOR_TAIL>
 * <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
 * 
 * <FACTOR> ::= ( <EXPR> ) | id
 * <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
 * 
 * <CONDITION> ::= <EXPR> <REL_OPER> <EXPR>
 * 
 * <ADD_OP> ::= + | -
 * <MULT_OP> ::= * | /
 * 
 * <REL_OPER> ::= > | < | ==
 * 
 * 
 */

/**
 * This is the syntax analyzer for the compiler implemented as a recursive
 * descent parser.
 */
class Parser {

    // The lexer, which will provide the tokens
    private final LexicalAnalyzer lexer;

    // The "code generator"
    private final CodeGenerator codeGenerator;

    /**
     * This is the constructor for the Parser class which
     * accepts a LexicalAnalyzer, and a CodeGenerator object as parameters.
     *
     * @param lexer         The TokenSet Object
     * @param codeGenerator The CodeGenerator Object
     */
    Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;

        // Change this to automatically prompt to see the Open WebGraphViz dialog or not.
        MAIN.PROMPT_FOR_GRAPHVIZ = false;
    }

    /*
     * Since the "Compiler" portion of the code knows nothing about the start rule,
     * the "analyze" method must invoke the start rule.
     *
     * Begin analyzing...
     *
     * @param treeRoot The tree root.
     */
    public void analyze(TreeNode treeRoot) {
        try {
            // THIS IS OUR START RULE
            START(treeRoot);
        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }


/////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////

/* NOTES:

 1. Each method implements a production rule.

 2. Each production-implementing method starts by adding itself to the tree via the codeGenerator,
    which has three methods of interest:

    * addNonTerminalToTree(parentNode) --- Adds a non-terminal node to the tree.
    * codeGenerator.addTerminalToTree(parentNode) --- Adds a terminal node to the tree.
    * syntaxError(message, node) --- Throws a ParseException with the given message and adds exception to the tree.
    
    The code generator calls return a *new* TreeNode object representing the newly added node.

 3. The lexer implements:
       * getCurrentToken() --- Returns the current token.
       * getCurrentLexeme() --- Returns the string (the "lexeme") that maps to the current token.
       * advanceToken() --- Advances to the next token.
*/

/////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////

    // <START> :== <SENTENCE> $$
    private void START(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        // Invoke the first rule.
        SENTENCE(thisNode);

        // Test for the end of input ($$ meta token).
        if (lexer.getCurrentToken() != TokenSet.$$) {
            String currentLexeme = lexer.getCurrentLexeme();
            var errorMessage =
                    "SYNTAX ERROR: 'End of File' was expected but '%s' was found.".formatted(currentLexeme);
            codeGenerator.syntaxError(errorMessage, thisNode);
        }
    }

    // <SENTENCE> ::= <NOUN_PHRASE> <VERB_PHRASE> <NOUN_PHRASE> <PREP_PHRASE> <SENTENCE_TAIL>
    private void SENTENCE(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        NOUN_PHRASE(thisNode);
        VERB_PHRASE(thisNode);
        NOUN_PHRASE(thisNode);
        PREP_PHRASE(thisNode);
        VARIABLE(parentNode);
        SENTENCE_TAIL(thisNode);
    }

    // <SENTENCE_TAIL> ::= <CONJ> <SENTENCE> | <EOS>
    private void SENTENCE_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.CONJUNCTION) {
            MATCH(thisNode, TokenSet.CONJUNCTION);
            SENTENCE(thisNode);
        } else {
            MATCH(thisNode, TokenSet.PERIOD);
        }
    }

    // <NOUN_PHRASE> ::= <ART> <ADJ_LIST> <NOUN>
    private void NOUN_PHRASE(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    
        MATCH(thisNode, TokenSet.ARTICLE);
        ADJ_LIST(thisNode);
        MATCH(thisNode, TokenSet.NOUN);
    }

    // <ADJ_LIST> ::= <ADJECTIVE> <ADJ_TAIL> | ε
    private void ADJ_LIST(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.ADJECTIVE) {
            MATCH(thisNode, TokenSet.ADJECTIVE);
            ADJ_TAIL(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    // <ADJ_TAIL> ::= <COMMA> <ADJECTIVE> <ADJ_TAIL> | ε
    private void ADJ_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.ADJ_SEP) {
            MATCH(thisNode, TokenSet.ADJ_SEP);
            MATCH(thisNode, TokenSet.ADJECTIVE);
            ADJ_TAIL(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    // <VERB_PHRASE> ::= <ADVERB> <VERB> | <VERB>
    private void VERB_PHRASE(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.ADVERB) {
            MATCH(thisNode, TokenSet.ADVERB);
        }

        MATCH(thisNode, TokenSet.VERB);
    }

    // <PREP_PHRASE> ::= <PREPOSITION> <NOUN_PHRASE> | ε
    private void PREP_PHRASE(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.PREPOSITION) {
            MATCH(thisNode, TokenSet.PREPOSITION);
            NOUN_PHRASE(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }
    private void RELATIONAL(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.RELATIONAL) {
            MATCH(thisNode, TokenSet.RELATIONAL);
        }
    }

    private void READ(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.READ) {
            MATCH(thisNode, TokenSet.READ);
        }
    }
    private void ADD(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.ADD) {
            MATCH(thisNode, TokenSet.ADD);
        }else {
            EMPTY(thisNode);
        }
    }
    private void MULTIPLY(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.MULTIPLY) {
            MATCH(thisNode, TokenSet.MULTIPLY);
        }else {
            EMPTY(thisNode);
        }
    }
    private void LET(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.READ) {
            MATCH(thisNode, TokenSet.READ);
        }
        
    }
    private void VARIABLE(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.VARIABLE) {
            MATCH(thisNode, TokenSet.VARIABLE);
            MATCH(thisNode, TokenSet.UNIDENTIFIED_TOKEN);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add an EMPTY terminal node (result of an Epsilon Production) to the parse tree.
     * Mainly, this is just done for better visualizing the complete parse tree.
     *
     * @param parentNode The parent of the terminal node.
     */
    private void EMPTY(final TreeNode parentNode) {
        codeGenerator.addEmptyToTree(parentNode);
    }

    /**
     * Match the current token with the expected token.
     * If they match, add the token to the parse tree, otherwise throw an exception.
     *
     * @param currentNode     The current terminal node.
     * @param expectedToken   The token to be matched.
     * @throws ParseException Thrown if the token does not match the expected token.
     */
    private void MATCH(final TreeNode currentNode, final TokenSet expectedToken) throws ParseException {
        final var currentToken = lexer.getCurrentToken();
        final var currentLexeme = lexer.getCurrentLexeme();

        if (currentToken == expectedToken) {
            codeGenerator.addTerminalToTree(currentNode, currentToken, currentLexeme);
            lexer.advanceToken();
        } else {
            final var errorMessage = "SYNTAX ERROR: '%s' was expected\nbut '%s' was found (%s)."
                    .formatted(expectedToken, currentLexeme, currentToken);

            codeGenerator.syntaxError(errorMessage, currentNode);
        }
    }



/* OUR ADDED METHODS - N(oah)


 * <UNTIL_R> ::= <UNTIL> <STMT> <REPEAT>
 * <ITE> ::= <IF> <CONDITION> <THEN> <STMT> <ELSE> <STMT> <ENDIF>
 * 
 * <PROGRAM> ::= <STMT_LIST> $$
 * <STMT_LIST> ::= <STMT> <STMT_LIST> | ε
 * 
 * <STMT> ::= <READ_STMT> | <WRITE_STMT> |  <VAR_DECL> | <SUBR_CALL> | let id <ASGN_STMT>
 * 
 * <READ_STMT> ::= read id
 * <WRITE_STMT> ::= write expr 
 * 
 * <VAR_DECL> ::= <VARIABLE> id
 * <SUBR_CALL> ::= id (<ARG_LIST>)
 * 
 * <ASGN_STMT> ::= = <EXPR> | <B_ARROW> <SUBR_CALL>
 * 
 * <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
 * <ARGS_TAIL> ::= , <ARG_LIST> | ε
 * 
 * <EXPR> ::= <TERM> <TERM_TAIL>
 * 
 * <TERM> ::= <FACTOR> <FACTOR_TAIL>
 * <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
 * 
 * <FACTOR> ::= ( <EXPR> ) | id
 * <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
 * 
 * <CONDITION> ::= <EXPR> <REL_OPER> <EXPR>
 * 
 * <ADD_OP> ::= + | -
 * <MULT_OP> ::= * | /
 * 
 * <REL_OPER> ::= > | < | ==
 * 
 * START OF METHODS FROM PSEUDOCODE (MAY NEED TO BE UPDATED TO BETTER REFLECT THE PSEUDOCODE
 */    
//  <STMT> ::= <READ_STMT> | <WRITE_STMT> |  <VAR_DECL> | <SUBR_CALL> | let id <ASGN_STMT>
 private void STMT(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    if (lexer.getCurrentToken() == TokenSet.LET) {
        MATCH(thisNode, TokenSet.LET);
        MATCH(thisNode, TokenSet.ID);
        MATCH(thisNode, TokenSet.ASSGN_STMT);
    }

    MATCH(thisNode, TokenSet.READ_STMT);
    // MATCH(thisNode, TokenSet.WRITE_STMT);
    //this may be the better implementation - N 
    WRITE_STMT(thisNode);
    READ_STMT(thisNode);
    MATCH(thisNode, TokenSet.SUBR_CALL);
}
// <WRITE_STMT> ::= write expr 
//uses the write from the tokenset but creates a rule from our new grammar - N
private void WRITE_STMT (final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree (parentNode) ;
    MATCH (thisNode, TokenSet.WRITE);
    EXPR (thisNode);
}
// <READ_STMT> ::= read id
private void READ_STMT(final TreeNode parentNode ) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    MATCH(thisNode, TokenSet.READ);
    // ID(thisNode);
    MATCH(thisNode, TokenSet.ID);
}
// <SUBR_CALL> ::= id (<ARG_LIST>)
private void SUBR_CALL (final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree (parentNode) ;
        // ID(thisNode);
        if (lexer.getCurrentToken() == TokenSet.ID) {
            MATCH(thisNode, TokenSet.ID);
            MATCH(thisNode, TokenSet.OPEN_P);
            ARG_LIST(thisNode);
            MATCH(thisNode, TokenSet.CLOSE_P);
        }
}
// <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
private void ARG_LIST(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

/*    if (lexer.getCurrentToken() == TokenSet.EXPR) {
        MATCH(thisNode, TokenSet.ARGS_TAIL);
        STMT_LIST(thisNode);
    } else {
        EMPTY(thisNode);
    } */

    EXPR(thisNode);
    ARGS_TAIL(thisNode);
}
//<ARGS_TAIL> ::= , <ARG_LIST> | ε
private void ARGS_TAIL(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    if (lexer.getCurrentToken() == TokenSet.ADJ_SEP) {
        MATCH(thisNode, TokenSet.ADJ_SEP);
        ARG_LIST(thisNode);
    } else {
        EMPTY(thisNode);
    }
}
//REVIEW
 //   <STMT_LIST> ::= <STMT> <STMT_LIST> | ε
 private void STMT_LIST(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    // if (lexer.getCurrentToken() == TokenSet.STMT) {
    //     MATCH(thisNode, TokenSet.STMT);
    //     STMT_LIST(thisNode);
    // } else {
    //     EMPTY(thisNode);
    // }
        STMT(thisNode);
        STMT_LIST(thisNode);
        EMPTY(thisNode);
}
//REVIEW
// <EXPR> ::= <TERM> <TERM_TAIL>
private void EXPR(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    if(lexer.getCurrentToken()== TokenSet.TERM){
        MATCH(thisNode, TokenSet.TERM);
        MATCH(thisNode, TokenSet.TERM_TAIL);
    }
}
//should be good but double check-N
// <TERM> ::= <FACTOR> <FACTOR_TAIL>
private void TERM(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    
    if(lexer.getCurrentToken()== TokenSet.FACTOR){
        MATCH(thisNode, TokenSet.FACTOR);
        FACTOR_TAIL(thisNode);
    }
}
//TERM PROBABLY NEEDS AN UPDATE - N 
// <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
private void TERM_TAIL(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    if (lexer.getCurrentToken() == TokenSet.ADD) {
        MATCH(thisNode, TokenSet.ADD);
        TERM(thisNode);
        TERM_TAIL(thisNode);
    } else {
        EMPTY(thisNode);
    }
}
//I created a few new Tokens in the token set that may seem redundant so feel free to edit things - N
// <FACTOR> ::= ( <EXPR> ) | id
private void FACTOR(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if(lexer.getCurrentToken() == TokenSet.OPEN_P){
        MATCH(thisNode, TokenSet.OPEN_P);
        MATCH(thisNode, TokenSet.EXPR);
        MATCH(thisNode, TokenSet.CLOSE_P);
    }
    MATCH(thisNode, TokenSet.ID);
}
// <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
private void FACTOR_TAIL(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if (lexer.getCurrentToken() == TokenSet.MULTIPLY) {
        MATCH(thisNode, TokenSet.MULTIPLY);
        MATCH(thisNode, TokenSet.FACTOR);
        FACTOR_TAIL(thisNode);
        } else {
        EMPTY(thisNode);
    }
}

//end of methods from pseudo-code

/* 
PROF has some methods for these already Up a little bit - N
//<ADD_OP> ::= + | -
private void ADD_OP(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
}
//<MULT_OP> ::= * | /
private void MULT_OP(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
}*/

}
