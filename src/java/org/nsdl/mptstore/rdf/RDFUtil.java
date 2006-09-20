package org.nsdl.mptstore.rdf;

import java.io.IOException;
import java.io.StringReader;

import java.net.URISyntaxException;

import java.text.ParseException;

public abstract class RDFUtil {

    private static final String _EXPECTED_ABS_URI = "Expected absolute URI";
    private static final String _EXPECTED_ACE = "Expected '@', '^', or EOF";
    private static final String _EXPECTED_C = "Expected '^'";
    private static final String _EXPECTED_G = "Expected '>'";
    private static final String _EXPECTED_L = "Expected '<'";
    private static final String _EXPECTED_Q = "Expected '\"'";
    private static final String _EXPECTED_QL = "Expected '\"' or '<'";
    private static final String _EXPECTED_ST = "Expected ' ' or TAB";
    private static final String _NON_ASCII_CHAR = "Non-ASCII character";
    private static final String _UNESCAPED_BACKSLASH = "Unescaped backslash";
    private static final String _ILLEGAL_ESCAPE = "Illegal Unicode escape sequence";
    private static final String _INCOMPLETE_ESCAPE = "Incomplete Unicode escape sequence";

    /**
     * @see <a href="http://www.w3.org/TR/rdf-testcases/#triple">
     *        N-Triples EBNF triple production</a>
     */
    public static Triple parseTriple(String triple) 
            throws ParseException {

        int i = firstWhitespacePos(triple);
        if (i == -1) {
            throw new ParseException(_EXPECTED_ST, triple.length());
        }

        return null;
    }

    private static int firstWhitespacePos(String s) {
        int spacePos = s.indexOf(" ");
        int tabPos = s.indexOf("\t");
        if (spacePos == -1) {
            return tabPos;
        } else if (tabPos == -1) {
            return spacePos;
        } else {
            if (spacePos < tabPos) {
                return spacePos;
            } else {
                return tabPos;
            }
        }
    }

    public static Node parseNode(String s) 
            throws ParseException {

        char first = s.charAt(0);

        if (first == '"') {
            return parseLiteral(s);
        } else if (first == '<') {
            return parseURIReference(s);
        } else {
            throw new ParseException(_EXPECTED_QL, 0);
        }
    }

    public static Literal parseLiteral(String s)
            throws ParseException {

        StringReader reader = new StringReader(s);

        try {

            int first = reader.read();

            if (first != '"') {
                throw new ParseException(_EXPECTED_Q, 0);
            }

            StringBuffer escaped = new StringBuffer();
    
            int c = reader.read();
            int i = 1;

            while (c != '"') {

                if (c == -1) {
                    throw new ParseException(_EXPECTED_Q, i);
                }
                escaped.append((char) c);

                if (c == '\\') {
                    c = reader.read(); i++;
                    if (c == -1) {
                        throw new ParseException(_EXPECTED_Q, i);
                    }
                    escaped.append((char) c);
                }

                c = reader.read(); i++;
            }

            String value;
            try {
                value = unescape(escaped.toString());
            } catch (ParseException e) {
                throw new ParseException(e.getMessage(),
                                         e.getErrorOffset() + 1);
            }

            // c == '"', read next char
            c = reader.read(); i++;

            if (c == '@') {
                return new Literal(value, s.substring(i + 1));
            } else if (c == '^') {
                c = reader.read(); i++;
                if (c != '^') {
                    throw new ParseException(_EXPECTED_C, i);
                }
                try {
                    URIReference datatype = parseURIReference(
                            s.substring(i + 1));
                    return new Literal(value, datatype);
                } catch (ParseException e) {
                    throw new ParseException(e.getMessage(), 
                                             e.getErrorOffset() + i);
                }
            } else if (c == -1) {
                return new Literal(value);
            } else {
                throw new ParseException(_EXPECTED_ACE, i);
            }

        } catch (IOException e) {
            // should not happen -- we're using a StringReader
            throw new RuntimeException("Unexpected IO error", e);
        }
    }

    public static URIReference parseURIReference(String s)
            throws ParseException {

        char first = s.charAt(0);

        if (first != '<') {
            throw new ParseException(_EXPECTED_L, 0);
        }

        char last = s.charAt(s.length() - 1);
        if (last != '>') {
            throw new ParseException(_EXPECTED_G, s.length() - 1);
        }

        try {
            return new URIReference(s.substring(1, s.length() - 1));
        } catch (URISyntaxException e) {
            throw new ParseException(_EXPECTED_ABS_URI, 1);
        }
    }

