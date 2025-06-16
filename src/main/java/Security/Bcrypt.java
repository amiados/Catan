package Security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class Bcrypt {

    private static final String BCRYPT_BASE64 =
            "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int SALT_LENGTH = 16;
    private static final String BCRYPT_MAGIC = "OrpheanBeholderScryDoubt";
    private static final SecureRandom RNG = new SecureRandom();

    /** מחזיר מערך 16 בייט של salt אקראי */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RNG.nextBytes(salt);
        return salt;
    }


    public static Blowfish_ECB eks(int cost, byte[] salt16, byte[] password) {
        Blowfish_ECB ecb = new Blowfish_ECB();

        // initial expand with salt and password
        ecb.keyExpansion(salt16, password);

        int rounds = 1 << cost;
        byte[] zerpSalt = new byte[16];
        for (int i = 0; i < rounds; i++) {
            ecb.keyExpansion(zerpSalt, password);
            ecb.keyExpansion(zerpSalt, salt16);
        }
        return ecb;
    }

    /**
     * Perform bcrypt crypt on the magic string, returning 24-byte hash.
     */
    public static byte[] bcryptCrypt(Blowfish_ECB state) {
        byte[] input = BCRYPT_MAGIC.getBytes(StandardCharsets.US_ASCII);
        int pad = 8 - (input.length % 8);
        byte[] buf = Arrays.copyOf(input, input.length + pad);
        Arrays.fill(buf, input.length, buf.length, (byte)pad);

        for (int i = 0; i < 64; i++) {
            for (int offset = 0; offset < buf.length; offset += 8) {
                int L = state.bytesToInt(buf, offset);
                int R = state.bytesToInt(buf, offset + 4);
                int[] out = state.encryptBlock(L, R);
                state.intToBytes(out[0], buf, offset);
                state.intToBytes(out[1], buf, offset + 4);
            }
        }
        return Arrays.copyOf(buf, 23);
    }


    public static String encodeBase64(byte[] d, int length) {
        StringBuilder rs = new StringBuilder();
        int off = 0;
        int c1, c2;
        while (off < d.length) {
            c1 = d[off++] & 0xff;
            rs.append(BCRYPT_BASE64.charAt((c1 >> 2) & 0x3f));
            c1 = (c1 & 0x03) << 4;
            if (off >= d.length) {
                rs.append(BCRYPT_BASE64.charAt(c1 & 0x3f));
                break;
            }

            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(BCRYPT_BASE64.charAt(c1 & 0x3f));
            c1 = (c2 & 0x0f) << 2;
            if (off >= d.length) {
                rs.append(BCRYPT_BASE64.charAt(c1 & 0x3f));
                break;
            }

            c2 = d[off++] & 0xff;
            c1 |= (c2 >> 6) & 0x03;
            rs.append(BCRYPT_BASE64.charAt(c1 & 0x3f));
            rs.append(BCRYPT_BASE64.charAt(c2 & 0x3f));
        }
        return rs.substring(0, length);
    }

    public static byte[] decodeBase64(String s, int maxLength) {
        byte[] result = new byte[maxLength];
        int off = 0, slen = s.length(), rsOff = 0;
        int c1, c2, c3, c4;
        int[] charIdx = new int[128];
        for (int i = 0; i < BCRYPT_BASE64.length(); i++) {
            charIdx[BCRYPT_BASE64.charAt(i)] = i;
        }
        while (off < slen - 1 && rsOff < maxLength) {
            c1 = charIdx[s.charAt(off++)];
            c2 = charIdx[s.charAt(off++)];
            result[rsOff++] = (byte) ((c1 << 2) | ((c2 & 0x30) >> 4));
            if (rsOff >= maxLength || off >= slen) break;

            c3 = charIdx[s.charAt(off++)];
            result[rsOff++] = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            if (rsOff >= maxLength || off >= slen) break;

            c4 = charIdx[s.charAt(off++)];
            result[rsOff++] = (byte) (((c3 & 0x03) << 6) | c4);
        }
        return result;
    }

    public static void main(String[] args) {
        // הסיסמה הקבועה שאנחנו בודקים
        String password = "MySecretP@ssw0rd";

        // 1) יצירת salt
        byte[] salt = Bcrypt.generateSalt();

        // 2) EksBlowfish setup + crypt
        Blowfish_ECB state = Bcrypt.eks(12, salt, password.getBytes(StandardCharsets.UTF_8));
        byte[] hashBytes = Bcrypt.bcryptCrypt(state);

        // 3) בניית המחרוזת המלאה בפורמט bcrypt
        String b64Salt = encodeBase64(salt, 22);
        String b64Hash = encodeBase64(hashBytes, 31);
        String bcryptString = String.format("$2a$%02d$%s%s", 12, b64Salt, b64Hash);

        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + bcryptString);
        System.out.println();

        // 4) בדיקה של אימות עבור password נכון ו־לא נכון
        testVerify(bcryptString, password);
        testVerify(bcryptString, "WrongPassword");
    }

    private static void testVerify(String bcryptString, String attempt) {
        // פריסת cost, salt, hash מתוך המחרוזת
        int cost = Integer.parseInt(bcryptString.substring(4,6));
        String rest = bcryptString.substring(7);
        String saltPart = rest.substring(0,22);
        String hashPart = rest.substring(22);

        byte[] parsedSalt = Bcrypt.decodeBase64(saltPart, 16);
        byte[] targetHash = Bcrypt.decodeBase64(hashPart, 23);

        // הרצת bcrypt שוב על הניסיון
        Blowfish_ECB st = Bcrypt.eks(cost, parsedSalt, attempt.getBytes(StandardCharsets.UTF_8));
        byte[] attemptHash = Bcrypt.bcryptCrypt(st);

        boolean matches = Arrays.equals(targetHash, attemptHash);
        System.out.printf("Verify attempt \"%s\": %s%n",
                attempt,
                matches ? "MATCH" : "NO MATCH");
    }

}
