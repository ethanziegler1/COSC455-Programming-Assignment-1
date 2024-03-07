/*
COURSE: COSC455101
Assignment: Program 1

Name: Ziegler, Ethan
*/
//  ************** REQUIRES JAVA 17 OR ABOVE! (https://adoptium.net/) ************** //
package compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remember this is part of a "fake" tokenizer, that when handed a string, it simply resolves to a
 * TOKEN object matching that string. All the Tokens/Terminals Used by the parser. The purpose of
 * the enum type here is to eliminate the need for direct character comparisons.
 * <p>
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! IMPORTANT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
 * -----------------------------------------------------------------------------<br>
 * IN *MOST* REAL CASES, THERE WILL BE ONLY ONE LEXEME PER compiler TokenSet!
 * <p>
 * The fact that several lexemes exist per token in this example is because this is to parse simple
 * In English sentences, most of the token types have many words (lexemes) that could fit.
 * *** This is generally NOT the case in most programming languages!!! ***
 */
public enum TokenSet {
    ARTICLE("a", "the"),
    CONJUNCTION("and", "or"),
    NOUN("dog", "cat", "rat", "house", "tree"),
    VERB("loves", "hates", "eats", "chases", "stalks"),
    ADJECTIVE("fast", "slow", "furry", "sneaky", "lazy", "tall"),
    ADJ_SEP(","),
    ADVERB("quickly", "secretly", "silently"),
    PREPOSITION("of", "on", "around", "with", "up"),
    PERIOD("."),
    READ("read"),
    LET("let"),
    WRITE("write"),
    VARIABLE("var"),
    EQUALS("="),
    ADD("+","-"),
    MULTIPLY("*","/"),
    RELATIONAL("<",">","=="),
    FACTOR("(,",")"),
    COMPARISON("if","then","else","endif"),
    REPEAT("until","repeat"),
    $$, // End of file

    // THESE ARE NOT USED IN THE GRAMMAR, BUT MIGHT BE USEFUL...  :)
    UNIDENTIFIED_TOKEN, // Would probably be an "ID" in a "real programming language" (HINT!!!)
    NUMBER; // A sequence of digits.

    /**
     * A list of all lexemes for each token.
     */
    private final List<String> lexemeList;

    TokenSet(final String... tokenStrings) {
        this.lexemeList = new ArrayList<>(tokenStrings.length);
        this.lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    /**
     * Get a TokenSet object from the Lexeme string.
     *
     * @param string The String (lexeme) to convert to a compiler.TokenSet
     * @return A compiler.TokenSet object based on the input String (lexeme)
     */
    static TokenSet getTokenFromLexeme(final String string) {
        // Just to be safe…
        final var lexeme = string.trim();

        // An empty string/lexeme should mean no more tokens to process.
        // Return the "end of input maker" if the string is empty.
        if (lexeme.isEmpty()) {
            return $$;
        }

        // Regex for one or more digits optionally followed by and more digits.
        // (doesn't handle "-", "+" etc., only digits)
        // Return the number token if the string represents a number.
        if (lexeme.matches(LexicalAnalyzer.NUMBER_REGEX)) {
            return NUMBER;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        // Return the matching token if found.
        for (var token : TokenSet.values()) {
            if (token.lexemeList.contains(lexeme)) {
                // early bailout from the loop.
                return token;
            }
        }

        // NOTE: UNIDENTIFIED_TOKEN could represent an ID, for example.
        // Return "UNIDENTIFIED_TOKEN" if no match was found.
        return UNIDENTIFIED_TOKEN;
    }
}