    /**
     * Unescape an N-Triples-escaped string.
     *
     * <ul>
     *   <li> All input characters are validated to be 7-bit ASCII.</li>
     *   <li> Unicode escapes (&#x5C;uxxxx and &#x5C;Uxxxxxxxx) are validated 
     *        to be complete and legal, and are restored to the value indicated 
     *        by the hexadecimal argument.</li>
     *   <li> Backslash-escaped values (\t, \r, \n, \", and \\) are restored 
     *        to their original form (tab, carriage return, linefeed, quote,
     *        and backslash, respectively).</li>
     * </ul>
     */
    protected static String unescape(String s)
            throws ParseException {

        // verify ascii input
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127) {
                throw new ParseException(_NON_ASCII_CHAR, i);
            }
        }

        int backslashPos = s.indexOf('\\');

        // return early if no escapes
        if (backslashPos == -1) {
            return s;
        }

        int i = 0;
        int len = s.length();
        StringBuffer buf = new StringBuffer(len);

        // unescape all
        while (backslashPos != -1) {
            buf.append(s.substring(i, backslashPos));

            if (backslashPos + 1 >= len) {
                throw new ParseException(_UNESCAPED_BACKSLASH, i);
            }

            char c = s.charAt(backslashPos + 1);

            if (c == 't') {
                buf.append('\t');
                i = backslashPos + 2;
            } else if (c == 'r') {
                buf.append('\r');
                i = backslashPos + 2;
            } else if (c == 'n') {
                buf.append('\n');
                i = backslashPos + 2;
            } else if (c == '"') {
                buf.append('"');
                i = backslashPos + 2;
            } else if (c == '\\') {
                buf.append('\\');
                i = backslashPos + 2;
            } else if (c == 'u') {
                if (backslashPos + 5 >= len) {
                    throw new ParseException(_INCOMPLETE_ESCAPE, i);
                }
                String xx = s.substring(backslashPos + 2, backslashPos + 6);
                try {
                    c = (char)Integer.parseInt(xx, 16);
                    buf.append( (char)c );
                    i = backslashPos + 6;
                } catch (NumberFormatException e) {
                    throw new ParseException(_ILLEGAL_ESCAPE, i);
                }
            } else if (c == 'U') {
                if (backslashPos + 9 >= len) {
                    throw new ParseException(_INCOMPLETE_ESCAPE, i);
                }
                String xx = s.substring(backslashPos + 2, backslashPos + 10);
                try {
                    c = (char)Integer.parseInt(xx, 16);
                    buf.append( (char)c );
                    i = backslashPos + 10;
                } catch (NumberFormatException e) {
                    throw new ParseException(_ILLEGAL_ESCAPE, i);
                }
            } else {
                throw new ParseException(_UNESCAPED_BACKSLASH, i);
            }

            backslashPos = s.indexOf('\\', i);
        }
        buf.append(s.substring(i));

        return buf.toString();
    }

    /**
     * Escape a string to N-Triples literal format.
     *
     * <ul>
     *   <li> Unicode escaping (&#x5C;uxxxx or &#x5C;Uxxxxxxxx, as
     *        appropriate) will be used for all characters in the 
     *        following ranges:
     *        <ul>
     *          <li> 0x0 through 0x8</li>
     *          <li> 0xB through 0xC</li>
     *          <li> 0xE through 0x1F</li>
     *          <li> 0x7F through 0xFFFF</li>
     *          <li> 0x10000 through 0x10FFFF</li>
     *        </ul>
     *   <li> Backslash escaping will be used for double quote (\"), 
     *        backslash (\\), line feed (\n), carriage return (\r), 
     *        and tab (\t) characters.</li>
     *   <li> All other characters will be represented as-is.</li>
     * </ul>
     */ 
    protected static String escape(String s) {

        int len = s.length();
        StringBuffer out = new StringBuffer(len * 2);

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            int cNum = (int)c;
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '"') {
                out.append("\\\"");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (
                    cNum >= 0x0 && cNum <= 0x8 ||
                    cNum == 0xB || cNum == 0xC ||
                    cNum >= 0xE && cNum <= 0x1F ||
                    cNum >= 0x7F && cNum <= 0xFFFF) {
                out.append("\\u");
                out.append(hexString(cNum, 4));
            } else if (cNum >= 0x10000 && cNum <= 0x10FFFF) {
                out.append("\\U");
                out.append(hexString(cNum, 8));
            } else {
                out.append(c);
            }
        }

        return out.toString();
    }

    /**
     * Get an uppercase hex string of the specified length, 
     * representing the given number.
     */
    private static String hexString(int num, int len) {
        StringBuffer out = new StringBuffer(len);
        String hex = Integer.toHexString(num).toUpperCase();
        int n = len - hex.length();
        for (int i = 0; i < n; i++) {
            out.append('0');
        }
        out.append(hex);
        return out.toString();
    }

}
