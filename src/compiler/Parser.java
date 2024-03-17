//  ************** REQUIRES JAVA 17 or later! (https://adoptium.net/) ************** //
package compiler;

import java.awt.font.TextHitInfo;
import java.sql.Array;
import java.util.ArrayList;
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

    private ArrayList symbolTable= new ArrayList();// The "code generator"
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
        MAIN.PROMPT_FOR_GRAPHVIZ = true;
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
            PROGRAM(treeRoot);
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
// PROGRAM ::= <STMT_LIST> <$$>
private void PROGRAM(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    STMT_LIST(thisNode);
    MATCH(thisNode, TokenSet.$$);
}

    // STMT_LIST ::= <STMT> <STMT_LIST> | <$$> | <epsilon>
    private void STMT_LIST(final TreeNode fromNode) throws ParseException {
        final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);

        switch (lexer.getCurrentToken()) {
            // First set of <stmt>
            case READ, WRITE, VAR_DECL, UNIDENTIFIED_TOKEN, LET, IF, UNTIL: {
                STMT(treeNode);
                STMT_LIST(treeNode);
            }
            default: EMPTY(treeNode);
        }
    }

    //  <STMT> ::= <READ_STMT> | <WRITE_STMT> |  <VAR_DECL> | <SUBR_CALL> | let id <ASGN_STMT>
private void STMT(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    switch (lexer.getCurrentToken()) {
        // First set of <stmt>
        case READ: {
            READ_STMT(thisNode);
            break;
        }
        case WRITE_STMT:{
            WRITE_STMT(thisNode);
            break;
        }
        case UNTIL:{
            UNTIL_STMT(thisNode);
            break;
        }
        case VAR_DECL:{
            MATCH(thisNode,TokenSet.VAR_DECL);
            MATCH(thisNode,TokenSet.UNIDENTIFIED_TOKEN);
            break;
        }
        case SUBR_CALL:{
            SUBR_CALL(thisNode);
            break;
        }
        case LET:{
            MATCH(thisNode,TokenSet.LET);
            MATCH(thisNode,TokenSet.UNIDENTIFIED_TOKEN);
            ASGN_STMT(thisNode);
            break;
        }
        case IF:{
            MATCH(thisNode,TokenSet.IF);
            EXPR(thisNode);
            MATCH(thisNode,TokenSet.THEN);
            EXPR(thisNode);
            break;
        }
        default: EMPTY(thisNode);
    }
}

//<UNTIL_STMT> ::= <until> <condition> <stmt_list> <repeat>
    private void UNTIL_STMT(final TreeNode parentNode) throws ParseException{
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.UNTIL);
        EXPR(thisNode);
        STMT_LIST(thisNode);
        MATCH(thisNode, TokenSet.REPEAT);
    }
