package kz.iitu.springlab.lab11;

import java.util.Arrays;
import java.util.List;

/** Task C. The Authorization Code flow with PKCE, in the correct order. */
public enum OAuthFlow {

    CREATE_VERIFIER(3, "The client creates a code_verifier and derives a code_challenge."),
    REDIRECT_TO_AUTHORIZE(5, "The client redirects the user to the authorization endpoint with state, redirect URI, scopes, and code_challenge."),
    USER_AUTHENTICATES(2, "The user authenticates and grants the requested consent."),
    REDIRECT_WITH_CODE(6, "The authorization server redirects to the registered redirect URI with an authorization code."),
    EXCHANGE_CODE(1, "The client exchanges the authorization code and its PKCE code_verifier at the token endpoint."),
    TOKENS_RETURNED(4, "The authorization server returns tokens to the client.");

    /** The number of the statement as printed in the handout. */
    public final int printedAs;
    public final String text;

    OAuthFlow(int printedAs, String text) {
        this.printedAs = printedAs;
        this.text = text;
    }

    /** The answer to write beside statements 1..6 as printed: their position in the flow. */
    public static List<Integer> answerForPrintedStatements() {
        Integer[] answer = new Integer[6];
        for (OAuthFlow step : values()) {
            answer[step.printedAs - 1] = step.ordinal() + 1;
        }
        return Arrays.asList(answer);
    }
}