// <WRITE_STMT> ::= write expr
//uses the write from the tokenset but creates a rule from our new grammar - N
private void WRITE_STMT (final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree (parentNode) ;
    MATCH (thisNode, TokenSet.WRITE);
    MATCH(thisNode, TokenSet.UNIDENTIFIED_TOKEN);
}
// <READ_STMT> ::= read id
private void READ_STMT(final TreeNode parentNode ) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    MATCH(thisNode, TokenSet.READ);
    MATCH(thisNode, TokenSet.UNIDENTIFIED_TOKEN);
}

    private void VARIABLE(final TreeNode parentNode) throws ParseException{
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        String currentLexeme = lexer.getCurrentLexeme();
        if (symbolTable.contains(currentLexeme)){
            codeGenerator.syntaxError("Already Declared this Variable", parentNode);
        }
        else{
            symbolTable.add(currentLexeme);
        }
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
//<ASGN_STMT> ::= = <EXPR> | <B_ARROW> <SUBR_CALL>
private void ASGN_STMT(final TreeNode fromNode) throws ParseException {
    final var treeNode = codeGenerator.addNonTerminalToTree(fromNode);
    if (lexer.getCurrentToken() == TokenSet.ASSIGN) {
        MATCH(treeNode, TokenSet.ASSIGN);
        EXPR(treeNode);
    } else if (lexer.getCurrentToken() == TokenSet.EQUALS) {
        MATCH(treeNode, TokenSet.EQUALS);
        EXPR(treeNode);
    }
    else if (lexer.getCurrentToken() == TokenSet.SUBR_CALL) {
        MATCH(treeNode, TokenSet.SUBR_CALL);
        SUBR_CALL(treeNode);
    } else {
        codeGenerator.syntaxError("Illegal assignment statement!", fromNode);
    }
}

// <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
private void ARG_LIST(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

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
// <EXPR> ::= <TERM> <TERM_TAIL>
private void EXPR(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

    if(lexer.getCurrentToken() == TokenSet.UNIDENTIFIED_TOKEN
            || lexer.getCurrentToken() == TokenSet.NUMBER
            || lexer.getCurrentToken() == TokenSet.OPEN_P) {
        TERM(thisNode);
        TERM_TAIl(thisNode);
    } else {
        ParseException e;
    }
}

    //<TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
    private void TERM_TAIl(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
        if (lexer.getCurrentToken() == TokenSet.ADD) {
            ADD_OP(thisNode);
            TERM(thisNode);
            TERM_TAIl(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.RELATIONAL) {
            MATCH(thisNode, TokenSet.RELATIONAL);
            TERM(thisNode);
            TERM_TAIl(thisNode);
        }
        else if(lexer.getCurrentToken() == TokenSet.UNIDENTIFIED_TOKEN
                || lexer.getCurrentToken() == TokenSet.READ
                || lexer.getCurrentToken() == TokenSet.WRITE
                || lexer.getCurrentToken() == TokenSet.$$){
            EMPTY(thisNode);
        }
        else {
            ParseException e;
        }
    }

// <TERM> ::= <FACTOR> <FACTOR_TAIL>
private void TERM(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    
    if(lexer.getCurrentToken()== TokenSet.UNIDENTIFIED_TOKEN
            || lexer.getCurrentToken()== TokenSet.NUMBER
            || lexer.getCurrentToken() == TokenSet.OPEN_P) {
        FACTOR(thisNode);
        FACTOR_TAIL(thisNode);
    }else {
        ParseException e;
    }
}

// <FACTOR> ::= ( <EXPR> ) | id
private void FACTOR(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if (lexer.getCurrentToken()== TokenSet.NUMBER) {
        MATCH(thisNode,TokenSet.NUMBER);
    } else if(lexer.getCurrentToken() == TokenSet.OPEN_P) {
        MATCH(thisNode, TokenSet.OPEN_P);
        EXPR(thisNode);
        MATCH(thisNode, TokenSet.CLOSE_P);
    } else if(lexer.getCurrentToken()== TokenSet.UNIDENTIFIED_TOKEN){
        MATCH(thisNode,TokenSet.UNIDENTIFIED_TOKEN);
    } else{
        ParseException e;
    }
}
// <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
private void FACTOR_TAIL(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if (lexer.getCurrentToken() == TokenSet.MULTIPLY) {
        MULT_OP(thisNode);
        FACTOR(thisNode);
        FACTOR_TAIL(thisNode);
        } else {
        EMPTY(thisNode);
    }
}


//PROF has some methods for these already Up a little bit - N
//<ADD_OP> ::= + | -
private void ADD_OP(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if (lexer.getCurrentToken() == TokenSet.ADD) {
        MATCH(thisNode, TokenSet.ADD);
    } else {
        ParseException e;
    }
}
//<MULT_OP> ::= * | /
private void MULT_OP(final TreeNode parentNode) throws ParseException {
    final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);
    if (lexer.getCurrentToken() == TokenSet.MULTIPLY) {
        MATCH(thisNode, TokenSet.MULTIPLY);
    } else {
        ParseException e;
    }
}


}
